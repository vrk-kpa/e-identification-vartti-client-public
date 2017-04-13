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
package fi.vm.kapa.identification.vartticlient.resources;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import fi.vm.kapa.identification.vartticlient.model.VarttiResponse;
import fi.vm.kapa.identification.vartticlient.services.VarttiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/")
public class VarttiResource {

    private final VarttiService service;
    private static final Logger logger = LoggerFactory.getLogger(VarttiResource.class);

    @Autowired
    public VarttiResource(VarttiService service) {
        this.service = service;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/person/{identifier}/{certSerial}")
    public Response getMessage(@PathParam("identifier") String identifier,
                               @PathParam("certSerial") String certSerial,
                               @NotNull @QueryParam("issuerCN") String issuerCN) {
        try {
            VarttiResponse varttiResponse = service.getVarttiResponse(identifier, certSerial, issuerCN);
            if ( varttiResponse.isSuccess() && varttiResponse.getError() == null ) {
                return Response.ok().entity(varttiResponse).build();
            } else {
                return Response.serverError().entity(varttiResponse).build();
            }
        } catch (Exception e) {
            logger.error("VarttiResource got exception: "+e.getMessage(), e);
            ResponseBuilder responseBuilder = Response.serverError();
            return responseBuilder.build();
        }

    }
}