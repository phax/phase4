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

import static org.junit.Assert.assertFalse;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.transform.TransformerException;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.scope.mock.ScopeTestRule;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A set of test-cases for encrypting and decrypting SOAP requests.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public final class EncryptionTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (EncryptionTest.class);

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

  private static String _prettyDocumentToString (final Document doc) throws TransformerException
  {
    try (final NonBlockingByteArrayOutputStream baos = new NonBlockingByteArrayOutputStream ())
    {
      XMLUtils.elementToStream (doc.getDocumentElement (), baos);
      return baos.getAsString (StandardCharsets.UTF_8);
    }
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
    final IAS4CryptoFactory aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();

    final Document aSoapDoc = _getSoapEnvelope11 ();
    final WSSecHeader aSecHeader = new WSSecHeader (aSoapDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (aSecHeader);
    aBuilder.setKeyIdentifierType (ECryptoKeyIdentifierType.ISSUER_SERIAL.getTypeID ());
    aBuilder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
    aBuilder.setUserInfo (aCryptoFactory.getKeyAlias (),
                          aCryptoFactory.getKeyPasswordPerAlias (aCryptoFactory.getKeyAlias ()));

    // final WSEncryptionPart encP = new WSEncryptionPart ("Messaging",
    // "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
    // "Element");
    final WSEncryptionPart aEncPart = new WSEncryptionPart ("Body", ESoapVersion.SOAP_11.getNamespaceURI (), "Element");
    aBuilder.getParts ().add (aEncPart);

    // Generate a session key
    final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_128);
    final SecretKey aSymmetricKey = aKeyGen.generateKey ();

    LOGGER.info ("Before Encryption AES 128/RSA-15....");
    final Document encryptedDoc = aBuilder.build (aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN), aSymmetricKey);
    LOGGER.info ("After Encryption AES 128/RSA-15....");
    final String sOutputString = _prettyDocumentToString (encryptedDoc);

    assertFalse (sOutputString.contains ("counter_port_type"));
  }

  @Test
  public void testAES128GCM () throws Exception
  {
    final IAS4CryptoFactory aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();

    final Document doc = _getSoapEnvelope11 ();
    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (secHeader);
    // builder.setUserInfo ("wss40");
    aBuilder.setUserInfo (aCryptoFactory.getKeyAlias (),
                          aCryptoFactory.getKeyPasswordPerAlias (aCryptoFactory.getKeyAlias ()));
    aBuilder.setKeyIdentifierType (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE.getTypeID ());
    aBuilder.setSymmetricEncAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());

    // Generate a session key
    final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_128);
    final SecretKey aSymmetricKey = aKeyGen.generateKey ();

    final Document aEncryptedDoc = aBuilder.build (aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN), aSymmetricKey);

    final String sOutputString = _prettyDocumentToString (aEncryptedDoc);
    // System.out.println (outputString);
    assertFalse (sOutputString.contains ("counter_port_type"));
  }
}
