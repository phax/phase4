/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.edelivery2;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.hc.core5.util.Timeout;

import com.helger.http.security.TrustManagerTrustAll;
import com.helger.http.tls.ETLSVersion;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;

/**
 * Special {@link HttpClientSettings} with better defaults for eDelivery AS4 2.0. TLS 1.2 is the
 * minimum required version; TLS 1.3 is preferred.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public class Phase4EDelivery2HttpClientSettings extends HttpClientSettings
{
  @SuppressWarnings ("hiding")
  public static final Timeout DEFAULT_CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds (1);
  @SuppressWarnings ("hiding")
  public static final Timeout DEFAULT_CONNECT_TIMEOUT = Timeout.ofSeconds (5);
  @SuppressWarnings ("hiding")
  public static final Timeout DEFAULT_RESPONSE_TIMEOUT = Timeout.ofSeconds (100);

  public Phase4EDelivery2HttpClientSettings () throws GeneralSecurityException
  {
    // eDelivery AS4 2.0 requires TLS v1.2 minimum, SHOULD support TLS v1.3
    // Try TLS 1.3 first, fall back to TLS 1.2
    SSLContext aSSLContext;
    try
    {
      aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_13.getID ());
    }
    catch (final GeneralSecurityException ex)
    {
      // TLS 1.3 not available, fall back to TLS 1.2
      aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_12.getID ());
    }
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
    setSSLContext (aSSLContext);

    setConnectionRequestTimeout (DEFAULT_CONNECTION_REQUEST_TIMEOUT);
    setConnectTimeout (DEFAULT_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_RESPONSE_TIMEOUT);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);
  }
}
