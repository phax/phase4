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
public final class MainPhase4PeppolSenderAlbertPatronConstantReceiver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderAlbertPatronConstantReceiver.class);

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9922:helger");
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
                                                                                                         "MIIFxDCCA6ygAwIBAgIQW6Adit2j6AVGQnfTlb9twjANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMjMwMzI0MDAwMDAwWhcNMjUwMzEzMjM1OTU5WjBRMQsw\r\n" +
                                                                                                         "CQYDVQQGEwJMVTEVMBMGA1UECgwMQWxiZXJ0UGF0cm9uMRcwFQYDVQQLDA5QRVBQ\r\n" +
                                                                                                         "T0wgVEVTVCBBUDESMBAGA1UEAwwJUE9QMDAwNTY0MIIBIjANBgkqhkiG9w0BAQEF\r\n" +
                                                                                                         "AAOCAQ8AMIIBCgKCAQEAn9niWNIX7QJMqWzgmku/2OqtjTaY8HnNjYOKpl89X+ec\r\n" +
                                                                                                         "00bZCkbfLxbq6Rmqx8CNGmgtTn0gtpbrjmSwYQ6leMuKvpynEsLa2DA8wPJiD6Ns\r\n" +
                                                                                                         "8C5/bgsT6WPTYS3P4ofxtw/sZ8MJNzvufyHV+Ktj9xtWoaiIJ74eaNKRbe1hsSK8\r\n" +
                                                                                                         "lm1K098e98BPLV3HMEeMnl3ER4CSzirEdCuCW7J9bKYtZLudKOrxmiK87sOuQfK1\r\n" +
                                                                                                         "epEirRBL4j3bNmBAoEpvVpGiXI1V4kC3cQrZB47kfp1H6E+/tcfP2l4ua2Jjpcgm\r\n" +
                                                                                                         "b4ALW04YtLKJtX6hEO4MezT0KM1BXnLeiSa1ksRAPQIDAQABo4IBfDCCAXgwDAYD\r\n" +
                                                                                                         "VR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCA6gwFgYDVR0lAQH/BAwwCgYIKwYBBQUH\r\n" +
                                                                                                         "AwIwHQYDVR0OBBYEFH22bS4zyentbStcRU7NQHWUvZzpMF0GA1UdHwRWMFQwUqBQ\r\n" +
                                                                                                         "oE6GTGh0dHA6Ly9wa2ktY3JsLnN5bWF1dGguY29tL2NhXzZhOTM3NzM0YTM5M2Ew\r\n" +
                                                                                                         "ODA1YmYzM2NkYThiMzMxMDkzL0xhdGVzdENSTC5jcmwwNwYIKwYBBQUHAQEEKzAp\r\n" +
                                                                                                         "MCcGCCsGAQUFBzABhhtodHRwOi8vcGtpLW9jc3Auc3ltYXV0aC5jb20wHwYDVR0j\r\n" +
                                                                                                         "BBgwFoAUa29LtvE3uis8fxjNuiuyuXwqN+swLQYKYIZIAYb4RQEQAwQfMB0GE2CG\r\n" +
                                                                                                         "SAGG+EUBEAECAwEBgamQ4QMWBjk1NzYwODA5BgpghkgBhvhFARAFBCswKQIBABYk\r\n" +
                                                                                                         "YUhSMGNITTZMeTl3YTJrdGNtRXVjM2x0WVhWMGFDNWpiMjA9MA0GCSqGSIb3DQEB\r\n" +
                                                                                                         "CwUAA4ICAQAzdc4fUWcBnO056a9CZsj4ofoKagb+3Y9iP4skGiMI0Wi3GhDpYTgS\r\n" +
                                                                                                         "7nrZP4rtrhBdkJ7qlEaSYCa3fxStOwnjGbMT4g7WB2XovCbDEZbd9qE2j6KR3xoS\r\n" +
                                                                                                         "22ZegnTNLywEe3g+2lRYhS7ws6BqUv50NPkxSpqPDwskmMDU+BsIrblzdDbbI6fO\r\n" +
                                                                                                         "QNHBwotlNvbq89oaOjQXH0WTRPAq1qnBKr9J64PMHt4+XrTPbl29B93y6+4bHXXP\r\n" +
                                                                                                         "5bunRA19GXp3/sUZqwfS3un5pfD10m5Qkjl3/bpv1uhMGn6Fl0icqq7uE6mg0FZg\r\n" +
                                                                                                         "A2mzlziBkcdgNTiX1Yu8iQUwOsShK5fD7YQpONXEKOjMjNjrw/OQg/iGoPlbAszq\r\n" +
                                                                                                         "KQWCiNlkF9HjWz6Hy35K15MFWWqgnKjToETt/+MqvCaYAk/nyKremN7IH7WcnDp3\r\n" +
                                                                                                         "G/yvKb95ukZ+qcIgC9S7mOfjyuNaPzlk/zkCQ1JT106ettgy9j99KTJagBYlIOIh\r\n" +
                                                                                                         "r8E+yGEA8k2/NjGKhq6f7AEMsmXeHg+ZVYFQph5wGdXSuFjjTkf9poGB93OZIkUP\r\n" +
                                                                                                         "YUr0TPRJMvWPfHsWdkX+dJJSVbmQKaM7JGj1TKrpbQ/yn5j+jt94AiCzIVEkDe3U\r\n" +
                                                                                                         "Oe8PlqaoGF+d6Csp9U25JzfpW0WadJLQsqtw9c1LgJKAMJ0bNVYtbQ==\r\n" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "https://albertpatron.com/as4/as4")
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
