/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.helger.as4server.supplementary.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.commons.io.resource.ClassPathResource;

/**
 * A set of test-cases for signing and verifying SOAP requests.
 */
public class SignatureTest
{
  private final WSSecurityEngine secEngine = new WSSecurityEngine ();
  private final Crypto crypto;

  public SignatureTest () throws Exception
  {
    crypto = AS4CryptoFactory.createCrypto ();
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
    final WSSecSignature builder = new WSSecSignature ();
    builder.setUserInfo (AS4CryptoFactory.getKeyAlias (), AS4CryptoFactory.getKeyPassword ());
    builder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    builder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    builder.setDigestAlgo ("http://www.w3.org/2001/04/xmlenc#sha256");
    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();
    final Document signedDoc = builder.build (doc, crypto, secHeader);

    final String outputString = XMLUtils.prettyDocumentToString (signedDoc);

    final WSHandlerResult results = verify (signedDoc);

    final WSSecurityEngineResult actionResult = results.getActionResults ()
                                                       .get (Integer.valueOf (WSConstants.SIGN))
                                                       .get (0);
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE));
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    final REFERENCE_TYPE referenceType = (REFERENCE_TYPE) actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
    assertTrue (referenceType == REFERENCE_TYPE.DIRECT_REF);
  }

  /**
   * Verifies the soap envelope. This method verifies all the signature
   * generated.
   *
   * @param env
   *        soap envelope
   * @throws java.lang.Exception
   *         Thrown when there is a problem in verification
   */
  private WSHandlerResult verify (final Document doc) throws Exception
  {
    return secEngine.processSecurityHeader (doc, null, null, crypto);
  }

  private Document _getSoapEnvelope11 () throws SAXException, IOException, ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource ("UserMessageWithoutWSSE.xml").getInputStream ());
  }
}
