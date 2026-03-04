/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.io.File;

import org.slf4j.Logger;

import com.helger.base.debug.GlobalDebug;
import com.helger.io.file.SimpleFileIO;
import com.helger.mime.CMimeType;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.sbdh.SBDMarshaller;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLocalHost8080PDF extends AbstractPhase4Sender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderLocalHost8080PDF.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Required for "http" only connections
    GlobalDebug.setDebugModeDirect (true);

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    Phase4LogCustomizer.setThreadLocalLogPrefix ("[SendPDFLocalHost8080] ");

    try
    {
      final byte [] aPDFbytes = SimpleFileIO.getAllFileBytes (new File ("src/test/resources/external/examples/factur-x/EN16931_Einfach.pdf"));
      if (aPDFbytes == null)
        throw new IllegalStateException ("Failed to read PDF bytes  to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final PeppolDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.urn_peppol_doctype_pdf_xml__urn_cen_eu_en16931_2017_conformant_urn_peppol_france_billing_Factur_X_1_0__D22B.getAsDocumentTypeIdentifier ();
      final EAS4UserMessageSendResult eResult = Phase4PeppolSender.builder ()
                                                                  .peppolAP_CAChecker (PeppolTrustedCA.peppolTestAP ())
                                                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                                                  .documentTypeID (aDocTypeID)
                                                                  .processID (EPredefinedProcessIdentifier.urn_peppol_france_billing_regulated)
                                                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID ("POP000306")
                                                                  .countryC1 ("AT")
                                                                  .payloadBinaryContent (aPDFbytes,
                                                                                         CMimeType.APPLICATION_PDF,
                                                                                         null)
                                                                  .sbdhStandard ("urn:peppol:doctype:pdf+xml")
                                                                  .sbdhTypeVersion ("0")
                                                                  .sbdhType ("factur-x")
                                                                  .sbdDocumentConsumer (x -> LOGGER.info (new SBDMarshaller ().setFormattedOutput (true)
                                                                                                                              .getAsString (x)))
                                                                  .receiverEndpointDetails (pem2certPHG3 (),
                                                                                            "http://localhost:8080/as4")
                                                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
