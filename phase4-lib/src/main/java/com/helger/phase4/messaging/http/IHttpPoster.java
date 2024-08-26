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
package com.helger.phase4.messaging.http;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.IHttpClientProvider;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.dump.IAS4OutgoingDumper;

/**
 * Interface for an HTTP POST sender.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public interface IHttpPoster
{
  /**
   * @return The internal http client factory used for http sending. May not be
   *         <code>null</code>.
   */
  @Nonnull
  HttpClientFactory getHttpClientFactory ();

  /**
   * Set the HTTP client provider to be used. This is e.g. necessary when a
   * custom SSL context or a proxy server is to be used. See
   * {@link BasicHttpPoster#createDefaultHttpClientFactory()} as the default
   * implementation of {@link IHttpClientProvider}. This factory is used for
   * http sending.
   *
   * @param aHttpClientFactory
   *        The HTTP client factory to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  IHttpPoster setHttpClientFactory (@Nonnull HttpClientFactory aHttpClientFactory);

  /**
   * @return The HTTP Post customizer to be used. May be <code>null</code>.
   */
  @Nullable
  Consumer <? super HttpPost> getHttpCustomizer ();

  /**
   * Set the HTTP Post Customizer to be used.
   *
   * @param aHttpCustomizer
   *        The new customizer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  IHttpPoster setHttpCustomizer (@Nullable Consumer <? super HttpPost> aHttpCustomizer);

  /**
   * @return <code>true</code> if HTTP header values should be quoted if they
   *         contain forbidden characters, <code>false</code> if not.
   */
  boolean isQuoteHttpHeaders ();

  /**
   * Enable or disable, if HTTP header values should be quoted or not. For
   * compatibility it is recommended, to not quote the values.
   *
   * @param bQuoteHttpHeaders
   *        <code>true</code> to quote them, <code>false</code> to not quote
   *        them.
   * @return this for chaining
   */
  @Nonnull
  IHttpPoster setQuoteHttpHeaders (boolean bQuoteHttpHeaders);

  /**
   * Send an arbitrary HTTP POST message to the provided URL, using the
   * contained HttpClientFactory as well as the customizer. Additionally the AS4
   * HTTP debugging is invoked in here.<br>
   * This method does NOT retry
   *
   * @param <T>
   *        Response data type
   * @param sURL
   *        The URL to send to. May neither be <code>null</code> nor empty.
   * @param aCustomHttpHeaders
   *        An optional http header map that should be applied. May be
   *        <code>null</code>.
   * @param aHttpEntity
   *        The HTTP entity to be send. May not be <code>null</code>.
   * @param aResponseHandler
   *        The HTTP response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @return The HTTP response data as indicated by the ResponseHandler. Should
   *         not be <code>null</code> but basically depends on the response
   *         handler.
   * @throws IOException
   *         In case of IO error
   */
  @Nullable
  <T> T sendGenericMessage (@Nonnull @Nonempty String sURL,
                            @Nullable HttpHeaderMap aCustomHttpHeaders,
                            @Nonnull HttpEntity aHttpEntity,
                            @Nonnull HttpClientResponseHandler <? extends T> aResponseHandler) throws IOException;

  /**
   * Send an arbitrary HTTP POST message to the provided URL, using the
   * contained HttpClientFactory as well as the customizer. Additionally the AS4
   * HTTP debugging is invoked in here.
   *
   * @param sURL
   *        The URL to send to. May neither be <code>null</code> nor empty.
   * @param aCustomHttpHeaders
   *        An optional http header map that should be applied. May be
   *        <code>null</code>.
   * @param aHttpEntity
   *        The HTTP entity to be send. May not be <code>null</code>.
   * @param sMessageID
   *        the AS4 message ID. May not be <code>null</code>.
   * @param aRetrySettings
   *        The retry settings to use. May not be <code>null</code>.
   * @param aResponseHandler
   *        The HTTP response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @param aOutgoingDumper
   *        An optional outgoing dumper for this message. May be
   *        <code>null</code> to use the global one.
   * @param aRetryCallback
   *        An optional retry callback that is invoked, before a retry happens.
   * @param <T>
   *        Response data type
   * @return The HTTP response data as indicated by the ResponseHandler. Should
   *         not be <code>null</code> but basically depends on the response
   *         handler.
   * @throws IOException
   *         In case of IO error
   */
  @Nullable
  <T> T sendGenericMessageWithRetries (@Nonnull String sURL,
                                       @Nullable HttpHeaderMap aCustomHttpHeaders,
                                       @Nonnull HttpEntity aHttpEntity,
                                       @Nonnull String sMessageID,
                                       @Nonnull HttpRetrySettings aRetrySettings,
                                       @Nonnull HttpClientResponseHandler <? extends T> aResponseHandler,
                                       @Nullable IAS4OutgoingDumper aOutgoingDumper,
                                       @Nullable IAS4RetryCallback aRetryCallback) throws IOException;
}
