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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.mail.internet.MimeMessage;

import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.xml.serialize.read.DOMReader;

public class TwoWayMEPTest extends AbstractUserMessageTestSetUpExt
{
  private PMode m_aPMode;

  @Before
  public void createTwoWayPMode ()
  {
    final PMode aPMode = ESENSPMode.createESENSPMode (AS4ServerConfiguration.getSettings ()
                                                                            .getAsString ("server.address",
                                                                                          "http://localhost:8080/as4"));
    // Setting second leg to the same as first
    final PModeLeg aLeg2 = aPMode.getLeg1 ();
    aLeg2.getSecurity ().setX509EncryptionAlgorithm (null);
    // ESENS PMode is One Way on default settings need to change to two way
    m_aPMode = new PMode ("esens-two-way",
                          PModeParty.createSimple (MockEbmsHelper.DEFAULT_PARTY_ID, CAS4.DEFAULT_ROLE),
                          PModeParty.createSimple (MockEbmsHelper.DEFAULT_PARTY_ID, CAS4.DEFAULT_ROLE),
                          aPMode.getAgreement (),
                          EMEP.TWO_WAY,
                          EMEPBinding.SYNC,
                          aPMode.getLeg1 (),
                          aLeg2,
                          aPMode.getPayloadService (),
                          aPMode.getReceptionAwareness ());

    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);
  }

  @Test
  public void receiveUserMessageAsResponseSuccess () throws Exception
  {
    final Document aDoc = _modifyUserMessage (m_aPMode.getID (), null, null, _defaultProperties ());
    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
    assertTrue (sResponse.contains ("UserMessage"));
    assertFalse (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains (m_aPMode.getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
  }

  @Test
  public void receiveUserMessageWithMimeAsResponseSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final Document aDoc = _modifyUserMessage (m_aPMode.getID (), null, null, _defaultProperties (), aAttachments);
    final MimeMessage aMimeMsg = new MimeMessageCreator (ESOAPVersion.SOAP_12).generateMimeMessage (aDoc, aAttachments);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);
    assertTrue (sResponse.contains ("UserMessage"));
    assertFalse (sResponse.contains ("Receipt"));
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
  public void testPModeWrongMPCLeg2 () throws Exception
  {
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    final Document aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    aEbms3UserMessage.setPayloadInfo (CreateUserMessage.createEbms3PayloadInfo (aPayload, null));

    // Default MessageInfo for testing
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Default CollaborationInfo for testing
    aEbms3UserMessage.setCollaborationInfo (CreateUserMessage.createEbms3CollaborationInfo (CAS4.DEFAULT_ACTION_URL,
                                                                                            null,
                                                                                            CAS4.DEFAULT_SERVICE_URL,
                                                                                            "4321",
                                                                                            null,
                                                                                            MockEbmsHelper.DEFAULT_AGREEMENT));

    // Default PartyInfo for testing
    aEbms3UserMessage.setPartyInfo (CreateUserMessage.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                            MockEbmsHelper.DEFAULT_PARTY_ID,
                                                                            CAS4.DEFAULT_RESPONDER_URL,
                                                                            MockEbmsHelper.DEFAULT_PARTY_ID));
    // Default MessageProperties for testing
    aEbms3UserMessage.setMessageProperties (_defaultProperties ());

    m_aPMode.getLeg2 ().getBusinessInfo ().setMPCID ("wrongmpc-id");

    final IPMode aPModeID = MetaAS4Manager.getPModeMgr ().findFirst (_getFirstPModeWithID (m_aPMode.getID ()));
    aEbms3UserMessage.getCollaborationInfo ().getAgreementRef ().setPmode (aPModeID.getID ());

    final Document aSignedDoc = CreateUserMessage.getUserMessageAsAS4UserMessage (ESOAPVersion.AS4_DEFAULT,
                                                                                  aEbms3UserMessage)
                                                 .setMustUnderstand (true)
                                                 .getAsSOAPDocument (aPayload);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testPModeConfigWithOnlyLeg2 () throws Exception
  {
    final PMode aPMode = ESENSPMode.createESENSPMode (AS4ServerConfiguration.getSettings ()
                                                                            .getAsString ("server.address",
                                                                                          "http://localhost:8080/as4"));
    // ESENS PMode is One Way on default settings need to change to two way

    aPMode.setMEP (EMEP.TWO_WAY);
    aPMode.setMEPBinding (EMEPBinding.SYNC);
    aPMode.setAgreement (aPMode.getAgreement ());
    aPMode.setLeg2 (aPMode.getLeg1 ());
    aPMode.setPayloadService (aPMode.getPayloadService ());
    aPMode.setReceptionAwareness (aPMode.getReceptionAwareness ());
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);

    final Document aDoc = _modifyUserMessage (aPMode.getID (), null, null, _defaultProperties ());
    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }
}
