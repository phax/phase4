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
package com.helger.as4.server.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.mock.MockPModeGenerator;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class PModeCheckTest extends AbstractUserMessageTestSetUpExt
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PModeCheckTest.class);

  private Ebms3UserMessage m_aEbms3UserMessage;
  private Node m_aPayload;

  @Before
  public void setupMessage ()
  {
    m_aEbms3UserMessage = new Ebms3UserMessage ();

    // Default Payload for testing
    try
    {
      m_aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
      m_aEbms3UserMessage.setPayloadInfo (CreateUserMessage.createEbms3PayloadInfo (m_aPayload, null));
    }
    catch (final SAXException ex)
    {
      s_aLogger.warn ("SOAPBodyPayload.xml could not be found no payload attached in PModeCheckTest setup", ex);
    }

    // Default MessageInfo for testing
    m_aEbms3UserMessage.setMessageInfo (CreateUserMessage.createEbms3MessageInfo (CAS4.LIB_NAME));

    // Default CollaborationInfo for testing
    m_aEbms3UserMessage.setCollaborationInfo (CreateUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                              "MyServiceTypes",
                                                                                              "QuoteToCollect",
                                                                                              "4321",
                                                                                              null,
                                                                                              MockEbmsHelper.DEFAULT_AGREEMENT));

    // Default PartyInfo for testing
    m_aEbms3UserMessage.setPartyInfo (CreateUserMessage.createEbms3PartyInfo (MockEbmsHelper.DEFAULT_INITIATOR_ROLE,
                                                                              MockEbmsHelper.DEFAULT_PARTY_ID,
                                                                              MockEbmsHelper.DEFAULT_RESPONDER_ROLE,
                                                                              MockEbmsHelper.DEFAULT_PARTY_ID));
    // Default MessageProperties for testing
    m_aEbms3UserMessage.setMessageProperties (_defaultProperties ());

  }

  @Test
  public void testWrongPModeID () throws Exception
  {
    m_aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode ("this-is-a-wrong-id");

    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);
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
      m_aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode (aPModeID.getConfigID ());

      final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                                                                                     m_aEbms3UserMessage)
                                                                                                    .setMustUnderstand (true)
                                                                                                    .getAsSOAPDocument (m_aPayload),
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
  public void testPModeWrongMPC () throws Exception
  {
    final String sPModeID = "pmode-" + GlobalIDFactory.getNewPersistentIntID ();
    final PMode aPMode = MockPModeGenerator.getTestPModeSetID (ESOAPVersion.AS4_DEFAULT, sPModeID);
    aPMode.getConfig ().getLeg1 ().getBusinessInfo ().setMPCID ("wrongmpc-id");
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();

    try
    {
      aPModeMgr.createPMode (aPMode);

      final IPMode aPModeID = MetaAS4Manager.getPModeMgr ().findFirst (_getFirstPModeWithID (sPModeID));
      m_aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode (aPModeID.getConfigID ());

      final Document aSignedDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                                    m_aEbms3UserMessage)
                                                   .setMustUnderstand (true)
                                                   .getAsSOAPDocument (m_aPayload);

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
  public void testWrongMPCShouldReturnFailure () throws Exception
  {
    m_aEbms3UserMessage.setMpc ("http://random.com/testmpc");
    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageMissingProperties () throws Exception
  {
    m_aEbms3UserMessage.setMessageProperties (null);
    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "");
  }

  @Test
  public void testUserMessageDifferentPropertiesValues () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    m_aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "");
  }

  @Test
  public void testUserMessageFinalRecipientButNoOriginalSender () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();
    aEbms3Properties.removeIf (prop -> prop.getName ().equals (CAS4.ORIGINAL_SENDER));

    assertEquals (1, aEbms3Properties.size ());

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    m_aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      "originalSender property is empty or not existant but mandatory");
  }

  @Test
  public void testUserMessageOriginalSenderButNoFinalRecipient () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();
    aEbms3Properties.removeIf ( (prop) -> prop.getName ().equals (CAS4.FINAL_RECIPIENT));

    assertEquals (1, aEbms3Properties.size ());

    aEbms3Properties.add (getRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    m_aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                            m_aEbms3UserMessage)
                                           .setMustUnderstand (true)
                                           .getAsSOAPDocument (m_aPayload);

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
   * Maybe refactoring into a test if http and SMTP addresses later on get
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
