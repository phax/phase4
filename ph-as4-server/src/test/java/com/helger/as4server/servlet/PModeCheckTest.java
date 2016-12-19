/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.mock.MockPModeGenerator;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeConfig;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.constants.AS4ServerTestHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.url.URLHelper;
import com.helger.photon.jetty.JettyRunner;
import com.helger.xml.serialize.read.DOMReader;

public class PModeCheckTest extends AbstractUserMessageSetUp
{
  private static final int PORT = URLHelper.getAsURL (PROPS.getAsString ("server.address")).getPort ();
  private static final int STOP_PORT = PORT + 1000;
  private static JettyRunner s_aJetty = new JettyRunner (PORT, STOP_PORT);
  private static AS4ResourceManager s_aResMgr;
  private static final Logger s_aLogger = LoggerFactory.getLogger (PModeCheckTest.class);

  private Ebms3UserMessage aEbms3UserMessage;
  private CreateUserMessage aUserMessage;
  private Node aPayload;

  @BeforeClass
  public static void startServer () throws Exception
  {
    s_aJetty.startServer ();
    s_aResMgr = new AS4ResourceManager ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr.close ();
    s_aJetty.shutDownServer ();
  }

  @Before
  public void setupMessage ()
  {
    aEbms3UserMessage = new Ebms3UserMessage ();
    aUserMessage = new CreateUserMessage ();
    // Default Payload for testing

    try
    {
      aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
      aEbms3UserMessage.setPayloadInfo (aUserMessage.createEbms3PayloadInfo (aPayload, null));
    }
    catch (final SAXException e)
    {
      s_aLogger.warn ("SOAPBodyPayload.xml could not be found no payload attached in PModeCheckTest setup");
    }

    // Default MessageInfo for testing
    aEbms3UserMessage.setMessageInfo (aUserMessage.createEbms3MessageInfo (CAS4.LIB_NAME));

    // Default CollaborationInfo for testing
    aEbms3UserMessage.setCollaborationInfo (aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                       "MyServiceTypes",
                                                                                       "QuoteToCollect",
                                                                                       "4321",
                                                                                       null,
                                                                                       AS4ServerTestHelper.DEFAULT_AGREEMENT));

    // Default PartyInfo for testing
    aEbms3UserMessage.setPartyInfo (aUserMessage.createEbms3PartyInfo (AS4ServerTestHelper.DEFAULT_INITIATOR_ROLE,
                                                                       AS4ServerTestHelper.DEFAULT_PARTY_ID,
                                                                       AS4ServerTestHelper.DEFAULT_RESPONDER_ROLE,
                                                                       AS4ServerTestHelper.DEFAULT_PARTY_ID));
    // Default MessageProperties for testing
    aEbms3UserMessage.setMessageProperties (_defaultProperties ());

  }

  @Test
  public void testWrongPModeID () throws Exception
  {
    aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode ("this-is-a-wrong-id");

    final Document aDoc = aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage)
                                      .setMustUnderstand (true)
                                      .getAsSOAPDocument (aPayload);
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testPModeLegNullReject () throws Exception
  {
    final String sPModeID = "pmode-" + GlobalIDFactory.getNewPersistentIntID ();
    final PMode aPMode = MockPModeGenerator.getTestPModeSetID (ESOAPVersion.AS4_DEFAULT, sPModeID);
    ((PModeConfig) aPMode.getConfig ()).setLeg1 (null);
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();

    try
    {
      aPModeMgr.createPMode (aPMode);

      final IPMode aPModeID = MetaAS4Manager.getPModeMgr ().findFirst (_getFirstPModeWithID (sPModeID));
      aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode (aPModeID.getID ());

      final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                                                                                aEbms3UserMessage)
                                                                                               .setMustUnderstand (true)
                                                                                               .getAsSOAPDocument (aPayload),
                                                                                   ESOAPVersion.AS4_DEFAULT,
                                                                                   null,
                                                                                   s_aResMgr,
                                                                                   false,
                                                                                   ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                   ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

      sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)),
                        false,
                        EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
    }
    finally
    {
      // The MockPModeGenerator generates automatically a PModeConfig, we need
      // too delete it after we are done with the test
      MetaAS4Manager.getPModeConfigMgr ().deletePModeConfig (aPMode.getConfigID ());
      aPModeMgr.deletePMode (aPMode.getID ());
    }
  }

  @Test
  public void testUserMessageMissingProperties () throws Exception
  {
    aEbms3UserMessage.setMessageProperties (null);
    final Document aDoc = aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage)
                                      .setMustUnderstand (true)
                                      .getAsSOAPDocument (aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "");
  }

  @Test
  public void testUserMessageDifferentPropertiesValues () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage)
                                      .setMustUnderstand (true)
                                      .getAsSOAPDocument (aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "");
  }

  @Test
  public void testUserMessageFinalRecipientButNoOriginalSender () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4ServerTestHelper.getEBMSProperties ();
    aEbms3Properties.removeIf ( (prop) -> prop.getName ().equals (AS4ServerTestHelper.ORIGINAL_SENDER));

    assertTrue (aEbms3Properties.size () == 1);

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage)
                                      .setMustUnderstand (true)
                                      .getAsSOAPDocument (aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      "originalSender property is empty or not existant but mandatory");
  }

  @Test
  public void testUserMessageOriginalSenderButNoFinalRecipient () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4ServerTestHelper.getEBMSProperties ();
    aEbms3Properties.removeIf ( (prop) -> prop.getName ().equals (AS4ServerTestHelper.FINAL_RECIPIENT));

    assertTrue (aEbms3Properties.size () == 1);

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = aUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage)
                                      .setMustUnderstand (true)
                                      .getAsSOAPDocument (aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      "finalRecipient property is empty or not existant but mandatory");
  }

  @Test
  public void testNoResponderInMessageInvalidShouldReturnErrorMessage () throws Exception
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/NoResponder.xml").getInputStream ());

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_INVALID_HEADER.getErrorCode ());
  }

  /**
   * Is ESENS specific, EBMS3 specification is the protocol an optional element.
   * Maybe refactoring into a test if http and smtp addresses later on get
   * converted right
   *
   * @throws Exception
   *         In case of an error
   */
  @Ignore
  @Test
  public void testPModeLegProtocolAddressReject () throws Exception
  {
    final String sPModeID = "pmode-" + GlobalIDFactory.getNewPersistentIntID ();
    final PMode aPMode = MockPModeGenerator.getTestPModeSetID (ESOAPVersion.AS4_DEFAULT, sPModeID);
    ((PModeConfig) aPMode.getConfig ()).setLeg1 (new PModeLeg (new PModeLegProtocol ("TestsimulationAddressWrong",
                                                                                     ESOAPVersion.AS4_DEFAULT),
                                                               null,
                                                               null,
                                                               null,
                                                               null));
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    try
    {
      aPModeMgr.createPMode (aPMode);

      final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (_modifyUserMessage (sPModeID,
                                                                                                       null,
                                                                                                       null,
                                                                                                       _defaultProperties ()),
                                                                                   ESOAPVersion.AS4_DEFAULT,
                                                                                   null,
                                                                                   s_aResMgr,
                                                                                   false,
                                                                                   ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                   ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

      sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)),
                        false,
                        EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
    }
    finally
    {
      aPModeMgr.deletePMode (aPMode.getID ());
    }
  }

  private Ebms3Property getRandomProperty ()
  {
    final Ebms3Property aRandomProperty = new Ebms3Property ();
    aRandomProperty.setName ("randomname" + UUID.randomUUID ());
    aRandomProperty.setValue ("randomvalue" + UUID.randomUUID ());

    return aRandomProperty;
  }
}
