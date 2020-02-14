package com.helger.phase4.peppol;

import java.security.GeneralSecurityException;

import org.apache.http.impl.client.HttpClientBuilder;

import com.helger.httpclient.HttpClientFactory;
import com.helger.phase4.CAS4Version;

/**
 * Special {@link HttpClientFactory} with better defaults for Peppol.
 *
 * @author Philip Helger
 */
public class Phase4PeppolHttpClientFactory extends HttpClientFactory
{
  public Phase4PeppolHttpClientFactory () throws GeneralSecurityException
  {
    super (new Phase4PeppolHttpClientSettings ());
  }

  @Override
  public HttpClientBuilder createHttpClientBuilder ()
  {
    final HttpClientBuilder ret = super.createHttpClientBuilder ();
    // Set an explicit user agent
    ret.setUserAgent ("phase4/" + CAS4Version.BUILD_VERSION + " github.com/phax/phase4");
    return ret;
  }
}
