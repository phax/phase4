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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
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

import com.helger.as4.client.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.server.AbstractClientSetUp;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;

public abstract class AbstractUserMessageTestSetUp extends AbstractClientSetUp
{
  protected static AS4ResourceManager s_aResMgr;
  private CloseableHttpClient m_aClient;

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
  public void setUpHttpClient () throws KeyManagementException, NoSuchAlgorithmException
  {
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
    final String sURL = m_aSettings.getAsString ("server.address", "http://localhost:8080/as4");

    LOG.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    final HttpPost aPost = new HttpPost (sURL);

    if (m_aSettings.getAsBoolean ("server.proxy.enabled", false))
    {
      aPost.setConfig (RequestConfig.custom ()
                                    .setProxy (new HttpHost (m_aSettings.getAsString ("server.proxy.address"),
                                                             m_aSettings.getAsInt ("server.proxy.port")))
                                    .build ());
    }
    return aPost;
  }

  @Nonnull
  private String _sendPlainMessage (@Nonnull final HttpPost aPost,
                                    @Nonnull final HttpEntity aHttpEntity,
                                    final boolean bExpectSuccess,
                                    @Nullable final String sErrorCode) throws IOException
  {
    aPost.setEntity (aHttpEntity);

    try
    {
      final CloseableHttpResponse aHttpResponse = m_aClient.execute (aPost);

      final int nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
      final HttpEntity aEntity = aHttpResponse.getEntity ();
      final String sResponse = aEntity == null ? "" : EntityUtils.toString (aEntity);

      if (bExpectSuccess)
      {
        assertTrue ("Server responded with an error.\nResponse: " + sResponse, !sResponse.contains ("Error"));
        assertTrue ("Server responded with an error code (" +
                    nStatusCode +
                    "). Content:\n" +
                    sResponse,
                    nStatusCode == HttpServletResponse.SC_OK || nStatusCode == HttpServletResponse.SC_NO_CONTENT);
      }
      else
      {
        if (sErrorCode.equals ("500"))
        {
          // Expecting Internal Servlet error
          assertEquals ("Server responded with internal servlet error", 500, nStatusCode);
        }
        else
        {
          // Status code may by 20x but may be an error anyway
          assertTrue ("Server responded with success or different error message but failure was expected." +
                      "StatusCode: " +
                      nStatusCode +
                      "\nResponse: " +
                      sResponse,
                      sResponse.contains (sErrorCode));
        }
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
                                    @Nullable final String sErrorCode) throws IOException, MessagingException
  {
    final HttpPost aPost = _createPost ();
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aHttpEntity.getMimeMessage (), aPost);
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sErrorCode);
  }

  /**
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bExpectSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sErrorCode
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @return Response as String
   * @throws IOException
   */
  @Nonnull
  protected String sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                     final boolean bExpectSuccess,
                                     @Nullable final String sErrorCode) throws IOException
  {
    final HttpPost aPost = _createPost ();
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sErrorCode);
  }
}
