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
import com.helger.phive.peppol.PeppolValidation2024_05;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Example for sending something to the Fujitsu [DE] Test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLameschConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderLameschConstantReceiver.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0088:4026227000001");
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
                                                                                                         "MIIF0TCCA7mgAwIBAgIQRTctJV1pW/0ana3kMO1nKzANBgkqhkiG9w0BAQsFADBr\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjIwMzE4MDAwMDAwWhcNMjQwMzA3MjM1OTU5WjBeMQsw\n" +
                                                                                                         "CQYDVQQGEwJMVTEiMCAGA1UECgwZTEFNRVNDSCBFWFBMT0lUQVRJT04gUy5BLjEX\n" +
                                                                                                         "MBUGA1UECwwOUEVQUE9MIFRFU1QgQVAxEjAQBgNVBAMMCVBPUDAwMDUwNjCCASIw\n" +
                                                                                                         "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMJYkGTIGCNGYDS8CA93HkuZwWNK\n" +
                                                                                                         "60TxY+TEJjzKdiCjhEu9sjnZrn7Y/YYjLhBbjR3AmFFDXCNGaFytA2g0gV+KMvUV\n" +
                                                                                                         "Algeh9g+BbhLMw1Azb1ywS/5VaYyJ0R4MbKuegleaY4yCbdTOHt8ovaF2flvJwax\n" +
                                                                                                         "T0kxFlB5noX2wRE5V13cQi6d2inECrJ1aKOW9QcCm7j5zXwth3/gBNm3u2ub5ECC\n" +
                                                                                                         "owzJZbS+71xiafi4GALU76oiuvBSAiBlNkYB26RiqIEJ2QTpPBY0/IG9HD69JfXO\n" +
                                                                                                         "6H1VhSMKLcDfizwgklPjG3QA9Rzy+K0iP3eXHKA1mx/DFn7w0PotBEFLce8CAwEA\n" +
                                                                                                         "AaOCAXwwggF4MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB\n" +
                                                                                                         "/wQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBQTH38JzO/k7u6NAwQZrR+HaWR6TDBd\n" +
                                                                                                         "BgNVHR8EVjBUMFKgUKBOhkxodHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82\n" +
                                                                                                         "YTkzNzczNGEzOTNhMDgwNWJmMzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcG\n" +
                                                                                                         "CCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1\n" +
                                                                                                         "dGguY29tMB8GA1UdIwQYMBaAFGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG\n" +
                                                                                                         "+EUBEAMEHzAdBhNghkgBhvhFARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4\n" +
                                                                                                         "RQEQBQQrMCkCAQAWJGFIUjBjSE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIw\n" +
                                                                                                         "PTANBgkqhkiG9w0BAQsFAAOCAgEAB19egyUBbGVbvJ7m68QhTS0YzlC5kSjXorK+\n" +
                                                                                                         "K/u03k4dMjzbTRuIvxgVMGGhroHzn91vhsp93rr85cPYL2CBr2XMW4iWPbxe4prO\n" +
                                                                                                         "F9MaAyfk3XeLCzuLbUq4x0pnJna50WfK6WPsyJUTEalxivEz55wTnmUKS1AQY8PQ\n" +
                                                                                                         "cIwQ1fCBw0HdGYwfR/Np3X7AK8+0d2h9Bu1BTtEcRFAX+K100VReGMSN2+wTXXbp\n" +
                                                                                                         "OI/BFMKqz5529jVX3YDjJQ6iNp0GNt6cTdyxNNISsVxa8iFiJtAbjakonu6p6QsR\n" +
                                                                                                         "2f9d04YkbvpjPqMgyVdkWqD9xlK9/wBKuJmUyfNsDBsvKRADhUNsHu8hGRrWEUlN\n" +
                                                                                                         "VXqu+yP8FNHupEz7Fq8xSDq2h47C0fAqoZE+BXaO68RU83Z228tifPUgw4eRJqfC\n" +
                                                                                                         "w0R8fvALPhOhcDBXrTFYglaDo7xNHfIvOwycDeVhY+O0RpWovXDEqGdfmXjCaO3X\n" +
                                                                                                         "4VvK+rXujpcbUVZGCELz9bQX3NTaVQ8O6PjwNxC0pXowEcir1tya7GHOZsT/Sros\n" +
                                                                                                         "Rmu6l/8EBncxPqrV7MpapYnLfTm9yIEwM++HRny8gWjlLrQ6Z4MRTwyi16xa4vFj\n" +
                                                                                                         "xe2qGSoZN84ZwzC/yQNXXxE0gnRrVjtD1ofTv9pzm14NZq+WFeICoDmX7gg8ESIS\n" +
                                                                                                         "9fFRZZA=" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://peppol.lamesch.lu/as4")
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                  .validationConfiguration (PeppolValidation2024_05.VID_OPENPEPPOL_INVOICE_UBL_V3,
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
