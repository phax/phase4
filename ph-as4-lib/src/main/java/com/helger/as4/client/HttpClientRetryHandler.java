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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.annotation.Nonnegative;
import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HttpContext;

import com.helger.commons.ValueEnforcer;

/**
 * HTTP client retry handler based on
 * http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d4e280
 *
 * @author Apache HC
 */
public class HttpClientRetryHandler implements HttpRequestRetryHandler
{
  private final int m_nMaxRetries;

  public HttpClientRetryHandler ()
  {
    this (5);
  }

  public HttpClientRetryHandler (@Nonnegative final int nMaxRetries)
  {
    ValueEnforcer.isGE0 (nMaxRetries, "MaxRetries");
    m_nMaxRetries = nMaxRetries;
  }

  @Nonnegative
  public int getMaxRetries ()
  {
    return m_nMaxRetries;
  }

  public boolean retryRequest (final IOException aEx, final int nExecutionCount, final HttpContext aContext)
  {
    if (nExecutionCount >= m_nMaxRetries)
    {
      // Do not retry if over max retry count
      return false;
    }
    if (aEx instanceof InterruptedIOException)
    {
      // Timeout
      return false;
    }
    if (aEx instanceof UnknownHostException)
    {
      // Unknown host
      return false;
    }
    if (aEx instanceof ConnectTimeoutException)
    {
      // Connection refused
      return false;
    }
    if (aEx instanceof SSLException)
    {
      // SSL handshake exception
      return false;
    }
    final HttpClientContext aClientContext = HttpClientContext.adapt (aContext);
    final HttpRequest aRequest = aClientContext.getRequest ();
    final boolean bIdempotent = !(aRequest instanceof HttpEntityEnclosingRequest);
    if (bIdempotent)
    {
      // Retry if the request is considered idempotent
      return true;
    }
    return false;
  }
}
