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
package com.helger.phase4.sender;

import java.io.IOException;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.response.ResponseHandlerHttpEntity;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientPullRequestMessage;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.incoming.AS4IncomingHandler;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingProfileSelector;
import com.helger.phase4.incoming.IAS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.incoming.IAS4UserMessageConsumer;
import com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.util.Phase4Exception;

import jakarta.mail.MessagingException;

/**
 * Helper class to send and AS4 message and handle an incoming AS4 response.
 *
 * @author Philip Helger
 */
public final class AS4BidirectionalClientHelper
{
  static final Logger LOGGER = LoggerFactory.getLogger (AS4BidirectionalClientHelper.class);

  private AS4BidirectionalClientHelper ()
  {}

  public static void sendAS4UserMessageAndReceiveAS4SignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                                   @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                   @Nonnull final IAS4PModeResolver aPModeResolver,
                                                                   @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                                   @Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                   @Nonnull final AS4ClientUserMessage aClientUserMsg,
                                                                   @Nonnull final Locale aLocale,
                                                                   @Nonnull final String sURL,
                                                                   @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                   @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                   @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                   @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                                   @Nullable final IAS4RetryCallback aRetryCallback,
                                                                   @Nullable final IAS4RawResponseConsumer aRawResponseConsumer,
                                                                   @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer,
                                                                   @Nullable final IAS4SignalMessageValidationResultHandler aSignalMsgValidationResultHandler) throws IOException,
                                                                                                                                                               Phase4Exception,
                                                                                                                                                               WSSecurityException,
                                                                                                                                                               MessagingException
  {
    LOGGER.info ("Sending AS4 UserMessage to '" +
                 sURL +
                 "' with max. " +
                 aClientUserMsg.httpRetrySettings ().getMaxRetries () +
                 " retries");

    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("  ServiceType = '" + aClientUserMsg.getServiceType () + "'");
      LOGGER.debug ("  Service = '" + aClientUserMsg.getServiceValue () + "'");
      LOGGER.debug ("  Action = '" + aClientUserMsg.getAction () + "'");
      LOGGER.debug ("  ConversationId = '" + aClientUserMsg.getConversationID () + "'");
      LOGGER.debug ("  MessageProperties:");
      for (final Ebms3Property p : aClientUserMsg.ebms3Properties ())
        LOGGER.debug ("    [" + p.getName () + "] = [" + p.getValue () + "]");
      LOGGER.debug ("  Attachments (" + aClientUserMsg.attachments ().size () + "):");
      for (final WSS4JAttachment a : aClientUserMsg.attachments ())
      {
        LOGGER.debug ("    [" +
                      a.getId () +
                      "] with [" +
                      a.getMimeType () +
                      "] and [" +
                      a.getCharsetOrDefault (null) +
                      "] and [" +
                      a.getCompressionMode () +
                      "] and [" +
                      a.getContentTransferEncoding () +
                      "]");
      }
    }

    final Wrapper <HttpResponse> aWrappedHttpResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aHttpResponseHdl = aHttpResponse -> {
      // throws an ExtendedHttpResponseException on exception
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;

      // Remember source response object
      aWrappedHttpResponse.set (aHttpResponse);

      // Read response payload
      return EntityUtils.toByteArray (aEntity);
    };

    // Main HTTP sending
    final AS4ClientSentMessage <byte []> aClientSentMessage = aClientUserMsg.sendMessageWithRetries (sURL,
                                                                                                     aHttpResponseHdl,
                                                                                                     aBuildMessageCallback,
                                                                                                     aOutgoingDumper,
                                                                                                     aRetryCallback);
    final String sRequestMessageID = aClientSentMessage.getMessageID ();
    LOGGER.info ("Successfully transmitted AS4 UserMessage with message ID '" +
                 sRequestMessageID +
                 "' to '" +
                 sURL +
                 "'");

    if (aRawResponseConsumer != null)
      aRawResponseConsumer.handleResponse (aClientSentMessage);

    // Try interpret result as SignalMessage
    if (aClientSentMessage.hasResponseContent () && aClientSentMessage.getResponseContent ().length > 0)
    {
      final IAS4IncomingMessageMetadata aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestMessageID)
                                                                                             .setRemoteAddr (sURL);

      // Validate the DSSig references between sent and received msg
      final IAS4SignalMessageConsumer aRealSignalMsgConsumer = new ValidatingAS4SignalMsgConsumer (aClientSentMessage,
                                                                                                   aSignalMsgConsumer,
                                                                                                   aSignalMsgValidationResultHandler);

      // Read response as EBMS3 Signal Message
      // Read it in any case to ensure signature validation etc. happens
      AS4IncomingHandler.parseSignalMessage (aCryptoFactorySign,
                                             aCryptoFactoryCrypt,
                                             aPModeResolver,
                                             aIAF,
                                             aIncomingProfileSelector,
                                             aClientUserMsg.getAS4ResourceHelper (),
                                             aClientUserMsg.getPMode (),
                                             aLocale,
                                             aResponseMessageMetadata,
                                             aWrappedHttpResponse.get (),
                                             aClientSentMessage.getResponseContent (),
                                             aIncomingDumper,
                                             aIncomingSecurityConfiguration,
                                             aIncomingReceiverConfiguration,
                                             aRealSignalMsgConsumer);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }

  public static void sendAS4PullRequestAndReceiveAS4UserMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                                 @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                 @Nonnull final IAS4PModeResolver aPModeResolver,
                                                                 @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                                 @Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                 @Nonnull final AS4ClientPullRequestMessage aClientPullRequest,
                                                                 @Nonnull final Locale aLocale,
                                                                 @Nonnull final String sURL,
                                                                 @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                 @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                 @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                 @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                                 @Nullable final IAS4RetryCallback aRetryCallback,
                                                                 @Nullable final IAS4RawResponseConsumer aResponseConsumer,
                                                                 @Nullable final IAS4UserMessageConsumer aUserMsgConsumer,
                                                                 @Nullable final IPMode aPMode) throws IOException,
                                                                                                Phase4Exception,
                                                                                                WSSecurityException,
                                                                                                MessagingException
  {
    LOGGER.info ("Sending AS4 PullRequest to '" +
                 sURL +
                 "' with max. " +
                 aClientPullRequest.httpRetrySettings ().getMaxRetries () +
                 " retries");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("  MPC = '" + aClientPullRequest.getMPC () + "'");

    final Wrapper <HttpResponse> aWrappedResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      // May throw an ExtendedHttpResponseException
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;

      // Remember HTTP Response
      aWrappedResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aEntity);
    };

    // Generic AS4 PullRequest sending
    final AS4ClientSentMessage <byte []> aClientSentMessage = aClientPullRequest.sendMessageWithRetries (sURL,
                                                                                                         aResponseHdl,
                                                                                                         aBuildMessageCallback,
                                                                                                         aOutgoingDumper,
                                                                                                         aRetryCallback);
    final String sRequestMessageID = aClientSentMessage.getMessageID ();
    LOGGER.info ("Successfully transmitted AS4 PullRequest with message ID '" +
                 sRequestMessageID +
                 "' to '" +
                 sURL +
                 "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aClientSentMessage);

    // Try to interpret result as UserMessage or SignalMessage
    if (aClientSentMessage.hasResponseContent () && aClientSentMessage.getResponseContent ().length > 0)
    {
      final IAS4IncomingMessageMetadata aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestMessageID)
                                                                                             .setRemoteAddr (sURL);

      // Read response as EBMS3 User Message or Signal Message
      // Read it in any case to ensure signature validation etc. happens
      AS4IncomingHandler.parseUserMessage (aCryptoFactorySign,
                                           aCryptoFactoryCrypt,
                                           aPModeResolver,
                                           aIAF,
                                           aIncomingProfileSelector,
                                           aClientPullRequest.getAS4ResourceHelper (),
                                           aPMode,
                                           aLocale,
                                           aResponseMessageMetadata,
                                           aWrappedResponse.get (),
                                           aClientSentMessage.getResponseContent (),
                                           aIncomingDumper,
                                           aIncomingSecurityConfiguration,
                                           aIncomingReceiverConfiguration,
                                           aUserMsgConsumer);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }

  public static void sendAS4PullRequestAndReceiveAS4UserOrSignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                                         @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                         @Nonnull final IAS4PModeResolver aPModeResolver,
                                                                         @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                                         @Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                         @Nonnull final AS4ClientPullRequestMessage aClientPullRequest,
                                                                         @Nonnull final Locale aLocale,
                                                                         @Nonnull final String sURL,
                                                                         @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                         @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                         @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                         @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                         @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                                         @Nullable final IAS4RetryCallback aRetryCallback,
                                                                         @Nullable final IAS4RawResponseConsumer aResponseConsumer,
                                                                         @Nullable final IAS4UserMessageConsumer aUserMsgConsumer,
                                                                         @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer,
                                                                         @Nullable final IAS4SignalMessageValidationResultHandler aSignalMsgValidationResultHandler,
                                                                         @Nullable final IPMode aPMode) throws IOException,
                                                                                                        Phase4Exception,
                                                                                                        WSSecurityException,
                                                                                                        MessagingException
  {
    LOGGER.info ("Sending AS4 PullRequest to '" +
                 sURL +
                 "' with max. " +
                 aClientPullRequest.httpRetrySettings ().getMaxRetries () +
                 " retries");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("  MPC = '" + aClientPullRequest.getMPC () + "'");

    final Wrapper <HttpResponse> aWrappedResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      // May throw an ExtendedHttpResponseException
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;

      // Remember HTTP Response
      aWrappedResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aEntity);
    };

    // Generic AS4 PullRequest sending
    final AS4ClientSentMessage <byte []> aClientSentMessage = aClientPullRequest.sendMessageWithRetries (sURL,
                                                                                                         aResponseHdl,
                                                                                                         aBuildMessageCallback,
                                                                                                         aOutgoingDumper,
                                                                                                         aRetryCallback);
    final String sRequestMessageID = aClientSentMessage.getMessageID ();
    LOGGER.info ("Successfully transmitted AS4 PullRequest with message ID '" +
                 sRequestMessageID +
                 "' to '" +
                 sURL +
                 "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aClientSentMessage);

    // Try to interpret result as UserMessage or SignalMessage
    if (aClientSentMessage.hasResponseContent () && aClientSentMessage.getResponseContent ().length > 0)
    {
      final IAS4IncomingMessageMetadata aResponseMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestMessageID)
                                                                                      .setRemoteAddr (sURL);

      // Validate the DSSig references between sent and received msg
      final IAS4SignalMessageConsumer aRealSignalMsgConsumer = new ValidatingAS4SignalMsgConsumer (aClientSentMessage,
                                                                                                   aSignalMsgConsumer,
                                                                                                   aSignalMsgValidationResultHandler);

      // Read response as EBMS3 User Message or Signal Message
      // Read it in any case to ensure signature validation etc. happens
      AS4IncomingHandler.parseUserOrSignalMessage (aCryptoFactorySign,
                                                   aCryptoFactoryCrypt,
                                                   aPModeResolver,
                                                   aIAF,
                                                   aIncomingProfileSelector,
                                                   aClientPullRequest.getAS4ResourceHelper (),
                                                   aPMode,
                                                   aLocale,
                                                   aResponseMetadata,
                                                   aWrappedResponse.get (),
                                                   aClientSentMessage.getResponseContent (),
                                                   aIncomingDumper,
                                                   aIncomingSecurityConfiguration,
                                                   aIncomingReceiverConfiguration,
                                                   aUserMsgConsumer,
                                                   aRealSignalMsgConsumer);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }
}
