/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2024 Philip Helger (www.helger.com)
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

import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.hc.core5.util.Timeout;

import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.http.tls.ETLSVersion;
import com.helger.http.tls.TLSConfigurationMode;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;

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

  public Phase4BDEWHttpClientSettings () throws GeneralSecurityException
  {
    // BDEW recommends at least TLS v1.2 [TR02102-2]
    final SSLContext aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_12.getID ());

    // TODO - trust store is required for mutual TLS (Spec section 2.2.6.1)
    // But we're basically trusting all hosts - the exact list is hard to
    // determine
    aSSLContext.init ((KeyManager []) null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
    setSSLContext (aSSLContext);

    // Cipher Suite follow BSI TR03116-3, section 4 as of 2022-12-06
    final TLSConfigurationMode aTLSConfigMode = new TLSConfigurationMode (new ETLSVersion [] { ETLSVersion.TLS_12,
                                                                                               ETLSVersion.TLS_13 },
                                                                          new String [] {
                                                                                          // TLS
                                                                                          // 1.3
                                                                                          "TLS_AES_128_GCM_SHA256",
                                                                                          "TLS_AES_256_GCM_SHA384",
                                                                                          "TLS_AES_128_CCM_SHA256",
                                                                                          // TLS
                                                                                          // 1.2
                                                                                          "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                                                                                          "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                                                                                          "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                                                                          "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384" });
    setTLSConfigurationMode (aTLSConfigMode);

    setConnectionRequestTimeout (DEFAULT_BDEW_CONNECTION_REQUEST_TIMEOUT);
    setConnectTimeout (DEFAULT_BDEW_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_BDEW_RESPONSE_TIMEOUT);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);
  }
}
