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
