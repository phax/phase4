/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;

import com.helger.as4.mock.MockPModeGenerator;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.settings.ISettings;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since all these classes need the same setup and a helper method, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractClientSetUp
{
  protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (AbstractClientSetUp.class);

  protected ISettings m_aSettings;
  protected int m_nStatusCode;
  protected CloseableHttpClient m_aClient;
  protected HttpPost m_aPost;

  @Before
  public void setUp () throws KeyManagementException, NoSuchAlgorithmException
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();

    m_aSettings = AS4ServerConfiguration.getSettings ();
    final String sURL = m_aSettings.getAsString ("server.address", "http://localhost:8080/as4");

    LOG.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, RandomHelper.getSecureRandom ());

    m_aClient = new HttpClientFactory (aSSLContext)
    {
      @Override
      @Nonnull
      public RequestConfig createRequestConfig ()
      {
        return RequestConfig.custom ()
                            .setCookieSpec (CookieSpecs.DEFAULT)
                            .setSocketTimeout (100_000)
                            .setConnectTimeout (5_000)
                            .setConnectionRequestTimeout (5_000)
                            .setCircularRedirectsAllowed (false)
                            .setRedirectsEnabled (true)
                            .build ();
      }
    }.createHttpClient ();

    m_aPost = new HttpPost (sURL);

    if (m_aSettings.getAsBoolean ("server.proxy.enabled", false))
    {
      m_aPost.setConfig (RequestConfig.custom ()
                                      .setProxy (new HttpHost (m_aSettings.getAsString ("server.proxy.address"),
                                                               m_aSettings.getAsInt ("server.proxy.port")))
                                      .build ());
    }

    // Create the mock PModes
    MockPModeGenerator.ensureMockPModesArePresent ();
  }

  @After
  public void after ()
  {
    if (m_aClient != null)
    {
      StreamHelper.close (m_aClient);
      m_aClient = null;
    }
  }
}
