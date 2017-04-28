package fi.vm.kapa.identification.vartticlient.services;

import fi.vm.kapa.identification.vartticlient.model.VarttiResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest

@TestPropertySource(locations = "classpath:test.properties", properties = {"vartti.client.dummydata.hetu=123456-7890", "vartti.client.dummydata=true"})
public class DummyVarttiServiceTests {

    @Autowired
    VarttiService varttiService;

    private static final Logger logger = LoggerFactory.getLogger(DummyVarttiServiceTests.class);

    @Test
    public void getHetuFromDummyVarttiService() throws Exception
    {
        VarttiResponse response = varttiService.getVarttiResponse("123","234","Issuer CN");
        assertEquals("123456-7890", response.getVarttiPerson().getHetu());
    }
}
