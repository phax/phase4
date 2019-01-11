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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.http.CHttp;
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
  // By default no special SSL context present
  private IHttpClientProvider m_aHTTPClientProvider = new HttpClientFactory ().setRetries (3);

  public BasicHttpPoster ()
  {}

  /**
   * @return The internal http client provider used in
   *         {@link #sendGenericMessage(String, HttpEntity, ResponseHandler)}.
   */
  @Nonnull
  protected IHttpClientProvider getHttpClientProvider ()
  {
    return m_aHTTPClientProvider;
  }

  /**
   * Set the HTTP client provider to be used. This is e.g. necessary when a
   * custom SSL context is to be used. See {@link HttpClientFactory} as the
   * default implementation of {@link IHttpClientProvider}. This provider is
   * used in {@link #sendGenericMessage(String, HttpEntity, ResponseHandler)}.
   *
   * @param aHttpClientProvider
   *        The HTTP client provider to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public BasicHttpPoster setHttpClientProvider (@Nonnull final IHttpClientProvider aHttpClientProvider)
  {
    ValueEnforcer.notNull (aHttpClientProvider, "HttpClientProvider");
    m_aHTTPClientProvider = aHttpClientProvider;
    return this;
  }

  /**
   * Customize the HTTP Post before it is to be sent.
   *
   * @param aPost
   *        The post to be modified. Never <code>null</code>.
   */
  @OverrideOnDemand
  protected void customizeHttpPost (@Nonnull final HttpPost aPost)
  {}

  @Nullable
  public <T> T sendGenericMessage (@Nonnull final String sURL,
                                   @Nonnull final HttpEntity aHttpEntity,
                                   @Nonnull final ResponseHandler <? extends T> aResponseHandler) throws Exception
  {
    ValueEnforcer.notEmpty (sURL, "URL");
    ValueEnforcer.notNull (aHttpEntity, "HttpEntity");

    try (final HttpClientManager aClient = new HttpClientManager (m_aHTTPClientProvider))
    {
      final HttpPost aPost = new HttpPost (sURL);
      if (aHttpEntity instanceof HttpMimeMessageEntity)
      {
        MessageHelperMethods.moveMIMEHeadersToHTTPHeader (((HttpMimeMessageEntity) aHttpEntity).getMimeMessage (),
                                                          aPost);
      }
      aPost.setEntity (aHttpEntity);

      // Overridable method
      customizeHttpPost (aPost);

      AS4HttpDebug.debug ( () -> {
        final StringBuilder ret = new StringBuilder ("SEND-START to ").append (sURL);
        try
        {
          ret.append ("\n");
          for (final Header h : aPost.getAllHeaders ())
            ret.append (h.getName ()).append ('=').append (h.getValue ()).append (CHttp.EOL);
          ret.append (CHttp.EOL);
          ret.append (EntityUtils.toString (aHttpEntity));
        }
        catch (final IOException ex)
        { /* ignore */ }
        return ret.toString ();
      });

      return aClient.execute (aPost, aResponseHandler);
    }
  }
}
