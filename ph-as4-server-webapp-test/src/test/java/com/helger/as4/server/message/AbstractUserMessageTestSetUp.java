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
package com.helger.as4.server.message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.server.AbstractClientSetUp;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientRetryHandler.ERetryMode;

public abstract class AbstractUserMessageTestSetUp extends AbstractClientSetUp
{
  public static final String SETTINGS_SERVER_PROXY_ENABLED = "server.proxy.enabled";
  public static final String SETTINGS_SERVER_PROXY_ADDRESS = "server.proxy.address";
  public static final String SETTINGS_SERVER_PROXY_PORT = "server.proxy.port";

  protected static AS4ResourceManager s_aResMgr;
  private CloseableHttpClient m_aClient;
  private final int m_nRetries;

  protected AbstractUserMessageTestSetUp ()
  {
    this (2);
  }

  protected AbstractUserMessageTestSetUp (@Nonnegative final int nRetries)
  {
    m_nRetries = ValueEnforcer.isGE0 (nRetries, "Retries");
  }

  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  @Before
  public void setUpHttpClient () throws GeneralSecurityException
  {
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, RandomHelper.getSecureRandom ());

    m_aClient = new HttpClientFactory ()
    {
      @Override
      @Nonnull
      public RequestConfig.Builder createRequestConfigBuilder ()
      {
        return super.createRequestConfigBuilder ().setSocketTimeout (500_000);
      }
    }.setSSLContext (aSSLContext).setRetries (m_nRetries).setRetryMode (ERetryMode.RETRY_ALWAYS).createHttpClient ();
  }

  @After
  public void destroyHttpClient ()
  {
    if (m_aClient != null)
    {
      StreamHelper.close (m_aClient);
      m_aClient = null;
    }
  }

  @Nonnull
  private HttpPost _createPost ()
  {
    final String sURL = m_aSettings.getAsString (MockJettySetup.SETTINGS_SERVER_ADDRESS,
                                                 AS4TestConstants.DEFAULT_SERVER_ADDRESS);

    LOG.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    final HttpPost aPost = new HttpPost (sURL);

    if (m_aSettings.getAsBoolean (SETTINGS_SERVER_PROXY_ENABLED, false))
    {
      // E.g. using little proxy for faking "no response"
      aPost.setConfig (RequestConfig.custom ()
                                    .setProxy (new HttpHost (m_aSettings.getAsString (SETTINGS_SERVER_PROXY_ADDRESS),
                                                             m_aSettings.getAsInt (SETTINGS_SERVER_PROXY_PORT)))
                                    .build ());
    }
    return aPost;
  }

  @Nonnull
  private String _sendPlainMessage (@Nonnull final HttpPost aPost,
                                    @Nonnull final HttpEntity aHttpEntity,
                                    final boolean bExpectSuccess,
                                    @Nullable final String sExecptedErrorCode) throws IOException
  {
    AS4HttpDebug.debug ( () -> {
      String ret = "TEST-SEND-START to " + aPost.getURI ();
      try
      {
        ret += " - " + EntityUtils.toString (aHttpEntity);
      }
      catch (final IOException ex)
      { /* ignore */ }
      return ret;
    });

    aPost.setEntity (aHttpEntity);

    try (final CloseableHttpResponse aHttpResponse = m_aClient.execute (aPost))
    {
      final int nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
      final HttpEntity aEntity = aHttpResponse.getEntity ();
      final String sResponse = aEntity == null ? "" : EntityUtils.toString (aEntity);

      AS4HttpDebug.debug ( () -> "TEST-SEND-RESPONSE received: " + sResponse);

      if (bExpectSuccess)
      {
        assertTrue ("Server responded with an error.\nResponse: " + sResponse, !sResponse.contains ("Error"));
        assertTrue ("Server responded with an error code (" + nStatusCode + "). Content:\n" + sResponse,
                    nStatusCode == HttpServletResponse.SC_OK || nStatusCode == HttpServletResponse.SC_NO_CONTENT);
      }
      else
      {
        // 200, 400 or 500
        assertTrue ("Server responded with StatusCode=" +
                    nStatusCode +
                    ". Response:\n" +
                    sResponse,
                    nStatusCode == HttpServletResponse.SC_OK ||
                               nStatusCode == HttpServletResponse.SC_BAD_REQUEST ||
                               nStatusCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue ("Server responded with different error message than expected (" +
                    sExecptedErrorCode +
                    ")." +
                    " StatusCode=" +
                    nStatusCode +
                    "\nResponse: " +
                    sResponse,
                    sResponse.contains (sExecptedErrorCode));
      }
      return sResponse;
    }
    catch (final HttpHostConnectException ex)
    {
      // No such server running
      fail ("No target AS4 server reachable: " + ex.getMessage () + " \n Check your properties!");
      throw new IllegalStateException ("Never reached!");
    }
  }

  @Nonnull
  protected String sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                    final boolean bExpectSuccess,
                                    @Nullable final String sExpectedErrorCode) throws IOException, MessagingException
  {
    final HttpPost aPost = _createPost ();
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aHttpEntity.getMimeMessage (), aPost);
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sExpectedErrorCode);
  }

  /**
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bExpectSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sExpectedErrorCode
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @return Response as String
   * @throws IOException
   */
  @Nonnull
  protected String sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                     final boolean bExpectSuccess,
                                     @Nullable final String sExpectedErrorCode) throws IOException
  {
    final HttpPost aPost = _createPost ();
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sExpectedErrorCode);
  }
}
