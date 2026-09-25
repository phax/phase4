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
package com.helger.phase4.peppol.receivers;

import java.io.File;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.AbstractPhase4Sender;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderHGKConstantReceiver extends AbstractPhase4Sender
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
      final var hcs = new Phase4PeppolHttpClientSettings ();
      hcs.setSSLContextTrustAll ();
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .httpClientFactory (hcs)
                                  .peppolAP_CAChecker (PeppolTrustedCA.peppolTestAP ())
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (pem2cert ("-----BEGIN CERTIFICATE-----\n" +
                                                                      "MIIFujCCA6KgAwIBAgIUYqGuXYtf/DMt4A23g88SKJEFVdkwDQYJKoZIhvcNAQEL\n" +
                                                                      "BQAwazELMAkGA1UEBhMCQkUxGTAXBgNVBAoTEE9wZW5QRVBQT0wgQUlTQkwxFjAU\n" +
                                                                      "BgNVBAsTDUZPUiBURVNUIE9OTFkxKTAnBgNVBAMTIFBFUFBPTCBBQ0NFU1MgUE9J\n" +
                                                                      "TlQgVEVTVCBDQSAtIEczMB4XDTI2MDExMzAwMDAwMFoXDTI4MDEwMjIzNTk1OVow\n" +
                                                                      "aDELMAkGA1UEBhMCREUxLDAqBgNVBAoMI0hHSyBIb3RlbC0gdW5kIEdhc3Ryb25v\n" +
                                                                      "bWllLSBLYXVmIGVHMRcwFQYDVQQLDA5QRVBQT0wgVEVTVCBBUDESMBAGA1UEAwwJ\n" +
                                                                      "UERFMDAwNjk1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnIjZRRSn\n" +
                                                                      "g97iftyjFbtiLZSoblMc5YH3ucyu0EzSfYUmv2/hB63uhPRCzMU1H9Rhyx0QBRQl\n" +
                                                                      "g4TM49Vz6AGvjfklZSdi3S169wzJufyTq+lKpnMSOzWGIUcEsSFA7zqdlX665kzQ\n" +
                                                                      "us/RXWm6Z4xWE3E8qsvWwpO+LBnwredrNRMUL7cQ85tT1kUQkpU7mTdFFSPRisfy\n" +
                                                                      "fNPhEA+MihVvQnWDg7Ai8AkLH3I3zOg+Ox4+vxOyOW17ahvZH8b4w66aIzVXRMiM\n" +
                                                                      "wNa4IeakBWUHFxC7RKRDd8d6Jka99gyxRasqluYito7kMJ7kh6xdoAyrySYPAxI5\n" +
                                                                      "64Mgj5DdbAPAvQIDAQABo4IBVzCCAVMwDgYDVR0PAQH/BAQDAgSwMBYGA1UdJQEB\n" +
                                                                      "/wQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBSaudtIu3l3HB/PHQzOzPFzc7K4VzBO\n" +
                                                                      "BgNVHR8ERzBFMEOgQaA/hj1odHRwOi8vY3JsLm9uZS5ubC5kaWdpY2VydC5jb20v\n" +
                                                                      "UEVQUE9MQUNDRVNTUE9JTlRURVNUQ0EtRzMuY3JsMB8GA1UdIwQYMBaAFLPMRO92\n" +
                                                                      "r4HJ3/NfpZ6Ica2foPdwMAwGA1UdEwEB/wQCMAAwgYoGCCsGAQUFBwEBBH4wfDAr\n" +
                                                                      "BggrBgEFBQcwAYYfaHR0cDovL29jc3Aub25lLm5sLmRpZ2ljZXJ0LmNvbTBNBggr\n" +
                                                                      "BgEFBQcwAoZBaHR0cDovL2NhY2VydHMub25lLm5sLmRpZ2ljZXJ0LmNvbS9QRVBQ\n" +
                                                                      "T0xBQ0NFU1NQT0lOVFRFU1RDQS1HMy5jcnQwDQYJKoZIhvcNAQELBQADggIBAHuz\n" +
                                                                      "bUo4HlNpzptzZncHGtzV24Bo7SfQ/O7agLoo4OA5W1dnNSqR9Rh9Zg9T4KAG0wfT\n" +
                                                                      "ceMDjAmNljPeXaSao9j9L960A0EdDOhspLw4UqR9aD92bGVpyNky1W7DqiLGnFhb\n" +
                                                                      "HGWASoTxrsM6TvOPaJr8tigjq6bcH5vZmFl7Br8pfERryfHqvI22TaDGomnbpnhp\n" +
                                                                      "yu+cpQ5q1K/pdXcGiLsBWA3kAHa9f7hzGQJiJp6oPGw3GHBSb/behuSxoamvb+nH\n" +
                                                                      "C0tcpNAIWmBaMSmehc1jrUE89FKtP742Pym/1Pw38QBPy/hCf6jTpj03iUq7Dvxx\n" +
                                                                      "NlF2XeRqwJQPUvdr+8kBrsJ75+g1xgr9ucBNybFVT7M/z23RXkhxTqvfjoGxND2b\n" +
                                                                      "fPD23DErSmiFobA+ofkcdcrdcKo7f/IlURgPVbrua6Z+vwlQwHwxMKK1bni/n4mU\n" +
                                                                      "n/nBPZ8s9acgZz2B7iQbusWKE3cKCKoqq4a6VeZ9BDDpB+DvvwobFgOgko7zPK64\n" +
                                                                      "Fb0lQQLxew5ifsxbOz0fh9qwLBpaoHoATfuSDWSTCCVes6eXpcXfa+UvfKyIrzLo\n" +
                                                                      "QvlOBn5zIYn3uR0tq74r1IqkVfJm7Xg0sgYktpaeFMXCW8TYQNKiGsTyViO8azpn\n" +
                                                                      "X3TQE+uxuaT3e6m2DFQwjfiaACZiTAvp/x2XxDuy\n" +
                                                                      "-----END CERTIFICATE-----\n"),
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
