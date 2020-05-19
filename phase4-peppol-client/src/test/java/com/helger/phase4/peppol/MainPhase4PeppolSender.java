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

import com.helger.bdve.peppol.PeppolValidation390;
import com.helger.bdve.result.ValidationResultList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.servlet.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.servlet.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The main class that requires manual configuration before it can be run. This
 * is a dummy and needs to be adopted to your needs.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSender.class);

  public static void main (final String [] args)
  {
    // Provide context
    GlobalDebug.setDebugModeDirect (false);
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    final File aSCPath = new File (AS4ServerConfiguration.getDataPath ()).getAbsoluteFile ();
    WebFileIO.initPaths (aSCPath, aSCPath.getAbsolutePath (), false);

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ();

      // Start configuring here
      IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9958:peppol-development-governikus-01");
      if (false)
        aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("0088:5050689000018as4");
      if (Phase4PeppolSender.builder ()
                            .setDocumentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                            .setProcessID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                            .setSenderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9914:phase4-test-sender"))
                            .setReceiverParticipantID (aReceiverID)
                            .setSenderPartyID ("POP000306")
                            .setPayload (aPayloadElement)
                            .setSMPClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER, aReceiverID, ESML.DIGIT_TEST))
                            .setRawResponseConsumer (new ResponseConsumerWriteToFile ())
                            .setValidationConfiguration (PeppolValidation390.VID_OPENPEPPOL_INVOICE_V3,
                                                         new Phase4PeppolValidatonResultHandler ()
                                                         {
                                                           @Override
                                                           public void onValidationSuccess (final ValidationResultList aValidationResult)
                                                           {
                                                             LOGGER.info ("Successfully validated XML payload");
                                                           }
                                                         })
                            .sendMessage ()
                            .isSuccess ())
      {
        LOGGER.info ("Successfully sent Peppol message via AS4");
      }
      else
      {
        LOGGER.error ("Failed to send Peppol message via AS4");
      }
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
