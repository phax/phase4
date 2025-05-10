/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mime.CMimeType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLocalHost8080PDF
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
                                                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                                                  .documentTypeID (aDocTypeID)
                                                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:peppol:france:billing:regulated"))
                                                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID ("POP000306")
                                                                  .countryC1 ("AT")
                                                                  .payloadBinaryContent (aPDFbytes,
                                                                                         CMimeType.APPLICATION_PDF,
                                                                                         null)
                                                                  .sbdhStandard ("urn:peppol:doctype:pdf+xml")
                                                                  .sbdhTypeVersion ("0")
                                                                  .sbdhType ("Invoice")
                                                                  .sbdDocumentConsumer (x -> LOGGER.info (new SBDMarshaller ().setFormattedOutput (true)
                                                                                                                              .getAsString (x)))
                                                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\n" +
                                                                                                                                         "MIIF0TCCA7mgAwIBAgIQcyxSArntXaqTdp2N6B0D2DANBgkqhkiG9w0BAQsFADBr\n" +
                                                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\n" +
                                                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\n" +
                                                                                                                                         "RVNUIENBIC0gRzIwHhcNMjUwMjI1MDAwMDAwWhcNMjcwMjE1MjM1OTU5WjBeMQsw\n" +
                                                                                                                                         "CQYDVQQGEwJBVDEiMCAGA1UECgwZSGVsZ2VyIElUIENvbnN1bHRpbmcgR21iSDEX\n" +
                                                                                                                                         "MBUGA1UECwwOUEVQUE9MIFRFU1QgQVAxEjAQBgNVBAMMCVBPUDAwMDMwNjCCASIw\n" +
                                                                                                                                         "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK7YW/30et1kZK/l3hKrxJEr0NkC\n" +
                                                                                                                                         "f/mzjkQPUh8jKyd4YZrgiod/Ry/xnp2eHcHV2Aiukk9kMkg8Ptf5W8jMgvlKeN58\n" +
                                                                                                                                         "dHp890vupeh4iOPdq0sJ9B3HJhXQHgxhe90CZIsJi8fn7fFawMHPuVDmwvrnzYWl\n" +
                                                                                                                                         "c0qF/xXFqM/NwBWiqKikp5lvVvZUehzJiRmEY0c1uFoXZClqUmcmmWGOBWzj8nW6\n" +
                                                                                                                                         "IeIsZ9GurNG+9zlT6L3JRJoJCluzTjjbk4XKqEQFiP4aiDAa1nuIzMea3DkB2nx4\n" +
                                                                                                                                         "0L8TwZEO2d8Xecr3xTfkyq92eHyStyIlEW1459bOSa56Yp6Mlu7JFKmTgLkCAwEA\n" +
                                                                                                                                         "AaOCAXwwggF4MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB\n" +
                                                                                                                                         "/wQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBSBn6GQ+OAyiDWmqpU18M9DwzYRwTBd\n" +
                                                                                                                                         "BgNVHR8EVjBUMFKgUKBOhkxodHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82\n" +
                                                                                                                                         "YTkzNzczNGEzOTNhMDgwNWJmMzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcG\n" +
                                                                                                                                         "CCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1\n" +
                                                                                                                                         "dGguY29tMB8GA1UdIwQYMBaAFGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG\n" +
                                                                                                                                         "+EUBEAMEHzAdBhNghkgBhvhFARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4\n" +
                                                                                                                                         "RQEQBQQrMCkCAQAWJGFIUjBjSE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIw\n" +
                                                                                                                                         "PTANBgkqhkiG9w0BAQsFAAOCAgEAIWvpuipkFN2cSIIntNeoKfne7q9dFzJIqVTa\n" +
                                                                                                                                         "y7ZeODtcoNqEsawMzGrAAgOzyzudq+rdF0FMaywTHHvtPfHWuK96UZVIPZs1CFcO\n" +
                                                                                                                                         "lKYkQD0k47YxHc9VJUwoCB4PLgQk5pqdfbIigLd5oFXZmgI786Pkouu0LBHsH0Im\n" +
                                                                                                                                         "2OPH6a9EdFBECBYnS+w2PTycF/mxEru0btz4i8ZIOj2pHRBAoBCItykIJwTbqknH\n" +
                                                                                                                                         "H8CAm2mmEnnSxyE1qDui++c811Qn8H0NtQg9x2E57XkNQTrEDMOgNw5dyrp5izUm\n" +
                                                                                                                                         "vNdLfxFvPmvadNWVRq52MD23jU2QM1byWoYyBynlvxII829ZshjUGlycnpc7NyuQ\n" +
                                                                                                                                         "VbbPlb9Ku07ILaBDrI7qXJ3+Y8x7HOJ9qmX9nK4s5smWx6tsOUov9ZYlMvAqKipk\n" +
                                                                                                                                         "ycBe6fTVwylzfZWNKPw/6hqMM0vOz49Yv5yxUvGHEvjyTUBrEgLJytrP4jvlvY1S\n" +
                                                                                                                                         "ORISzCOn5IgZuHXYzTfd03+Z+uc0VgQHjyfNlTx/tMmrA4gCp4J6+G/mo5XXlSWF\n" +
                                                                                                                                         "fAExypSh97GCTm+BKN5KkpyR7+1WevpyFEKK0ug+9Dr8KkSKnGSuVJ7XVhDbV5oP\n" +
                                                                                                                                         "eTOT4HmMktEynedS61JX9We6Ilex07ak4tYouBdjwfyQmOJIgTrtchKmi5okoZeF\n" +
                                                                                                                                         "ZShLhVM=\n" +
                                                                                                                                         "-----END CERTIFICATE-----\n" +
                                                                                                                                         ""),
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
