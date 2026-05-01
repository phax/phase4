/*
 * Copyright (C) 2019-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.peppol;

import org.apache.hc.core5.util.Timeout;

import com.helger.base.CGlobal;
import com.helger.http.tls.ETLSVersion;
import com.helger.http.tls.TLSConfigurationMode;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;
import com.helger.security.revocation.CertificateRevocationCheckerDefaults;

/**
 * Special {@link HttpClientSettings} with better defaults for Peppol.<br>
 * Was originally in phase4-peppol-client but was moved to phase4-profile-peppol in v2.7.4
 *
 * @author Philip Helger
 * @since 0.9.10, 2.7.4
 */
public class Phase4PeppolHttpClientSettings extends HttpClientSettings
{
  public static final Timeout DEFAULT_PEPPOL_CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds (1);
  public static final Timeout DEFAULT_PEPPOL_CONNECT_TIMEOUT = Timeout.ofSeconds (5);
  // 2 minutes according new Peppol SLAs
  public static final Timeout DEFAULT_PEPPOL_RESPONSE_TIMEOUT = Timeout.ofMinutes (2);

  public Phase4PeppolHttpClientSettings ()
  {
    setTLSConfigurationMode (new TLSConfigurationMode (new ETLSVersion [] { ETLSVersion.TLS_13, ETLSVersion.TLS_12 },
                                                       CGlobal.EMPTY_STRING_ARRAY));
    setRevocationCheckMode (CertificateRevocationCheckerDefaults.getRevocationCheckMode ());

    setConnectionRequestTimeout (DEFAULT_PEPPOL_CONNECTION_REQUEST_TIMEOUT);
    setConnectTimeout (DEFAULT_PEPPOL_CONNECT_TIMEOUT);
    setResponseTimeout (DEFAULT_PEPPOL_RESPONSE_TIMEOUT);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);
  }
}
