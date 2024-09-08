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
package com.helger.phase4.server.message;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.core5.http.HttpEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3From;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3To;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.marshaller.Ebms3MessagingMarshaller;
import com.helger.phase4.marshaller.Soap12EnvelopeMarshaller;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.soap12.Soap12Body;
import com.helger.phase4.soap12.Soap12Envelope;
import com.helger.phase4.soap12.Soap12Header;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link Ebms3Messaging}.
 *
 * @author Philip Helger
 */
public final class Ebms3MessagingTest extends AbstractUserMessageTestSetUp
{
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";
  private static final String SOAP_12_PARTY_ID = "APP_000000000012";
  private static final ESoapVersion SOAP_VERSION = ESoapVersion.SOAP_12;

  @Nullable
  private Document _getMessagingAsSoapDocument (@Nonnull final Ebms3Messaging aEbms3Messaging)
  {
    final Document aEbms3Document = new Ebms3MessagingMarshaller ().getAsDocument (aEbms3Messaging);
    if (aEbms3Document == null)
      throw new IllegalStateException ("Failed to write EBMS3 Messaging to XML");

    // Creating SOAP 12 Envelope
    final Soap12Envelope aSoapEnv = new Soap12Envelope ();
    aSoapEnv.setHeader (new Soap12Header ());
    aSoapEnv.setBody (new Soap12Body ());
    aSoapEnv.getHeader ().addAny (aEbms3Document.getDocumentElement ());
    return new Soap12EnvelopeMarshaller ().getAsDocument (aSoapEnv);
  }

  @Test
  public void testMoreThenOneSignalMessage () throws Exception
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

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsSoapDocument (aEbms3Messaging),
                                                  SOAP_VERSION.getMimeType ());
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testOnlyEbmsMessaging () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsSoapDocument (aEbms3Messaging),
                                                  SOAP_VERSION.getMimeType ());
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithTooManyPartyIds () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    // Message Info

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final String sPModeID = SOAP_12_PARTY_ID + "-" + SOAP_12_PARTY_ID;

    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sPModeID,
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 AS4TestConstants.TEST_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_INITIATOR_URL);
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    aEbms3To.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setTo (aEbms3To);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    aEbms3Messaging.addUserMessage (aEbms3UserMessage);

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsSoapDocument (aEbms3Messaging),
                                                  SOAP_VERSION.getMimeType ());
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageNoMessageProperties () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();
    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    // Message Info

    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final String sPModeID = SOAP_12_PARTY_ID + "-" + SOAP_12_PARTY_ID;

    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sPModeID,
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 AS4TestConstants.TEST_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_INITIATOR_URL);
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3From.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    aEbms3To.addPartyId (MessageHelperMethods.createEbms3PartyId (SOAP_12_PARTY_ID));
    aEbms3PartyInfo.setTo (aEbms3To);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    aEbms3Messaging.addUserMessage (aEbms3UserMessage);

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsSoapDocument (aEbms3Messaging),
                                                  SOAP_VERSION.getMimeType ());
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testSendReceipt () throws Exception
  {
    // Fake an incoming message
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final String sPModeID = SOAP_12_PARTY_ID + "-" + SOAP_12_PARTY_ID;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sPModeID,
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 AS4TestConstants.TEST_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);

    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                                      SOAP_12_PARTY_ID,
                                                                                      CAS4.DEFAULT_RESPONDER_URL,
                                                                                      SOAP_12_PARTY_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final Ebms3UserMessage aEbms3UserMessage = new Ebms3UserMessage ();
    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Now send receipt
    final Document aDoc = MockMessages.createReceiptMessage (ESoapVersion.AS4_DEFAULT, aEbms3UserMessage, null)
                                      .getAsSoapDocument ();

    // We've got our response
    sendPlainMessage (new HttpXMLEntity (aDoc, SOAP_VERSION.getMimeType ()), true, null);
  }
}
