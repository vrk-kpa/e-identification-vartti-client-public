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

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@Configuration
public class HttpClientConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientConfiguration.class);

    @Value("${vartti.keystore}")
    private String varttiKeystoreFilename;
    @Value("${vartti.keystore.password}")
    private String varttiKeystorePass;
    @Value("${vartti.truststore}")
    private String varttiTruststoreFilename;
    @Value("${vartti.truststore.password}")
    private String varttiTruststorePass;

    @Value("${vartti.api.logging}")
    private boolean varttiAPILogging;
    @Value("${vartti.connection.timeout}")
    private int varttiConnectionTimeout;
    @Value("${vartti.read.timeout}")
    private int varttiReadTimeout;
    @Value("${vartti.proxy.uri}")
    private String varttiProxyURI;


    // Generate new client with Jackson as JSON implementation
    //  - uri as parameter to decide whether SSL is needed for connection
    @Bean(name = "httpsClient")
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Client getHttpsClient()
    {
        ClientConfig clientConfig = getClientConfig();
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);
        logger.info("Creating SSL context");
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(varttiTruststoreFilename)
                .trustStorePassword(varttiTruststorePass)
                .keyStoreFile(varttiKeystoreFilename)
                .keyStorePassword(varttiKeystorePass);
        SSLContext ssl = sslConfig.createSSLContext();
        clientBuilder.sslContext(ssl);
        Client client = clientBuilder.build();
        client.register(JacksonFeature.class);
        client.register(LoggingFeature.class);
        return client;
    }

    @Bean(name = "httpClient")
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Client getHttpClient()
    {
        ClientConfig clientConfig = getClientConfig();
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);
        Client client = clientBuilder.build();
        client.register(JacksonFeature.class);
        client.register(LoggingFeature.class);
        return client;
    }

    private ClientConfig getClientConfig() {
        ClientConfig clientConfig = new ClientConfig()
                .connectorProvider(new ApacheConnectorProvider())
                .property(ClientProperties.CONNECT_TIMEOUT, varttiConnectionTimeout)
                .property(ClientProperties.READ_TIMEOUT, varttiReadTimeout);

        // If proxy is defined, configure it into use
        if (StringUtils.isNotBlank(varttiProxyURI))
        {
            logger.info("Using proxy server: "+varttiProxyURI);
            clientConfig = clientConfig.property(ClientProperties.PROXY_URI, varttiProxyURI);
        }
        if (varttiAPILogging)
        {
            // Note: .property returns updated configuration but it is not guaranteed to be the same object
            clientConfig = clientConfig.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
            clientConfig = clientConfig.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, "INFO"); // java.util.logging.Level
        }
        return clientConfig;
    }

}
