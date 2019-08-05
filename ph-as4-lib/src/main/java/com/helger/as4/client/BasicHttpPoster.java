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
package com.helger.as4.client;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IConsumer;
import com.helger.commons.http.CHttp;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.IHttpClientProvider;

/**
 * A generic HTTP POST wrapper based on {@link IHttpClientProvider} and
 * {@link HttpPost}.
 *
 * @author Philip Helger
 */
public class BasicHttpPoster
{
  /**
   * @return The default {@link HttpClientFactory} to be used.
   * @since 0.8.3
   */
  @Nonnull
  public static HttpClientFactory createDefaultHttpClientFactory ()
  {
    return new HttpClientFactory ();
  }

  // By default no special SSL context present
  private HttpClientFactory m_aHttpClientFactory = createDefaultHttpClientFactory ();
  private IConsumer <? super HttpPost> m_aHttpCustomizer;

  public BasicHttpPoster ()
  {}

  /**
   * @return The internal http client factory used in
   *         {@link #sendGenericMessage(String, HttpEntity, ResponseHandler)}.
   */
  @Nonnull
  public final HttpClientFactory getHttpClientFactory ()
  {
    return m_aHttpClientFactory;
  }

  /**
   * Set the HTTP client provider to be used. This is e.g. necessary when a
   * custom SSL context or a proxy server is to be used. See
   * {@link #createDefaultHttpClientFactory()} as the default implementation of
   * {@link IHttpClientProvider}. This factory is used in
   * {@link #sendGenericMessage(String, HttpEntity, ResponseHandler)}.
   *
   * @param aHttpClientFactory
   *        The HTTP client factory to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final BasicHttpPoster setHttpClientFactory (@Nonnull final HttpClientFactory aHttpClientFactory)
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    m_aHttpClientFactory = aHttpClientFactory;
    return this;
  }

  /**
   * @return The HTTP Post customizer to be used. May be <code>null</code>.
   * @since 0.8.3
   */
  @Nullable
  public final IConsumer <? super HttpPost> getHttpCustomizer ()
  {
    return m_aHttpCustomizer;
  }

  /**
   * Set the HTTP Post Customizer to be used.
   *
   * @param aHttpCustomizer
   *        The new customizer. May be <code>null</code>.
   * @return this for chaining
   * @since 0.8.3
   */
  @Nonnull
  public final BasicHttpPoster setHttpCustomizer (@Nullable final IConsumer <? super HttpPost> aHttpCustomizer)
  {
    m_aHttpCustomizer = aHttpCustomizer;
    return this;
  }

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
   * @param aHttpEntity
   *        The HTTP entity to be send. May not be <code>null</code>.
   * @param aResponseHandler
   *        The Http response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @return The HTTP response. May be <code>null</code>.
   * @throws MessagingException
   *         In case moving HTTP headers from the Mime part to the HTTP message
   *         fails
   * @throws IOException
   *         In case of IO error
   */
  @Nullable
  public final <T> T sendGenericMessage (@Nonnull @Nonempty final String sURL,
                                         @Nonnull final HttpEntity aHttpEntity,
                                         @Nonnull final ResponseHandler <? extends T> aResponseHandler) throws MessagingException,
                                                                                                        IOException
  {
    ValueEnforcer.notEmpty (sURL, "URL");
    ValueEnforcer.notNull (aHttpEntity, "HttpEntity");

    try (final HttpClientManager aClient = new HttpClientManager (m_aHttpClientFactory))
    {
      final HttpPost aPost = new HttpPost (sURL);
      if (aHttpEntity instanceof HttpMimeMessageEntity)
      {
        MessageHelperMethods.moveMIMEHeadersToHTTPHeader (((HttpMimeMessageEntity) aHttpEntity).getMimeMessage (),
                                                          aPost);
      }
      aPost.setEntity (aHttpEntity);

      // Invoke optional customizer
      if (m_aHttpCustomizer != null)
        m_aHttpCustomizer.accept (aPost);

      // Debug sending
      {
        AS4HttpDebug.debug ( () -> {
          final StringBuilder ret = new StringBuilder ("SEND-START to ").append (sURL).append ("\n");
          try
          {
            for (final Header aHeader : aPost.getAllHeaders ())
              ret.append (aHeader.getName ()).append (": ").append (aHeader.getValue ()).append (CHttp.EOL);
            ret.append (CHttp.EOL);
            if (aHttpEntity.isRepeatable ())
              ret.append (EntityUtils.toString (aHttpEntity));
            else
              ret.append ("## The payload is marked as 'not repeatable' and is the therefore not printed in debugging");
          }
          catch (final Exception ex)
          {
            ret.append ("## Exception listing payload: " + ex.getClass ().getName () + " -- " + ex.getMessage ())
               .append (CHttp.EOL);
            ret.append ("## ").append (StackTraceHelper.getStackAsString (ex));
          }
          return ret.toString ();
        });
      }

      return aClient.execute (aPost, aResponseHandler);
    }
  }
}
