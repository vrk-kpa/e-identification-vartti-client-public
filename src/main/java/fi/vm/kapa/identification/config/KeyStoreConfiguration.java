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
package fi.vm.kapa.identification.config;

import fi.vm.kapa.identification.vartticlient.exception.VarttiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeyStoreConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(KeyStoreConfiguration.class);

    @Value("${vartti.keystore}")
    private String varttiKeystoreFilename;
    @Value("${vartti.keystore.alias}")
    private String varttiKeystoreAlias;
    @Value("${vartti.keystore.password}")
    private String varttiKeystorePass;
    @Value("${vartti.keystore.keypassword}")
    private String varttiKeystoreKeyPass;
    @Value("${vartti.truststore}")
    private String varttiTruststoreFilename;
    @Value("${vartti.truststore.password}")
    private String varttiTruststorePass;

    @Bean
    KeyStore provideKeystore() throws VarttiServiceException
    {
        KeyStore varttiKeystore;

        try (FileInputStream is = new FileInputStream(varttiKeystoreFilename)) { // try-with-resources; AutoCloseable
            varttiKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            varttiKeystore.load(is, varttiKeystorePass.toCharArray());
        }
        catch (IOException e)
        {
            logger.error("Unable to access Vartti keystore: "+e.getMessage(), e);
            throw new VarttiServiceException("Unable to access Vartti keystore: "+e.getMessage());
        }
        catch (KeyStoreException |NoSuchAlgorithmException |CertificateException e)
        {
            logger.error("Error creating KeyStore instance: "+e.getMessage(), e);
            throw new VarttiServiceException("Error creating KeyStore instance: "+e.getMessage());
        }
        return varttiKeystore;
    }

}
