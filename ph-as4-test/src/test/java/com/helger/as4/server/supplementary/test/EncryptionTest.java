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
package com.helger.as4.server.supplementary.test;

import static org.junit.Assert.assertFalse;

import javax.annotation.Nullable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A set of test-cases for encrypting and decrypting SOAP requests.
 */
public final class EncryptionTest
{
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger (EncryptionTest.class);

  private final Crypto m_aCrypto;
  private final AS4CryptoFactory m_aAS4CryptoFactory;
  private final CryptoProperties m_aCryptoProperties;

  public EncryptionTest () throws Exception
  {
    m_aAS4CryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;
    m_aCrypto = m_aAS4CryptoFactory.getCrypto ();
    m_aCryptoProperties = m_aAS4CryptoFactory.getCryptoProperties ();
  }

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
    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (secHeader);
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (m_aCryptoProperties.getKeyAlias (), m_aCryptoProperties.getKeyPassword ());

    // final WSEncryptionPart encP = new WSEncryptionPart ("Messaging",
    // "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
    // "Element");
    final WSEncryptionPart encP = new WSEncryptionPart ("Body", ESOAPVersion.SOAP_11.getNamespaceURI (), "Element");
    aBuilder.getParts ().add (encP);

    LOGGER.info ("Before Encryption AES 128/RSA-15....");
    final Document encryptedDoc = aBuilder.build (m_aCrypto);
    LOGGER.info ("After Encryption AES 128/RSA-15....");
    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);

    assertFalse (outputString.contains ("counter_port_type"));
  }

  @Test
  public void testAES128GCM () throws Exception
  {
    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecEncrypt builder = new WSSecEncrypt (secHeader);
    // builder.setUserInfo ("wss40");
    builder.setUserInfo (m_aCryptoProperties.getKeyAlias (), m_aCryptoProperties.getKeyPassword ());
    builder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    builder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
    final Document encryptedDoc = builder.build (m_aCrypto);

    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);
    // System.out.println (outputString);
    assertFalse (outputString.contains ("counter_port_type"));
  }
}
