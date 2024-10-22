/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
import org.apache.wss4j.dom.str.STRParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.scope.mock.ScopeTestRule;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A set of test-cases for signing and verifying SOAP requests.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public final class SignatureTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Before
  public void before ()
  {
    // Ensure WSSConfig is initialized
    WSSConfigManager.getInstance ();
  }

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
    final IAS4CryptoFactory aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();

    final Document aDoc = _getSoapEnvelope11 ();
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecSignature aBuilder = new WSSecSignature (aSecHeader);
    aBuilder.setUserInfo (aCryptoFactory.getKeyAlias (),
                          aCryptoFactory.getKeyPasswordPerAlias (aCryptoFactory.getKeyAlias ()));
    aBuilder.setKeyIdentifierType (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE.getTypeID ());
    aBuilder.setSignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256.getAlgorithmURI ());
    // PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getAlgorithmURI ());
    final Document signedDoc = aBuilder.build (aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN));

    // final String outputString = XMLUtils.prettyDocumentToString (signedDoc);

    final WSSecurityEngine aSecEngine = new WSSecurityEngine ();
    aSecEngine.setWssConfig (WSSConfigManager.getInstance ().createWSSConfig ());
    final WSHandlerResult aResults = aSecEngine.processSecurityHeader (signedDoc,
                                                                       null,
                                                                       null,
                                                                       aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN));

    final WSSecurityEngineResult actionResult = aResults.getActionResults ()
                                                        .get (Integer.valueOf (WSConstants.SIGN))
                                                        .get (0);
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_CERTIFICATE));
    assertNotNull (actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    final STRParser.REFERENCE_TYPE referenceType = (STRParser.REFERENCE_TYPE) actionResult.get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
    assertSame (STRParser.REFERENCE_TYPE.DIRECT_REF, referenceType);
  }
}
