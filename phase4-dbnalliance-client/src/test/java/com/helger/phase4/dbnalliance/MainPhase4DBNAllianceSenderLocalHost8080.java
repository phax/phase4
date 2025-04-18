/*
 * Copyright (C) 2024-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance;

import java.io.File;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.commons.debug.GlobalDebug;
import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Special main class with a constant receiver. This one skips the SMP lookup.
 *
 * @author Philip Helger
 */
public final class MainPhase4DBNAllianceSenderLocalHost8080
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4DBNAllianceSenderLocalHost8080.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Required for "http" only connections
    GlobalDebug.setDebugModeDirect (true);

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
      final BDXR2ParticipantIdentifier aReceiverID = Phase4DBNAllianceSender.IF.createParticipantIdentifier ("us:ein",
                                                                                                             "365060483");
      final EAS4UserMessageSendResult eResult = Phase4DBNAllianceSender.builder ()
                                                                       .documentTypeID (SimpleIdentifierFactory.INSTANCE.createDocumentTypeIdentifier ("bdx-docid-qns",
                                                                                                                                                       "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##DBNAlliance-1.0-data-Core"))
                                                                       .processID (Phase4DBNAllianceSender.IF.createProcessIdentifier (null,
                                                                                                                                       "bdx:noprocess"))
                                                                       .senderParticipantID (Phase4DBNAllianceSender.IF.createParticipantIdentifier ("us:ein",
                                                                                                                                                     "365060483"))
                                                                       .receiverParticipantID (aReceiverID)
                                                                       .fromPartyID ("365060483")
                                                                       .payloadElement (aPayloadElement)
                                                                       .receiverEndpointDetails (CertificateHelper.convertStringToCertficate ("-----BEGIN CERTIFICATE-----\n" +
                                                                                                                                              "MIIFJDCCBAygAwIBAgIUT6KGH19DzQ39GUvgFl/9pNUCRigwDQYJKoZIhvcNAQEL\n" +
                                                                                                                                              "BQAwgbgxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIEwVUZXhhczEQMA4GA1UEBxMHSG91\n" +
                                                                                                                                              "c3RvbjEOMAwGA1UEERMFNzcwNTYxHjAcBgNVBAkTFTMgUml2ZXIgV2F5IFN1aXRl\n" +
                                                                                                                                              "IDkyMDEqMCgGA1UEChMhRGlnaXRhbCBCdXNpbmVzcyBOZXR3b3JrIEFsbGlhbmNl\n" +
                                                                                                                                              "MSswKQYDVQQDEyJEQk5BbGxpYW5jZSBEZW1vIEludGVybWVkaWF0ZSBUZXN0MB4X\n" +
                                                                                                                                              "DTI0MTEyOTEzNTYyMloXDTI1MTEyOTEzNTYyMlowgYkxHTAbBgkqhkiG9w0BCQEW\n" +
                                                                                                                                              "DmRpcmtAYmlsbGl0LmV1MRowGAYDVQQDDBFCRTpFTjo6MDU2Mzg0Njk0NDEcMBoG\n" +
                                                                                                                                              "A1UECwwTQmlsbGl0IERCTkEgVEVTVCBBUDESMBAGA1UECgwJQmlsbGl0IGJ2MQ0w\n" +
                                                                                                                                              "CwYDVQQIDARPLVZMMQswCQYDVQQGEwJCRTCCASIwDQYJKoZIhvcNAQEBBQADggEP\n" +
                                                                                                                                              "ADCCAQoCggEBALpxGh3+MI2x/c/vNCGQ5/jar1S/7OaFJ9HeDCfDKVgzv/B1PxdG\n" +
                                                                                                                                              "elY8k1CNbdljdFvDxjjOCWfiOHMLxKDexMN+LA4o+tzYy0IDUnGPNHyY1ptX61Sf\n" +
                                                                                                                                              "tqIoOC2uG942ehLiqbei9bMPUgwvSedvSkxYdo2d+pqCkl1zSTeEZTz6MLapHLkQ\n" +
                                                                                                                                              "4H0+5QRJd0nficM0CCAyBVxV3pnG1sA63XC0RwIY2qFo4CvN4+RylEIhCsSEVcaW\n" +
                                                                                                                                              "e+dgdcEa3tJqKj240y3r8upsuZaeQV/M1UikD8uJgaourEqpDdbIeIhNEAJco9z4\n" +
                                                                                                                                              "1gdPn/ykOl96m2nDF4ClEUVYBKmVPpRQQz8CAwEAAaOCAVEwggFNMAwGA1UdEwEB\n" +
                                                                                                                                              "/wQCMAAwHQYDVR0OBBYEFO4WgeeajIw0/hw/GdHdKt2+jbA7MB8GA1UdIwQYMBaA\n" +
                                                                                                                                              "FKK1Hb8CC6ZJcjOA8rhWbyJ5+CP1MA4GA1UdDwEB/wQEAwIEsDCBlQYIKwYBBQUH\n" +
                                                                                                                                              "AQEEgYgwgYUwLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3NwLmRlbW8ub25lLmRpZ2lj\n" +
                                                                                                                                              "ZXJ0LmNvbTBUBggrBgEFBQcwAoZIaHR0cDovL2NhY2VydHMuZGVtby5vbmUuZGln\n" +
                                                                                                                                              "aWNlcnQuY29tL0RCTkFsbGlhbmNlRGVtb0ludGVybWVkaWF0ZVRlc3QuY3J0MFUG\n" +
                                                                                                                                              "A1UdHwROMEwwSqBIoEaGRGh0dHA6Ly9jcmwuZGVtby5vbmUuZGlnaWNlcnQuY29t\n" +
                                                                                                                                              "L0RCTkFsbGlhbmNlRGVtb0ludGVybWVkaWF0ZVRlc3QuY3JsMA0GCSqGSIb3DQEB\n" +
                                                                                                                                              "CwUAA4IBAQBbzLqVazRmMa97dtu1MO9c/iiDDeGfBaUu2IoeyF7b9aHe0cGVqeZN\n" +
                                                                                                                                              "V00ZAT/lu/ANl8E2IhB0HrkMUDdJrpOnp42SP4I8rvrSIJZ4hXqYg69qw6gMG1CT\n" +
                                                                                                                                              "6m75nmoaYvaTz+X0Y6JrxBrxT+BiXt9MvMdlFPo88LL0aHQM2uCgCP4P6ypg58gQ\n" +
                                                                                                                                              "t57xKNk1elaxceN/aH8d88aW2HafM4GsBgH5XT4d7Nhw8kYMDo9SBpKF9Y/OFZO5\n" +
                                                                                                                                              "FoH4kjpB9jeYQBY1OTdLhXMR0pej4u5ce0l3yujPIPpP8qiVByySm5/y46DQ4f8I\n" +
                                                                                                                                              "8CDoy3zqWaXfamOYQoqHMgEcGGdAul7t\n" +
                                                                                                                                              "-----END CERTIFICATE-----\n"),
                                                                                                 "http://localhost:8080/as4")
                                                                       .sendMessageAndCheckForReceipt ();
      LOGGER.info ("DBNAlliance send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending DBNAlliance message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
