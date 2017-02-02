package com.helger.as4.client;

import javax.annotation.Nonnull;

import org.apache.http.impl.client.HttpClientBuilder;

import com.helger.httpclient.HttpClientFactory;

/**
 * Special {@link HttpClientFactory} using a retry handler by default.
 * 
 * @author Philip Helger
 */
public class AS4HttpClientFactory extends HttpClientFactory
{
  @Override
  @Nonnull
  public HttpClientBuilder createHttpClientBuilder ()
  {
    final HttpClientBuilder ret = super.createHttpClientBuilder ();
    ret.setRetryHandler (new HttpClientRetryHandler (3));
    return ret;
  }
}
