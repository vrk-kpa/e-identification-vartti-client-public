/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.identification.vartticlient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties", properties = {"vartti.client.dummydata=false"})
public class VarttiSignerTest {

    private static final String TEST_SIGN_DATA = "Suomi.fi;00098705718;HetuHaku;20161111015354Z;";
    private static final String EXPECTED_SIGNATURE = "DUKKHYPTxdixgNapTTczPr5RoJlDqRyLdVp4YuQEUN+lUlf5o/gPsn6LlrY9VYcBIiOo1sD9am9tFpMmkxbPxu2DJvPUWB/8ul/eBsYa5aydcsRKrku9f3KjrIO5+DOw5Q6C0p9rnjOBkEPXCeDFr6exUdpXpCTHTpgQuJLdib6smYcRf7ThPeIdcHLEWV1Me/ocZh19I3l2iJPb4uM43y8MGLDk6ix3HQjgA/BUyDM30DevhQtWSXRmTvET5uaBdO+OMsWBUpnZsYnXRKJV47H2FgqiC51QYGUuuSOdIoWJVIlytCytWKtpq+8aPNrMhqkTpBit+eh6q86ZAIlDnA==";

    @Value("${vartti.keystore}")
    private String varttiKeystoreFilename;
    @Value("${vartti.keystore.alias}")
    private String varttiKeystoreAlias;
    @Value("${vartti.keystore.password}")
    private String varttiKeystorePass;
    @Value("${vartti.keystore.keypassword}")
    private String varttiKeystoreKeyPass;

    @Autowired
    private VarttiSigner varttiSigner;
    private KeyStore varttiTestKeystore;

    @Before
    public void setUp() throws Exception {
        // Initialize test keystore for signature testing
        File file = new File(varttiKeystoreFilename);
        FileInputStream is = new FileInputStream(file);
        varttiTestKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        varttiTestKeystore.load(is, varttiKeystorePass.toCharArray());
        varttiTestKeystore.getKey(varttiKeystoreAlias, varttiKeystoreKeyPass.toCharArray());
    }

    // Test signature generation
    @Test
    public void testSignatureGeneration() throws Exception
    {
        String signature = varttiSigner.generateSignature(TEST_SIGN_DATA);
        assertEquals(EXPECTED_SIGNATURE, signature);
    }


}