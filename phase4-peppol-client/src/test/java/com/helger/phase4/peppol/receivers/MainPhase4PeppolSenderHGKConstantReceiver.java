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
package com.helger.phase4.peppol.receivers;

import java.io.File;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.Phase4PeppolSender;
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
public final class MainPhase4PeppolSenderHGKConstantReceiver
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderHGKConstantReceiver.class);

  public static void send ()
  {
    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:hgk-test");
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
                                                                                                         "MIIF2zCCA8OgAwIBAgIQcnE1ulLcsDGjCIXOyGKkezANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjQwODAxMDAwMDAwWhcNMjYwNzIyMjM1OTU5WjBoMQsw\r\n" +
                                                                                                         "CQYDVQQGEwJERTEsMCoGA1UECgwjSEdLIEhvdGVsLSB1bmQgR2FzdHJvbm9taWUt\r\n" +
                                                                                                         "IEthdWYgZUcxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRIwEAYDVQQDDAlQREUw\r\n" +
                                                                                                         "MDA2OTUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDE7KkKf6so0SoA\r\n" +
                                                                                                         "6qiPJMrAP5OygFpPe2FIGn5PHdOLzHeQX9nj12yPnHTVfDShBpHYBjPmcsAU4vxH\r\n" +
                                                                                                         "XviWBDj4c+Kkw4DdTwcPBciQOddsX99uaKWOoIQONiZ//wYqn+YvDTuoF3MNSNwj\r\n" +
                                                                                                         "nkSFm8p67JzPEbVKe1pLpqcrXiWAnlyvFJXsxtCk+U0x0bHNLy3VDgMIq6Q62aYO\r\n" +
                                                                                                         "y79PNjWb1glLcc3hqnZL0BMYvMJtXS3rRAPbMdjrkFDVhwiLj3UiOl+YB8OH7dWM\r\n" +
                                                                                                         "7eSHyt2LoqJVlrMb2XwxKV2w0F9DPyLjSIeKAigRLMV46jL45h2Uzc4qwT5fE5qV\r\n" +
                                                                                                         "9QPHOCF7AgMBAAGjggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwID\r\n" +
                                                                                                         "qDAWBgNVHSUBAf8EDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQUtJbPROqMxVPmncQ8\r\n" +
                                                                                                         "2UGclvwehZkwXQYDVR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0\r\n" +
                                                                                                         "aC5jb20vY2FfNmE5Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0\r\n" +
                                                                                                         "Q1JMLmNybDA3BggrBgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2kt\r\n" +
                                                                                                         "b2NzcC5zeW1hdXRoLmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo3\r\n" +
                                                                                                         "6zAtBgpghkgBhvhFARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4\r\n" +
                                                                                                         "MDkGCmCGSAGG+EUBEAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZ\r\n" +
                                                                                                         "WFYwYUM1amIyMD0wDQYJKoZIhvcNAQELBQADggIBACPZn5pPjZYeiOgiJi1fixNa\r\n" +
                                                                                                         "z+jhulLrX6z9iKltPWT3p6tSlxIystrKXN2TkS3TV0gRQuElxZpGCrDtaG3MBAx6\r\n" +
                                                                                                         "klqXtgDjrlscmT9cHRzRHxcXucfFdWgA2/GdYH2Fbp0lKMICs7BKYcKACAspUCJd\r\n" +
                                                                                                         "8opLJDTgqHo2VtE6JbCpLhTl5FyasxwJXW8QhPPHCcjWEURLmE0mWSfAN5WPZV8i\r\n" +
                                                                                                         "wyLyyqlrVDkgiN+zMwIqwPcDfWBe6pbxUEPp3Mab1ZGgmhPj7h6aAXZzDsE/vvT1\r\n" +
                                                                                                         "14v3yZDRivChYtearyvLNavucX5vcIrmRG6QUgcIZIsKPGJ7rjdqIQmEXW2ksjqt\r\n" +
                                                                                                         "Bc1bTf4MFRGbs6l5/FtU5T7XcxZAzC0962LLZbbeUFw3XQpfxVX3xC064DKwL2ZJ\r\n" +
                                                                                                         "eEAMc3K7jGPNUNHCBYEZIpDfDz/BfPloq9vSyBftE+9FgC2maiFDLbV9L32bdfgy\r\n" +
                                                                                                         "qn4TeMY3NzyaPSWvyYAJoef77tUKxmXyVA2RRQzeshCp/7/cPxTYTFcizms4jSvL\r\n" +
                                                                                                         "s7has48UZy5I0dfcp0G1hy/z+Jgm+DypO3ik940ODgIsz3/aWrGro/LziblMzYcX\r\n" +
                                                                                                         "V5TvDo+C28RtPHGyOxasNiEn9lCgHIy2qMte/qXyUN07x2HZ3SXKYfu4oHqv3V0p\r\n" +
                                                                                                         "mfF2j1gvXLr6J06KGoX+\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://peppol-ap-test.h-g-k.de/as4")
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
