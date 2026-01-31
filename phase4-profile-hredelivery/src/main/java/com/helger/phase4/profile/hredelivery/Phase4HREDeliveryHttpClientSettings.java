/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.hredelivery;

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
 * Special {@link HttpClientSettings} with better defaults for HR eDelivery
 *
 * @author Philip Helger
 * @since 4.0.2
 */
public class Phase4HREDeliveryHttpClientSettings extends HttpClientSettings
{
  public static final Timeout DEFAULT_HR_EDELIVERY_CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds (1);
  public static final Timeout DEFAULT_HR_EDELIVERY_CONNECT_TIMEOUT = Timeout.ofSeconds (5);
  public static final Timeout DEFAULT_HR_EDELIVERY_RESPONSE_TIMEOUT = Timeout.ofMinutes (2);

  public Phase4HREDeliveryHttpClientSettings ()
  {
    try
    {
      // HR eDelivery requires TLS v1.2
      final SSLContext aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_12.getID ());
      // But we're basically trusting all hosts - the exact list is hard to
      // determine
      aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
      setSSLContext (aSSLContext);
    }
    catch (final GeneralSecurityException ex)
    {
      throw new IllegalStateException ("Failed to initialize SSLContext for Phase4HREDeliveryHttpClientSettings", ex);
    }

    setConnectionRequestTimeout (DEFAULT_HR_EDELIVERY_CONNECTION_REQUEST_TIMEOUT);
    setConnectTimeout (DEFAULT_HR_EDELIVERY_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_HR_EDELIVERY_RESPONSE_TIMEOUT);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);

    // Explicitly disable retry here - done on the phase4-level
    setRetryCount (0);
  }
}
