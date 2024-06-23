/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.euctp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.AS4CryptoProperties;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.mpc.MPC;
import com.helger.phase4.profile.euctp.EEuCtpAction;
import com.helger.phase4.profile.euctp.EEuCtpService;
import com.helger.phase4.profile.euctp.EuCtpPMode;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

public class MainPhase4EuCtpSenderExample
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4EuCtpSenderExample.class);

  @Nonnull
  private static AS4CryptoProperties _buildAs4CryptoProperties ()
  {
    final AS4CryptoProperties as4SigningProperties = new AS4CryptoProperties ();
    as4SigningProperties.setKeyStorePath (System.getenv ("AS4_SIGNING_KEYSTORE_PATH"));
    as4SigningProperties.setKeyStoreType (EKeyStoreType.PKCS12);
    as4SigningProperties.setKeyStorePassword (System.getenv ("AS4_SIGNING_KEYSTORE_PASSWORD"));
    as4SigningProperties.setKeyAlias (System.getenv ("AS4_SIGNING_KEY_ALIAS"));
    as4SigningProperties.setKeyPassword (System.getenv ("AS4_SIGNING_KEY_PASSWORD"));

    // must include the Taxud CA and intermediate certificates
    as4SigningProperties.setTrustStorePath (System.getenv ("AS4_SIGNING_TRUST_KEYSTORE_PATH"));
    as4SigningProperties.setTrustStoreType (EKeyStoreType.PKCS12);
    as4SigningProperties.setTrustStorePassword (System.getenv ("AS4_SIGNING_TRUST_KEYSTORE_PASSWORD"));
    return as4SigningProperties;
  }

  public static void main (final String [] args)
  {
    // Create scope for global variables that can be shut down gracefully
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Optional dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try
    {
      final KeyStore aSslKeyStore = KeyStore.getInstance ("pkcs12");
      final char [] aKeyStorePassword = System.getenv ("AS4_SSL_KEYSTORE_PASSWORD").toCharArray ();
      try (InputStream aIS = new FileInputStream (System.getenv ("AS4_SSL_KEYSTORE_PATH")))
      {
        aSslKeyStore.load (aIS, aKeyStorePassword);
      }

      final Phase4EuCtpHttpClientSettings aHttpClientSettings = new Phase4EuCtpHttpClientSettings (aSslKeyStore,
                                                                                                   aKeyStorePassword);

      final AS4CryptoProperties as4SigningProperties = _buildAs4CryptoProperties ();
      final AS4CryptoFactoryProperties cryptoFactoryProperties = new AS4CryptoFactoryProperties (as4SigningProperties);

      // configured on the STI
      final String fromPartyID = System.getenv ("AS4_FROM_PARTY_ID");

      if (false)
        _sendENSFilling (aHttpClientSettings, fromPartyID, cryptoFactoryProperties);
      else
        if (false)
        {
          final Wrapper <Ebms3SignalMessage> aSignalMsgHolder = new Wrapper <> ();
          _sendConnectionTest (aHttpClientSettings, fromPartyID, aSignalMsgHolder, cryptoFactoryProperties);
        }
        else
          _sendPullRequest (aHttpClientSettings, fromPartyID, cryptoFactoryProperties);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending euctp message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }

  private static void _sendPullRequest (final Phase4EuCtpHttpClientSettings aHttpClientSettings,
                                        final String fromPartyID,
                                        final AS4CryptoFactoryProperties cryptoFactoryProperties) throws Phase4Exception
  {
    final Wrapper <Ebms3UserMessage> aUserMessageHolder = new Wrapper <> ();
    final Wrapper <Ebms3SignalMessage> aSignalMessageHolder = new Wrapper <> ();
    final String sMPC = "urn:fdc:ec.europa.eu:2019:eu_ics2_c2t/EORI/" + fromPartyID;
    final IMPCManager aMpcMgr = MetaAS4Manager.getMPCMgr ();
    if (!aMpcMgr.containsWithID (sMPC))
    {
      // this will be needed when parsing the UserMessage
      aMpcMgr.createMPC (new MPC (sMPC));
    }

    final List <String> attachmentsAsString = new ArrayList <> ();
    final ESuccess eSuccess = new EuCtpPullRequestBuilder ().httpClientFactory (aHttpClientSettings)
                                                            .endpointURL ("https://conformance.customs.ec.europa.eu:8445/domibus/services/msh")
                                                            .mpc (sMPC)
                                                            .userMsgConsumer ( (aUserMsg, aMessageMetadata, aState) -> {
                                                              aUserMessageHolder.set (aUserMsg);

                                                              if (aState.hasDecryptedAttachments ())
                                                              {
                                                                // Remember all
                                                                // attachments
                                                                // here
                                                                for (final WSS4JAttachment attachment : aState.getDecryptedAttachments ())
                                                                {
                                                                  final String parsedFile = StreamHelper.getAllBytesAsString (attachment.getSourceStream (),
                                                                                                                              StandardCharsets.UTF_8);
                                                                  attachmentsAsString.add (parsedFile);
                                                                }
                                                              }
                                                            })
                                                            .signalMsgConsumer ( (aSignalMsg, aMMD, aState) -> {
                                                              aSignalMessageHolder.set (aSignalMsg);
                                                            })
                                                            .cryptoFactory (cryptoFactoryProperties)
                                                            .sendMessage ();
    //
    LOGGER.info ("euctp pull request result: " + eSuccess);
    LOGGER.info ("Pulled User Message: " + aUserMessageHolder.get ());
    LOGGER.info ("Pulled Signal Message: " + aSignalMessageHolder.get ());
    LOGGER.info ("Attachments: " + attachmentsAsString);
  }

  private static void _sendConnectionTest (final Phase4EuCtpHttpClientSettings aHttpClientSettings,
                                           final String fromPartyID,
                                           final Wrapper <Ebms3SignalMessage> aSignalMsgHolder,
                                           final AS4CryptoFactoryProperties cryptoFactoryProperties)
  {
    ESimpleUserMessageSendResult eResult;
    eResult = Phase4EuCtpSender.builder ()
                               .httpClientFactory (aHttpClientSettings)
                               .fromPartyID (fromPartyID)
                               .fromPartyIDType (EuCtpPMode.DEFAULT_PARTY_TYPE_ID)
                               .fromRole ("Trader")
                               .toPartyID ("sti-taxud")
                               .toRole ("Customs")
                               .toPartyIDType (EuCtpPMode.DEFAULT_CUSTOMS_PARTY_TYPE_ID)
                               .endpointURL ("https://conformance.customs.ec.europa.eu:8445/domibus/services/msh")
                               .service ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service")
                               .action (EuCtpPMode.ACTION_TEST)
                               .signalMsgConsumer ( (aSignalMsg, aMMD, aState) -> aSignalMsgHolder.set (aSignalMsg))
                               .cryptoFactory (cryptoFactoryProperties)
                               // .payload(new
                               // AS4OutgoingAttachment.Builder().data(aPayloadBytes).mimeTypeXML())
                               .sendMessageAndCheckForReceipt ();
    LOGGER.info ("euctp send result: " + eResult);
    LOGGER.info ("Signal Message: " + aSignalMsgHolder.get ());
  }

  private static void _sendENSFilling (final Phase4EuCtpHttpClientSettings aHttpClientSettings,
                                       final String fromPartyID,
                                       final AS4CryptoFactoryProperties cryptoFactoryProperties)
  {
    // Read XML payload to send
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (new ClassPathResource ("/external/examples/base-example.xml"));

    final Wrapper <Ebms3SignalMessage> aSignalMsgHolder = new Wrapper <> ();
    ESimpleUserMessageSendResult eResult;
    eResult = Phase4EuCtpSender.builder ()
                               .httpClientFactory (aHttpClientSettings)
                               .fromPartyID (fromPartyID)
                               .fromPartyIDType (EuCtpPMode.DEFAULT_PARTY_TYPE_ID)
                               .fromRole ("Trader")
                               .toPartyID ("sti-taxud")
                               .toRole ("Customs")
                               .toPartyIDType (EuCtpPMode.DEFAULT_CUSTOMS_PARTY_TYPE_ID)
                               .endpointURL ("https://conformance.customs.ec.europa.eu:8445/domibus/services/msh")
                               .service (EuCtpPMode.DEFAULT_SERVICE_TYPE, EEuCtpService.TRADER_TO_CUSTOMS)
                               .action (EEuCtpAction.IE3F26)
                               .signalMsgConsumer ( (aSignalMsg, aMMD, aState) -> aSignalMsgHolder.set (aSignalMsg))
                               .cryptoFactory (cryptoFactoryProperties)
                               .conversationID (UUID.randomUUID ().toString ())
                               .payload (new AS4OutgoingAttachment.Builder ().compressionGZIP ()
                                                                             .data (aPayloadBytes)
                                                                             .mimeTypeXML ())
                               .sendMessageAndCheckForReceipt ();
    LOGGER.info ("euctp send result: " + eResult);
    LOGGER.info ("Signal Message: " + aSignalMsgHolder.get ());
  }
}
