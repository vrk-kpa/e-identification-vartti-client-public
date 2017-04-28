package fi.vm.kapa.identification.vartticlient.services;

import fi.vm.kapa.identification.vartticlient.VarttiSigner;
import fi.vm.kapa.identification.vartticlient.exception.VarttiServiceException;
import fi.vm.kapa.identification.vartticlient.model.VarttiExtRequest;
import fi.vm.kapa.identification.vartticlient.model.VarttiExtResponse;
import fi.vm.kapa.identification.vartticlient.model.VarttiResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.client.Client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest

@TestPropertySource(locations = "classpath:test.properties", properties = {"vartti.client.dummydata=false"})
public class RealVarttiServiceFullTests {


    private static final String TEST_HETU = "111111-9999";
    private static final String FAKE_SIGNATURE = "SIGNATURE";

    @Mock
    private VarttiSigner varttiSigner;
    @Mock
    private Client client;
    @Autowired
    private RealVarttiService varttiService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        varttiService = spy(new RealVarttiService(mock(Client.class), mock(VarttiSigner.class)));

        doReturn(FAKE_SIGNATURE).
                when(varttiSigner).generateSignature(any());
        doReturn(getSuccessVarttiExtResponse(TEST_HETU))
                .when(varttiService).getVarttiExtResponse(argThat(new IsSuccessVarttiExtRequest()));
        doThrow(new VarttiServiceException("Vartti connection error: 404"))
                .when(varttiService).getVarttiExtResponse(AdditionalMatchers.not(argThat(new IsSuccessVarttiExtRequest())));
    }

    // Get VarttiResponse with success
    @Test
    public void getSuccessResponseFromVartti() throws Exception
    {
        VarttiResponse response = varttiService.getVarttiResponse("EXISTING","123456","CN");
        assertEquals(TEST_HETU, response.getVarttiPerson().getHetu());
    }

    // Get VarttiResponse with failure
    @Test
    public void getNotFoundResponseFromVartti() throws Exception
    {
        VarttiResponse response = varttiService.getVarttiResponse("NONEXISTING","123456","CN");
        assertFalse(response.isSuccess());
    }

    // Vartti-client must return failure if Vartti returns OK + empty hetu (theoretical case)
    @Test
    public void getEmptyHetuFromVartti() throws Exception
    {
        doReturn(getSuccessVarttiExtResponse("")).when(varttiService).getVarttiExtResponse(any());
        VarttiResponse response = varttiService.getVarttiResponse("NONEXISTING","123456","CN");
        assertFalse(response.isSuccess());
    }

    private VarttiExtResponse getSuccessVarttiExtResponse(String hetu) throws VarttiServiceException
    {
        VarttiExtResponse extResponse = new VarttiExtResponse();
        extResponse.setHetu(hetu);
        extResponse.setHttpStatusCode(200);
        return extResponse;
    }

}

class IsSuccessVarttiExtRequest extends ArgumentMatcher<VarttiExtRequest> {

    public boolean matches(Object req)
    {
        return !((VarttiExtRequest) req).getSearchTerm().equals("NONEXISTING");
    }
}
