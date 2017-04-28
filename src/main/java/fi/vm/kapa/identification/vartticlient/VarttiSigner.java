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

import fi.vm.kapa.identification.vartticlient.exception.VarttiInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Base64;

public class VarttiSigner {
    private static final Logger logger = LoggerFactory.getLogger(VarttiSigner.class);

    private final Key key;

    public VarttiSigner(Key key) {
        this.key = key;
    }

    public String generateSignature(String signData) throws VarttiInitException
    {
        try {
            Signature sig = Signature.getInstance("SHA1WithRSA");
            sig.initSign((PrivateKey)key);
            sig.update(signData.getBytes("UTF-16LE")); // In C# (Vartti impl.) Encoding.Unicode
            return Base64.getEncoder().encodeToString(sig.sign());
        }
        catch (NoSuchAlgorithmException|InvalidKeyException|
                UnsupportedEncodingException |SignatureException|NullPointerException e)
        {
            logger.error("Unable to generate signature: "+e.getMessage(), e);
            throw new VarttiInitException("Unable to generate signature: "+e.getMessage());
        }
    }

}
