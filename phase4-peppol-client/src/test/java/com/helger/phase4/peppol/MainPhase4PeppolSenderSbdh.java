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
package com.helger.phase4.peppol;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.resource.FileSystemResource;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.photon.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * The main class that requires manual configuration before it can be run. This
 * is a dummy and needs to be adopted to your needs.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderSbdh
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderSbdh.class);

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
      final PeppolSBDHData aSbdh = new PeppolSBDHDocumentReader (PeppolIdentifierFactory.INSTANCE).extractData (new FileSystemResource ("src/test/resources/external/examples/base-sbdh.xml"));
      if (aSbdh == null)
        throw new IllegalStateException ("Failed to read SBDH file to be send");

      // Start configuring here
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.sbdhBuilder ()
                                  .payloadAndMetadata (aSbdh)
                                  .senderPartyID ("POP000306")
                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                     aSbdh.getReceiverAsIdentifier (),
                                                                     ESML.DIGIT_TEST))
                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol SBDH message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
