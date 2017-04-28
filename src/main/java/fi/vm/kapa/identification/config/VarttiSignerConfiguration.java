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

import fi.vm.kapa.identification.vartticlient.VarttiSigner;
import fi.vm.kapa.identification.vartticlient.exception.VarttiServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.security.*;

@Configuration
@Import(KeyStoreConfiguration.class)
public class VarttiSignerConfiguration {

    @Value("${vartti.keystore.alias}")
    private String varttiKeystoreAlias;
    @Value("${vartti.keystore.keypassword}")
    private String varttiKeystoreKeyPass;

    @Autowired
    KeyStore varttiKeyStore;

    @Bean
    VarttiSigner providerVarttiSigner() throws VarttiServiceException {
        try {
            Key key = varttiKeyStore.getKey(varttiKeystoreAlias, varttiKeystoreKeyPass.toCharArray());
            return new VarttiSigner(key);
        } catch (KeyStoreException e) {
            throw new VarttiServiceException("KeyStore problem: ", e);
        } catch (NoSuchAlgorithmException e) {
            throw new VarttiServiceException("KeyStore algorithm problem: ", e);
        } catch (UnrecoverableKeyException e) {
            throw new VarttiServiceException("KeyStore key problem: ", e);
        }
    }

}
