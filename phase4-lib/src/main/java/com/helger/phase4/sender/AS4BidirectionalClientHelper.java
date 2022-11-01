/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
import javax.mail.MessagingException;

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
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.client.IAS4SignalMessageConsumer;
import com.helger.phase4.client.IAS4UserMessageConsumer;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.servlet.AS4IncomingHandler;
import com.helger.phase4.servlet.AS4IncomingMessageMetadata;
import com.helger.phase4.servlet.IAS4IncomingProfileSelector;
import com.helger.phase4.util.Phase4Exception;

/**
 * Helper class to send and AS4 message and handle an incoming AS4 response.
 *
 * @author Philip Helger
 */
public final class AS4BidirectionalClientHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4BidirectionalClientHelper.class);

  private AS4BidirectionalClientHelper ()
  {}

  public static void sendAS4UserMessageAndReceiveAS4SignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                                   @Nonnull final IPModeResolver aPModeResolver,
                                                                   @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                                   @Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                   @Nonnull final AS4ClientUserMessage aClientUserMsg,
                                                                   @Nonnull final Locale aLocale,
                                                                   @Nonnull final String sURL,
                                                                   @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                   @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                   @Nullable final IAS4RetryCallback aRetryCallback,
                                                                   @Nullable final IAS4RawResponseConsumer aResponseConsumer,
                                                                   @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer) throws IOException,
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

    final Wrapper <HttpResponse> aWrappedResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      // throws an ExtendedHttpResponseException on exception
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;
      aWrappedResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aEntity);
    };

    final AS4ClientSentMessage <byte []> aResponseEntity = aClientUserMsg.sendMessageWithRetries (sURL,
                                                                                                  aResponseHdl,
                                                                                                  aBuildMessageCallback,
                                                                                                  aOutgoingDumper,
                                                                                                  aRetryCallback);
    LOGGER.info ("Successfully transmitted AS4 UserMessage with message ID '" +
                 aResponseEntity.getMessageID () +
                 "' to '" +
                 sURL +
                 "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aResponseEntity);

    // Try interpret result as SignalMessage
    if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
    {
      final IAS4IncomingMessageMetadata aMessageMetadata = new AS4IncomingMessageMetadata (EAS4MessageMode.RESPONSE).setRemoteAddr (sURL);

      // Read response as EBMS3 Signal Message
      // Read it in any case to ensure signature validation etc. happens
      final Ebms3SignalMessage aSignalMessage = AS4IncomingHandler.parseSignalMessage (aCryptoFactory,
                                                                                       aPModeResolver,
                                                                                       aIAF,
                                                                                       aIncomingProfileSelector,
                                                                                       aClientUserMsg.getAS4ResourceHelper (),
                                                                                       aClientUserMsg.getPMode (),
                                                                                       aLocale,
                                                                                       aMessageMetadata,
                                                                                       aWrappedResponse.get (),
                                                                                       aResponseEntity.getResponse (),
                                                                                       aIncomingDumper);
      if (aSignalMessage != null && aSignalMsgConsumer != null)
        aSignalMsgConsumer.handleSignalMessage (aSignalMessage);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }

  public static void sendAS4PullRequestAndReceiveAS4UserMessage (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                                 @Nonnull final IPModeResolver aPModeResolver,
                                                                 @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                                 @Nonnull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                 @Nonnull final AS4ClientPullRequestMessage aClientPullRequest,
                                                                 @Nonnull final Locale aLocale,
                                                                 @Nonnull final String sURL,
                                                                 @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                 @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                 @Nullable final IAS4RetryCallback aRetryCallback,
                                                                 @Nullable final IAS4RawResponseConsumer aResponseConsumer,
                                                                 @Nullable final IAS4UserMessageConsumer aUserMsgConsumer) throws IOException,
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
      aWrappedResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aEntity);
    };
    final AS4ClientSentMessage <byte []> aResponseEntity = aClientPullRequest.sendMessageWithRetries (sURL,
                                                                                                      aResponseHdl,
                                                                                                      aBuildMessageCallback,
                                                                                                      aOutgoingDumper,
                                                                                                      aRetryCallback);
    LOGGER.info ("Successfully transmitted AS4 PullRequest with message ID '" +
                 aResponseEntity.getMessageID () +
                 "' to '" +
                 sURL +
                 "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aResponseEntity);

    // Try interpret result as SignalMessage
    if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
    {
      final IAS4IncomingMessageMetadata aMessageMetadata = new AS4IncomingMessageMetadata (EAS4MessageMode.RESPONSE).setRemoteAddr (sURL);

      // Read response as EBMS3 User Message
      // Read it in any case to ensure signature validation etc. happens
      final Ebms3UserMessage aUserMessage = AS4IncomingHandler.parseUserMessage (aCryptoFactory,
                                                                                 aPModeResolver,
                                                                                 aIAF,
                                                                                 aIncomingProfileSelector,
                                                                                 aClientPullRequest.getAS4ResourceHelper (),
                                                                                 null,
                                                                                 aLocale,
                                                                                 aMessageMetadata,
                                                                                 aWrappedResponse.get (),
                                                                                 aResponseEntity.getResponse (),
                                                                                 aIncomingDumper);
      if (aUserMessage != null && aUserMsgConsumer != null)
        aUserMsgConsumer.handleUserMessage (aUserMessage);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }
}
