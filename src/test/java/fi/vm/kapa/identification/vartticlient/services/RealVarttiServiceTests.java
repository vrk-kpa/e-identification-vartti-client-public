package fi.vm.kapa.identification.vartticlient.services;

import fi.vm.kapa.identification.vartticlient.VarttiSigner;
import fi.vm.kapa.identification.vartticlient.model.VarttiExtRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.client.Client;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@SpringBootTest

@TestPropertySource(locations = "classpath:test.properties", properties = {"vartti.client.dummydata=false"})
public class RealVarttiServiceTests {

    private RealVarttiService varttiService;

    @Value("${vartti.application.name}")
    private String varttiApplicationName;
    @Value("${vartti.role.name}")
    private String varttiRoleName;
    @Value("${vartti.signature.validity.mins}")
    private int varttiSignatureValidityTime;

    private static final String TEST_IDENTIFIER = "00098705718";
    private static final String TEST_CERTSERIAL = "09134911344";
    private static final String TEST_ISSUERCN = "VRK CA for Test Purposes - G2";

    private static final Logger logger = LoggerFactory.getLogger(RealVarttiServiceTests.class);

    @Before
    public void setUp() throws Exception {
        varttiService = spy(new RealVarttiService(mock(Client.class), mock(VarttiSigner.class)));

    }

    // Test VarttiExtRequest generation, check that all parameters are as expected
    // (Except for signature, which is tested separately)
    @Test
    public void generateVarttiExtRequestAndVerifyRequestFields() throws Exception
    {

        ReflectionTestUtils.setField(varttiService, "varttiApplicationName", varttiApplicationName);
        ReflectionTestUtils.setField(varttiService, "varttiRoleName", varttiRoleName);

        VarttiExtRequest varttiExtRequest = varttiService.generateVarttiExtRequest(TEST_IDENTIFIER, TEST_CERTSERIAL, TEST_ISSUERCN);
        assertEquals(varttiExtRequest.getSearchTerm(), TEST_IDENTIFIER);
        assertEquals(varttiExtRequest.getUserContext().getAuthenticatedUser(), TEST_IDENTIFIER);
        assertEquals(varttiExtRequest.getUserContext().getApplicationName(), varttiApplicationName);
        assertEquals(varttiExtRequest.getUserContext().getRoleName(), varttiRoleName);
        assertEquals(varttiExtRequest.getCertificate().getSerialNumber(), TEST_CERTSERIAL);
        assertEquals(varttiExtRequest.getCertificate().getIssuerCN(), TEST_ISSUERCN);

        // Timestamp (expiration) validation
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX").withZone(ZoneId.of("UTC")); // 20161111015354Z
        ZonedDateTime dateTimeNow = ZonedDateTime.now().minusSeconds(1); // Now -1sec because dateTimeRequest time is in seconds precision, others in ms
        ZonedDateTime dateTimeRequest = ZonedDateTime.parse(varttiExtRequest.getUserContext().getExpiresAt(), dateTimeFormatter);
        ZonedDateTime dateTimeExpired = ZonedDateTime.now().plusMinutes(varttiSignatureValidityTime).plusMinutes(1);
        assertTrue(dateTimeRequest.isAfter(dateTimeNow));      // Check that expiresAt > now
        assertTrue(dateTimeRequest.isBefore(dateTimeExpired)); // Check that expiresAt < expired
    }
}
