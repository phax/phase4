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

import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;

import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Example for sending something to the my test endpoint [AT].
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderHelgerLargeFileLocal
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderHelgerLargeFileLocal.class);

  public static void send ()
  {
    try
    {
      final FileSystemResource aRes = new FileSystemResource (new File ("src/test/resources/external/examples/large-files/base-example-large-10m.xml"));
      if (!aRes.exists ())
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final Wrapper <Phase4Exception> aSendingExceptionKeeper = new Wrapper <> ();
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .httpClientFactory (new HttpClientSettings ().setRetryCount (0)
                                                                               .setResponseTimeout (Timeout.ofMinutes (5)))
                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aRes)
                                  .compressPayload (false)
                                  .receiverEndpointDetails (CertificateHelper.convertStringToCertficateOrNull ("-----BEGIN CERTIFICATE-----\r\n" +
                                                                                                               "MIIF2DCCA8CgAwIBAgIQYbLe2oppC0RLwIXuavv2ijANBgkqhkiG9w0BAQsFADBr\r\n" +
                                                                                                               "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\r\n" +
                                                                                                               "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\r\n" +
                                                                                                               "RVNUIENBIC0gRzIwHhcNMjMwMzA2MDAwMDAwWhcNMjUwMjIzMjM1OTU5WjBlMQsw\r\n" +
                                                                                                               "CQYDVQQGEwJBVDEpMCcGA1UECgwgUGhpbGlwIEhlbGdlciBJVCBDb25zdWx0aW5n\r\n" +
                                                                                                               "IGUuVS4xFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRIwEAYDVQQDDAlQT1AwMDAz\r\n" +
                                                                                                               "MDYwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDN7itSJEk2TfPQr+GV\r\n" +
                                                                                                               "1WM4oshzt/FmDyRlH1gu2AKwTiD2uwvsxEB5OsWrN0tfJOpkVMmo9rdL8+Vik2K5\r\n" +
                                                                                                               "6sJi72lV3S+aYPTC1yMN+fCj4JpP0HMGs5mAfcSjx3McGwu7u3fIa1wwHqRKEIE2\r\n" +
                                                                                                               "RVhH3MtfhpEMeonJMQDNZKvEHx5rlqSJ9mvSvHt+ErZ1p16x0p5B+rBeaFKOI2zb\r\n" +
                                                                                                               "A/bph4nj7n5gG5FbjfJdjG1AfKYt2+8koGcnBL/5tXHRF9LZsAFOdnaC9JpWOTkW\r\n" +
                                                                                                               "iGeVAqYoYVqH1fEWgK8ZX8u4RMSIJxCZpxh4M1j8JFY7ER61dBilL7/mqZa3fWc6\r\n" +
                                                                                                               "LL3tAgMBAAGjggF8MIIBeDAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIDqDAW\r\n" +
                                                                                                               "BgNVHSUBAf8EDDAKBggrBgEFBQcDAjAdBgNVHQ4EFgQUe1fJU+ZGii244TnHXTHH\r\n" +
                                                                                                               "eIgR4g0wXQYDVR0fBFYwVDBSoFCgToZMaHR0cDovL3BraS1jcmwuc3ltYXV0aC5j\r\n" +
                                                                                                               "b20vY2FfNmE5Mzc3MzRhMzkzYTA4MDViZjMzY2RhOGIzMzEwOTMvTGF0ZXN0Q1JM\r\n" +
                                                                                                               "LmNybDA3BggrBgEFBQcBAQQrMCkwJwYIKwYBBQUHMAGGG2h0dHA6Ly9wa2ktb2Nz\r\n" +
                                                                                                               "cC5zeW1hdXRoLmNvbTAfBgNVHSMEGDAWgBRrb0u28Te6Kzx/GM26K7K5fCo36zAt\r\n" +
                                                                                                               "BgpghkgBhvhFARADBB8wHQYTYIZIAYb4RQEQAQIDAQGBqZDhAxYGOTU3NjA4MDkG\r\n" +
                                                                                                               "CmCGSAGG+EUBEAUEKzApAgEAFiRhSFIwY0hNNkx5OXdhMmt0Y21FdWMzbHRZWFYw\r\n" +
                                                                                                               "YUM1amIyMD0wDQYJKoZIhvcNAQELBQADggIBAD48IqM2cE7dKNvlKTeTXSrkaf2D\r\n" +
                                                                                                               "35lmc19oLASrzZd2OluXSdukCTZIbFwcSdWIJDkTxQB9cnqqUAaxjYTbeCAAAIdE\r\n" +
                                                                                                               "wiknFZ1k0/rKws3Azb656rzfdRPLOaRBnLj5zNbnTClPMcbrkq5M0cyZ79CFsRj5\r\n" +
                                                                                                               "JarUcGt0FAqG3Fgs+3FkbJR9wGpaIiag/+EqDk+EaDRCDInbVKtq6l3uwaObDNl/\r\n" +
                                                                                                               "mrmfPN9r6BNhIJvJ7fPZt6VMxYFVVa3GTJlIs61UB0sTcs+ZyvaXWxauNnnDQgub\r\n" +
                                                                                                               "3tCIkvXA9K9qM5V4gq1DfBTQj2epnnf6o+KndPTizNIuh4JSzcWT06beLOCbDHF1\r\n" +
                                                                                                               "BTKOpbTgo/a35cBj6KLz/IFoQIGokbwUFS5qiI9+TYsvcUkA7cOy6BAKBxVEVTjC\r\n" +
                                                                                                               "0faALWvahBdjnW+ZM5/bHZkce4zAO+hVrHkGRU1F8i78/tTW68wctKgA11EAdETg\r\n" +
                                                                                                               "9GivfJ1UlVk0/yaPBnc64lonvp9kpznh/I6gs6wPzJT03IivcPObXjF6MN7eXsLY\r\n" +
                                                                                                               "Pewt6D0SBequ+/2fDIqvAppY6ltRW5BiO7lTyxCCzDjXlZYFvQ0pibEwaNVqm4HG\r\n" +
                                                                                                               "6QcqOYE7gkpkJuBrwt0pppF0KoY8R3SBcCRSFh0JJjC3pZiYDCgco0udVynuGSN/\r\n" +
                                                                                                               "q39FJgIVebxRFgoj\r\n" +
                                                                                                               "-----END CERTIFICATE-----\r\n" +
                                                                                                               ""),
                                                            "http://localhost:8080/as4")
                                  .disableValidation ()
                                  .sendMessageAndCheckForReceipt (aSendingExceptionKeeper::set);
      LOGGER.info ("Peppol send result: " + eResult);

      final Phase4Exception aSendingEx = aSendingExceptionKeeper.get ();
      if (aSendingEx != null)
      {
        if (aSendingEx.getCause () instanceof ExtendedHttpResponseException)
        {
          final ExtendedHttpResponseException exr = (ExtendedHttpResponseException) aSendingEx.getCause ();
          LOGGER.error ("Error sending Peppol message via AS4 due to HTTP status code " + exr.getStatusCode ());
        }
        else
          LOGGER.error ("Exception sending AS4 user message", aSendingEx);
      }
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
