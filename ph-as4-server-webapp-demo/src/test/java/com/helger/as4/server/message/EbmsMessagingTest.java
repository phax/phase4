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
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3From;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3PartyId;
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
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class EbmsMessagingTest extends AbstractUserMessageTestSetUp
{

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

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging));
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void onlyEbmsMessagingTest () throws Exception
  {
    final Ebms3Messaging aEbms3Messaging = new Ebms3Messaging ();

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging));
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

    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    sPModeID = MockEbmsHelper.SOAP_12_PARTY_ID + "-" + MockEbmsHelper.SOAP_12_PARTY_ID;

    aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo (AS4TestConstants.TEST_ACTION,
                                                                              AS4TestConstants.TEST_SERVICE_TYPE,
                                                                              AS4TestConstants.TEST_SERVICE,
                                                                              AS4TestConstants.TEST_CONVERSATION_ID,
                                                                              sPModeID,
                                                                              MockEbmsHelper.DEFAULT_AGREEMENT);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();

    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_SENDER_URL);
    {
      final Ebms3PartyId aEbms3PartyId = new Ebms3PartyId ();
      aEbms3PartyId.setValue (MockEbmsHelper.SOAP_12_PARTY_ID);
      aEbms3From.addPartyId (aEbms3PartyId);

      final Ebms3PartyId aEbms3PartyId2 = new Ebms3PartyId ();
      aEbms3PartyId2.setValue (MockEbmsHelper.SOAP_12_PARTY_ID);
      aEbms3From.addPartyId (aEbms3PartyId2);
    }
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    {
      final Ebms3PartyId aEbms3PartyId = new Ebms3PartyId ();
      aEbms3PartyId.setValue (MockEbmsHelper.SOAP_12_PARTY_ID);
      aEbms3To.addPartyId (aEbms3PartyId);
    }
    aEbms3PartyInfo.setTo (aEbms3To);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    aEbms3Messaging.addUserMessage (aEbms3UserMessage);

    final HttpEntity aEntity = new HttpXMLEntity (_getMessagingAsDocument (aEbms3Messaging));
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
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    sPModeID = MockEbmsHelper.SOAP_12_PARTY_ID + "-" + MockEbmsHelper.SOAP_12_PARTY_ID;
    aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo (AS4TestConstants.TEST_ACTION,
                                                                              AS4TestConstants.TEST_SERVICE_TYPE,
                                                                              AS4TestConstants.TEST_SERVICE,
                                                                              AS4TestConstants.TEST_CONVERSATION_ID,
                                                                              sPModeID,
                                                                              MockEbmsHelper.DEFAULT_AGREEMENT);

    final Ebms3PartyInfo aEbms3PartyInfo = new Ebms3PartyInfo ();
    // From => Sender
    final Ebms3From aEbms3From = new Ebms3From ();
    aEbms3From.setRole (CAS4.DEFAULT_SENDER_URL);
    {
      final Ebms3PartyId aEbms3PartyId = new Ebms3PartyId ();
      aEbms3PartyId.setValue (MockEbmsHelper.SOAP_12_PARTY_ID);
      aEbms3From.addPartyId (aEbms3PartyId);
    }
    aEbms3PartyInfo.setFrom (aEbms3From);

    // To => Receiver
    final Ebms3To aEbms3To = new Ebms3To ();
    aEbms3To.setRole (CAS4.DEFAULT_RESPONDER_URL);
    {
      final Ebms3PartyId aEbms3PartyId = new Ebms3PartyId ();
      aEbms3PartyId.setValue (MockEbmsHelper.SOAP_12_PARTY_ID);
      aEbms3To.addPartyId (aEbms3PartyId);
    }
    aEbms3PartyInfo.setTo (aEbms3To);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);

    aEbms3UserMessage.setPartyInfo (aEbms3PartyInfo);
    aEbms3UserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aEbms3UserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Now send receipt
    final Document aDoc = MockMessages.testReceiptMessage (ESOAPVersion.AS4_DEFAULT, aEbms3UserMessage, null);

    // We've got our response
    sendPlainMessage (new HttpXMLEntity (aDoc), true, null);
  }
}
