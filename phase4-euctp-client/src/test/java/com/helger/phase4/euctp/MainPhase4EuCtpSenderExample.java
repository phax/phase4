/*
 * Copyright (C) 2024 Philip Helger (www.helger.com)
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientErrorMessage;
import com.helger.phase4.client.AS4ClientReceiptMessage;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.euctp.Phase4EuCtpSender.EuCtpPullRequestBuilder;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.mpc.MPC;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.profile.euctp.EEuCtpAction;
import com.helger.phase4.profile.euctp.EEuCtpService;
import com.helger.phase4.profile.euctp.EuCtpPMode;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;
import com.helger.security.keystore.TrustStoreDescriptor;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

import jakarta.mail.MessagingException;

public class MainPhase4EuCtpSenderExample
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4EuCtpSenderExample.class);

  @Nonnull
  private static IAS4CryptoFactory _buildAs4CryptoFactory ()
  {
    return new AS4CryptoFactoryInMemoryKeyStore (KeyStoreAndKeyDescriptor.builder ()
                                                                         .type (EKeyStoreType.PKCS12)
                                                                         .path (System.getenv ("AS4_SIGNING_KEYSTORE_PATH"))
                                                                         .password (System.getenv ("AS4_SIGNING_KEYSTORE_PASSWORD"))
                                                                         .keyAlias (System.getenv ("AS4_SIGNING_KEY_ALIAS"))
                                                                         .keyPassword (System.getenv ("AS4_SIGNING_KEY_PASSWORD"))
                                                                         .build (),
                                                 // must include the Taxud CA
                                                 // and intermediate
                                                 // certificates
                                                 TrustStoreDescriptor.builder ()
                                                                     .type (EKeyStoreType.PKCS12)
                                                                     .path (System.getenv ("AS4_SIGNING_TRUST_KEYSTORE_PATH"))
                                                                     .password (System.getenv ("AS4_SIGNING_TRUST_KEYSTORE_PASSWORD"))
                                                                     .build ());
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

      final IAS4CryptoFactory cryptoFactoryProperties = _buildAs4CryptoFactory ();

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
                                        final IAS4CryptoFactory cryptoFactory) throws Phase4Exception
  {
    final Wrapper <Ebms3UserMessage> aUserMessageHolder = new Wrapper <> ();
    final Wrapper <Ebms3SignalMessage> aSignalMessageHolder = new Wrapper <> ();
    final Wrapper <Document> aSoapDocHolder = new Wrapper <> ();
    final String sMPC = "urn:fdc:ec.europa.eu:2019:eu_ics2_c2t/EORI/" + fromPartyID;
    final IMPCManager aMpcMgr = MetaAS4Manager.getMPCMgr ();
    if (!aMpcMgr.containsWithID (sMPC))
    {
      // this will be needed when parsing the UserMessage
      aMpcMgr.createMPC (new MPC (sMPC));
    }

    final List <String> attachmentsAsString = new ArrayList <> ();
    final EuCtpPullRequestBuilder prBuilder = Phase4EuCtpSender.builderPullRequest ()
                                                               .httpClientFactory (aHttpClientSettings)
                                                               .endpointURL ("https://conformance.customs.ec.europa.eu:8445/domibus/services/msh")
                                                               .mpc (sMPC)
                                                               .userMsgConsumer ( (aEbmsUserMsg,
                                                                                   aIncomingMessageMetadata,
                                                                                   aIncomingState) -> {
                                                                 aUserMessageHolder.set (aEbmsUserMsg);
                                                                 aSoapDocHolder.set (aIncomingState.getEffectiveDecryptedSoapDocument ());
                                                                 if (aIncomingState.hasDecryptedAttachments ())
                                                                 {
                                                                   /*
                                                                    * Remember
                                                                    * all
                                                                    * attachments
                                                                    * here
                                                                    */
                                                                   for (final WSS4JAttachment attachment : aIncomingState.getDecryptedAttachments ())
                                                                   {
                                                                     final String parsedFile = StreamHelper.getAllBytesAsString (attachment.getSourceStream (),
                                                                                                                                 StandardCharsets.UTF_8);
                                                                     attachmentsAsString.add (parsedFile);
                                                                   }
                                                                 }
                                                               })
                                                               .signalMsgConsumer ( (aEbmsSignalMsg,
                                                                                     aIncomingMessageMetadata,
                                                                                     aIncomingState) -> {
                                                                 aSignalMessageHolder.set (aEbmsSignalMsg);
                                                                 aSoapDocHolder.set (aIncomingState.getEffectiveDecryptedSoapDocument ());
                                                               })
                                                               .cryptoFactory (cryptoFactory);
    final ESuccess eSuccess = prBuilder.sendMessage ();
    //
    LOGGER.info ("euctp pull request result: " + eSuccess);
    LOGGER.info ("Pulled User Message: " + aUserMessageHolder.get ());
    LOGGER.info ("Pulled Signal Message: " + aSignalMessageHolder.get ());
    LOGGER.info ("Attachments: " + attachmentsAsString);

    if (eSuccess.isSuccess () && aUserMessageHolder.isSet ())
    {
      _sendReceipt (aUserMessageHolder, aSoapDocHolder, prBuilder);
    }
  }

  private static void _sendReceipt (final Wrapper <Ebms3UserMessage> aUserMessageHolder,
                                    final Wrapper <Document> aSoapDocHolder,
                                    final EuCtpPullRequestBuilder prBuilder)
  {
    // Send another Receipt
    final Ebms3UserMessage aUserMessage = aUserMessageHolder.get ();
    final String sUserMessageID = aUserMessage.getMessageInfo ().getMessageId ();
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4ClientSentMessage <byte []> aSentMessage;
      // TODO decide what to do
      if (true)
      {
        // receipt
        final AS4ClientReceiptMessage aReceiptMessage = new AS4ClientReceiptMessage (aResHelper);
        aReceiptMessage.setRefToMessageID (sUserMessageID);
        aReceiptMessage.setNonRepudiation (EuCtpPMode.DEFAULT_SEND_RECEIPT_NON_REPUDIATION);
        aReceiptMessage.setSoapDocument (aSoapDocHolder.get ());
        aReceiptMessage.setReceiptShouldBeSigned (true);
        aReceiptMessage.getHttpPoster ().setHttpClientFactory (prBuilder.httpClientFactory ());
        aReceiptMessage.setCryptoFactorySign (prBuilder.cryptoFactorySign ());
        aReceiptMessage.setCryptoFactoryCrypt (prBuilder.cryptoFactoryCrypt ());
        aReceiptMessage.setEbms3UserMessage (aUserMessage);

        final IPMode aPMode = prBuilder.pmode ();
        if (aPMode != null)
        {
          final PModeLeg aEffectiveLeg = prBuilder.useLeg1 () ? aPMode.getLeg1 () : aPMode.getLeg2 ();
          aReceiptMessage.signingParams ().setFromPMode (aEffectiveLeg.getSecurity ());
        }

        aReceiptMessage.cryptParams ()
                       .setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
        aReceiptMessage.cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
        aReceiptMessage.cryptParams ().setEncryptSymmetricSessionKey (false);

        // Other signing parameters are located in the PMode security part
        aReceiptMessage.signingParams ()
                       .setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
        aReceiptMessage.signingParams ().setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
        // Use the BST value type "#X509PKIPathv1"
        aReceiptMessage.signingParams ().setUseSingleCertificate (false);

        aSentMessage = aReceiptMessage.sendMessageWithRetries (prBuilder.endpointURL (),
                                                               new ResponseHandlerByteArray (),
                                                               prBuilder.buildMessageCallback (),
                                                               prBuilder.outgoingDumper (),
                                                               prBuilder.retryCallback ());
      }
      else
      {
        // error
        final AS4ClientErrorMessage aErrorMessage = new AS4ClientErrorMessage (aResHelper);
        aErrorMessage.errorMessages ()
                     .add (EEbmsError.EBMS_OTHER.errorBuilder (Locale.US)
                                                .refToMessageInError (sUserMessageID)
                                                .errorDetail ("This is why it failed")
                                                .build ());
        aErrorMessage.setRefToMessageID (sUserMessageID);
        aErrorMessage.setErrorShouldBeSigned (true);
        aErrorMessage.getHttpPoster ().setHttpClientFactory (prBuilder.httpClientFactory ());
        aErrorMessage.setCryptoFactorySign (prBuilder.cryptoFactorySign ());
        aErrorMessage.setCryptoFactoryCrypt (prBuilder.cryptoFactoryCrypt ());

        final IPMode aPMode = prBuilder.pmode ();
        if (aPMode != null)
        {
          final PModeLeg aEffectiveLeg = prBuilder.useLeg1 () ? aPMode.getLeg1 () : aPMode.getLeg2 ();
          aErrorMessage.signingParams ().setFromPMode (aEffectiveLeg.getSecurity ());
        }

        aErrorMessage.cryptParams ()
                     .setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
        aErrorMessage.cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
        aErrorMessage.cryptParams ().setEncryptSymmetricSessionKey (false);

        // Other signing parameters are located in the PMode security part
        aErrorMessage.signingParams ()
                     .setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
        aErrorMessage.signingParams ().setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
        // Use the BST value type "#X509PKIPathv1"
        aErrorMessage.signingParams ().setUseSingleCertificate (false);

        aSentMessage = aErrorMessage.sendMessageWithRetries (prBuilder.endpointURL (),
                                                             new ResponseHandlerByteArray (),
                                                             prBuilder.buildMessageCallback (),
                                                             prBuilder.outgoingDumper (),
                                                             prBuilder.retryCallback ());
      }

      if (aSentMessage.hasResponseStatusLine ())
        LOGGER.info ("Receipt response: " + aSentMessage.getResponseStatusLine ());
      if (aSentMessage.hasResponseContent ())
        LOGGER.info ("Receipt content length: " + aSentMessage.getResponseContent ().length);
    }
    catch (IOException | WSSecurityException | MessagingException ex)
    {
      LOGGER.error ("Failed to send back Error/Receipt", ex);
    }
  }

  private static void _sendConnectionTest (final Phase4EuCtpHttpClientSettings aHttpClientSettings,
                                           final String fromPartyID,
                                           final Wrapper <Ebms3SignalMessage> aSignalMsgHolder,
                                           final IAS4CryptoFactory cryptoFactory)
  {
    EAS4UserMessageSendResult eResult;
    eResult = Phase4EuCtpSender.builderUserMessage ()
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
                               .cryptoFactory (cryptoFactory)
                               // .payload(new
                               // AS4OutgoingAttachment.Builder().data(aPayloadBytes).mimeTypeXML())
                               .sendMessageAndCheckForReceipt ();
    LOGGER.info ("euctp send result: " + eResult);
    LOGGER.info ("Signal Message: " + aSignalMsgHolder.get ());
  }

  private static void _sendENSFilling (final Phase4EuCtpHttpClientSettings aHttpClientSettings,
                                       final String fromPartyID,
                                       final IAS4CryptoFactory cryptoFactory)
  {
    // Read XML payload to send
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (new ClassPathResource ("/external/examples/base-example.xml"));

    final Wrapper <Ebms3SignalMessage> aSignalMsgHolder = new Wrapper <> ();
    EAS4UserMessageSendResult eResult;
    eResult = Phase4EuCtpSender.builderUserMessage ()
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
                               .cryptoFactory (cryptoFactory)
                               .conversationID (UUID.randomUUID ().toString ())
                               .payload (new AS4OutgoingAttachment.Builder ().compressionGZIP ()
                                                                             .data (aPayloadBytes)
                                                                             .mimeTypeXML ())
                               .sendMessageAndCheckForReceipt ();
    LOGGER.info ("euctp send result: " + eResult);
    LOGGER.info ("Signal Message: " + aSignalMsgHolder.get ());
  }
}
