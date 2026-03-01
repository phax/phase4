/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
import org.w3c.dom.Element;

import com.helger.base.debug.GlobalDebug;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderLocalHost8080 extends AbstractPhase4Sender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4PeppolSenderLocalHost8080.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Required for "http" only connections
    GlobalDebug.setDebugModeDirect (true);

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    Phase4LogCustomizer.setThreadLocalLogPrefix ("[SendLocalHost8080] ");

    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/" +
                                                                      (true ? "base-example.xml"
                                                                            : "large-files/base-example-large-1gb.xml")))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ("Failed to read XML file to be send");

      // Start configuring here
      final boolean bUseG3 = true;
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final EAS4UserMessageSendResult eResult = Phase4PeppolSender.builder ()
                                                                  .peppolAP_CAChecker (PeppolTrustedCA.peppolTestAP ())
                                                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                                                  .receiverParticipantID (aReceiverID)
                                                                  .senderPartyID ("POP000306")
                                                                  .countryC1 ("AT")
                                                                  .payload (aPayloadElement)
                                                                  .receiverEndpointDetails (pem2cert (bUseG3 ? "-----BEGIN CERTIFICATE-----\n" +
                                                                                                               "MIIFsDCCA5igAwIBAgIUF5iu6+gA+IBNSaWYX4mmzDxrQskwDQYJKoZIhvcNAQEL\n" +
                                                                                                               "BQAwazELMAkGA1UEBhMCQkUxGTAXBgNVBAoTEE9wZW5QRVBQT0wgQUlTQkwxFjAU\n" +
                                                                                                               "BgNVBAsTDUZPUiBURVNUIE9OTFkxKTAnBgNVBAMTIFBFUFBPTCBBQ0NFU1MgUE9J\n" +
                                                                                                               "TlQgVEVTVCBDQSAtIEczMB4XDTI1MDkwOTAwMDAwMFoXDTI3MDgyOTIzNTk1OVow\n" +
                                                                                                               "XjELMAkGA1UEBhMCQVQxIjAgBgNVBAoMGUhlbGdlciBJVCBDb25zdWx0aW5nIEdt\n" +
                                                                                                               "YkgxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRIwEAYDVQQDDAlQT1AwMDAzMDYw\n" +
                                                                                                               "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC9hHOrfLXSMBe6VXXU1dBX\n" +
                                                                                                               "6oqnRvx3IUwrBIn+NGuYl04FOm7nm7tZ6QIM2KTzuz7v9HFIgamHlxfHHk4GHYOL\n" +
                                                                                                               "ye6xQfEEK51WxUt6N/erpttJ1dnr7X2kr2qbMTmWzPmW0puKi+ugFaLgMrkxu3yS\n" +
                                                                                                               "GVKYRWnv1xGsukcgNeLQbOFwmMf47NfkiRNz9NymYTSc0/dgN6HCl1DHB1UtfoH1\n" +
                                                                                                               "qWTnKkEK8B0puX59k8KqC8Qt/zbgyrC9n12V+b6GhixvS8ngmTp2AcBGcRaydL/G\n" +
                                                                                                               "nyT+MTx463Ia5AUYhy+KmYh40hljXwFUl2j5pn36IvK0mQgNxPcurmAvhmcmjY/f\n" +
                                                                                                               "AgMBAAGjggFXMIIBUzAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBS8P6H20f6L7fgp\n" +
                                                                                                               "9wPp3pDKTaAGhTAfBgNVHSMEGDAWgBSzzETvdq+Byd/zX6WeiHGtn6D3cDAOBgNV\n" +
                                                                                                               "HQ8BAf8EBAMCBLAwFgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwgYoGCCsGAQUFBwEB\n" +
                                                                                                               "BH4wfDArBggrBgEFBQcwAYYfaHR0cDovL29jc3Aub25lLm5sLmRpZ2ljZXJ0LmNv\n" +
                                                                                                               "bTBNBggrBgEFBQcwAoZBaHR0cDovL2NhY2VydHMub25lLm5sLmRpZ2ljZXJ0LmNv\n" +
                                                                                                               "bS9QRVBQT0xBQ0NFU1NQT0lOVFRFU1RDQS1HMy5jcnQwTgYDVR0fBEcwRTBDoEGg\n" +
                                                                                                               "P4Y9aHR0cDovL2NybC5vbmUubmwuZGlnaWNlcnQuY29tL1BFUFBPTEFDQ0VTU1BP\n" +
                                                                                                               "SU5UVEVTVENBLUczLmNybDANBgkqhkiG9w0BAQsFAAOCAgEAXbHWJXYKfhIin2T1\n" +
                                                                                                               "o/RciJeAXEPbyh/lEqvw1TaSWxz0swo0HhVxC9XAA2ufZaZjOgps4tZTmxG9IxkX\n" +
                                                                                                               "XO9B3nngMU3DOOfW73cYA0mFrDHLnaALaxgenCVgLyN/f22/8cbHYoUDNoOSBVCZ\n" +
                                                                                                               "j0TFNpnMQvVXvzvr8lNjpf6hTevPvZEa+8Vy5aBlwbbGh1L8cgc1WToJ9OX7n7zz\n" +
                                                                                                               "1waKwqyPukTjsNLiz7Nc6nOtBXUCgoTY+9G24FEN0k11kkM3cnhTODeQFZiRGHKv\n" +
                                                                                                               "RdaZpBODyuVm/U8d5ewAFvpW1N+wTOVkB372Ctg0mMtN+6xd5/VJdoTYOFkjQ/Fa\n" +
                                                                                                               "m32bZVJ2z8O5meGqWoDHLKoZfMYj6VMiwjTtsF1MK0qgj9AKvCcE6NGn5R8hZikq\n" +
                                                                                                               "tp/Kvx4I8X94dK+jZr7AKZlJsqaQGhEo1BX0LN2JcnwAc3oNHOyzj3exfkPspUjf\n" +
                                                                                                               "9DgwLZ+PNrSSLk+tJsMxEJTSIntSSpZOwhGtws5ZtQX3UEp2SwSUrnylCkgXq7yq\n" +
                                                                                                               "qA3lX/8chHDoI+A1VNR1VOhO2cJN3I+mj16GoEFGxqroB9dcI2ARA9qpKXFX6P1g\n" +
                                                                                                               "MPg8omdlrplC5QrXgIF23tXDeOmK/ezvlp2kP2g/KiCbVqvt8l2wwaTRqzNnSC0T\n" +
                                                                                                               "RJ8ByMAV0Fj5XGS1Kl5UGv/BqGY=\n" +
                                                                                                               "-----END CERTIFICATE-----\n"
                                                                                                             : "-----BEGIN CERTIFICATE-----\n" +
                                                                                                               "MIIF0TCCA7mgAwIBAgIQcyxSArntXaqTdp2N6B0D2DANBgkqhkiG9w0BAQsFADBr\n" +
                                                                                                               "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDEWMBQGA1UE\n" +
                                                                                                               "CxMNRk9SIFRFU1QgT05MWTEpMCcGA1UEAxMgUEVQUE9MIEFDQ0VTUyBQT0lOVCBU\n" +
                                                                                                               "RVNUIENBIC0gRzIwHhcNMjUwMjI1MDAwMDAwWhcNMjcwMjE1MjM1OTU5WjBeMQsw\n" +
                                                                                                               "CQYDVQQGEwJBVDEiMCAGA1UECgwZSGVsZ2VyIElUIENvbnN1bHRpbmcgR21iSDEX\n" +
                                                                                                               "MBUGA1UECwwOUEVQUE9MIFRFU1QgQVAxEjAQBgNVBAMMCVBPUDAwMDMwNjCCASIw\n" +
                                                                                                               "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK7YW/30et1kZK/l3hKrxJEr0NkC\n" +
                                                                                                               "f/mzjkQPUh8jKyd4YZrgiod/Ry/xnp2eHcHV2Aiukk9kMkg8Ptf5W8jMgvlKeN58\n" +
                                                                                                               "dHp890vupeh4iOPdq0sJ9B3HJhXQHgxhe90CZIsJi8fn7fFawMHPuVDmwvrnzYWl\n" +
                                                                                                               "c0qF/xXFqM/NwBWiqKikp5lvVvZUehzJiRmEY0c1uFoXZClqUmcmmWGOBWzj8nW6\n" +
                                                                                                               "IeIsZ9GurNG+9zlT6L3JRJoJCluzTjjbk4XKqEQFiP4aiDAa1nuIzMea3DkB2nx4\n" +
                                                                                                               "0L8TwZEO2d8Xecr3xTfkyq92eHyStyIlEW1459bOSa56Yp6Mlu7JFKmTgLkCAwEA\n" +
                                                                                                               "AaOCAXwwggF4MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgOoMBYGA1UdJQEB\n" +
                                                                                                               "/wQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBSBn6GQ+OAyiDWmqpU18M9DwzYRwTBd\n" +
                                                                                                               "BgNVHR8EVjBUMFKgUKBOhkxodHRwOi8vcGtpLWNybC5zeW1hdXRoLmNvbS9jYV82\n" +
                                                                                                               "YTkzNzczNGEzOTNhMDgwNWJmMzNjZGE4YjMzMTA5My9MYXRlc3RDUkwuY3JsMDcG\n" +
                                                                                                               "CCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL3BraS1vY3NwLnN5bWF1\n" +
                                                                                                               "dGguY29tMB8GA1UdIwQYMBaAFGtvS7bxN7orPH8Yzborsrl8KjfrMC0GCmCGSAGG\n" +
                                                                                                               "+EUBEAMEHzAdBhNghkgBhvhFARABAgMBAYGpkOEDFgY5NTc2MDgwOQYKYIZIAYb4\n" +
                                                                                                               "RQEQBQQrMCkCAQAWJGFIUjBjSE02THk5d2Eya3RjbUV1YzNsdFlYVjBhQzVqYjIw\n" +
                                                                                                               "PTANBgkqhkiG9w0BAQsFAAOCAgEAIWvpuipkFN2cSIIntNeoKfne7q9dFzJIqVTa\n" +
                                                                                                               "y7ZeODtcoNqEsawMzGrAAgOzyzudq+rdF0FMaywTHHvtPfHWuK96UZVIPZs1CFcO\n" +
                                                                                                               "lKYkQD0k47YxHc9VJUwoCB4PLgQk5pqdfbIigLd5oFXZmgI786Pkouu0LBHsH0Im\n" +
                                                                                                               "2OPH6a9EdFBECBYnS+w2PTycF/mxEru0btz4i8ZIOj2pHRBAoBCItykIJwTbqknH\n" +
                                                                                                               "H8CAm2mmEnnSxyE1qDui++c811Qn8H0NtQg9x2E57XkNQTrEDMOgNw5dyrp5izUm\n" +
                                                                                                               "vNdLfxFvPmvadNWVRq52MD23jU2QM1byWoYyBynlvxII829ZshjUGlycnpc7NyuQ\n" +
                                                                                                               "VbbPlb9Ku07ILaBDrI7qXJ3+Y8x7HOJ9qmX9nK4s5smWx6tsOUov9ZYlMvAqKipk\n" +
                                                                                                               "ycBe6fTVwylzfZWNKPw/6hqMM0vOz49Yv5yxUvGHEvjyTUBrEgLJytrP4jvlvY1S\n" +
                                                                                                               "ORISzCOn5IgZuHXYzTfd03+Z+uc0VgQHjyfNlTx/tMmrA4gCp4J6+G/mo5XXlSWF\n" +
                                                                                                               "fAExypSh97GCTm+BKN5KkpyR7+1WevpyFEKK0ug+9Dr8KkSKnGSuVJ7XVhDbV5oP\n" +
                                                                                                               "eTOT4HmMktEynedS61JX9We6Ilex07ak4tYouBdjwfyQmOJIgTrtchKmi5okoZeF\n" +
                                                                                                               "ZShLhVM=\n" +
                                                                                                               "-----END CERTIFICATE-----\n"),
                                                                                            "http://localhost:8080/as4")
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
