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
import com.helger.phase4.peppol.Phase4PeppolValidatonResultHandler;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phive.peppol.PeppolValidationBisSG;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderConstantReceiverSG2
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderConstantReceiverSG2.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/Singapore invoice valid 1.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0192:810418052");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#conformant#urn:fdc:peppol.eu:2017:poacc:billing:international:sg:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\n" +
                                                                                                         "MIIF2zCCA8OgAwIBAgIQDyXLqdKdJNF+okAUGXN3izANBgkqhkiG9w0BAQsFADBr\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjAwMjE3MDAwMDAwWhcNMjIwMjA2MjM1OTU5WjBoMRIw\n" +
                                                                                                         "EAYDVQQDDAlQU0cwMDAzNzIxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMSwwKgYD\n" +
                                                                                                         "VQQKDCNPdmVyc2VhLUNoaW5lc2UgQmFua2luZyBDb3Jwb3JhdGlvbjELMAkGA1UE\n" +
                                                                                                         "BhMCU0cwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDOCLZbXTlMNHwi\n" +
                                                                                                         "1zFQRL4WjenhuLRg3LSYm3QWiiA9qGWn8zxx+cOjY+Ftal6xZXcXaZVB6rCse3aT\n" +
                                                                                                         "dcCov5wEeB9ZZiQKjzkprmXHIYrLpGy57uPUd8YU81MBmRRo8krU/zsZUS3s/8Dp\n" +
                                                                                                         "VLxUMNqci09j9bV9vzSQHO4cA+wyi9+NsSmglKVK0KaW+1TA6WwpMgtT0DowbR+I\n" +
                                                                                                         "MPNqdc/EviLTDHH3WJR8Fdy9D7ew4+9nQynR5m1HrDSr/v7jGHuZntNbnhmmDl9Z\n" +
                                                                                                         "4O+XvXznKFHvmgNy8ODOXI4uLhVgwzQY7gWfggs/Y8P/L/KfB/9AuFcn89dPnpu7\n" +
                                                                                                         "w0zURag9AgMBAAGjggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwID\n" +
                                                                                                         "qDAWBgNVHSUBAf8EDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQUoJ6VTF/oZIdteq2w\n" +
                                                                                                         "NUobO/nfV2YwXQYDVR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0\n" +
                                                                                                         "aC5jb20vY2FfNmE5Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0\n" +
                                                                                                         "Q1JMLmNybDA3BggrBgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2kt\n" +
                                                                                                         "b2NzcC5zeW1hdXRoLmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo3\n" +
                                                                                                         "6zAtBgpghkgBhvhFARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4\n" +
                                                                                                         "MDkGCmCGSAGG+EUBEAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZ\n" +
                                                                                                         "WFYwYUM1amIyMD0wDQYJKoZIhvcNAQELBQADggIBAE4naY2C13P4CYR+R3sDwBDU\n" +
                                                                                                         "JNG6TnFMojGvRXIXSdMFbUotGnED8nhA8q8fS3HIRlXDBnBODODuQBmTPj+zjR2U\n" +
                                                                                                         "6NFa+ji0mH5obimY/DhB3XeGI3wwxZEr/gi2dDjpdIRpAfwvoIo2E/JczXacNk9C\n" +
                                                                                                         "Koa5LRpSGLXAwtuPvrDkvcdGYZG5IxldnTcVpan8jU92OjEg8QNWYvtuYTygVYBU\n" +
                                                                                                         "itFt0vTNxs5+TivDFtOf+SjGmNzKtlFhb5ITM0QWARuTHmBYy5fUZfbKWvTWcOZl\n" +
                                                                                                         "ieuYT+s1o7tIkLJ62Y7/Qn3xWfVfDEBD40bYVCBQWHv8IGHQUDC/ZdMoeM1Yx3tm\n" +
                                                                                                         "POveFZodbbI8SjTrpJuGO8nZ/f6dXG4JUXOSs9lR1B17T5FnprQn+B9xiFuVU6mb\n" +
                                                                                                         "rxoXT7hlt1s+ntFXB8MecZk+a4lprjBumZRqwkQmFjMDZaATZs9hKANstda1qPrz\n" +
                                                                                                         "RxtRrgzU1dZfUkDNxfEauLug9VPl64UvNgWIhsSfLbPj+AkFn9rXXg2PmeKx7AEL\n" +
                                                                                                         "gXFSqahkh+96DcKlIc49Os2ew16iJEC+X+kMsMxjZXjkQoMxI1LF7IhGABQctof2\n" +
                                                                                                         "QsSm4/2kuGIsWKRy+kf7s7xJT7olo1hBQQzNf1J1GN4WbXVWtkxD1jyXML+HnYfh\n" +
                                                                                                         "47A9zzduKT3CycRoqxaB\n" +
                                                                                                         "-----END CERTIFICATE-----"),
                                                            "https://dev.einvoicing.i-portal.biz/msh")
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                  .validationConfiguration (PeppolValidationBisSG.VID_OPENPEPPOL_BIS3_SG_UBL_INVOICE_2023_7,
                                                            new Phase4PeppolValidatonResultHandler ())
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
