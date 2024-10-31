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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolValidatonResultHandler;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phive.peppol.PeppolValidation2024_05;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Example for sending something to the Qvalia [SE] test endpoint.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderQvalia
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderQvalia.class);

  public static void send ()
  {
    try
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/external/examples/base-example.xml"))
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
          LOGGER.info ("Sending out AS4 message with conversation ID '" +
                       aUserMsg.getEbms3UserMessage ().getCollaborationInfo ().getConversationId () +
                       "'");
        }
      };

      // Invalid certificate is valid until 2029
      final IAS4CryptoFactory cf = true ? AS4CryptoFactoryConfiguration.getDefaultInstance ()
                                        : new AS4CryptoFactoryInMemoryKeyStore (KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.JKS,
                                                                                                                   "invalid-keystore-pw-peppol.jks",
                                                                                                                   "peppol".toCharArray ()),
                                                                                "1",
                                                                                "peppol".toCharArray (),
                                                                                KeyStoreHelper.loadKeyStore (PeppolKeyStoreHelper.TRUSTSTORE_TYPE,
                                                                                                             PeppolKeyStoreHelper.Config2018.TRUSTSTORE_AP_PRODUCTION_CLASSPATH,
                                                                                                             PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD.toCharArray ())
                                                                                              .getKeyStore ());
      final EAS4UserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .httpRetrySettings (new HttpRetrySettings ().setMaxRetries (0))
                                  .cryptoFactory (cf)
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID ("POP000306")
                                  .countryC1 ("AT")
                                  .payload (aPayloadElement)
                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                     aReceiverID,
                                                                     ESML.DIGIT_TEST))
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                  .validationConfiguration (PeppolValidation2024_05.VID_OPENPEPPOL_INVOICE_UBL_V3,
                                                            new Phase4PeppolValidatonResultHandler ())
                                  .buildMessageCallback (aBuildMessageCallback)
                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol send result: " + eResult);
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

    final AS4OutgoingDumperFileBased aODF = new AS4OutgoingDumperFileBased ()
    {
      @Override
      protected OutputStream openOutputStream (@Nonnull final EAS4MessageMode eMsgMode,
                                               @Nullable final IAS4IncomingMessageMetadata aMessageMetadata,
                                               @Nullable final IAS4IncomingMessageState aState,
                                               @Nonnull @Nonempty final String sMessageID,
                                               @Nullable final HttpHeaderMap aCustomHeaders,
                                               @Nonnegative final int nTry) throws IOException
      {
        final OutputStream ret = super.openOutputStream (eMsgMode,
                                                         aMessageMetadata,
                                                         aState,
                                                         sMessageID,
                                                         aCustomHeaders,
                                                         nTry);

        // Write headers into a separate file
        File aHeaderFile = getFileProvider ().getFile (eMsgMode, sMessageID, nTry);
        aHeaderFile = new File (aHeaderFile.getParentFile (), aHeaderFile.getName () + ".headers");
        final IJsonObject aHeaders = new JsonObject ();
        for (final Map.Entry <String, ICommonsList <String>> e : aCustomHeaders)
          if (e.getValue ().size () == 1)
            aHeaders.add (e.getKey (), e.getValue ().getFirstOrNull ());
          else
            aHeaders.add (e.getKey (), new JsonArray ().addAll (e.getValue ()));
        SimpleFileIO.writeFile (aHeaderFile,
                                aHeaders.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED),
                                StandardCharsets.UTF_8);

        // Return the main OS
        return ret;
      }
    };
    aODF.setIncludeHeaders (false);
    AS4DumpManager.setOutgoingDumper (aODF);

    try
    {
      send ();
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
