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

import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.base.CGlobal;
import com.helger.http.tls.ETLSVersion;
import com.helger.http.tls.TLSConfigurationMode;
import com.helger.httpclient.HttpClientSettings;
import com.helger.peppol.security.MozillaNSSTrustStore;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.security.revocation.CertificateRevocationCheckerDefaults;

/**
 * Special {@link HttpClientSettings} with better defaults for Peppol.<br>
 * Was originally in phase4-peppol-client but was moved to phase4-profile-peppol in v2.7.4
 * <p>
 * <b>Behaviour change in 4.5.0:</b> Prior to 4.5.0 this class installed an SSLContext that trusted
 * <i>all</i> server certificates by default. Starting with 4.5.0 no custom SSLContext is installed
 * and TLS connections fall back to the JVM default truststore (<code>cacerts</code>). If a Peppol
 * AP certificate does not chain to a CA in <code>cacerts</code> the TLS handshake will now fail
 * with <code>PKIX path building failed</code>. Three explicit choices are available:
 * </p>
 * <ul>
 * <li>{@link HttpClientSettings#setSSLContextTrustAll()} - restore the pre-4.5.0 "trust everything"
 * behaviour. Only suitable for local / integration tests.</li>
 * <li>{@link #setSSLContextPeppolMozillaNSS()} - use the Mozilla NSS root certificate trust store
 * shipped with <code>peppol-commons</code>. This is the strictest option for production Peppol
 * connections and matches the trust store browsers use.</li>
 * <li>Leave the default - the JVM default truststore is used and the configured revocation check
 * mode (see {@link CertificateRevocationCheckerDefaults}) is applied.</li>
 * </ul>
 * <p>
 * See the <a href="https://github.com/phax/phase4/wiki/Migrations">Migrations</a> wiki page for
 * details.
 * </p>
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

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4PeppolHttpClientSettings.class);

  // Used to log the AP TLS defaults note only once per JVM lifetime
  private static final AtomicBoolean DEFAULTS_LOGGED = new AtomicBoolean (false);

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

    // Emit the AP TLS defaults note once per JVM so users of pre-4.5.0 code see what changed
    if (DEFAULTS_LOGGED.compareAndSet (false, true))
    {
      LOGGER.info ("Phase4PeppolHttpClientSettings: AP TLS connections use the JVM default truststore" +
                   " (no implicit trust-all) and the configured revocation check mode '" +
                   CertificateRevocationCheckerDefaults.getRevocationCheckMode () +
                   "'. Call HttpClientSettings.setSSLContextTrustAll() to restore pre-4.5.0 behaviour or" +
                   " Phase4PeppolHttpClientSettings.setSSLContextPeppolMozillaNSS() to use the Mozilla NSS root truststore.");
    }
  }

  /**
   * Install an {@link SSLContext} that uses the Mozilla NSS root certificate trust store provided
   * by <code>peppol-commons</code>. This is the strictest production option for Peppol TLS
   * connections - it limits trust to the same set of root CAs that Mozilla products use, instead
   * of relying on what happens to be in the JVM's <code>cacerts</code>.
   * <p>
   * As soon as a custom {@link SSLContext} is set, the revocation check mode configured via
   * {@link #setRevocationCheckMode(com.helger.security.revocation.ERevocationCheckMode)} is no
   * longer applied - the custom SSLContext takes precedence. If revocation checking is required
   * in combination with the Mozilla NSS root trust store, build a PKIX-aware SSLContext
   * separately and install it via {@link #setSSLContext(SSLContext)}.
   * </p>
   *
   * @return this for chaining
   * @throws GeneralSecurityException
   *         if the SSLContext cannot be created
   * @see MozillaNSSTrustStore#TRUSTSTORE
   * @since 4.5.1
   */
  @NonNull
  public final Phase4PeppolHttpClientSettings setSSLContextPeppolMozillaNSS () throws GeneralSecurityException
  {
    final TrustManagerFactory aTMF = TrustManagerFactory.getInstance (TrustManagerFactory.getDefaultAlgorithm ());
    aTMF.init (MozillaNSSTrustStore.TRUSTSTORE);

    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init ((KeyManager []) null, aTMF.getTrustManagers (), null);
    setSSLContext (aSSLContext);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Phase4PeppolHttpClientSettings: installed SSLContext using the Mozilla NSS root truststore from peppol-commons.");
    return this;
  }
}
