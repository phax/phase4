/*
 * Copyright (C) 2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.dbna.EDBNASML;
import com.helger.smpclient.url.DBNAURLProviderSMP;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

public class MainPhase4DBNAllianceSenderExample
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4DBNAllianceSenderExample.class);

  public static void main (final String [] args)
  {
    // Create scope for global variables that can be shut down gracefully
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Optional dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      // Read XML payload to send
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read file to be send");

      // Start configuring here
      final BDXR2ParticipantIdentifier aReceiver = Phase4DBNAllianceSender.IF.createParticipantIdentifier ("us:ein",
                                                                                                           "365060483");
      final BDXR2ClientReadOnly aSMPClient = new BDXR2ClientReadOnly (DBNAURLProviderSMP.INSTANCE.getSMPURIOfParticipant (aReceiver,
                                                                                                                          EDBNASML.TEST.getZoneName ()));
      aSMPClient.setVerifySignature (false);

      final EAS4UserMessageSendResult eResult;
      eResult = Phase4DBNAllianceSender.builder ()
                                       .documentTypeID (SimpleIdentifierFactory.INSTANCE.createDocumentTypeIdentifier ("bdx-docid-qns",
                                                                                                                       "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##DBNAlliance-1.0-data-Core"))
                                       .processID (Phase4DBNAllianceSender.IF.createProcessIdentifier (null,
                                                                                                       "bdx:noprocess"))
                                       .senderParticipantID (Phase4DBNAllianceSender.IF.createParticipantIdentifier ("us:ein",
                                                                                                                     "365060483"))
                                       .receiverParticipantID (aReceiver)
                                       .fromPartyID ("365060483")
                                       .payloadElement (aPayloadElement)
                                       .smpClient (aSMPClient)
                                       .sendMessageAndCheckForReceipt ();
      LOGGER.info ("DBNAlliance send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending BDEW message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
