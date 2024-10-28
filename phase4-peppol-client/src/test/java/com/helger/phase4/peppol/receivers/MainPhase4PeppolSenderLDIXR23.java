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
package com.helger.phase4.peppol.receivers;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.timing.StopWatch;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Example for sending something to the Dataport [DE] test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLDIXR23
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderLDIXR23.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/xrechnung-2.3.1-ubl-inv.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      final String sReceiverID = "0204:07-49849849499-27";
      final StopWatch aSW = StopWatch.createdStarted ();
      final ICommonsMap <String, String> aRcvToMsgIDMap = new CommonsLinkedHashMap <> ();
      try
      {
        // Start configuring here
        final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (sReceiverID);
        final IAS4ClientBuildMessageCallback aBuildMessageCallback = new IAS4ClientBuildMessageCallback ()
        {
          public void onAS4Message (final AbstractAS4Message <?> aMsg)
          {
            final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
            LOGGER.info ("Sending out AS4 message with message ID '" +
                         aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId () +
                         "'");
            LOGGER.info ("Sending out AS4 message with conversation ID '" +
                         aUserMsg.getEbms3UserMessage ().getCollaborationInfo ().getConversationId () +
                         "'");
          }
        };
        final EAS4UserMessageSendResult eResult = Phase4PeppolSender.builder ()
                                                                    .documentTypeID (EPredefinedDocumentTypeIdentifier.XRECHNUNG_INVOICE_UBL_V23)
                                                                    .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                                                    .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                    .receiverParticipantID (aReceiverID)
                                                                    .senderPartyID ("POP000306")
                                                                    .countryC1 ("AT")
                                                                    .payload (aPayloadElement)
                                                                    .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                                                       aReceiverID,
                                                                                                       ESML.DIGIT_TEST))
                                                                    .buildMessageCallback (aBuildMessageCallback)
                                                                    .sendMessageAndCheckForReceipt ();
        LOGGER.info ("Peppol send result: " + eResult);
        if (!aRcvToMsgIDMap.containsKey (sReceiverID))
          aRcvToMsgIDMap.put (sReceiverID, "Result: " + eResult);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        if (!aRcvToMsgIDMap.containsKey (sReceiverID))
          aRcvToMsgIDMap.put (sReceiverID, "Exception: " + ex.getMessage ());
      }
      aSW.stop ();

      LOGGER.info ("Took " + aSW.getDuration ());
      for (final var e : aRcvToMsgIDMap.entrySet ())
        LOGGER.info (e.getKey () + " - " + e.getValue ());
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
