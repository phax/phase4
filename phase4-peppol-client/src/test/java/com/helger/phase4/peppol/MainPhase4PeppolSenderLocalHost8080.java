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
package com.helger.phase4.peppol;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.debug.GlobalDebug;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLocalHost8080
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderLocalHost8080.class);

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
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/" +
                                                                      (true ? "base-example.xml"
                                                                            : "large-files/base-example-large-100m.xml")))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIF2DCCA8CgAwIBAgIQBcAefsdQIk7jCCLVCAicizANBgkqhkiG9w0BAQsFADBr MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU RVNUIENBIC0gRzIwHhcNMjEwNTI0MDAwMDAwWhcNMjMwNTE0MjM1OTU5WjBlMQsw CQYDVQQGEwJBVDEpMCcGA1UECgwgUGhpbGlwIEhlbGdlciBJVCBDb25zdWx0aW5n IGUuVS4xFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRIwEAYDVQQDDAlQT1AwMDAz MDYwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCZ5JhELH0Dt9BIViJM +G1KaXNkTJLXQnJk/iBRz1DamVDEFgO7TK68iJ61Uo3K5YyG+hry88Xuq+3ld5sA o/bHkPM+jXkxXSypa7xJooWtmPVNsTanMXWSwckOCuXN1g3+cSXucgJSCGlxJ7C6 48rsbb0w0Ax7/rc0L5oSMoG3D/PS+8JwMOzskp1h/obQ2inwUmHYQ8k3XnugjQGi dZk3Yg3F262bGtjDoBALJoscz6tQzYl5cSYvxG17U8a4d9la1tFFGO7nKJOYRoAj QlbHZmG0X9NJycL9GlCN8lc4kQdqy/0yWMdFD8VavS3hPJXYgnoB6+7tUXqwkwCM yPELAgMBAAGjggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIDqDAW BgNVHSUBAf8EDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQUgsniY6pSk2BsXSwTUpfO 8e4MwBMwXQYDVR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0aC5j b20vY2FfNmE5Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0Q1JM LmNybDA3BggrBgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2ktb2Nz cC5zeW1hdXRoLmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo36zAt BgpghkgBhvhFARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4MDkG CmCGSAGG+EUBEAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZWFYw YUM1amIyMD0wDQYJKoZIhvcNAQELBQADggIBAEKChnxTRm0v2witAJaXR/COiMsf wmbdc24q59ePcij9hrcFmJdfBahZE2iwf3HVg1I0ZH73ltd8B4J6xUCE63YjzMQC weDTr/enKmsEYOmAS5tHL7XGVjrA6DII0q2ZuTL9rwFs6iQnTMrUDIdhaWaGlPRC MKu46I+uu44hhfDj6Z6f40wyfrGbyTYvzNHpCN66rUTH0Tsp3myvgX0KAD3F/6iD zcb5m2OMz4uv/ES5soOWRuso9vZ/l4hM9TTTWn0MZMo6pKCBAjdKqCD+/SHkLnX0 0PcAaUZSWxxX24EEIO43/ayuwclHK9onRve9YA/jmbLOFh1SLP+ce/NTDGI5mOZa qCYNqAH8ehiDvy4HmBkeLWZyWktUVMi+v6F/dufDVvlh4kNIDXHR8yYykWTLA8Od g5+h7nFsLmrPsuRojYBFbOHhMtljaabVRfgis1Fbccc8rp2gY+jPeT6yeoLuVgJb Ai5OnpFLh8IrYpbI1X07/Rq65/rH6cZ6ycvBZOb7QnvFUiQ8xScqJGZ25mLTdwzj PyuBgo8SPGQCiEI/30nWouK6tSl20MFj6gI8TkNUSrhLi/EFxxIpgZW08CGf4Hso vspud5kJ+cB9hYT1Xqbjq0RT9asvMz3g6HMyD/64ip9BXocAbe6zb4ueU8tKkDIP QN2iwe7sEx4qUZf3" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
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
