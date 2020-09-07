/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.config.IConfig;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientRetryHandler.ERetryMode;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4CryptoFactoryPropertiesFile;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.http.AS4HttpDebug;
import com.helger.phase4.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.profile.cef.AS4CEFProfileRegistarSPI;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.util.AS4ResourceHelper;

public abstract class AbstractUserMessageTestSetUp extends AbstractAS4TestSetUp
{
  public static final String SETTINGS_SERVER_PROXY_ENABLED = "server.proxy.enabled";
  public static final String SETTINGS_SERVER_PROXY_ADDRESS = "server.proxy.address";
  public static final String SETTINGS_SERVER_PROXY_PORT = "server.proxy.port";

  protected static final String DEFAULT_PARTY_ID = "APP_MOCK_DUMMY_001";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractUserMessageTestSetUp.class);

  protected static AS4ResourceHelper s_aResMgr;

  protected final IAS4CryptoFactory m_aCryptoFactory = AS4CryptoFactoryPropertiesFile.getDefaultInstance ();
  protected final AS4CryptParams m_aCryptParams = AS4CryptParams.createDefault ().setAlias ("ph-as4");
  private CloseableHttpClient m_aHttpClient;
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
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
    MetaAS4Manager.getProfileMgr ().setDefaultProfileID (AS4CEFProfileRegistarSPI.AS4_PROFILE_ID);
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    // s_aResMgr is closed by MockJettySetup
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  @Before
  public void setUpHttpClient () throws GeneralSecurityException
  {
    final HttpClientSettings aHCS = new HttpClientSettings ();
    aHCS.setSSLContextTrustAll ();
    aHCS.setSocketTimeoutMS (500_000);
    aHCS.setRetryCount (m_nRetries);
    aHCS.setRetryMode (ERetryMode.RETRY_ALWAYS);
    m_aHttpClient = new HttpClientFactory (aHCS).createHttpClient ();
  }

  @After
  public void destroyHttpClient ()
  {
    if (m_aHttpClient != null)
    {
      StreamHelper.close (m_aHttpClient);
      m_aHttpClient = null;
    }
  }

  @Nonnull
  private HttpPost _createPost ()
  {
    final IConfig aConfig = AS4Configuration.getConfig ();
    final String sURL = aConfig.getAsString (MockJettySetup.SETTINGS_SERVER_ADDRESS, AS4TestConstants.DEFAULT_SERVER_ADDRESS);

    LOGGER.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    final HttpPost aPost = new HttpPost (sURL);

    if (aConfig.getAsBoolean (SETTINGS_SERVER_PROXY_ENABLED, false))
    {
      // E.g. using little proxy for faking "no response"
      final HttpHost aProxyHost = new HttpHost (aConfig.getAsString (SETTINGS_SERVER_PROXY_ADDRESS),
                                                aConfig.getAsInt (SETTINGS_SERVER_PROXY_PORT));
      LOGGER.info ("Using proxy host " + aProxyHost.toString ());
      aPost.setConfig (RequestConfig.custom ().setProxy (aProxyHost).build ());
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
      final StringBuilder aSB = new StringBuilder ();
      aSB.append ("TEST-SEND-START to ").append (aPost.getURI ()).append ("\n");
      try
      {
        final Header [] aHeaders = aPost.getAllHeaders ();
        if (ArrayHelper.isNotEmpty (aHeaders))
        {
          for (final Header aHeader : aHeaders)
            aSB.append (aHeader.getName ()).append ('=').append (aHeader.getValue ()).append ("\n");
          aSB.append ("\n");
        }
        aSB.append (EntityUtils.toString (aHttpEntity));
      }
      catch (final IOException ex)
      {
        aSB.append (StackTraceHelper.getStackAsString (ex));
      }
      return aSB.toString ();
    });

    aPost.setEntity (aHttpEntity);

    try (final CloseableHttpResponse aHttpResponse = m_aHttpClient.execute (aPost))
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
  protected final String sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                          final boolean bExpectSuccess,
                                          @Nullable final String sExpectedErrorCode) throws IOException, MessagingException
  {
    final HttpPost aPost = _createPost ();
    MessageHelperMethods.forEachHeaderAndRemoveAfterwards (aHttpEntity.getMimeMessage (), aPost::addHeader, true);
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
  protected final String sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                           final boolean bExpectSuccess,
                                           @Nullable final String sExpectedErrorCode) throws IOException
  {
    final HttpPost aPost = _createPost ();
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
  protected final String sendPlainMessageAndWait (@Nonnull final HttpEntity aHttpEntity,
                                                  final boolean bExpectSuccess,
                                                  @Nullable final String sExpectedErrorCode) throws IOException
  {
    final String ret = sendPlainMessage (aHttpEntity, bExpectSuccess, sExpectedErrorCode);
    if (false)
    {
      LOGGER.info ("Waiting for 0.5 seconds");
      ThreadHelper.sleep (500);
    }
    return ret;
  }
}
