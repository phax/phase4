/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.client;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IConsumer;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.IHttpClientProvider;
import com.helger.phase4.http.AS4HttpDebug;

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
  private boolean m_bQuoteHttpHeaders = false;

  public BasicHttpPoster ()
  {}

  /**
   * @return The internal http client factory used for http sending.
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
   * {@link IHttpClientProvider}. This factory is used for http sending.
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
   * @return <code>true</code> if HTTP header values should be quoted if they
   *         contain forbidden characters, <code>false</code> if not.
   * @since v0.9.1
   */
  public final boolean isQuoteHttpHeaders ()
  {
    return m_bQuoteHttpHeaders;
  }

  /**
   * Enable or disable, if HTTP header values should be quoted or not. For
   * compatibility it is recommended, to not quote the values.
   *
   * @param bQuoteHttpHeaders
   *        <code>true</code> to quote them, <code>false</code> to not quote
   *        them.
   * @return this for chaining
   * @since v0.9.1
   */
  @Nonnull
  public final BasicHttpPoster setQuoteHttpHeaders (final boolean bQuoteHttpHeaders)
  {
    m_bQuoteHttpHeaders = bQuoteHttpHeaders;
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
   * @param aCustomHeaders
   *        An optional http header map that should be applied. May be
   *        <code>null</code>.
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
                                         @Nullable final HttpHeaderMap aCustomHeaders,
                                         @Nonnull final ResponseHandler <? extends T> aResponseHandler) throws MessagingException,
                                                                                                        IOException
  {
    ValueEnforcer.notEmpty (sURL, "URL");
    ValueEnforcer.notNull (aHttpEntity, "HttpEntity");

    try (final HttpClientManager aClient = new HttpClientManager (m_aHttpClientFactory))
    {
      final HttpPost aPost = new HttpPost (sURL);

      if (aCustomHeaders != null)
      {
        // Always unify line endings
        // By default quoting is disabled
        aCustomHeaders.forEachSingleHeader (aPost::addHeader, true, m_bQuoteHttpHeaders);
      }

      aPost.setEntity (aHttpEntity);

      // Invoke optional customizer
      if (m_aHttpCustomizer != null)
        m_aHttpCustomizer.accept (aPost);

      // Debug sending
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

      return aClient.execute (aPost, aResponseHandler);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("HttpClientFactory", m_aHttpClientFactory)
                                       .append ("HttpCustomizer", m_aHttpCustomizer)
                                       .getToString ();

  }
}
