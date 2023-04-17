/*
 * Copyright (C) 2023 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.bdew;

import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.http.tls.ETLSVersion;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;

/**
 * Special {@link HttpClientSettings} with better defaults for BDEW.
 *
 * @author Gregor Scholtysik
 */
public class Phase4BDEWHttpClientSettings extends HttpClientSettings
{
  public static final Timeout DEFAULT_BDEW_CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds (1);
  public static final Timeout DEFAULT_BDEW_CONNECT_TIMEOUT = Timeout.ofSeconds (5);
  public static final Timeout DEFAULT_BDEW_RESPONSE_TIMEOUT = Timeout.ofSeconds (100);

  public Phase4BDEWHttpClientSettings() throws GeneralSecurityException
  {
    // BDEW recommends at least TLS v1.2 [TR02102-2]
    final SSLContext aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_12.getID ());
    // But we're basically trusting all hosts - the exact list is hard to
    // determine
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
    setSSLContext (aSSLContext);

    setConnectionRequestTimeout (DEFAULT_BDEW_CONNECTION_REQUEST_TIMEOUT);
    setConnectTimeout (DEFAULT_BDEW_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_BDEW_RESPONSE_TIMEOUT);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);
  }
}
