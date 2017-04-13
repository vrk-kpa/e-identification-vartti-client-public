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
package fi.vm.kapa.identification.vartticlient.services;

import fi.vm.kapa.identification.vartticlient.exception.*;
import fi.vm.kapa.identification.vartticlient.model.*;
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
import org.springframework.http.HttpStatus;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.StringJoiner;

public class RealVarttiService implements VarttiService {

    @Value("${vartti.url}")
    private String varttiEndpoint;
    @Value("${vartti.application.name}")
    private String varttiApplicationName;
    @Value("${vartti.role.name}")
    private String varttiRoleName;
    @Value("${vartti.signature.validity.mins}")
    private int varttiSignatureValidityTime;
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
    @Value("${vartti.api.logging}")
    private boolean varttiAPILogging;
    @Value("${vartti.connection.timeout}")
    private int varttiConnectionTimeout;
    @Value("${vartti.read.timeout}")
    private int varttiReadTimeout;
    @Value("${vartti.proxy.uri}")
    private String varttiProxyURI;

    private KeyStore varttiKeystore;

    private static final Logger logger = LoggerFactory.getLogger(RealVarttiService.class);

    @Override
    public VarttiResponse getVarttiResponse(String identifier, String certSerial, String issuerCN)
    {
        VarttiResponse varttiResponse = new VarttiResponse();
        try {
            initializeVarttiKeystore();
            VarttiExtRequest varttiExtRequest = generateVarttiExtRequest(identifier, certSerial, issuerCN, varttiKeystore);
            VarttiExtResponse varttiExtResponse = getVarttiExtResponse(varttiExtRequest); // Vartti API
            varttiResponse.setSuccess(true);
            VarttiPerson varttiPerson = new VarttiPerson();
            varttiPerson.setHetu(varttiExtResponse.getHetu());
            varttiResponse.setVarttiPerson(varttiPerson);
        }
        catch (Exception e)
        {
            logger.error("Vartti connection error: {}", e.getMessage(), e);
            varttiResponse.setSuccess(false);
            varttiResponse.setError(e.getMessage());
        }
        return varttiResponse;
    }

    private VarttiExtResponse getVarttiExtResponse(VarttiExtRequest req) throws VarttiServiceException
    {
        Response response = getVarttiExtHttpResponse(req);
        if (response.getStatus() == HttpStatus.OK.value()) {
            return response.readEntity(VarttiExtResponse.class);
        } else {
            logger.error("Vartti connection error: " + response.getStatus());
            throw new VarttiServiceException("Vartti connection error: " + response.getStatus());
        }
    }

    private Response getVarttiExtHttpResponse(VarttiExtRequest req) throws VarttiServiceException
    {
        Response response;
        try {
            WebTarget webTarget = getClient(varttiEndpoint).target(varttiEndpoint);
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            response = invocationBuilder.post(Entity.entity(req,MediaType.APPLICATION_JSON));
        }
        catch (Exception e) {
            logger.error("Vartti connection failed: "+e.getMessage(), e);
            throw new VarttiServiceException("Vartti connection failed.");
        }
        return response;
    }

    // Generate new client with Jackson as JSON implementation
    //  - uri as parameter to decide whether SSL is needed for connection
    private Client getClient(String uri)
    {
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
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);
        if (uri.startsWith("https"))
        {
            logger.info("Creating SSL context for URI: "+uri);
            SslConfigurator sslConfig = SslConfigurator.newInstance()
                    .trustStoreFile(varttiTruststoreFilename)
                    .trustStorePassword(varttiTruststorePass)
                    .keyStoreFile(varttiKeystoreFilename)
                    .keyStorePassword(varttiKeystorePass);
            SSLContext ssl = sslConfig.createSSLContext();
            clientBuilder.sslContext(ssl);
        }
        Client client = clientBuilder.build();
        client.register(JacksonFeature.class);
        client.register(LoggingFeature.class);
        return client;
    }

    private VarttiExtRequest generateVarttiExtRequest(String identifier, String certSerial, String issuerCN, KeyStore signingKeyStore) throws VarttiServiceException
    {
        VarttiExtRequest varttiRequest = new VarttiExtRequest();
        VarttiExtUserContext varttiContext = new VarttiExtUserContext();
        VarttiExtCertificate varttiCertificate = new VarttiExtCertificate();
        varttiRequest.setSearchTerm(identifier);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX").withZone(ZoneId.of("UTC")); // Format v2.0-Tieto: 20161111015354Z
        // Generate expiration timestamp
        ZonedDateTime dateTime = ZonedDateTime.now().plusMinutes(varttiSignatureValidityTime); // Set ExpiresAt to be 30 minutes from now
        String dateTimeString = dateTime.format(dateTimeFormatter);

        // Generate UserContext signature value
        StringJoiner joiner = new StringJoiner(";","",";"); // delimiter, prefix, suffix
        // Data to sign; Example from specification: Suomi.fi;00098705718;HetuHaku;20161111015354Z;
        joiner.add(varttiApplicationName).add(identifier).add(varttiRoleName).add(dateTimeString);
        String signData = joiner.toString();
        logger.info("Data to sign: "+signData);
        String signature = generateSignature(signingKeyStore, varttiKeystoreAlias, varttiKeystoreKeyPass, signData);

        // Populate and set UserContext
        varttiContext.setExpiresAt(dateTimeString);
        varttiContext.setAuthenticatedUser(identifier);
        varttiContext.setApplicationName(varttiApplicationName);
        varttiContext.setRoleName(varttiRoleName);
        varttiContext.setSignature(signature);
        varttiRequest.setUserContext(varttiContext);

        // Populate and set Certificate
        varttiCertificate.setIssuerCN(issuerCN);
        varttiCertificate.setSerialNumber(certSerial);
        varttiRequest.setCertificate(varttiCertificate);

        return varttiRequest;
    }

    private String generateSignature(KeyStore ks, String alias, String keyPassword, String signData) throws VarttiServiceException
    {
        try {
            Key key = ks.getKey(alias, keyPassword.toCharArray()); // instanceof PrivateKey
            Signature sig = Signature.getInstance("SHA1WithRSA");
            sig.initSign((PrivateKey)key);
            sig.update(signData.getBytes("UTF-16LE")); // In C# (Vartti impl.) Encoding.Unicode
            return Base64.getEncoder().encodeToString(sig.sign());
        }
        catch (KeyStoreException|NoSuchAlgorithmException|UnrecoverableKeyException|InvalidKeyException|
                UnsupportedEncodingException|SignatureException|NullPointerException e)
        {
            logger.error("Unable to generate signature: "+e.getMessage(), e);
            throw new VarttiServiceException("Unable to generate signature");
        }
    }

    // Initialize Vartti keystore (needed for creating cryptographic signature)
    private void initializeVarttiKeystore() throws VarttiServiceException
    {
        if (varttiKeystore != null) {
            return; // If already initialized, no need to re-init
        }

        try (FileInputStream is = new FileInputStream(varttiKeystoreFilename)) { // try-with-resources; AutoCloseable
            varttiKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            varttiKeystore.load(is, varttiKeystorePass.toCharArray());
        }
        catch (IOException e)
        {
            logger.error("Unable to access Vartti keystore: "+e.getMessage(), e);
            throw new VarttiServiceException("Unable to access keystore");
        }
        catch (KeyStoreException|NoSuchAlgorithmException|CertificateException e)
        {
            logger.error("Error creating KeyStore instance: "+e.getMessage(), e);
            throw new VarttiServiceException("Unable to create keystore instance");
        }
    }

}