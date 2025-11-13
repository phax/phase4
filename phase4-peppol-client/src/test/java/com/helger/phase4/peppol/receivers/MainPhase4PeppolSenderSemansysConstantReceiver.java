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

import com.helger.peppol.security.PeppolTrustedCA;
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
 * Example for sending something to the Semansys [NL] Test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderSemansysConstantReceiver
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderSemansysConstantReceiver.class);

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
                                  .peppolAP_CAChecker (PeppolTrustedCA.peppolTestAP ())
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\n" +
                                                                                                         "MIIF0DCCA7igAwIBAgIQJ22Gqs2sr75qnAwvjhocojANBgkqhkiG9w0BAQsFADBrMQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UECxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBURVNUIENBIC0gRzIwHhcNMjUwNTIzMDAwMDAwWhcNMjcwNTEzMjM1OTU5WjBdMQswCQYDVQQGEwJOTDEhMB8GA1UECgwYU2VtYW5zeXMgVGVjaG5vbG9naWVzIEJWMRcwFQYDVQQLDA5QRVBQT0wgVEVTVCBBUDESMBAGA1UEAwwJUE5MMDAwODE3MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxKwaSTPww+FY4HG2vePkokAKUYPdZyPpLDfHr+1Gcmeb7l8lg/fUqcthuw/+SgErqnifM4s+YtMLjQk9sCEwzx2dHZHq5ckLQM6gsKcRkjArXqO6ArcXsGgmvBS7ouXLC0jBJBdfjLmQPsi2b7+oogAP+vIAkwXXw/q/l5BLXMbCwq9hTyhTGbsuGLf6W52PEOd840zxGYmPOmRBhizd0KXlrmFZkb/CJOrIDMfTrRa08UAsMN1aiztAOozso0kZmsI983IEbvFig1bhxlMcTWMKJjolQO6gIfly17P64UPSZ9fG9Ry6C8ajNkEZ+cW+UpnoC+gVcAYveyIiJQIOzwIDAQABo4IBfDCCAXgwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gwFgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFEyApzJkHxxhIxQYNgKszOjWZb/zMF0GA1UdHwRWMFQwUqBQoE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGguY29tL2NhXzZhOTM3NzM0YTM5M2EwODA1YmYzM2NkYThiMzMxMDkzL0xhdGVzdENSTC5jcmwwNwYIKwYBBQUHAQEEKzApMCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9jc3Auc3ltYXV0aC5jb20wHwYDVR0jBBgwFoAUa29LtvE3uis8fxjNuiuyuXwqN+swLQYKYIZIAYb4RQEQAwQfMB0GE2CGSAGG+EUBEAECAwEBgamQ4QMWBjk1NzYwODA5BgpghkgBhvhFARAFBCswKQIBABYkYUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhWMGFDNWpiMjA9MA0GCSqGSIb3DQEBCwUAA4ICAQAp2mnQLzYwBYTDlRoN3OWHoZbato0JKXO0NC/S6FTrfRHMuhQOz7YXLW2pAe9l4I6B09lKVx4RIL0lJL3xETFUoifUF9s8e4zbuM/Y6EduorfpLcw+E0H28X33FgmviX2eT4NtU6GcMWZKtTpJPbdHYdq2bTrdFwRH46SingRgvRjFM1FVvPeZ5YFeo8oWVhdyjB9x2CX+qYYKXpaSnJSOBvJZ4Lxv6gSLcJXFhH95ws6FbQjg0/1hiHMJgaJvAYo4BEbAYrPr5QG9YPF5wJeycSLg47p2urGfnSksXCSV+Q+lhgghzgg08P7tT8MbOJVB9bXJ44h6LZiKlQD+wwDFteMJOYg/Fsucjzq1miSMXKVKSjs4ZNYQlQy7J1ae30TaoNfLT6xgreZP4nUMMXyJKRWVED9jrzkcBwaN/R7w3KR4gIq5MBK3hvTT9vDary7zAgopiWhoMs9AMGnfZ3M6RV4c8EhYNJcsW9aDq1GuDm6GG22/nFXabW27uU5vcbN2rPj3iVi/KAbR5t+hNOYLjSRKnADEFOGVMdSn1atg0s0qH5vUuv/qxeb4dM03IAjHZs2RZzOIZafg1pHq5EUsxpHkpPuO4MvH11Qym6K24/9oO/gLXj4rnk1pwMHWzk0B7zDp5wUvrBz00YakvKVIpcAAu+R44NGu+2+gQoo31g==\n" +
                                                                                                         "-----END CERTIFICATE-----"),
                                                            "http://edelivery-test1.semansys.com:8080/as4")
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
