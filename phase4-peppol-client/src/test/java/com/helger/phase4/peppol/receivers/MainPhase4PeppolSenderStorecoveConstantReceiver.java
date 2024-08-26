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

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver for Storecove [NL]. This one
 * skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderStorecoveConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderStorecoveConstantReceiver.class);

  public static void send ()
  {
    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0106:sc998899889");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIF0TCCA7mgAwIBAgIQbXAEkESKryKJazdxZUo39zANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMTgxMDE3MDAwMDAwWhcNMjAxMDA2MjM1OTU5WjBeMRIw\r\n" +
                                                                                                         "EAYDVQQDDAlQTkwwMDAxNTExFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMSIwIAYD\r\n" +
                                                                                                         "VQQKDBlTdG9yZWNvdmUgKERhdGFqdXN0IEIuVi4pMQswCQYDVQQGEwJOTDCCASIw\r\n" +
                                                                                                         "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKj00j0ybln4DoN2dyDaLFzKztmr\r\n" +
                                                                                                         "f772Oq1DuPuk/D590Duci1eQZYAGvm58s19aNS+lVwombpEA7luxjiX+UfAQ0EN7\r\n" +
                                                                                                         "Ih3qaAZ5eZOPQh+1zr1dWArFYK/0yn0PwhGVXCLuyWTFxQrmitwSjuHIjUKDxuC8\r\n" +
                                                                                                         "4vzvNyxZzADUqGQMoafDE9IZsRUMY7DAlqaFE3NLa8riCmWIalmwhDDmlxu09nqc\r\n" +
                                                                                                         "ot1uk3n1sCF/vpvzkdjuVUn4oiHG1rkBpJVr7UjQx6nbIuVVENGrJ+HT0CG1exMS\r\n" +
                                                                                                         "Yo+onqrRjWyad+S6HYcO6tIRx1pl7wyk8a9z2um/G9ipwNlxGXFf7KlSh58CAwEA\r\n" +
                                                                                                         "AaOCAXwwggF4MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB\r\n" +
                                                                                                         "/wQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBR2wGGZLQlLSW5S2lwZEs0ydisI1zBd\r\n" +
                                                                                                         "BgNVHR8EVjBUMFKgUKBOhkxodHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82\r\n" +
                                                                                                         "YTkzNzczNGEzOTNhMDgwNWJmMzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcG\r\n" +
                                                                                                         "CCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1\r\n" +
                                                                                                         "dGguY29tMB8GA1UdIwQYMBaAFGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG\r\n" +
                                                                                                         "+EUBEAMEHzAdBhNghkgBhvhFARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4\r\n" +
                                                                                                         "RQEQBQQrMCkCAQAWJGFIUjBjSE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIw\r\n" +
                                                                                                         "PTANBgkqhkiG9w0BAQsFAAOCAgEABsDkoUebHcy/cEeGYV3OR6iPIzGSjz2d3chY\r\n" +
                                                                                                         "D30wtt5jPS1RAJ0/Mh2v7hoVMMVxj2vSC/SOirrk0zAf22R3oCmMBnLt/Q9bMaav\r\n" +
                                                                                                         "L1XvFq2fUTZbcoa9zQHwITbtWY+YV/ArqutuTqloKYwNyieh3YmqiDMm99IjAYyk\r\n" +
                                                                                                         "LB2TnlxSm5McNlmkOsvYu/VxUDqM4aejyiTFIbCGM+k2HpsNlOi67pUbg5wK/2PU\r\n" +
                                                                                                         "AUcTv3PXbGlUSmjXDdmQw5EGn4CYt/+oRjMwzpg7DejCCnGuGnggwWCE/uh4+bpE\r\n" +
                                                                                                         "HXfc2QE+SB9UlHNjyxpUWQMxWCGsGDr8Pm3Ncm88XmBd0v6+YytYiV8/hhn63KJt\r\n" +
                                                                                                         "ycRoP7a79T3kILWm1CyCjDwVH9Pnkqot4lQZow1BfwoL/AMTbkKDsi/GiN3yq9NN\r\n" +
                                                                                                         "LVjh2ww1kEs3ti5s01nuaTnywvCBf5fqFXZyVtevn7T+ye8df/LfrjXKeo3i45PY\r\n" +
                                                                                                         "3DVW1F06BGHGQa9IMF4GNHX94BRgee8jmIbHmusxP3ynXi0zr7lmk3tyWXUIp581\r\n" +
                                                                                                         "NP9BV7SyeG0ToBRiSoBUUUDP2Tqesqgj84+qRGC7uNMUOkU3gRNrJcBDaNr+cQMA\r\n" +
                                                                                                         "GXjzBQuF1wdECMQAHNTrwzHziw4S/ZMf5KELc1ezYF61NAfCb8voMzFCJn4n1XK+\r\n" +
                                                                                                         "hBQpCt8=\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            false ? "https://o96st7ob6h.execute-api.eu-west-1.amazonaws.com/prod/as4"
                                                                  : "https://accap.mypeppol.app/as4")
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
  }

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      send ();
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
