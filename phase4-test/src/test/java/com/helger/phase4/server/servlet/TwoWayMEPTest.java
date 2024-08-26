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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.test.profile.TestPMode;
import com.helger.xml.serialize.read.DOMReader;

public final class TwoWayMEPTest extends AbstractUserMessageTestSetUpExt
{
  private final ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;
  private PMode m_aPMode;

  @Before
  public void before ()
  {
    final PMode aPMode = TestPMode.createTestPMode (AS4TestConstants.TEST_INITIATOR,
                                                    AS4TestConstants.TEST_RESPONDER,
                                                    MockJettySetup.getServerAddressFromSettings (),
                                                    (i, r) -> "pmode" + GlobalIDFactory.getNewPersistentLongID (),
                                                    false);
    // Setting second leg to the same as first
    final PModeLeg aLeg2 = aPMode.getLeg1 ();

    // ESENS PMode is One Way on default settings need to change to two way
    m_aPMode = new PMode (aPMode.getID (),
                          PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                          PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                          aPMode.getAgreement (),
                          EMEP.TWO_WAY,
                          EMEPBinding.SYNC,
                          aPMode.getLeg1 (),
                          aLeg2,
                          aPMode.getPayloadService (),
                          aPMode.getReceptionAwareness ());

    // Delete old PMode since it is getting created in the ESENS createPMode
    MetaAS4Manager.getPModeMgr ().deletePMode (aPMode.getID ());
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);
  }

  @Test
  public void testReceiveUserMessageAsResponseSuccess () throws Exception
  {
    final Document aDoc = modifyUserMessage (m_aPMode.getID (),
                                             null,
                                             null,
                                             createDefaultProperties (),
                                             null,
                                             null,
                                             null);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.USERMESSAGE_ASSERTCHECK));
    assertFalse (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (m_aPMode.getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
  }

  @Test
  public void testReceiveUserMessageWithMimeAsResponseSuccessWithoutEncryption () throws Exception
  {
    m_aPMode.getLeg2 ().getSecurity ().setX509EncryptionAlgorithm (null);
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);

    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final Document aDoc = modifyUserMessage (m_aPMode.getID (),
                                             null,
                                             null,
                                             createDefaultProperties (),
                                             aAttachments,
                                             null,
                                             null);
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.USERMESSAGE_ASSERTCHECK));
    assertFalse (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (m_aPMode.getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
    // Checking if he adds the attachment to the response message, the mock spi
    // just adds the xml that gets sent in the original message and adds it to
    // the response
    assertTrue (sResponse.contains ("<dummy>This is a test XML</dummy>"));
  }

  @Test
  public void testReceiveUserMessageWithMimeAsResponseSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final Document aDoc = modifyUserMessage (m_aPMode.getID (),
                                             (String) null,
                                             // Alias for encryption
                                             "ph-as4",
                                             createDefaultProperties (),
                                             aAttachments,
                                             null,
                                             null);
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.USERMESSAGE_ASSERTCHECK));
    assertFalse (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (m_aPMode.getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
    // Checking if he adds the attachment to the response message, the mock spi
    // just adds the xml that gets sent in the original message and adds it to
    // the response
    assertTrue (sResponse.contains (m_aPMode.getLeg2 ()
                                            .getSecurity ()
                                            .getX509EncryptionAlgorithm ()
                                            .getAlgorithmURI ()));
  }

  @Test
  public void testPModeWrongMPCLeg2 () throws Exception
  {
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    final Document aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    aEbms3UserMessage.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null));

    // Default MessageInfo for testing
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Default CollaborationInfo for testing
    aEbms3UserMessage.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo (null,
                                                                                               DEFAULT_AGREEMENT,
                                                                                               null,
                                                                                               null,
                                                                                               CAS4.DEFAULT_SERVICE_URL,
                                                                                               CAS4.DEFAULT_ACTION_URL,
                                                                                               AS4TestConstants.TEST_CONVERSATION_ID));

    // Default PartyInfo for testing
    aEbms3UserMessage.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                               DEFAULT_PARTY_ID,
                                                                               CAS4.DEFAULT_RESPONDER_URL,
                                                                               DEFAULT_PARTY_ID));
    // Default MessageProperties for testing
    aEbms3UserMessage.setMessageProperties (createDefaultProperties ());

    m_aPMode.getLeg2 ().getBusinessInfo ().setMPCID ("wrongmpc-id");

    final IPMode aPMode = MetaAS4Manager.getPModeMgr ().getPModeOfID (m_aPMode.getID ());
    aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode (aPMode.getID ());

    final Document aSignedDoc = AS4UserMessage.create (m_eSoapVersion, aEbms3UserMessage)
                                              .setMustUnderstand (true)
                                              .getAsSoapDocument (aPayload);

    sendPlainMessage (new HttpXMLEntity (aSignedDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testPModeWithOnlyLeg2 () throws Exception
  {
    m_aPMode.setLeg1 (null);

    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);

    final Document aDoc = modifyUserMessage (m_aPMode.getID (),
                                             null,
                                             null,
                                             createDefaultProperties (),
                                             null,
                                             null,
                                             null);
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testPModeWithTwoWayButNoLeg2 () throws Exception
  {
    m_aPMode.setLeg2 (null);
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);

    final Document aDoc = modifyUserMessage (m_aPMode.getID (),
                                             null,
                                             null,
                                             createDefaultProperties (),
                                             null,
                                             null,
                                             null);
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }
}
