/*
  The MIT License
  Copyright (c) 2015 Population Register Centre

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
 */
package fi.vm.kapa.identification.config;

import fi.vm.kapa.identification.vartticlient.VarttiSigner;
import fi.vm.kapa.identification.vartticlient.services.DummyVarttiService;
import fi.vm.kapa.identification.vartticlient.services.RealVarttiService;
import fi.vm.kapa.identification.vartticlient.services.VarttiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.inject.Named;
import javax.ws.rs.client.Client;

@Configuration
@Import({ HttpClientConfiguration.class, VarttiSignerConfiguration.class })
public class VarttiServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VarttiServiceConfiguration.class);

    @Value("${vartti.client.dummydata}")
    private boolean varttiDummyDataEnabled;
    @Value("${vartti.url}")
    private String varttiEndpoint;

    @Autowired
    @Named("httpsClient")
    Client httpsClient;

    @Autowired
    @Named("httpClient")
    Client httpClient;

    @Autowired
    VarttiSigner varttiSigner;

    @Bean(name = "VarttiService")
    VarttiService provideVarttiService() {
        if (varttiDummyDataEnabled) {
            logger.info("Returning new DummyVarttiService instance");
            return new DummyVarttiService();
        } else {
            logger.info("Returning new RealVarttiService instance");
            if (varttiEndpoint.startsWith("https")) {
                return new RealVarttiService(httpsClient, varttiSigner);
            } else {
                return new RealVarttiService(httpClient, varttiSigner);
            }
        }
    }
}
