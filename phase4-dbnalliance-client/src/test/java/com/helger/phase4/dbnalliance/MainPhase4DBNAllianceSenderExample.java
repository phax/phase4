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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

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
      final byte [] aPayloadBytes = Files.readAllBytes (new File ("src/test/resources/external/examples/base-example.xml").toPath ());
      if (aPayloadBytes == null)
        throw new IllegalStateException ("Failed to read file to be send");

      // Start configuring here
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4DBNAllianceSender.builder ()
                                       .fromPartyID ("AS4-Sender")
                                       .toPartyID ("AS4-Receiver")
                                       .endpointURL ("https://receiver.example.org/dbna/as4")
                                       .service ("AS4-Service")
                                       .action ("AS4-Action")
                                       .payload (AS4OutgoingAttachment.builder ()
                                                                      .data (aPayloadBytes)
                                                                      .compressionGZIP ()
                                                                      .mimeTypeXML ()
                                                                      .charset (StandardCharsets.UTF_8))
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
