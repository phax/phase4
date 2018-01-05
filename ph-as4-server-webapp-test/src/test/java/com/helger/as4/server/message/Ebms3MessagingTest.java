/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.server.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.marshaller.Ebms3WriterBuilder;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.domain.UserMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3To;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.soap12.Soap12Body;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.as4lib.soap12.Soap12Header;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class Ebms3MessagingTest extends AbstractUserMessageTestSetUp
{
  private final ESOAPVersion m_eSOAPVersion = ESOAPVersion.SOAP_12;

  private Document _getMessagingAsDocument (final Ebms3Messaging aEbms3Messaging)
  {
    final Document aEbms3Document = Ebms3WriterBuilder.ebms3Messaging ().getAsDocument (aEbms3Messaging);
    if (aEbms3Document == null)
      throw new IllegalStateException ("Failed to write EBMS3 Messaging to XML");

    // Creating SOAP 12 Envelope
    final Soap12Envelope aSoapEnv = new Soap12Envelope ();
    aSoapEnv.setHeader (new Soap12Header ());
    aSoapEnv.setBody (new Soap12Body ());
    aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
    return Ebms3WriterBuilder.soap12 ().getAsDocument (aSoapEnv);
  }

  @Test
  public void moreThenOneSignalMessageTest () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();
    final List <Ebms3SignalMessage> aSignalMsgList = new ArrayList <> ();

    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    aSignalMessage.setAny (null);

    // Message Info
    aSignalMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // PullRequest
    final Ebms3PullRequest aEbms3PullRequest = new Ebms3PullRequest ();
    aEbms3PullRequest.setMpc (AS4TestConstants.DEFAULT_MPC);
    aSignalMessage.setPullRequest (aEbms3PullRequest);

    aEbms3Messaging.setSignalMessage (aSignalMsgList);

    aSignalMsgList.add (aSignalMessage);
    aSignalMsgList.add (aSignalMessage);

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging), m_eSOAPVersion);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void onlyEbmsMessagingTest () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging), m_eSOAPVersion);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void userMessageWithTooMuchPartyIdsTest () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    // Message Info

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();
    final String sPModeID;
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    sPModeID = MockEbmsHelper.SOAP_12_PARTY_ID + "-" + MockEbmsHelper.SOAP_12_PARTY_ID;

    aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo (AS4TestConstants.TEST_ACTION,
                                                                               AS4TestConstants.TEST_SERVICE_TYPE,
                                                                               AS4TestConstants.TEST_SERVICE,
                                                                               AS4TestConstants.TEST_CONVERSATION_ID,
                                                                               sPModeID,
                                                                               MockEbmsHelper.DEFAULT_AGREEMENT);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_SENDER_URL);
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (MockEbmsHelper.SOAP_12_PARTY_ID));
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (MockEbmsHelper.SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    aEbms3To.addPartyId (MessageHelperMethods.createEbms3PartyId (MockEbmsHelper.SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setTo (aEbms3To);

    final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    aEbms3Messaging.addUserMessage (aEbms3UserMessage);

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging), m_eSOAPVersion);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  // @Ignore
  @Test
  public void sendReceiptTest () throws Exception
  {
    // Fake an incoming message
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();
    final String sPModeID;
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    sPModeID = MockEbmsHelper.SOAP_12_PARTY_ID + "-" + MockEbmsHelper.SOAP_12_PARTY_ID;
    aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo (AS4TestConstants.TEST_ACTION,
                                                                               AS4TestConstants.TEST_SERVICE_TYPE,
                                                                               AS4TestConstants.TEST_SERVICE,
                                                                               AS4TestConstants.TEST_CONVERSATION_ID,
                                                                               sPModeID,
                                                                               MockEbmsHelper.DEFAULT_AGREEMENT);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();
    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_SENDER_URL);
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (MockEbmsHelper.SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    aEbms3To.addPartyId (MessageHelperMethods.createEbms3PartyId (MockEbmsHelper.SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setTo (aEbms3To);

    final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Now send receipt
    final Document aDoc = MockMessages.testReceiptMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage, null);

    // We've got our response
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSOAPVersion), true, null);
  }
}
