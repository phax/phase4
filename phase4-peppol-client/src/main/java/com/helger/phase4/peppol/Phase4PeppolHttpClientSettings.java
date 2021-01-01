/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4Version;

/**
 * Special {@link HttpClientSettings} with better defaults for Peppol.
 *
 * @author Philip Helger
 * @since 0.9.10
 */
public class Phase4PeppolHttpClientSettings extends HttpClientSettings
{
  public Phase4PeppolHttpClientSettings () throws GeneralSecurityException
  {
    // Peppol requires TLS v1.2
    final SSLContext aSSLContext = SSLContext.getInstance ("TLSv1.2");
    // But we're basically trusting all hosts - the exact list is hard to
    // determine
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
    setSSLContext (aSSLContext);

    setConnectionRequestTimeoutMS (100_000);
    setConnectionTimeoutMS (5_000);
    setSocketTimeoutMS (100_000);

    // Set an explicit user agent
    setUserAgent ("phase4/" + CAS4Version.BUILD_VERSION + " https://github.com/phax/phase4");
  }
}
