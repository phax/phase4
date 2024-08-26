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
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderACubeConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderACubeConstantReceiver.class);

  public static void send ()
  {
    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:acube-test");
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
                                                                                                         "MIIFxTCCA62gAwIBAgIQDjHzGDpB5UJnWaf8cWYOgzANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjAxMDEyMDAwMDAwWhcNMjIxMDAyMjM1OTU5WjBSMRIw\r\n" +
                                                                                                         "EAYDVQQDDAlQSVQwMDA0MjAxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRYwFAYD\r\n" +
                                                                                                         "VQQKDA1BLUN1YmUgUy5yLmwuMQswCQYDVQQGEwJJVDCCASIwDQYJKoZIhvcNAQEB\r\n" +
                                                                                                         "BQADggEPADCCAQoCggEBANULkeetINrkekj2Q3Hx8NTVi3YN9aO2p212+DYCYMfw\r\n" +
                                                                                                         "g4xTTHkAEHxCA38w4SocCkjjoQjDuynxTv7Id/cXWyuiq/ah9ow2mI5ORp5Gm+3u\r\n" +
                                                                                                         "rq9jBrfsRq6G1Fg5mkm8QN3a5ZpkyMIw2iJYk7PLYBhvHe3VYXwwsq/Ld1bTH4sl\r\n" +
                                                                                                         "XX5LzSuyh2UhfKV2tw7g0ZnJfyXiAvDIMPlpIJc+29dm970j2oWLSd6Ap6EOJUJ8\r\n" +
                                                                                                         "IwDApXpfzkRbi0QYeE8u/wHkOICa0O6qHCszYIh4xCNu+eC+jw+o9XNdPGFPoJDG\r\n" +
                                                                                                         "AcigsaNOL500exDqjThgsOvPFx28LN/W8c6Prd5JCZkCAwEAAaOCAXwwggF4MAwG\r\n" +
                                                                                                         "A1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB/wQMMAoGCCsGAQUF\r\n" +
                                                                                                         "BwMCMB0GA1UdDgQWBBTlxoT0QOlJgsPler2xkgdmYX1gXTBdBgNVHR8EVjBUMFKg\r\n" +
                                                                                                         "UKBOhkxodHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82YTkzNzczNGEzOTNh\r\n" +
                                                                                                         "MDgwNWJmMzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcGCCsGAQUFBwEBBCsw\r\n" +
                                                                                                         "KTAnBggrBgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1dGguY29tMB8GA1Ud\r\n" +
                                                                                                         "IwQYMBaAFGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG+EUBEAMEHzAdBhNg\r\n" +
                                                                                                         "hkgBhvhFARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4RQEQBQQrMCkCAQAW\r\n" +
                                                                                                         "JGFIUjBjSE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIwPTANBgkqhkiG9w0B\r\n" +
                                                                                                         "AQsFAAOCAgEAHDt+Q+x6ZWSa9DVmsWFgNynpNDVFsvpvfgPRihQ8oDYXvZ8gbLHM\r\n" +
                                                                                                         "NgPS4IR/zantLhO9sjR7kkK6K5peVopw6IHxoXINvY02cP6zTQBVwG5ec4elcoXX\r\n" +
                                                                                                         "1KE/+FFKlTMbq+oXPPqCJmnKRNJZ9s59nb08R2DyTHzyBiQK5XIEFnOVAMwbPoE6\r\n" +
                                                                                                         "BIL/u9Rv/e7+7XdWDLTGKgNBX+pwG0eZj6zjMcogdhHOQDXkjWAdK4xPu0mNiwks\r\n" +
                                                                                                         "6S9uOstRtkxdL0N4Gx9aIqdRxZwWOY8FVEf1WRh8OkUjJcAAy8FQlIQysNaJBRLA\r\n" +
                                                                                                         "Kbf8sG8gWRAZp4qGPLZia6bGw2I5AW21//mLBo6B3cIXmvcAFT120RSJ4B/yL5g0\r\n" +
                                                                                                         "wqZNHtjAaZ3LaIFoR0GsgEN+zOOu7u6sxuS7E+GR+UhvyIb+iLfXLq40guDQ6Fax\r\n" +
                                                                                                         "0grI0ZG7VqFGQvgcJ42UtMNFoANIb0ZSywFLe0TpI1kP5ZR4SgQ6kVwG0OIKOpoP\r\n" +
                                                                                                         "82zmMF7idl0lsqdVl5Tn3nb48e9NWZ8DpK8D/GfTETgyqrBMI5AdnD0agnLvrSNz\r\n" +
                                                                                                         "RHHsMUhiz0aE3MoAOCo5aoH6hwbZsT2ptZGJ4fWfiCYIlRcAmQegNlWcDnrCPmI9\r\n" +
                                                                                                         "CIWVQ447eZ92vl7AjTe1+qAwdXgNoLplO6nVKgX4eBEj1fXTTqsIZPo=\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://peppol-receiver-sandbox.acubeapi.com")
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
