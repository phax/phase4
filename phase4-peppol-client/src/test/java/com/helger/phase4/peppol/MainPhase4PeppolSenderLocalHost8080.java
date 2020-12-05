/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.phase4.servlet.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.servlet.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.servlet.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLocalHost8080
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderLocalHost8080.class);

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
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .payload (aPayloadElement)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                         "MIIF2DCCA8CgAwIBAgIQKR+R7Xx3fiCbQqLrW+xzFjANBgkqhkiG9w0BAQsFADBr\n" +
                                                                                                         "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\n" +
                                                                                                         "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\n" +
                                                                                                         "RVNUIENBIC0gRzIwHhcNMTkwNzAxMDAwMDAwWhcNMjEwNjIwMjM1OTU5WjBlMRIw\n" +
                                                                                                         "EAYDVQQDDAlQT1AwMDAzMDYxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMSkwJwYD\n" +
                                                                                                         "VQQKDCBQaGlsaXAgSGVsZ2VyIElUIENvbnN1bHRpbmcgZS5VLjELMAkGA1UEBhMC\n" +
                                                                                                         "QVQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCkq8xggMHekg5jmK/E\n" +
                                                                                                         "0xw+2hBU4F61DQktWJRfPdRnZZx1MZIVGrRc9SuAPfE/+ScYXtPNHhVEOQrOz2BU\n" +
                                                                                                         "zhOps6puKRI9RBJQe83H1jhOkhisR/GnfvxvCdKEOl/S1O7TxqMWhwFIikwVraep\n" +
                                                                                                         "WuYAhz/PECyuNAhdgqlwiwfKRZkFOnj9eUtRpWV5emf2qqPv0A8Q1HSf72NzXdov\n" +
                                                                                                         "RsOW3/TQ0AC+wX9jDnAgkl3WlMConEsEDdq5ftiC/8StBOieYGLLVo9Ms/rEWGtN\n" +
                                                                                                         "I+nxb2MsNYjlbVUL/rpSGJmGa2Thjb9yLS6h3eR9NSilTELS45yyR/S70VaJu7QQ\n" +
                                                                                                         "4XEbAgMBAAGjggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIDqDAW\n" +
                                                                                                         "BgNVHSUBAf8EDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQUoXX+d4BRN03q5VzhgeWh\n" +
                                                                                                         "RMmatjQwXQYDVR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0aC5j\n" +
                                                                                                         "b20vY2FfNmE5Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0Q1JM\n" +
                                                                                                         "LmNybDA3BggrBgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2ktb2Nz\n" +
                                                                                                         "cC5zeW1hdXRoLmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo36zAt\n" +
                                                                                                         "BgpghkgBhvhFARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4MDkG\n" +
                                                                                                         "CmCGSAGG+EUBEAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZWFYw\n" +
                                                                                                         "YUM1amIyMD0wDQYJKoZIhvcNAQELBQADggIBAC52VNwo92Gw4yYUudR8fZJtd7sc\n" +
                                                                                                         "jKNAwpEditJg3UQKnw9Sc0LETqxAVIJRpdDTp99JF8j1dy2LYmNuR1i7ZoI75scw\n" +
                                                                                                         "jK/71EteA5IqQBgB56Rx3pWyv0Nuz/Zo5VXCgrxqizLXOOMxCo6slstbFUrVbQIn\n" +
                                                                                                         "UGMYMW66R1sfBTxFd29rv9mkFl657tB6x21LL3pM6f23Q6pzXNmrMr1p6dFV6Kpe\n" +
                                                                                                         "ob3x64AjyabXxsTlbmRCzkMDvoBNKpy2NwdfptknZjgF2LnJPFlk7tGskbkAoE4i\n" +
                                                                                                         "wfvdFSyQvP36VwTiIgtkkQlhL+wRNhEkA4XotCHwyMgKD4RviZJ4acDEsOEXwGUT\n" +
                                                                                                         "orB6WfEiczRGwqzQGeUq3qQy9UYwbaGl3UTIPR/SrQTWLINuX4Rmg+Lb+WhwLXhl\n" +
                                                                                                         "+t79X90CGN6KnrAfNumTnjAvSH/ssKzWhJdnimuT0JBy0VZfVD2axjOcpYvgkits\n" +
                                                                                                         "lsISVqHjIzpUBY1UCEV+jzKktUqTKTGHDQAdQGAjfl6IdrpSIpasBOZ+IAW51yF6\n" +
                                                                                                         "tUD6XRdDJak2MKYTD0hkdTYu72NOWfN4VmWLYPpHTPsT8UZRyZyvPsNzT3Jr2OAH\n" +
                                                                                                         "2mXvBcFvVD5bxFMQG6t3NxFOFTyh+U7f2NGs1mhX8AZ4dzI0ClSLJsjKOHJEQDhI\n" +
                                                                                                         "TsWPMVgDKpgJ4Dm7" +
                                                                                                         "-----END CERTIFICATE-----\r\n"),
                                                            "http://localhost:8080/as4")
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
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
