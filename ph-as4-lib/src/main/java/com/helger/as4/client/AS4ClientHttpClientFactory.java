/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.apache.http.impl.client.HttpClientBuilder;

import com.helger.as4.http.HttpClientRetryHandler;
import com.helger.commons.ValueEnforcer;
import com.helger.httpclient.HttpClientFactory;

/**
 * Special {@link HttpClientFactory} using a retry handler by default.
 *
 * @author Philip Helger
 */
public class AS4ClientHttpClientFactory extends HttpClientFactory
{
  public static final int DEFAULT_RETRIES = 3;
  private int m_nRetries = DEFAULT_RETRIES;

  public AS4ClientHttpClientFactory (@Nullable final SSLContext aDefaultSSLContext)
  {
    super (aDefaultSSLContext);
  }

  @Nonnegative
  public int getRetries ()
  {
    return m_nRetries;
  }

  @Nonnull
  public AS4ClientHttpClientFactory setRetries (@Nonnegative final int nRetries)
  {
    ValueEnforcer.isGE0 (nRetries, "Retries");
    m_nRetries = nRetries;
    return this;
  }

  @Override
  @Nonnull
  public HttpClientBuilder createHttpClientBuilder ()
  {
    final HttpClientBuilder ret = super.createHttpClientBuilder ();
    ret.setRetryHandler (new HttpClientRetryHandler (m_nRetries));
    return ret;
  }
}
