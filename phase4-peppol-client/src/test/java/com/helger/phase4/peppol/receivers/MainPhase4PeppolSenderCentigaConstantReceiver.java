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
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Example for sending something to the Centiga/Intunor [NO] test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderCentigaConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderCentigaConstantReceiver.class);

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0192:992156678");
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
                                                                                                         "MIIFvzCCA6egAwIBAgIQVFtugctfDz+N0MrziDs+GDANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjIwNzI5MDAwMDAwWhcNMjQwNzE4MjM1OTU5WjBMMQsw\r\n" +
                                                                                                         "CQYDVQQGEwJOTzEQMA4GA1UECgwHSW50dW5vcjEXMBUGA1UECwwOUEVQUE9MIFRF\r\n" +
                                                                                                         "U1QgQVAxEjAQBgNVBAMMCVBOTzAwMDM3NTCCASIwDQYJKoZIhvcNAQEBBQADggEP\r\n" +
                                                                                                         "ADCCAQoCggEBALXAtmz0+gbTOHYZR9FQ6e3UdKah74hSV/nehlcKSvyBAwmQtB3T\r\n" +
                                                                                                         "0PvrCuKi33wjWVK3+s49SfkDrwRwRIOkarJWUp9VJipZkIpzmZnSzbWa92Tdy2NC\r\n" +
                                                                                                         "XRS8WAgVeJOJ2L8ct4eS2U3uHANydhCZDW73lSmYhvDj8IGz6kto8E1y/kBIgr79\r\n" +
                                                                                                         "VGewXPV8Bi80dp+0flHQVPaq65TUIe9+UohCIIQ3Z+7uGvusOmgK+fZskQZAsIkJ\r\n" +
                                                                                                         "q7Cew2KHHPFUnUdseRIeLXJn4b1xVcV9Cvv0iTStNIuYv3lGFkqyAQKLmZ3nSgD4\r\n" +
                                                                                                         "i6uDZ2VM51iuPMHMW3v2mmOipO5xGWG7rmcCAwEAAaOCAXwwggF4MAwGA1UdEwEB\r\n" +
                                                                                                         "/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB/wQMMAoGCCsGAQUFBwMCMB0G\r\n" +
                                                                                                         "A1UdDgQWBBTo7CzXXvwVYZTgJItnJal8ifMcLDBdBgNVHR8EVjBUMFKgUKBOhkxo\r\n" +
                                                                                                         "dHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82YTkzNzczNGEzOTNhMDgwNWJm\r\n" +
                                                                                                         "MzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcGCCsGAQUFBwEBBCswKTAnBggr\r\n" +
                                                                                                         "BgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1dGguY29tMB8GA1UdIwQYMBaA\r\n" +
                                                                                                         "FGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG+EUBEAMEHzAdBhNghkgBhvhF\r\n" +
                                                                                                         "ARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4RQEQBQQrMCkCAQAWJGFIUjBj\r\n" +
                                                                                                         "SE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIwPTANBgkqhkiG9w0BAQsFAAOC\r\n" +
                                                                                                         "AgEASl4jYpksiu4byHblcXaBuPmkD5hluqJTFfHJZ5CakoRRlDoNPy7z3B7ej7QQ\r\n" +
                                                                                                         "Jd01Z+LeyotY/dYuPnz2sDtItwO8osUo6J6vQ3aJTYNtV3qIlWkBiSKglfEtPKj5\r\n" +
                                                                                                         "J66/w4ZLSSN2d68b3eHNclpg/Ou2XGUV3EpJmsXUUQ3eVG/UD7S6bvL2a3+qDq+7\r\n" +
                                                                                                         "UqlqHnsBbKeoABOkjXTSL0wQhgxPVEH448L80ICqSOFnXT+DSJhi5z1+NInnYDIQ\r\n" +
                                                                                                         "NQSddq8vSPSzzdqQ5Nv50MRlvzsG5JcyewKTEokSh1nf2g6TbaGxXiPz//W4MAl+\r\n" +
                                                                                                         "TNmeWgW2u4M7jvAJxeoDE4tlRODhkOprbHS6YaPJxIQvvmSBAstZn59hT2cJs73P\r\n" +
                                                                                                         "VwvYTaCGd767pLLCgTyi54Pb9bGGZ2Ky8aixz6knE4nZ1l7EO23qJGMEsrmasrfq\r\n" +
                                                                                                         "Qu8gnVGRxHUkPqDTfEINRDdhwUFtLz7JjAU+4pUrX0M4YDG0XAeEhhZFmUixjhse\r\n" +
                                                                                                         "vlOoaNdohRjhlqCyjk6pj5eBLIlPR6ljsXURs088Z9jaedvI89rwff0R7arDy5OO\r\n" +
                                                                                                         "lJ+Z/l9Ertnl7fHTxURa6RncPI6PaOQncFm1NiJrVaa0fkigkCgNTw/VaEl2kt8a\r\n" +
                                                                                                         "5bVjNcCN/QIRpgpyFZIlO30Rb7eEkal/+hVqByWRM77w55U=\r\n" +
                                                                                                         "-----END CERTIFICATE-----"),
                                                            "https://peppolap.stage.conta.no/as4")
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
