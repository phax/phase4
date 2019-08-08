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

import static org.junit.Assert.assertFalse;

import javax.annotation.Nullable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.crypto.AS4CryptoFactory;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A set of test-cases for encrypting and decrypting SOAP requests.
 */
public final class EncryptionTest
{
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger (EncryptionTest.class);

  public EncryptionTest ()
  {}

  @Nullable
  private static Document _getSoapEnvelope11 ()
  {
    return DOMReader.readXMLDOM (new ClassPathResource ("UserMessageWithoutWSSE.xml"));
  }

  /**
   * Test that encrypt and decrypt a WS-Security envelope. This test uses the
   * RSA_15 algorithm to transport (wrap) the symmetric key.
   * <p/>
   *
   * @throws Exception
   *         Thrown when there is any problem in signing or verification
   */
  @Test
  public void testEncryptionDecryptionAES128GCM () throws Exception
  {
    final AS4CryptoFactory aCryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;

    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (secHeader);
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (aCryptoFactory.getCryptoProperties ().getKeyAlias (),
                          aCryptoFactory.getCryptoProperties ().getKeyPassword ());

    // final WSEncryptionPart encP = new WSEncryptionPart ("Messaging",
    // "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
    // "Element");
    final WSEncryptionPart encP = new WSEncryptionPart ("Body", ESOAPVersion.SOAP_11.getNamespaceURI (), "Element");
    aBuilder.getParts ().add (encP);

    LOGGER.info ("Before Encryption AES 128/RSA-15....");
    final Document encryptedDoc = aBuilder.build (aCryptoFactory.getCrypto ());
    LOGGER.info ("After Encryption AES 128/RSA-15....");
    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);

    assertFalse (outputString.contains ("counter_port_type"));
  }

  @Test
  public void testAES128GCM () throws Exception
  {
    final AS4CryptoFactory aCryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;

    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecEncrypt builder = new WSSecEncrypt (secHeader);
    // builder.setUserInfo ("wss40");
    builder.setUserInfo (aCryptoFactory.getCryptoProperties ().getKeyAlias (),
                         aCryptoFactory.getCryptoProperties ().getKeyPassword ());
    builder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    builder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
    final Document encryptedDoc = builder.build (aCryptoFactory.getCrypto ());

    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);
    // System.out.println (outputString);
    assertFalse (outputString.contains ("counter_port_type"));
  }
}
