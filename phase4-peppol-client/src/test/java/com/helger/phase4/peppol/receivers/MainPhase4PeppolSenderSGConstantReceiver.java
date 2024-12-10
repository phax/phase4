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
 * Special main class with a constant receiver. This one skips the SMP
 * lookup.<br>
 * Will not work, because SG requires production certificate.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderSGConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderSGConstantReceiver.class);

  public static void send ()
  {
    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/test-order.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0195:SGTSTIMDADEMO02");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:fdc:peppol.eu:poacc:trns:order:3::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:poacc:bis:ordering:3"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIFqTCCA5GgAwIBAgIQUBObPc0ef+XbT0ob4X0yETANBgkqhkiG9w0BAQsFADBO\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEkMCIGA1UE\r\n" +
                                                                                                         "AxMbUEVQUE9MIEFDQ0VTUyBQT0lOVCBDQSAtIEcyMB4XDTI0MDkyMjAwMDAwMFoX\r\n" +
                                                                                                         "DTI2MDkxMjIzNTk1OVowUzELMAkGA1UEBhMCU0cxETAPBgNVBAoMCERhdGFwb3N0\r\n" +
                                                                                                         "MR0wGwYDVQQLDBRQRVBQT0wgUFJPRFVDVElPTiBBUDESMBAGA1UEAwwJUFNHMDAw\r\n" +
                                                                                                         "MjMwMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2qwzi5Juh3qxFcCk\r\n" +
                                                                                                         "h3d9nH6a+sn4NzXJWHyDKabacx5laLEKCATHX74KIaRdv73gioBv/yImApq6lb+r\r\n" +
                                                                                                         "FDV5O1m4oWoUAjRFwYhqwHNmO3wqaDhML/F9zCsQjHkpgIu44c3xmiCU3wTko+uP\r\n" +
                                                                                                         "EIii8iHYPACBcAE04weLxTcJGVUpGOAbP/TaaWsmkJ8vFUSa/jeKaZfhjbZsCRIc\r\n" +
                                                                                                         "CCteeEp8CQRAQj0FEGOropb6IHe3xtYTxi7hhukg0dap7fuTWYlUxmo4WK6CfsGE\r\n" +
                                                                                                         "KqDH8Mtlm9IEwtYw5c4C9ADkurAIMPvhPV/+MHdv97giQPjbAjD7LUey4cmC2vDN\r\n" +
                                                                                                         "n59GIQIDAQABo4IBfDCCAXgwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gw\r\n" +
                                                                                                         "FgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFDIb1kfEciv63yQ5LraS\r\n" +
                                                                                                         "K4oZrZTBMF0GA1UdHwRWMFQwUqBQoE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGgu\r\n" +
                                                                                                         "Y29tL2NhXzdiZWRjYmNjNGY3MjRlZmUzMGQ1MDA2ZGRhNjgxYmEwL0xhdGVzdENS\r\n" +
                                                                                                         "TC5jcmwwNwYIKwYBBQUHAQEEKzApMCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9j\r\n" +
                                                                                                         "c3Auc3ltYXV0aC5jb20wHwYDVR0jBBgwFoAUhyXfWyOmxDv5n98bpSCR3eT0PIEw\r\n" +
                                                                                                         "LQYKYIZIAYb4RQEQAwQfMB0GE2CGSAGG+EUBEAECAwEBgbLX9DYWBjk1NzYwODA5\r\n" +
                                                                                                         "BgpghkgBhvhFARAFBCswKQIBABYkYUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhW\r\n" +
                                                                                                         "MGFDNWpiMjA9MA0GCSqGSIb3DQEBCwUAA4ICAQAzoXpxSfpfvp1csa2yWE+m6URL\r\n" +
                                                                                                         "vSlFNt4Aq4jB4qssVikPaMn+NPJ2LCMA1C0iIHSX8tEznoC2U2UFkAiue8yxDEko\r\n" +
                                                                                                         "R7DUYLoE/SQjoUxEgOBimqEUh3p2of6JGPUJI80JNA6JDr9VIJtSnXd8uEXbZfUw\r\n" +
                                                                                                         "0P10E6NXPLM+VJ/7rI6ol3o2+lQH1gJJzP2IGrT98R1tRhH8ui2TbHhObZnBrQrN\r\n" +
                                                                                                         "E/vF65/r3cX086Jr57xZRT/8x9VjviCvw7LN4SSiSAsf4KIAebWV5zKG5BQHcdHr\r\n" +
                                                                                                         "6GToX2K6wOMhBifHR0PxCqz2uA107PbKaXcqVxDjA2EHE2PC5s2o0ZBpIIOwbHus\r\n" +
                                                                                                         "+Xt2tLik6msfMHJI7tN1THihKFdwTiqD2SZhl87sVakDZPCWZnRhX2Ta/5+v6/Om\r\n" +
                                                                                                         "38K7UpcBS+TWMUKsbcMy9tXoVtsNZ5lmlnlB6t1X8/mmy0AHLBXnqbZl+3l3rrOM\r\n" +
                                                                                                         "8l50NmnHlmYK+0aUzqzlWa646iZwQRuELiAy+CBfa5m41NAeko2W4B+qeSZSMQqo\r\n" +
                                                                                                         "UG8MRNXyc1JT6BONASJptmEyP5QOnfon4oFLDIjoSEI0NOdifztU05dmOBqQeqTj\r\n" +
                                                                                                         "lxAZFQEgVjRyI78IT94rUntgGiefRLP5Hf9dF9rBrvPPt7tu05XpNCpXQrjUB1aS\r\n" +
                                                                                                         "E836ClOWfRrhujgItg==\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n" +
                                                                                                         ""),
                                                            "https://peppol.datapost.com.sg/as4app/as4")
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
