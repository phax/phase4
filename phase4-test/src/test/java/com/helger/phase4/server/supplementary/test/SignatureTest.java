/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.server.supplementary.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import javax.annotation.Nullable;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.crypto.AS4CryptoFactory;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A set of test-cases for signing and verifying SOAP requests.
 */
public final class SignatureTest
{
  @Nullable
  private static Document _getSoapEnvelope11 ()
  {
    return DOMReader.readXMLDOM (new ClassPathResource ("UserMessageWithoutWSSE.xml"));
  }

  /**
   * The test uses the Issuer Serial key identifier type.
   * <p/>
   *
   * @throws java.lang.Exception
   *         Thrown when there is any problem in signing or verification
   */
  @Test
  public void testX509SignatureIS () throws Exception
  {
    final AS4CryptoFactory aCryptoFactory = AS4CryptoFactory.getDefaultInstance ();

    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecSignature aBuilder = new WSSecSignature (secHeader);
    aBuilder.setUserInfo (aCryptoFactory.getCryptoProperties ().getKeyAlias (),
                          aCryptoFactory.getCryptoProperties ().getKeyPassword ());
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256.getAlgorithmURI ());
    // PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getAlgorithmURI ());
    final Document signedDoc = aBuilder.build (aCryptoFactory.getCrypto ());

    // final String outputString = XMLUtils.prettyDocumentToString (signedDoc);

    WSHandlerResult aResults;
    {
      final WSSecurityEngine aSecEngine = new WSSecurityEngine ();
      aResults = aSecEngine.processSecurityHeader (signedDoc, null, null, aCryptoFactory.getCrypto ());
    }

    final WSSecurityEngineResult actionResult = aResults.getActionResults ()
                                                        .get (Integer.valueOf (WSConstants.SIGN))
                                                        .get (0);
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE));
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    final REFERENCE_TYPE referenceType = (REFERENCE_TYPE) actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
    assertSame (referenceType, REFERENCE_TYPE.DIRECT_REF);
  }
}
