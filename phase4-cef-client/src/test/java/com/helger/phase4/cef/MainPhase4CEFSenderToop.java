/*
 * Copyright (C) 2020-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.cef;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Example for sending something to the TOOP test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4CEFSenderToop
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4CEFSenderToop.class);
  private static final ISMLInfo SML_TOOP = new SMLInfo ("tooptest",
                                                        "SMK TOOP",
                                                        "toop.acc.edelivery.tech.ec.europa.eu.",
                                                        "https://acc.edelivery.tech.ec.europa.eu/edelivery-sml",
                                                        true);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      final byte [] aPayloadBytes = SimpleFileIO.getAllFileBytes (new File ("src/test/resources/external/examples/base-example.xml"));
      if (aPayloadBytes == null)
        throw new IllegalStateException ();

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4CEFSender.IF.createParticipantIdentifier ("iso6523-actorid-upis",
                                                                                                 "9915:tooptest");
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
      if (Phase4CEFSender.builder ()
                         .documentTypeID (Phase4CEFSender.IF.createDocumentTypeIdentifier ("toop-doctypeid-qns",
                                                                                           "urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.registeredorganization::1.40"))
                         .processID (Phase4CEFSender.IF.createProcessIdentifier ("toop-procid-agreement",
                                                                                 "urn:eu.toop.process.datarequestresponse"))
                         .senderParticipantID (Phase4CEFSender.IF.createParticipantIdentifier ("iso6523-actorid-upis",
                                                                                               "9914:phase4-test-sender"))
                         .receiverParticipantID (aReceiverID)
                         .fromPartyID (new SimpleParticipantIdentifier ("type", "POP000306"))
                         .fromRole ("http://www.toop.eu/edelivery/gateway")
                         .toPartyID (new SimpleParticipantIdentifier ("type", "POP000306"))
                         .toRole ("http://www.toop.eu/edelivery/gateway")
                         .payload (AS4OutgoingAttachment.builder ().data (aPayloadBytes).mimeTypeXML ())
                         .smpClient (new BDXRClientReadOnly (Phase4CEFSender.URL_PROVIDER, aReceiverID, SML_TOOP))
                         .buildMessageCallback (aBuildMessageCallback)
                         .sendMessage ()
                         .isSuccess ())
      {
        LOGGER.info ("Successfully sent CEF message via AS4");
      }
      else
      {
        LOGGER.error ("Failed to send CEF message via AS4");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending CEF message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
