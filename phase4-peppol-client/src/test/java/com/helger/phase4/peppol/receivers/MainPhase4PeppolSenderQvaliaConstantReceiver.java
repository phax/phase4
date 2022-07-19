/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.http.HttpRetrySettings;
import com.helger.phase4.messaging.domain.AS4UserMessage;
import com.helger.phase4.messaging.domain.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolValidatonResultHandler;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.phive.peppol.PeppolValidation3_14_0;
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
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/examples/base-example.xml"))
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

      final IAS4CryptoFactory cf = AS4CryptoFactoryProperties.getDefaultInstance ();
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                  .cryptoFactory (cf)
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIFxzCCA6+gAwIBAgIQTJRiSywVkdw+JqmDB29xYjANBgkqhkiG9w0BAQsFADBr MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU RVNUIENBIC0gRzIwHhcNMjAwODIxMDAwMDAwWhcNMjIwODExMjM1OTU5WjBUMRIw EAYDVQQDDAlQU0UwMDAwOTQxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRgwFgYD VQQKDA9RdmFsaWEgR3JvdXAgQUIxCzAJBgNVBAYTAlNFMIIBIjANBgkqhkiG9w0B AQEFAAOCAQ8AMIIBCgKCAQEArMPej9tY7q6Yf1X3ki4yDcMIuVBrBvwEluXxBoLI C02BeGA2gFln2gGz/2ydFF1hokW5rmoUrJwHT2WoxHK2evvbiyDIOh3ULNKD5Qml XMZMPOHrMth0r3v0Fuvvl8t88pcB2zJxmysHa+Y0HDb6O2W9uZssMZr25eKPb3p+ /qUe0/UoAsPW+u7mhjZdjIsUz+MtYlUyAedcQptM6uh18f6eR2mL0mnp0V2jeBH9 s8YVxh4YoL3pfFVEYGXR/KoRrTdz2CMDhDjO+A4sUsidr7C5gy1CKTUKxUHoensA 0KJKMIJ3ZetslMnizSP2Y7600N8ryh4VC+tQWD8NRwYWxQIDAQABo4IBfDCCAXgw DAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gwFgYDVR0lAQH/BAwwCgYIKwYB BQUHAwIwHQYDVR0OBBYEFJCO623l0AmEUOcvRYZ5TuX43LSjMF0GA1UdHwRWMFQw UqBQoE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGguY29tL2NhXzZhOTM3NzM0YTM5 M2EwODA1YmYzM2NkYThiMzMxMDkzL0xhdGVzdENSTC5jcmwwNwYIKwYBBQUHAQEE KzApMCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9jc3Auc3ltYXV0aC5jb20wHwYD VR0jBBgwFoAUa29LtvE3uis8fxjNuiuyuXwqN+swLQYKYIZIAYb4RQEQAwQfMB0G E2CGSAGG+EUBEAECAwEBgamQ4QMWBjk1NzYwODA5BgpghkgBhvhFARAFBCswKQIB ABYkYUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhWMGFDNWpiMjA9MA0GCSqGSIb3 DQEBCwUAA4ICAQAAZNS3JzZn5rOcWKtYe+6Eh7nao8FQaMTXtKPOPWat00GUZwYX mqy2tyRTGzKWf+dByiGhfC0IdNlhHZJ7fH0TdXabFX9MaySm25TUJsaAX5eUTVMM C4XZJEX9ngie2NHTPqJsJPS/7utRONizjFa3PbI4STRt8SQNA3L/yxrf3H/Qujnd F5L6KTdIRU0pP+4qs4qbcbGcEzaZieOyNoSP2r1z3MIn+JVOHQetOrPkXKpuwFcD bdzaFLnZqiPRkI3ibpsml5fFLWwE9QTn+PcORZXvURlT0Ndm88FvDQWzAZ8y/f32 Fj0dCIFk+ACSwQnZsdVIcSzkDRYzQtxOmQW282rMO3TCgrKCztHQ/so+t7Mg4Qf+ wN/DmyZAdjhuzqnyX3mOxxbvFTqveatVL5PGMIbjhzWmrEztMgwdSB5DsS6OeaBL H3MohqqDECtiD5/U5FziHTDqSr6LIljz7zLK/7aK9I/h0xuoqzQGCqhn5mrB+ube L7SFxT6nM5pw5ZPvqIS0h4lOTWCq5Y5j2aXmo23trDfMoWqRncOlX18cnAThlnT2 RydPPOVrcFk4JehM2oRgm3ldo+Pj4lhQQ5WnBdX9o7yvnZhn96KyVqiMU25TDf6M JNLrljdeiEAl3pNsqzXSJ7nREhWzVI2IWe+NXEOUN3z7PgeKH0sh2KdS7g==" +
                                                                                                         "-----END CERTIFICATE-----\r\n" +
                                                                                                         ""),
                                                            "https://peppol-ap-test.qvalia.com/peppol/as4")
                                  .validationConfiguration (PeppolValidation3_14_0.VID_OPENPEPPOL_INVOICE_UBL_V3,
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
