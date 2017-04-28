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

import fi.vm.kapa.identification.vartticlient.*;
import fi.vm.kapa.identification.vartticlient.exception.*;
import fi.vm.kapa.identification.vartticlient.model.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    private Client client;
    private VarttiSigner varttiSigner;

    private static final Logger logger = LoggerFactory.getLogger(RealVarttiService.class);

    public RealVarttiService(Client client, VarttiSigner varttiSigner) {
        this.client = client;
        this.varttiSigner = varttiSigner;
    }

    @Override
    public VarttiResponse getVarttiResponse(String identifier, String certSerial, String issuerCN)
    {
        VarttiResponse varttiResponse = new VarttiResponse();
        varttiResponse.setSuccess(false); // success=false by default

        VarttiExtRequest varttiExtRequest;

        // Phase 1: Initialize Vartti request
        try
        {
            varttiExtRequest = generateVarttiExtRequest(identifier, certSerial, issuerCN);
        }
        catch (VarttiInitException e)
        {
            // Initialization should never fail if configuration is good (always log these on ERROR level)
            logger.error("Vartti initialization error: {}", e.getMessage(), e);
            varttiResponse.setError(e.getMessage());
            return varttiResponse;
        }

        // Phase 2: Perform actual request to Vartti API
        try
        {
            VarttiExtResponse varttiExtResponse = getVarttiExtResponse(varttiExtRequest); // Vartti API
            int statusCode = varttiExtResponse.getHttpStatusCode();

            // VarttiExtRequest was received with successful status
            if (statusCode == HttpStatus.OK.value())
            {
                String hetu = varttiExtResponse.getHetu();
                if (StringUtils.isBlank(hetu)) {
                    throw new VarttiServiceException("Vartti returned OK status with empty hetu.");
                }
                varttiResponse.setSuccess(true);
                VarttiPerson varttiPerson = new VarttiPerson();
                varttiPerson.setHetu(hetu);
                varttiResponse.setVarttiPerson(varttiPerson);
            }
            // VarttiExtRequest was received with error code
            else
            {
                String errorMessage = statusCode + " " + varttiExtResponse.getErrorCode() + ": " + varttiExtResponse.getErrorMessage();
                varttiResponse.setError(errorMessage);
                if (!(statusCode == HttpStatus.BAD_REQUEST.value() || statusCode == HttpStatus.NOT_FOUND.value()))
                {
                    // Vartti seems to return 400 (Bad Request) with errorCodes:
                    //   VarmennetiedotServiceError.HetuNotFound
                    //   VarmennetiedotServiceError.CertificateIssuerCNInvalid
                    // According to examples in Vartti specification, it SHOULD return 404 in CertificateIssuerCNInvalid case.
                    // Also, status code 404 would be a good fit for HetuNotFound case. But currently it seems to be 400.
                    throw new VarttiServiceException("Vartti returned unexpected error: "+errorMessage);
                }
            }
        }
        catch (VarttiConnectionException |VarttiParsingException e)
        {
            logger.warn(e.getMessage());
            varttiResponse.setError(e.getMessage());
        }
        catch (VarttiServiceException e)
        {
            logger.error(e.getMessage());
            varttiResponse.setError(e.getMessage());
        }
        return varttiResponse;
    }

    protected VarttiExtResponse getVarttiExtResponse(VarttiExtRequest req)
            throws VarttiServiceException, VarttiConnectionException, VarttiParsingException
    {
        Response response;
        VarttiExtResponse extResponse;
        try {
            WebTarget webTarget = client.target(varttiEndpoint);
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            response = invocationBuilder.post(Entity.entity(req,MediaType.APPLICATION_JSON));
        }
        catch (Exception e) {
            throw new VarttiConnectionException("Vartti connection failed: "+e.getMessage());
        }
        try {
            extResponse = response.readEntity(VarttiExtResponse.class);
        }
        catch (Exception e)
        {
            throw new VarttiParsingException("Vartti response parsing failed: "+e.getMessage());
        }
        return extResponse;
    }

    protected VarttiExtRequest generateVarttiExtRequest(String identifier, String certSerial, String issuerCN) throws VarttiInitException
    {
        VarttiExtRequest varttiRequest = new VarttiExtRequest();
        VarttiExtUserContext varttiContext = new VarttiExtUserContext();
        VarttiExtCertificate varttiCertificate = new VarttiExtCertificate();
        varttiRequest.setSearchTerm(identifier);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX").withZone(ZoneId.of("UTC")); // 20161111015354Z
        // Generate expiration timestamp
        ZonedDateTime dateTime = ZonedDateTime.now().plusMinutes(varttiSignatureValidityTime); // Set ExpiresAt to be 30 minutes from now
        String dateTimeString = dateTime.format(dateTimeFormatter);

        // Generate UserContext signature value
        StringJoiner joiner = new StringJoiner(";","",";"); // delimiter, prefix, suffix
        // Data to sign; Example from specification: Suomi.fi;00098705718;HetuHaku;20161111015354Z;
        joiner.add(varttiApplicationName).add(identifier).add(varttiRoleName).add(dateTimeString);
        String signData = joiner.toString();
        logger.info("Data to sign: "+signData);
        String signature = varttiSigner.generateSignature(signData);

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

}