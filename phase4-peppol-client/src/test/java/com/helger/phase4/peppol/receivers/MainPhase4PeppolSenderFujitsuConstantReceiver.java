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
public final class MainPhase4PeppolSenderFujitsuConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderFujitsuConstantReceiver.class);

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
                                                                                                         "MIIF2TCCA8GgAwIBAgIQT4nfwxxvMgudci9olw9WzjANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjEwNjA3MDAwMDAwWhcNMjMwNTI4MjM1OTU5WjBmMQsw\r\n" +
                                                                                                         "CQYDVQQGEwJERTEqMCgGA1UECgwhRnVqaXRzdSBUZWNobm9sb2d5IFNvbHV0aW9u\r\n" +
                                                                                                         "cyBHbWJIMRcwFQYDVQQLDA5QRVBQT0wgVEVTVCBBUDESMBAGA1UEAwwJUERFMDAw\r\n" +
                                                                                                         "NDY5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqhcmqlHe5KXAKJfa\r\n" +
                                                                                                         "eklmjlVc1HYthwWdAaiVDWhfPEQpkQ0G8/c0iLSITVUWAqyrI2bWqykNaQnED0DW\r\n" +
                                                                                                         "nk7PCp775eYCPcXny4D/n2aII1OE6euL1b943gDmpK527mWIcTTkay9dzzTJD0Gw\r\n" +
                                                                                                         "z2wJwckN1MAss8Bwc5POqWOrP/5yQa8Wy+E8Ik6KNAOh3s5UcxUnjlCruxFkwkRf\r\n" +
                                                                                                         "/+EA3qpsH0E1ucCjhLcMkZnbgD9hKcmSZACyKQKhuYQK+Nfy2h5pOQ33l6LYK6CF\r\n" +
                                                                                                         "zE7hBtJGzdH+M5bgreFg1ZYtFWNqHtnPjZ0LBMIaMidPiFzNJLXv0mEdeKEgT88o\r\n" +
                                                                                                         "Icv/JQIDAQABo4IBfDCCAXgwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gw\r\n" +
                                                                                                         "FgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFOFfAuPTCyx0J/gLfh5k\r\n" +
                                                                                                         "6lQqhTRjMF0GA1UdHwRWMFQwUqBQoE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGgu\r\n" +
                                                                                                         "Y29tL2NhXzZhOTM3NzM0YTM5M2EwODA1YmYzM2NkYThiMzMxMDkzL0xhdGVzdENS\r\n" +
                                                                                                         "TC5jcmwwNwYIKwYBBQUHAQEEKzApMCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9j\r\n" +
                                                                                                         "c3Auc3ltYXV0aC5jb20wHwYDVR0jBBgwFoAUa29LtvE3uis8fxjNuiuyuXwqN+sw\r\n" +
                                                                                                         "LQYKYIZIAYb4RQEQAwQfMB0GE2CGSAGG+EUBEAECAwEBgamQ4QMWBjk1NzYwODA5\r\n" +
                                                                                                         "BgpghkgBhvhFARAFBCswKQIBABYkYUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhW\r\n" +
                                                                                                         "MGFDNWpiMjA9MA0GCSqGSIb3DQEBCwUAA4ICAQCgcw/f8WWjmtCtxxj7aaJcpixm\r\n" +
                                                                                                         "VhMgUlwu5UCp+7XfzA7bMEE34zG0S3ZDJ2VOecejsOWVeiQzxtjizJWFM8SCI/zA\r\n" +
                                                                                                         "dHs4TIM2LCPlyigg7GvoD0he/TfCbTXTE/VqJrYdf6cM+JPWsoZowZZbJPf0JflN\r\n" +
                                                                                                         "BmKOE1gQgRzdWWWDp0/qE4vcwyGWvrhL15QFtHhJsFUEnFJ6ydg/VQkmSQXyun5L\r\n" +
                                                                                                         "F4GxUvu9ZcQvhwXgqHnnNfTgw3uP8fsmPFuJkpvPrHKV65KN26ULbXA9xb/MRRPp\r\n" +
                                                                                                         "D5Q1eRYSw8xUybFUet1sBIG6BGgvIIdHj02o+PrS+7jRrJ/RWgXkNQWRs7gznFFp\r\n" +
                                                                                                         "H+2/qZChfp64OvD6RDxD8+zAl11acAfGQ8uaP6LdoTrog3jsa/y7EO7iNVdZZPxr\r\n" +
                                                                                                         "e04Kx5L3ZUWfv/Pc60bMWjnVbkPnfBzkwmwmr93YdjH4aw8XRk/sl33jdYs8Lwkg\r\n" +
                                                                                                         "imTcDDrts//eoTJZuZQZiGt9yWluOmkBIqjwWr92+GZmx7Sz9Gaw2HusMk07T4s2\r\n" +
                                                                                                         "IuS4/tVf+Dv+KWbzYlrm6BQhsBJEOE1SIpuCrLEjZZP+lrxjPug3OvbSO2si2usx\r\n" +
                                                                                                         "8+T/JLIQyQVksyIKmoMmvffY8izKqOktprbXW4bAgxN/j3vLPK7MGQYhV2EHE096\r\n" +
                                                                                                         "9woj7C7n+FanKIOpjQ==\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://testb2bportal.ts.fujitsu.com/ws/msh/receive")
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
