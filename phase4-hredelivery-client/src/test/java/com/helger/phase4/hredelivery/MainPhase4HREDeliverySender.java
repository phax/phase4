/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery;

import java.io.File;

import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.helger.hredelivery.commons.EHREDeliverySML;
import com.helger.hredelivery.commons.security.HREDeliveryTrustedCA;
import com.helger.hredelivery.commons.smp.HRMPSClientReadOnly;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.photon.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The main class that requires manual configuration before it can be run. This is a dummy and needs
 * to be adopted to your needs.
 *
 * @author Philip Helger
 */
public final class MainPhase4HREDeliverySender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MainPhase4HREDeliverySender.class);

  public static void main (final String [] args)
  {
    // Provide context
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    final File aSCPath = AS4Configuration.getDumpBasePathFile ();
    WebFileIO.initPaths (aSCPath, aSCPath.getAbsolutePath (), false);

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
      final IParticipantIdentifier aReceiverID = Phase4HREdeliverySender.IF.createParticipantIdentifierWithDefaultScheme ("9915:helger");
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4HREdeliverySender.builder ()
                                       .apCAChecker (HREDeliveryTrustedCA.hrEdeliveryFinaDemo ())
                                       .documentTypeID (Phase4HREdeliverySender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                       .processID (Phase4HREdeliverySender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                       .senderParticipantID (Phase4HREdeliverySender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                       .receiverParticipantID (aReceiverID)
                                       .senderPartyID ("POP000306")
                                       .payload (aPayloadElement)
                                       .smpClient (new HRMPSClientReadOnly (aReceiverID, EHREDeliverySML.DEMO))
                                       .sendMessageAndCheckForReceipt ();
      LOGGER.info ("HR eDelivery send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending HR eDelivery message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
