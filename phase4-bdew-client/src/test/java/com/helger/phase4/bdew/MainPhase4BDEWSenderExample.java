/*
 * Copyright (C) 2023-2025 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.bdew;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.bdew.Phase4BDEWSender.BDEWPayloadParams;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

public class MainPhase4BDEWSenderExample
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4BDEWSenderExample.class);

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

      final BDEWPayloadParams aBDEWPayloadParams = new BDEWPayloadParams ();
      aBDEWPayloadParams.setDocumentType ("DT1");
      aBDEWPayloadParams.setDocumentDate (PDTFactory.getCurrentLocalDate ());
      aBDEWPayloadParams.setDocumentNumber (1234);
      aBDEWPayloadParams.setFulfillmentDate (PDTFactory.getCurrentLocalDate ().minusMonths (2));
      aBDEWPayloadParams.setSubjectPartyId ("Party1");
      aBDEWPayloadParams.setSubjectPartyRole ("Role1");

      final Wrapper <Ebms3SignalMessage> aSignalMsgHolder = new Wrapper <> ();

      // Start configuring here
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4BDEWSender.builder ()
                                .encryptionKeyIdentifierType (ECryptoKeyIdentifierType.X509_KEY_IDENTIFIER)
                                .signingKeyIdentifierType (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE)
                                .fromPartyID ("AS4-Sender")
                                .fromRole (CAS4.DEFAULT_INITIATOR_URL)
                                .toPartyID ("AS4-Receiver")
                                .toRole (CAS4.DEFAULT_RESPONDER_URL)
                                .endpointURL ("https://receiver.example.org/bdew/as4")
                                .service ("AS4-Service")
                                .action ("AS4-Action")
                                .payload (AS4OutgoingAttachment.builder ()
                                                               .data (aPayloadBytes)
                                                               .compressionGZIP ()
                                                               .mimeTypeXML ()
                                                               .charset (StandardCharsets.UTF_8), aBDEWPayloadParams)
                                .signalMsgConsumer ( (aSignalMsg, aMMD, aState) -> aSignalMsgHolder.set (aSignalMsg))
                                .sendMessageAndCheckForReceipt ();
      LOGGER.info ("BDEW send result: " + eResult);
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
