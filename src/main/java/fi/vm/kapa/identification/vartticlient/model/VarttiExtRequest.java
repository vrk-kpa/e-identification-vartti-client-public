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
package fi.vm.kapa.identification.vartticlient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VarttiExtRequest {

    @JsonProperty("Certificate")
    private VarttiExtCertificate certificate;
    @JsonProperty("SearchTerm")
    private String searchTerm;
    @JsonProperty("UserContext")
    private VarttiExtUserContext userContext;


    // Getters and setters

    public VarttiExtCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(VarttiExtCertificate certificate) {
        this.certificate = certificate;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public VarttiExtUserContext getUserContext() {
        return userContext;
    }

    public void setUserContext(VarttiExtUserContext userContext) {
        this.userContext = userContext;
    }

    public String toString()
    {
        return String.format("{searchTerm=%s, certificate=%s, userContext=%s}",
                searchTerm,
                certificate.toString(),
                userContext.toString());
    }
}
