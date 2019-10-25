/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.id.factory.FileIntIDFactory;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.servlet.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.serialize.read.DOMReader;

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
    GlobalIDFactory.setPersistentIntIDFactory (new FileIntIDFactory (WebFileIO.getDataIO ().getFile ("ids.dat")));

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      // Start configuring here
      final HttpClientFactory aHCF = new Phase4HttpClientFactory ();
      final IPMode aSrcPMode = Phase4PeppolSender.PMODE_RESOLVER.getPModeOfID (null, "s", "a", "i", "r", null);
      final IDocumentTypeIdentifier aDocTypeID = Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
      final IProcessIdentifier aProcID = Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
      final IParticipantIdentifier aSenderID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9914:abc");
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      final String sSenderPartyID = "POP000306";
      final String sConversationID = UUID.randomUUID ().toString ();
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ();
      final IMimeType aMimeType = CMimeType.APPLICATION_XML;
      final boolean bCompress = true;
      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  ESML.DIGIT_TEST);
      final Consumer <AS4ClientSentMessage <byte []>> aResponseConsumer = aResponseEntity -> {
        if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
        {
          final String sMessageID = aResponseEntity.getMessageID ();
          final String sFilename = "outgoing/" +
                                   PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                                   "-" +
                                   FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
                                   "-response.xml";
          final File aResponseFile = new File (AS4ServerConfiguration.getDataPath (), sFilename);
          if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
            LOGGER.info ("Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
          else
            LOGGER.error ("Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
        }
      };
      final Consumer <Ebms3SignalMessage> aSignalMsgConsumer = null;

      // Start sending
      if (Phase4PeppolSender.sendAS4Message (aHCF,
                                             aSrcPMode,
                                             aDocTypeID,
                                             aProcID,
                                             aSenderID,
                                             aReceiverID,
                                             sSenderPartyID,
                                             sConversationID,
                                             aPayloadElement,
                                             aMimeType,
                                             bCompress,
                                             aSMPClient,
                                             aResponseConsumer,
                                             aSignalMsgConsumer)
                            .isSuccess ())
      {
        LOGGER.info ("Finished sending PEPPOL message via AS4");
      }
      else
      {
        LOGGER.error ("Failed to send PEPPOL message via AS4");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending PEPPOL message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
