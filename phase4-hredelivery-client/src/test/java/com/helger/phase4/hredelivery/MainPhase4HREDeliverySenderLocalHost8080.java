/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery;

import java.io.File;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.base.debug.GlobalDebug;
import com.helger.hredelivery.commons.security.HREDeliveryTrustedCA;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4HREDeliverySenderLocalHost8080
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4HREDeliverySenderLocalHost8080.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Required for "http" only connections
    GlobalDebug.setDebugModeDirect (true);

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4HREdeliverySender.IF.createParticipantIdentifierWithDefaultScheme ("9934:87550089326");
      final EAS4UserMessageSendResult eResult = Phase4HREdeliverySender.builder ()
                                                                       .apCAChecker (HREDeliveryTrustedCA.hrEdeliveryFinaDemo ())
                                                                       .documentTypeID (Phase4HREdeliverySender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:mfin.gov.hr:cius-2025:1.0#conformant#urn:mfin.gov.hr:ext-2025:1.0::2.1"))
                                                                       .processID (Phase4HREdeliverySender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:eracun.hr:poacc:en16931:any"))
                                                                       .senderParticipantID (Phase4HREdeliverySender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                       .receiverParticipantID (aReceiverID)
                                                                       .senderPartyID ("POP000306")
                                                                       .payload (aPayloadElement)
                                                                       .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\n" +
                                                                                                                                              "MIIG1jCCBL6gAwIBAgIRAJ+/3S/qnzEAUbpIbmHvrwcwDQYJKoZIhvcNAQELBQAw\n" +
                                                                                                                                              "SDELMAkGA1UEBhMCSFIxHTAbBgNVBAoTFEZpbmFuY2lqc2thIGFnZW5jaWphMRow\n" +
                                                                                                                                              "GAYDVQQDExFGaW5hIERlbW8gQ0EgMjAyMDAeFw0yNTEwMzExMDI4MTRaFw0zMDA3\n" +
                                                                                                                                              "MzExMjMwMThaMHUxCzAJBgNVBAYTAkhSMRMwEQYDVQQKEwpPUFVTQ0FQSVRBMRYw\n" +
                                                                                                                                              "FAYDVQRhEw1IUjUyNDI0OTA5MjAyMQ4wDAYDVQQHEwVFU1BPTzEpMCcGA1UEAxMg\n" +
                                                                                                                                              "T3B1c0NhcGl0YSBCdXNpbmVzcyBOZXR3b3JrLXRlc3QwggEiMA0GCSqGSIb3DQEB\n" +
                                                                                                                                              "AQUAA4IBDwAwggEKAoIBAQC2GWaTK8HccJLWyOJG5s9AL7fUS3LGGRXYCxXZS8Zn\n" +
                                                                                                                                              "5iPY5aKMuHBRNQ0pAb0sIjN1mQosRGs+t6ihKDZ1MS017LLpBFhskoNosIC5I8Iy\n" +
                                                                                                                                              "BsFfy3UrPKkVH0YXshcBd4dSua8ZD8l0QwN8JZovzV0bhmrZT0H/1behTzOgdirf\n" +
                                                                                                                                              "ep/WXT/4Q1Fmgy1QiT7D3aLO7SNuM02k8I1pWMBzKZr0G7wNsPBI3mLObbns2rpW\n" +
                                                                                                                                              "+KPon/jD3XpYPFEycaxp4jbwYL7oDyQLLZkGvKya3SgXufuwyt5JPXohLqOqPgGQ\n" +
                                                                                                                                              "0/oDv0ilt0hyZQX7esoiFBuYok6+SCjxLfc74XzYJSmzAgMBAAGjggKMMIICiDAO\n" +
                                                                                                                                              "BgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwQGCCsGAQUFBwMCMIGs\n" +
                                                                                                                                              "BgNVHSAEgaQwgaEwgZQGCSt8iFAFIQ8DATCBhjBBBggrBgEFBQcCARY1aHR0cDov\n" +
                                                                                                                                              "L2RlbW8tcGtpLmZpbmEuaHIvY3BzL2Nwc25xY2RlbW8yMDE0djItMC1oci5wZGYw\n" +
                                                                                                                                              "QQYIKwYBBQUHAgEWNWh0dHA6Ly9kZW1vLXBraS5maW5hLmhyL2Nwcy9jcHNucWNk\n" +
                                                                                                                                              "ZW1vMjAxNHYyLTAtZW4ucGRmMAgGBgQAj3oBATB9BggrBgEFBQcBAQRxMG8wKAYI\n" +
                                                                                                                                              "KwYBBQUHMAGGHGh0dHA6Ly9kZW1vMjAxNC1vY3NwLmZpbmEuaHIwQwYIKwYBBQUH\n" +
                                                                                                                                              "MAKGN2h0dHA6Ly9kZW1vLXBraS5maW5hLmhyL2NlcnRpZmlrYXRpL2RlbW8yMDIw\n" +
                                                                                                                                              "X3N1Yl9jYS5jZXIwJwYDVR0RBCAwHoEca2F0aS5rYWl2YWFyYUBvcHVzY2FwaXRh\n" +
                                                                                                                                              "LmNvbTCBtAYDVR0fBIGsMIGpMIGmoIGjoIGghihodHRwOi8vZGVtby1wa2kuZmlu\n" +
                                                                                                                                              "YS5oci9jcmwvZGVtbzIwMjAuY3JshnRsZGFwOi8vZGVtby1sZGFwLmZpbmEuaHIv\n" +
                                                                                                                                              "Y249RmluYSUyMERlbW8lMjBDQSUyMDIwMjAsbz1GaW5hbmNpanNrYSUyMGFnZW5j\n" +
                                                                                                                                              "aWphLGM9SFI/Y2VydGlmaWNhdGVSZXZvY2F0aW9uTGlzdCUzQmJpbmFyeTAfBgNV\n" +
                                                                                                                                              "HSMEGDAWgBSVr1LVwunXN4Q+bkmPyR/qXCtfzzAdBgNVHQ4EFgQUqKuAUN9+zvY9\n" +
                                                                                                                                              "VSRFkYA18DlUiAMwCQYDVR0TBAIwADANBgkqhkiG9w0BAQsFAAOCAgEAiHSUPJsk\n" +
                                                                                                                                              "vwC0CSrcgVM+dVUGiRaoTcf/z/vtT3Ro3KKGLqiiR7zBgJBiC+Zu4uBdlhfZ7EgE\n" +
                                                                                                                                              "yF4u8IGeTRwyXxa/gfGJd9Ve0S8ykT3CYoh/1UaeXQ/ftQ+H/4P8C0njKvPWp4MP\n" +
                                                                                                                                              "ymw+jffOUGn4M1ut7QjMaXqQ3Z2cGLOK5bFHfNPUGl3CSte4G6CE0J6uRydfqz5+\n" +
                                                                                                                                              "F0A5sIY+bKXQrw/AOq9T0zsjjwosz5oltW/45bmTX/JSJY3Z1jRUwD4x+76R4bXu\n" +
                                                                                                                                              "9DREyEj0GJKzj0hCYaw8qUVvXTz5MExbElUxH4Imhn2IC3g1N+uJZotNgTxgqHJ3\n" +
                                                                                                                                              "hj5DlD2cDt1zMy1WLecAnI3JDPyd6FHF2qI8jsCamwbO6OIApTJwyvVnGxkYzT6H\n" +
                                                                                                                                              "kN8cViACc9YURrAUP8HMQ92lcWGLAH/cgj6BqdndHdWayz6AGe136nGVdPiOHrPp\n" +
                                                                                                                                              "Zh6T/9XoZHJvFRp3uT34QeSBpatZFtWKdsUPC4CYzPGlE1UxW9T776BaziiDoAGT\n" +
                                                                                                                                              "ASj6h3TSDXr9NehfxL5paVa9WRsU+A/v4DdVOhPNda2ClGMPVwUGz9krOatXULTH\n" +
                                                                                                                                              "cBXMbhel+hwMxaAv/l8LcN/ftOgzpf+3rcK/MJTLM/oCT/YRpC10piXqvFyUAGf+\n" +
                                                                                                                                              "785O5I8Zz8hZTkoJHPL6sy1n4qu4pj0FIzo=\n" +
                                                                                                                                              "-----END CERTIFICATE-----\n" +
                                                                                                                                              ""),
                                                                                                 "http://localhost:8080/as4")
                                                                       .sendMessageAndCheckForReceipt ();
      LOGGER.info ("HR eDelivery send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending HR eDelivery message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
