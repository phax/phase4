/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server;

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

import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.mock.MockPModeGenerator;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractClientSetUp
{
  protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (AbstractClientSetUp.class);
  protected static final ConfigFile PROPS = new ConfigFileBuilder ().addPath ("private-as4.properties")
                                                                    .addPath ("as4.properties")
                                                                    .build ();

  protected int m_nStatusCode;
  protected String m_sResponse;
  protected CloseableHttpClient m_aClient;
  protected HttpPost m_aPost;

  @Before
  public void setUp () throws KeyManagementException, NoSuchAlgorithmException
  {
    final String sURL = PROPS.getAsString ("server.address");

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

    if (PROPS.getAsBoolean ("server.proxy.enabled", false))
    {
      m_aPost.setConfig (RequestConfig.custom ()
                                      .setProxy (new HttpHost (PROPS.getAsString ("server.proxy.address"),
                                                               PROPS.getAsInt ("server.proxy.port")))
                                      .build ());
    }

    for (final ESOAPVersion e : ESOAPVersion.values ())
    {
      final PMode aPMode = MockPModeGenerator.getTestPModeWithSecurity (e);
      if (!MetaAS4Manager.getPModeMgr ().containsWithID (aPMode.getID ()))
        MetaAS4Manager.getPModeMgr ().createPMode (aPMode);
    }
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
