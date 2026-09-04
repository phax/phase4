/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.helger.base.array.ArrayHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.http.CHttp;
import com.helger.http.CHttpHeader;
import com.helger.http.header.HttpHeaderMap;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientPullRequestMessage;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.incoming.AS4IncomingHandler;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingProfileSelector;
import com.helger.phase4.incoming.IAS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.incoming.IAS4UserMessageConsumer;
import com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.GenericAS4HttpResponseHandler;
import com.helger.phase4.messaging.http.GenericAS4HttpResponseHandler.HttpResponseData;
import com.helger.phase4.model.EAS4ResponseType;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.model.soapfault.AS4SoapFault;
import com.helger.phase4.model.soapfault.AS4SoapFaultException;
import com.helger.phase4.util.Phase4Exception;
import com.helger.xml.sax.DoNothingSAXErrorHandler;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.read.DOMReaderSettings;

import jakarta.mail.MessagingException;

/**
 * Helper class to send and AS4 message and handle an incoming AS4 response.
 *
 * @author Philip Helger
 */
public final class AS4BidirectionalClientHelper
{
  static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4BidirectionalClientHelper.class);

  private AS4BidirectionalClientHelper ()
  {}

  /**
   * Try to interpret the provided response payload as a SOAP Fault. Any XML parsing errors are
   * collected silently, because non-XML response payloads are a valid case here.
   *
   * @param aResponsePayload
   *        The response payload to check. May be <code>null</code>.
   * @param sRawXML
   *        The raw XML string for dumping. May be <code>null</code> in which case the parsed
   *        document is serialized on demand.
   * @return <code>null</code> if the response payload is not a SOAP Fault.
   */
  @Nullable
  private static AS4SoapFault _findSoapFault (final byte @Nullable [] aResponsePayload, @Nullable final String sRawXML)
  {
    if (ArrayHelper.isEmpty (aResponsePayload))
      return null;

    final Document aSoapDocument = DOMReader.readXMLDOM (aResponsePayload,
                                                         new DOMReaderSettings ().setErrorHandler (new DoNothingSAXErrorHandler ()));
    return AS4SoapFault.createOrNull (aSoapDocument, sRawXML);
  }

  /**
   * Handle a received SOAP Fault: route the raw fault XML through the incoming dumper and invoke
   * the SOAP Fault consumer, defaulting to error logging.
   */
  private static void _handleReceivedSoapFault (@NonNull final AS4SoapFault aSoapFault,
                                                @Nullable final String sSentMessageID,
                                                @Nullable final AS4ClientSentMessage <byte []> aClientSentMessage,
                                                @Nullable final AS4IncomingMessageMetadata aResponseMessageMetadata,
                                                @Nullable final HttpResponse aHttpResponse,
                                                @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                @Nullable final IAS4SoapFaultConsumer aSoapFaultConsumer) throws Phase4Exception
  {
    // Fallback to global dumper if none is provided
    final IAS4IncomingDumper aRealIncomingDumper = aIncomingDumper != null ? aIncomingDumper : AS4DumpManager
                                                                                                             .getIncomingDumper ();
    if (aRealIncomingDumper != null)
    {
      if (aResponseMessageMetadata != null)
      {
        // Create header map from response headers
        final HttpHeaderMap aHttpHeaders = new HttpHeaderMap ();
        if (aHttpResponse != null)
          for (final Header aHeader : aHttpResponse.getHeaders ())
            aHttpHeaders.addHeader (aHeader.getName (), aHeader.getValue ());

        try
        {
          final OutputStream aDumpOS = aRealIncomingDumper.onNewRequest (aResponseMessageMetadata, aHttpHeaders);
          if (aDumpOS != null)
          {
            try
            {
              // Prefer the received bytes over the serialized fault
              if (aClientSentMessage != null && aClientSentMessage.hasResponseContent ())
                aDumpOS.write (aClientSentMessage.getResponseContent ());
              else
                aDumpOS.write (aSoapFault.getRawXML ().getBytes (StandardCharsets.UTF_8));
            }
            finally
            {
              StreamHelper.close (aDumpOS);
            }
            aRealIncomingDumper.onEndRequest (aResponseMessageMetadata, null);
          }
        }
        catch (final IOException ex)
        {
          LOGGER.error ("Failed to dump the received SOAP Fault", ex);
        }
      }
      else
        LOGGER.warn ("Cannot dump the received SOAP Fault, because no message metadata is available");
    }

    // Invoke the callback, defaulting to error logging
    final IAS4SoapFaultConsumer aRealSoapFaultConsumer = aSoapFaultConsumer != null ? aSoapFaultConsumer
                                                                                    : new LoggingAS4SoapFaultConsumer ();
    aRealSoapFaultConsumer.handleSoapFault (sSentMessageID, aSoapFault, aClientSentMessage);
  }

  @Deprecated (forRemoval = true, since = "4.6.0")
  public static void sendAS4UserMessageAndReceiveAS4SignalMessage (@NonNull final IAS4CryptoFactory aCryptoFactorySign,
                                                                   @NonNull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                   @NonNull final IAS4PModeResolver aPModeResolver,
                                                                   @NonNull final IAS4IncomingAttachmentFactory aIAF,
                                                                   @NonNull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                   @NonNull final AS4ClientUserMessage aClientUserMsg,
                                                                   @NonNull final Locale aLocale,
                                                                   @NonNull final String sURL,
                                                                   @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                   @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                   @NonNull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                   @NonNull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                                   @Nullable final IAS4RetryCallback aRetryCallback,
                                                                   @Nullable final IAS4RawResponseConsumer aRawResponseConsumer,
                                                                   @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer,
                                                                   @Nullable final IAS4SignalMessageValidationResultHandler aSignalMsgValidationResultHandler) throws IOException,
                                                                                                                                                               Phase4Exception,
                                                                                                                                                               WSSecurityException,
                                                                                                                                                               MessagingException
  {
    sendAS4UserMessageAndReceiveAS4SignalMessage (aCryptoFactorySign,
                                                  aCryptoFactoryCrypt,
                                                  aPModeResolver,
                                                  aIAF,
                                                  aIncomingProfileSelector,
                                                  aClientUserMsg,
                                                  aLocale,
                                                  sURL,
                                                  aBuildMessageCallback,
                                                  aOutgoingDumper,
                                                  aIncomingDumper,
                                                  aIncomingSecurityConfiguration,
                                                  aIncomingReceiverConfiguration,
                                                  aRetryCallback,
                                                  aRawResponseConsumer,
                                                  aSignalMsgConsumer,
                                                  aSignalMsgValidationResultHandler,
                                                  null);
  }

  /**
   * Same as the other overload, but with an additional SOAP Fault consumer that is invoked if the
   * synchronous response is a plain SOAP Fault instead of an ebMS Signal Message. If a received
   * SOAP Fault has a permanent disposition, all HTTP level retries are stopped and an
   * {@link AS4SoapFaultException} is thrown.
   *
   * @since 4.6.0
   */
  public static void sendAS4UserMessageAndReceiveAS4SignalMessage (@NonNull final IAS4CryptoFactory aCryptoFactorySign,
                                                                   @NonNull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                   @NonNull final IAS4PModeResolver aPModeResolver,
                                                                   @NonNull final IAS4IncomingAttachmentFactory aIAF,
                                                                   @NonNull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                   @NonNull final AS4ClientUserMessage aClientUserMsg,
                                                                   @NonNull final Locale aLocale,
                                                                   @NonNull final String sURL,
                                                                   @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                   @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                   @NonNull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                   @NonNull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                                   @Nullable final IAS4RetryCallback aRetryCallback,
                                                                   @Nullable final IAS4RawResponseConsumer aRawResponseConsumer,
                                                                   @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer,
                                                                   @Nullable final IAS4SignalMessageValidationResultHandler aSignalMsgValidationResultHandler,
                                                                   @Nullable final IAS4SoapFaultConsumer aSoapFaultConsumer) throws IOException,
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

    // Define the HTTP response handler
    final Wrapper <HttpResponse> aWrappedHttpResponse = new Wrapper <> ();
    final Wrapper <AS4SoapFault> aSoapFaultKeeper = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aHttpResponseHdl = aHttpResponse -> {
      // Remember source response object
      aWrappedHttpResponse.set (aHttpResponse);

      // A SOAP Fault can never be contained in a multipart response
      final Header aContentTypeHeader = aHttpResponse.getFirstHeader (CHttpHeader.CONTENT_TYPE);
      final boolean bMayBeSoapFault = aContentTypeHeader == null ||
                                      !StringHelper.startsWithIgnoreCase (aContentTypeHeader.getValue (), "multipart");

      final byte [] aResponsePayload;
      try
      {
        // Accepts all response codes, if enabled in the configuration
        final HttpResponseData aResponseData = GenericAS4HttpResponseHandler.INSTANCE.handleResponse (aHttpResponse);

        // Read response payload
        aResponsePayload = EntityUtils.toByteArray (aResponseData.entity ());
      }
      catch (final ExtendedHttpResponseException ex)
      {
        // Only thrown for non-success status codes, if "accept all status codes" is disabled.
        // Check if the response body contains a SOAP Fault, to eventually stop pointless retries
        if (bMayBeSoapFault)
        {
          final AS4SoapFault aSoapFault = _findSoapFault (ex.directGetResponseBody (), ex.getResponseBodyAsString ());
          if (aSoapFault != null)
          {
            aSoapFaultKeeper.set (aSoapFault);

            // Only throw exception for permanent errors
            if (aSoapFault.getDisposition ().isPermanent ())
              throw new AS4SoapFaultException (aSoapFault);
          }
        }
        throw ex;
      }

      // Check for a SOAP Fault independent of the HTTP status code, because non-conforming peers
      // may return a fault with HTTP 200
      if (bMayBeSoapFault)
      {
        final AS4SoapFault aSoapFault = _findSoapFault (aResponsePayload, null);
        if (aSoapFault != null)
          aSoapFaultKeeper.set (aSoapFault);
      }
      return aResponsePayload;
    };

    final AS4ClientSentMessage <byte []> aClientSentMessage;
    try
    {
      // Main HTTP sending
      aClientSentMessage = aClientUserMsg.sendMessageWithRetries (sURL,
                                                                  aHttpResponseHdl,
                                                                  aBuildMessageCallback,
                                                                  aOutgoingDumper,
                                                                  aRetryCallback);
    }
    catch (final AS4SoapFaultException ex)
    {
      // A SOAP Fault with a permanent disposition stopped all retries
      LOGGER.info ("The synchronous AS4 response was classified as " + EAS4ResponseType.SOAP_FAULT);
      AS4IncomingMessageMetadata aResponseMessageMetadata = null;
      if (ex.hasSentMessageID ())
      {
        aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (ex.getSentMessageID ())
                                                             .setRemoteAddr (sURL);
        if (aWrappedHttpResponse.isSet ())
          aResponseMessageMetadata.setResponseHttpStatusCode (aWrappedHttpResponse.get ().getCode ());
      }
      _handleReceivedSoapFault (ex.getSoapFault (),
                                ex.getSentMessageID (),
                                null,
                                aResponseMessageMetadata,
                                aWrappedHttpResponse.get (),
                                aIncomingDumper,
                                aSoapFaultConsumer);
      throw ex;
    }
    catch (final IOException ex)
    {
      // If a SOAP Fault with a transient disposition was detected on the way (only possible if
      // "accept all status codes" is disabled), surface it nevertheless
      final AS4SoapFault aSoapFault = aSoapFaultKeeper.get ();
      if (aSoapFault != null)
      {
        LOGGER.info ("The synchronous AS4 response was classified as " + EAS4ResponseType.SOAP_FAULT);
        _handleReceivedSoapFault (aSoapFault,
                                  null,
                                  null,
                                  null,
                                  aWrappedHttpResponse.get (),
                                  aIncomingDumper,
                                  aSoapFaultConsumer);
      }
      throw ex;
    }
    final String sRequestAS4MessageID = aClientSentMessage.getMessageID ();
    LOGGER.info ("Successfully transmitted AS4 UserMessage with message ID '" +
                 sRequestAS4MessageID +
                 "' to '" +
                 sURL +
                 "'");

    if (aRawResponseConsumer != null)
      aRawResponseConsumer.handleResponse (aClientSentMessage);

    // Try interpret result as SignalMessage
    final EAS4ResponseType eResponseType;
    if (aClientSentMessage.hasResponseContent () && aClientSentMessage.getResponseContent ().length > 0)
    {
      final AS4IncomingMessageMetadata aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestAS4MessageID)
                                                                                            .setRemoteAddr (sURL)
                                                                                            .setRemoteTlsPeerCerts (aClientSentMessage.getRemoteTlsPeerCerts ());
      if (aWrappedHttpResponse.isSet ())
      {
        // Remember HTTP response status code retrieved
        final int nResponseHttpStatusCode = aWrappedHttpResponse.get ().getCode ();
        aResponseMessageMetadata.setResponseHttpStatusCode (nResponseHttpStatusCode);
        if (nResponseHttpStatusCode >= CHttp.HTTP_MULTIPLE_CHOICES)
          LOGGER.warn ("HTTP response uses non-success status code " + nResponseHttpStatusCode);
      }

      final AS4SoapFault aSoapFault = aSoapFaultKeeper.get ();
      if (aSoapFault != null)
      {
        // A SOAP Fault was received - it cannot be interpreted as an ebMS Signal Message
        eResponseType = EAS4ResponseType.SOAP_FAULT;
        _handleReceivedSoapFault (aSoapFault,
                                  sRequestAS4MessageID,
                                  aClientSentMessage,
                                  aResponseMessageMetadata,
                                  aWrappedHttpResponse.get (),
                                  aIncomingDumper,
                                  aSoapFaultConsumer);
      }
      else
      {
        // Validate the DSSig references between sent and received msg
        final IAS4SignalMessageConsumer aRealSignalMsgConsumer = new ValidatingAS4SignalMsgConsumer (aClientSentMessage,
                                                                                                     aSignalMsgConsumer,
                                                                                                     aSignalMsgValidationResultHandler);

        // Read response as EBMS3 Signal Message
        // Read it in any case to ensure signature validation etc. happens
        final Ebms3SignalMessage aSignalMsg = AS4IncomingHandler.parseSignalMessage (aCryptoFactorySign,
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
        if (aSignalMsg == null)
          eResponseType = EAS4ResponseType.UNPARSABLE;
        else
          if (aSignalMsg.hasErrorEntries ())
            eResponseType = EAS4ResponseType.EBMS_ERROR;
          else
            if (aSignalMsg.getReceipt () != null)
              eResponseType = EAS4ResponseType.RECEIPT;
            else
              eResponseType = EAS4ResponseType.UNPARSABLE;
      }
    }
    else
    {
      LOGGER.info ("AS4 ResponseEntity is empty");
      eResponseType = EAS4ResponseType.EMPTY;
    }
    LOGGER.info ("The synchronous AS4 response was classified as " + eResponseType);
  }

  public static void sendAS4PullRequestAndReceiveAS4UserMessage (@NonNull final IAS4CryptoFactory aCryptoFactorySign,
                                                                 @NonNull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                 @NonNull final IAS4PModeResolver aPModeResolver,
                                                                 @NonNull final IAS4IncomingAttachmentFactory aIAF,
                                                                 @NonNull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                 @NonNull final AS4ClientPullRequestMessage aClientPullRequest,
                                                                 @NonNull final Locale aLocale,
                                                                 @NonNull final String sURL,
                                                                 @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                 @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                 @NonNull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                 @NonNull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
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

    final Wrapper <HttpResponse> aWrappedHttpResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = GenericAS4HttpResponseHandler.INSTANCE.handleResponse (aHttpResponse);

      // Remember HTTP Response
      aWrappedHttpResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aResponseData.entity ());
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
      final AS4IncomingMessageMetadata aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestMessageID)
                                                                                            .setRemoteAddr (sURL)
                                                                                            .setRemoteTlsPeerCerts (aClientSentMessage.getRemoteTlsPeerCerts ());
      if (aWrappedHttpResponse.isSet ())
      {
        // Remember HTTP response status code retrieved
        final int nHttpStatusCode = aWrappedHttpResponse.get ().getCode ();
        aResponseMessageMetadata.setResponseHttpStatusCode (nHttpStatusCode);
        if (nHttpStatusCode >= CHttp.HTTP_MULTIPLE_CHOICES)
          LOGGER.warn ("HTTP response uses status code " + nHttpStatusCode);
      }

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
                                           aWrappedHttpResponse.get (),
                                           aClientSentMessage.getResponseContent (),
                                           aIncomingDumper,
                                           aIncomingSecurityConfiguration,
                                           aIncomingReceiverConfiguration,
                                           aUserMsgConsumer);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
  }

  public static void sendAS4PullRequestAndReceiveAS4UserOrSignalMessage (@NonNull final IAS4CryptoFactory aCryptoFactorySign,
                                                                         @NonNull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                                         @NonNull final IAS4PModeResolver aPModeResolver,
                                                                         @NonNull final IAS4IncomingAttachmentFactory aIAF,
                                                                         @NonNull final IAS4IncomingProfileSelector aIncomingProfileSelector,
                                                                         @NonNull final AS4ClientPullRequestMessage aClientPullRequest,
                                                                         @NonNull final Locale aLocale,
                                                                         @NonNull final String sURL,
                                                                         @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                                                         @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                                         @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                                         @NonNull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                                         @NonNull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
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

    final Wrapper <HttpResponse> aWrappedHttpResponse = new Wrapper <> ();
    final HttpClientResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = GenericAS4HttpResponseHandler.INSTANCE.handleResponse (aHttpResponse);

      // Remember HTTP Response
      aWrappedHttpResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aResponseData.entity ());
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
      final AS4IncomingMessageMetadata aResponseMessageMetadata = AS4IncomingMessageMetadata.createForResponse (sRequestMessageID)
                                                                                            .setRemoteAddr (sURL)
                                                                                            .setRemoteTlsPeerCerts (aClientSentMessage.getRemoteTlsPeerCerts ());
      if (aWrappedHttpResponse.isSet ())
      {
        // Remember HTTP response status code retrieved
        final int nHttpStatusCode = aWrappedHttpResponse.get ().getCode ();
        aResponseMessageMetadata.setResponseHttpStatusCode (nHttpStatusCode);
        if (nHttpStatusCode >= CHttp.HTTP_MULTIPLE_CHOICES)
          LOGGER.warn ("HTTP response uses status code " + nHttpStatusCode);
      }

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
                                                   aResponseMessageMetadata,
                                                   aWrappedHttpResponse.get (),
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
