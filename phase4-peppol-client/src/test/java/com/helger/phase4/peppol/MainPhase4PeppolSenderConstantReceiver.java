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

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phive.peppol.PeppolValidation2024_05;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderConstantReceiver.class);

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9930:INPOSIATEST");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .checkReceiverAPCertificate (false)
                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIFzzCCA7egAwIBAgIQE9UWdJAj7xeapgMq0uA0nTANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjAwMTA2MDAwMDAwWhcNMjExMjI2MjM1OTU5WjBcMRIw\r\n" +
                                                                                                         "EAYDVQQDDAlQT1AwMDAyMDIxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMSAwHgYD\r\n" +
                                                                                                         "VQQKDBdJTlBPU0lBIFNvbHV0aW9ucyBHbWJIIDELMAkGA1UEBhMCRVMwggEiMA0G\r\n" +
                                                                                                         "CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDDo3ByOQy0/xkRvraL6YYLdcNdcXVw\r\n" +
                                                                                                         "rJMHp39Tv0WTZ3efhcsO8lS9dPN7QvNSxJ87msjfrRB15ZZULWCDANtmkvOk+3t+\r\n" +
                                                                                                         "S7vd5R1OmL/fZiKr7yGAvO1L/RQp/WTE5jtOLy6BDDs5mvckhvbO8Hf1u6e9gYGV\r\n" +
                                                                                                         "Dksxs7RECv9xYQVj4CUK2BQ26sfHP6TTCMAt4kJqHcNSqXlCZ5V9XrpuJw2uuPsg\r\n" +
                                                                                                         "0+RLw2uAPY2HLqil/fYC8+CMw3+d9a0kUBkULJIVlorMUQ4dMEj8rzfSJ4Q1L1pU\r\n" +
                                                                                                         "UcqWQT8uuW9Ii+38nPzM6ac93K7EDbh2EN4Uqa1AmFChopd/UYRM5dCtAgMBAAGj\r\n" +
                                                                                                         "ggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIDqDAWBgNVHSUBAf8E\r\n" +
                                                                                                         "DDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQU7ft5Uz1oYnT/ZkzeY5yO1rQbdhUwXQYD\r\n" +
                                                                                                         "VR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0aC5jb20vY2FfNmE5\r\n" +
                                                                                                         "Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0Q1JMLmNybDA3Bggr\r\n" +
                                                                                                         "BgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2ktb2NzcC5zeW1hdXRo\r\n" +
                                                                                                         "LmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo36zAtBgpghkgBhvhF\r\n" +
                                                                                                         "ARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4MDkGCmCGSAGG+EUB\r\n" +
                                                                                                         "EAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZWFYwYUM1amIyMD0w\r\n" +
                                                                                                         "DQYJKoZIhvcNAQELBQADggIBACFbupStt66573YBXhHij67vTfJ02Ub0vSPuYTUu\r\n" +
                                                                                                         "vynCzT2RBWPOZZ/qPHgACg633Zxx3NINRbHdiclRrhUqh7AhyF9T9cBvZOCziVq1\r\n" +
                                                                                                         "+iueifXAYMb0mGJ6L+6AXJthfZKpz1WdM3rft+ycG5e8Sjw/7t+xKybr+r+7fwc/\r\n" +
                                                                                                         "knyV3j4qWGVmQXbFwpCYO82N+YHKIGjqtrAe3kceMegGZdEDkuPL4DcAxW+OfjYO\r\n" +
                                                                                                         "Udgr/2vCbp4jAoRc7GAZOLhqHTZW06dzz8sTphrLzwi+/a3oEfb4xCUrnqN1b5Vc\r\n" +
                                                                                                         "vOu8JE/nS9SJ2NMJ5RVjopcCCX3AIzD7uZUCSNnPTnhYULM2WXClow00yOLRE/mR\r\n" +
                                                                                                         "xjCjMU36IdsDl4y0UexYjuN3feBFhsS8U5R1BvJfNZ7og3/vUIm+nvOBYLDp9waE\r\n" +
                                                                                                         "MpoUsfE72kCYrq7uaqZmT7G8hB5WRy7QCMbWlMhcFEAss674EI7wZANJNjuj+qVA\r\n" +
                                                                                                         "S0IKcv1qnbCJTs9c0KxqNnBc7mX4gTKUahVYyC8rhNIMr68EGWAZdh1YS3oxJx/y\r\n" +
                                                                                                         "WH9rG/0Mw9CiNLMMT2SSiCvTaM0GKHYZr0+1jWSBi4yEJZhS7EXPmC51z7O6ek27\r\n" +
                                                                                                         "SQbv4FcK5PhrJEbvA/HYDwGbMEeyqnLlkns0KOZ7V/PK+rLF17VNRnBEXINHqzd1\r\n" +
                                                                                                         "njEO\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://www.zweikommadrei.de/as4")
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
