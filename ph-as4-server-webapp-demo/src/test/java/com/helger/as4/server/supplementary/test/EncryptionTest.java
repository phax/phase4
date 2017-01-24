/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import java.io.IOException;

import javax.crypto.KeyGenerator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.io.resource.ClassPathResource;

/**
 * A set of test-cases for encrypting and decrypting SOAP requests.
 */
public class EncryptionTest
{
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (EncryptionTest.class);

  private final WSSecurityEngine secEngine = new WSSecurityEngine ();
  private final Crypto m_aCrypto;
  private final AS4CryptoFactory m_aAS4CryptoFactory;
  private final CryptoProperties m_aCryptoProperties;

  public EncryptionTest () throws Exception
  {
    m_aAS4CryptoFactory = new AS4CryptoFactory ();
    m_aCrypto = m_aAS4CryptoFactory.getCrypto ();
    m_aCryptoProperties = m_aAS4CryptoFactory.getCryptoProperties ();
  }

  /**
   * Setup method
   *
   * @throws java.lang.Exception
   *         Thrown when there is a problem in setup
   */
  @Before
  public void setUp () throws Exception
  {
    final KeyGenerator keyGen = KeyGenerator.getInstance ("AES");
    keyGen.init (128);
    secEngine.setWssConfig (WSSConfig.getNewInstance ());
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
    final WSSecEncrypt builder = new WSSecEncrypt ();
    builder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    builder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getXMLID ());
    builder.setSymmetricKey (null);
    builder.setUserInfo (m_aCryptoProperties.getKeyAlias (), m_aCryptoProperties.getKeyPassword ());

    final Document doc = _getSoapEnvelope11 ();
    WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    // final WSEncryptionPart encP = new WSEncryptionPart ("Messaging",
    // "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
    // "Element");
    final WSEncryptionPart encP = new WSEncryptionPart ("Body", ESOAPVersion.SOAP_11.getNamespaceURI (), "Element");
    builder.getParts ().add (encP);
    secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();
    LOG.info ("Before Encryption AES 128/RSA-15....");
    final Document encryptedDoc = builder.build (doc, m_aCrypto, secHeader);
    LOG.info ("After Encryption AES 128/RSA-15....");
    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);

    assertFalse (outputString.contains ("counter_port_type"));

  }

  private Document _getSoapEnvelope11 () throws SAXException, IOException, ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource ("UserMessageWithoutWSSE.xml").getInputStream ());
  }

  @Test
  public void testAES128GCM () throws Exception
  {
    final WSSecEncrypt builder = new WSSecEncrypt ();
    // builder.setUserInfo ("wss40");
    builder.setUserInfo (m_aCryptoProperties.getKeyAlias (), m_aCryptoProperties.getKeyPassword ());
    builder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    builder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getXMLID ());
    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();
    final Document encryptedDoc = builder.build (doc, m_aCrypto, secHeader);

    final String outputString = XMLUtils.prettyDocumentToString (encryptedDoc);
    // System.out.println (outputString);
    assertFalse (outputString.contains ("counter_port_type"));
  }
}
