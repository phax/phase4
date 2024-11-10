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
package com.helger.phase4.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.spi.MockAS4IncomingMessageProcessingStatusSPI;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreAndKeyDescriptor;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;

import jakarta.mail.MessagingException;

/**
 * Test class for class {@link AS4ClientUserMessage}
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
public final class AS4ClientUserMessageTest extends AbstractAS4TestSetUp
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ClientUserMessageTest.class);
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";
  private static final String SERVER_URL = MockJettySetup.getServerAddressFromSettings ();

  @WillNotClose
  private static AS4ResourceHelper s_aResMgr;

  @BeforeClass
  public static void beforeClass () throws Exception
  {
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT);
  }

  @AfterClass
  public static void afterClass () throws Exception
  {
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (null);
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  private static final class TestClientUserMessage extends AS4ClientUserMessage
  {
    public TestClientUserMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
    {
      super (aResHelper);
    }

    @Nullable
    @VisibleForTesting
    public IMicroDocument sendMessageAndGetMicroDocument (@Nonnull final String sURL) throws WSSecurityException,
                                                                                      IOException,
                                                                                      MessagingException
    {
      final int nOldStarted = MockAS4IncomingMessageProcessingStatusSPI.getStarted ();
      final int nOldEnded = MockAS4IncomingMessageProcessingStatusSPI.getEnded ();

      final IAS4ClientBuildMessageCallback aCallback = null;
      final IAS4OutgoingDumper aOutgoingDumper = null;
      final IAS4RetryCallback aRetryCallback = null;
      final IMicroDocument ret = sendMessageWithRetries (sURL,
                                                         new ResponseHandlerMicroDom (),
                                                         aCallback,
                                                         aOutgoingDumper,
                                                         aRetryCallback).getResponseContent ();
      AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " +
                                 MicroWriter.getNodeAsString (ret, AS4HttpDebug.getDebugXMLWriterSettings ()));

      final int nNewStarted = MockAS4IncomingMessageProcessingStatusSPI.getStarted ();
      final int nNewEnded = MockAS4IncomingMessageProcessingStatusSPI.getEnded ();
      assertTrue (nNewStarted > nOldStarted);
      assertTrue (nNewEnded > nOldEnded);

      return ret;
    }
  }

  /**
   * To reduce the amount of code in each test, this method sets the basic
   * attributes that are needed for a successful message to build. <br>
   * Only needed for positive messages.
   *
   * @return the AS4Client with the set attributes to continue
   */
  @Nonnull
  private static TestClientUserMessage _createMandatoryAttributesSuccessMessage ()
  {
    final TestClientUserMessage aClient = new TestClientUserMessage (s_aResMgr);
    aClient.setSoapVersion (ESoapVersion.SOAP_12);

    final String sSenderID = "MyPartyIDforSending";
    final String sResponderID = "MyPartyIDforReceving";

    // Use a pmode that you know is currently running on the server your trying
    // to send the message too
    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
    aClient.setAgreementRefValue (DEFAULT_AGREEMENT);
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    aClient.setFromPartyID (sSenderID);
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    aClient.setToPartyID (sResponderID);
    aClient.ebms3Properties ().setAll (AS4TestConstants.getEBMSProperties ());

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
    final IKeyStoreAndKeyDescriptor aKSD = KeyStoreAndKeyDescriptor.builder ()
                                                                   .type (EKeyStoreType.JKS)
                                                                   .path ("keys/dummy-pw-test.jks")
                                                                   .password ("test")
                                                                   .keyAlias ("ph-as4")
                                                                   .keyPassword ("test")
                                                                   .build ();
    aClient.setCryptoFactory (new AS4CryptoFactoryInMemoryKeyStore (aKSD, null));
    aClient.cryptParams ().setAlias (aKSD.getKeyAlias ());
    return aClient;
  }

  private static void _ensureInvalidState (@Nonnull final AS4ClientUserMessage aClient)
  {
    try
    {
      aClient.buildMessage ("bla", null);
      fail ();
    }
    catch (final Exception ex)
    {
      // expected
    }
  }

  private static void _ensureValidState (@Nonnull final AS4ClientUserMessage aClient)
  {
    try
    {
      aClient.buildMessage ("bla", null);
      // expected
    }
    catch (final Exception ex)
    {
      fail ();
    }
  }

  @Test
  public void testBuildMessageMandatoryCheckFailure () throws Exception
  {
    final AS4ClientUserMessage aClient = new AS4ClientUserMessage (s_aResMgr);
    _ensureInvalidState (aClient);
    aClient.setAction ("AnAction");
    _ensureInvalidState (aClient);
    aClient.setServiceType ("MyServiceType");
    _ensureInvalidState (aClient);
    aClient.setServiceValue ("OrderPaper");
    _ensureInvalidState (aClient);
    aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
    _ensureInvalidState (aClient);
    aClient.setAgreementRefValue (DEFAULT_AGREEMENT);
    _ensureInvalidState (aClient);
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setFromPartyID ("MyPartyIDforSending");
    _ensureInvalidState (aClient);
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    _ensureInvalidState (aClient);
    aClient.setToPartyID ("MyPartyIDforReceving");
    // From now on the message is valid
    _ensureValidState (aClient);
    aClient.ebms3Properties ().setAll (new CommonsArrayList <> ());
    _ensureValidState (aClient);
    aClient.ebms3Properties ().setAll (AS4TestConstants.getEBMSProperties ());
    _ensureValidState (aClient);
  }

  @Test
  public void testSendBodyPayloadMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_PAYLOAD_XML)));
    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendBodyPayloadSignedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_PAYLOAD_XML)));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendBodyPayloadEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_PAYLOAD_XML)));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendBodyPayloadSignedEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_PAYLOAD_XML)));

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendOneAttachmentSignedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendOneAttachmentEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    LOGGER.info (MicroWriter.getNodeAsString (aDoc));
  }

  @Test
  public void testSendOneAttachmentSignedEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendManyAttachmentSignedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                           CMimeType.IMAGE_JPG,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendManyAttachmentEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                           CMimeType.IMAGE_JPG,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendManyAttachmentSignedEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                           CMimeType.APPLICATION_XML,
                           (EAS4CompressionMode) null);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                           CMimeType.IMAGE_JPG,
                           (EAS4CompressionMode) null);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendOneAttachmentCompressedSignedEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                           CMimeType.IMAGE_JPG,
                           EAS4CompressionMode.GZIP);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testSendManyAttachmentCompressedSignedEncryptedMessageSuccessful () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                           CMimeType.APPLICATION_XML,
                           EAS4CompressionMode.GZIP);
    aClient.addAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                           CMimeType.IMAGE_JPG,
                           EAS4CompressionMode.GZIP);

    // Keystore
    _setKeyStoreTestData (aClient);

    // Sign specific
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);

    // Encrypt specific
    aClient.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

    final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument (SERVER_URL);
    assertTrue (MicroWriter.getNodeAsString (aDoc).contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testBuildMessageWithOwnPrefix () throws Exception
  {
    final TestClientUserMessage aClient = _createMandatoryAttributesSuccessMessage ();
    final String sMessageIDPrefix = "ThisIsANewPrefixForTestingPurpose@";
    aClient.setMessageIDFactory ( () -> sMessageIDPrefix + MessageHelperMethods.createRandomMessageID ());
    final String sMessageID = aClient.createMessageID ();

    assertTrue (EntityUtils.toString (aClient.buildMessage (sMessageID, null).getHttpEntity ())
                           .contains (sMessageIDPrefix));
  }
}
