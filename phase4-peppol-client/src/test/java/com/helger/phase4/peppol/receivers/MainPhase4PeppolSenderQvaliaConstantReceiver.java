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
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolValidatonResultHandler;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phive.peppol.PeppolValidation2024_05;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Example for sending something to the Qvalia [SE] test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderQvaliaConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderQvaliaConstantReceiver.class);

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0007:5567321707");
      final IAS4ClientBuildMessageCallback aBuildMessageCallback = new IAS4ClientBuildMessageCallback ()
      {
        public void onAS4Message (final AbstractAS4Message <?> aMsg)
        {
          final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
          LOGGER.info ("Sending out AS4 message with message ID '" +
                       aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId () +
                       "'");
        }
      };

      final IAS4CryptoFactory cf = AS4CryptoFactoryConfiguration.getDefaultInstance ();
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                  .cryptoFactory (cf)
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIFxzCCA6+gAwIBAgIQPTQFeVdn6tiW/2yg/RWC5jANBgkqhkiG9w0BAQsFADBr MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU RVNUIENBIC0gRzIwHhcNMjIwNjE1MDAwMDAwWhcNMjQwNjA0MjM1OTU5WjBUMQsw CQYDVQQGEwJTRTEYMBYGA1UECgwPUXZhbGlhIEdyb3VwIEFCMRcwFQYDVQQLDA5Q RVBQT0wgVEVTVCBBUDESMBAGA1UEAwwJUFNFMDAwMDk0MIIBIjANBgkqhkiG9w0B AQEFAAOCAQ8AMIIBCgKCAQEA2YAPM1AZ/kdh8sgWozYr0h8tqiTD2g70yyQVRB2e 8DXzuqQ/plAIJnNPNWgc4bJNp+Zamexx9eHCUDjasAsFDmyoyOEKensqUhdvTcmp U7thxQEQZbE8loh05xRYRDkRRAp8WZj90t1iADqkl+cuRS1UHHiXm3+sPaO5aK8J iBJKc0A6a/0Ui7OHcsuLQ6uaLaE9VNG/ZMXG3FBgxwGzPH2ajYfPS7Xt2lceVkQM VO0UDaDdHhu/C1an3EjMnvcgypyFLWhgLxR7nuxNUhVOQPrT8lau5ZMkoMQqcTB2 PPOPIhX7xEBpI4NBkzhbk8wBAGEbVEFSbC0Y9DkVu248ewIDAQABo4IBfDCCAXgw DAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gwFgYDVR0lAQH/BAwwCgYIKwYB BQUHAwIwHQYDVR0OBBYEFM2oQA528A/6Wf2mCHe3ezL4pu25MF0GA1UdHwRWMFQw UqBQoE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGguY29tL2NhXzZhOTM3NzM0YTM5 M2EwODA1YmYzM2NkYThiMzMxMDkzL0xhdGVzdENSTC5jcmwwNwYIKwYBBQUHAQEE KzApMCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9jc3Auc3ltYXV0aC5jb20wHwYD VR0jBBgwFoAUa29LtvE3uis8fxjNuiuyuXwqN+swLQYKYIZIAYb4RQEQAwQfMB0G E2CGSAGG+EUBEAECAwEBgamQ4QMWBjk1NzYwODA5BgpghkgBhvhFARAFBCswKQIB ABYkYUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhWMGFDNWpiMjA9MA0GCSqGSIb3 DQEBCwUAA4ICAQARgU8wT2cl322fyZ396dlCeel7Bg46IgH9/ZtNYidqkxCoUIm2 yUzdbPPPeYthrtcLuJrA3oeBO8xHOC8vbfBxxBwNxxFOL6gAKpuKuPEq9MxU6vLT VdXNT6Nql5b1O+eNQtt1QWmGRSIsqUYs8BHcjGC7RITI8OfBQSURY+s1F65YzJYo jOEFeJW/CYhWsVwkOXy/GBV+WAGm3G57se4Yxswir5VVMydqBl4WxR5Vd0yiMfNA iPxZ27uJqmueojH7Fqy4PljbclvBrsZqfr/Z9/w9M0+wtAJ3Fe9xyDLzfEvfYaOO CNpm/iU5h+vP3fLTpwu2kmM63mGbeOr3sBNfqBDsVtkQkv3lxiY/aHvLTJM6uPIp qqoAWwSZVnHwRsDAK4+5PP8GnlzOJLmzTwLvugF826jektgqIBrh/HCPzAajbKpL clHtSieMPFEQ8ZTT2MZFbcpf0bSxy6Ft/311JkGpfp+q0Ud8YJFjWSfQ4Gm1T0GL GMrgEKLsgqTc+QZeQE1FkY/vn9BRb3VqMv7eJFPhhEb54rQcFXAWVuNqxpcOoxBd pbItP8Hlps4tApCllrPiZQ9hHslu5CHU99Pi7kJiIERte+MkqCHEms88GBbHOXg4 tlaNP/7eITPpE00dXzPihcrii9zrNCjODZQhmX8/TTbMrNbG+/pLWXYvHg==" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://peppol-ap-test.qvalia.com/peppol/as4")
                                  .validationConfiguration (PeppolValidation2024_05.VID_OPENPEPPOL_INVOICE_UBL_V3,
                                                            new Phase4PeppolValidatonResultHandler ())
                                  .buildMessageCallback (aBuildMessageCallback)
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
