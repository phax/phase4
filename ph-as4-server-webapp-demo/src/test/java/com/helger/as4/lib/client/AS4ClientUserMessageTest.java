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
package com.helger.as4.lib.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Nonnull;

import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.client.AS4ClientUserMessage;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link AS4ClientUserMessage}
 *
 * @author Martin Bayerl
 */
public final class AS4ClientUserMessageTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4ClientUserMessageTest.class);

  private static AS4ResourceManager s_aResMgr;
  private static final String SERVER_URL = "http://127.0.0.1:8080/as4";

  private static final String RECEIPT_CHECK = "Receipt";

  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
    MockPModeGenerator.ensureMockPModesArePresent ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  /**
   * To reduce the amount of code in each test, this method sets the basic
   * attributes that are needed for a successful message to build. <br>
   * Only needed for positive messages.
   *
   * @return the AS4Client with the set attributes to continue
   */
  @Nonnull
  private static AS4ClientUserMessage _getMandatoryAttributesSuccessMessage ()
  {
    final AS4ClientUserMessage aClient = new AS4ClientUserMessage (s_aResMgr);
    aClient.setSOAPVersion (ESOAPVersion.SOAP_12);

    final String sSenderID = "MyPartyIDforSending";
    final String sResponderID = "MyPartyIDforReceving";

    // Use a pmode that you know is currently running on the server your trying
    // to send the message too
    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID ("9898");
    aClient.setAgreementRefValue (MockEbmsHelper.DEFAULT_AGREEMENT);
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    aClient.setFromPartyID (sSenderID);
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    aClient.setToPartyID (sResponderID);
    aClient.setEbms3Properties (MockEbmsHelper.getEBMSProperties ());

    return aClient;
  }

  /**
   * Sets the keystore attributes, it uses the dummy keystore
   * keys/dummy-pw-test.jks
   *
   * @param aClient
   *        the client on which these attributes should be set
   * @return the client to continue working with it
   */
  @Nonnull
  private static AS4ClientUserMessage _setKeyStoreTestData (@Nonnull final AS4ClientUserMessage aClient)
  {
    aClient.setKeyStoreAlias ("ph-as4");
    aClient.setKeyStorePassword ("test");
    aClient.setKeyStoreFile (new ClassPathResource ("keys/dummy-pw-test.jks").getAsFile ());
    aClient.setKeyStoreType ("jks");
    return aClient;
  }

  private static void _ensureInvalidState (@Nonnull final AS4ClientUserMessage aClient) throws Exception
  {
    try
    {
      aClient.buildMessage ();
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
  }

  private static void _ensureValidState (@Nonnull final AS4ClientUserMessage aClient) throws Exception
  {
    try
    {
      aClient.buildMessage ();
    }
    catch (final IllegalStateException ex)
    {
      fail ();
    }
  }

  @Test
  public void buildMessageMandatoryCheckFailure () throws Exception
  {
    final AS4ClientUserMessage aClient = new AS4ClientUserMessage (s_aResMgr);
    _ensureInvalidState (aClient);
    aClient.setAction ("AnAction");
    _ensureInvalidState (aClient);
    aClient.setServiceType ("MyServiceType");
    _ensureInvalidState (aClient);
    aClient.setServiceValue ("OrderPaper");
    _ensureInvalidState (aClient);
    aClient.setConversationID ("9898");
    _ensureInvalidState (aClient);
    aClient.setAgreementRefValue (MockEbmsHelper.DEFAULT_AGREEMENT);
    _ensureInvalidState (aClient);
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setFromPartyID ("MyPartyIDforSending");
    _ensureInvalidState (aClient);
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setToPartyID ("MyPartyIDforReceving");
    _ensureInvalidState (aClient);
    aClient.setEbms3Properties (MockEbmsHelper.getEBMSProperties ());
    _ensureValidState (aClient);
  }

  @Test
  public void buildMessageKeystoreCheckFailure () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();

    // Set sign attributes, to get to the check, the check only gets called if
    // sign or encrypt needs to be done for the usermessage
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // No Keystore attributes set
    _ensureInvalidState (aClient);
    aClient.setKeyStoreFile (new ClassPathResource ("keys/dummy-pw-test.jks").getAsFile ());
    _ensureInvalidState (aClient);
    aClient.setKeyStoreType ("jks");
    _ensureInvalidState (aClient);
    aClient.setKeyStoreAlias ("ph-as4");
    _ensureInvalidState (aClient);
    aClient.setKeyStorePassword ("test");
    _ensureValidState (aClient);

  }

  @Test
  public void sendBodyPayloadMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource ("PayloadXML.xml")));
    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendBodyPayloadSignedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource ("PayloadXML.xml")));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendBodyPayloadEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource ("PayloadXML.xml")));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendBodyPayloadSignedEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource ("PayloadXML.xml")));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendOneAttachmentSignedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendOneAttachmentEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    s_aLogger.info (MicroWriter.getNodeAsString (aDoc));
  }

  @Test
  public void sendOneAttachmentSignedEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendManyAttachmentSignedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml2.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/test-img.jpg").getAsFile (), CMimeType.IMAGE_JPG);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendManyAttachmentEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml2.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/test-img.jpg").getAsFile (), CMimeType.IMAGE_JPG);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendManyAttachmentSignedEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml2.xml").getAsFile (), CMimeType.APPLICATION_XML);
    aClient.addAttachment (new ClassPathResource ("attachment/test-img.jpg").getAsFile (), CMimeType.IMAGE_JPG);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendOneAttachmentCompressedSignedEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml2.xml").getAsFile (),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (new ClassPathResource ("attachment/test-img.jpg").getAsFile (),
                           CMimeType.IMAGE_JPG,
                           EAS4CompressionMode.GZIP);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void sendManyAttachmentCompressedSignedEncryptedMessageSuccessful () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml.xml").getAsFile (),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (new ClassPathResource ("attachment/shortxml2.xml").getAsFile (),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (new ClassPathResource ("attachment/test-img.jpg").getAsFile (),
                           CMimeType.IMAGE_JPG,
                           EAS4CompressionMode.GZIP);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (RECEIPT_CHECK));
  }

  @Test
  public void buildMessageWithOwnPrefix () throws Exception
  {
    final AS4ClientUserMessage aClient = _getMandatoryAttributesSuccessMessage ();
    final String sMessageIDPrefix = "ThisIsANewPrefixForTestingPurpose";
    aClient.setMessageIDPrefix (sMessageIDPrefix);

    assertTrue (EntityUtils.toString (aClient.buildMessage ()).contains (sMessageIDPrefix));
  }
}
