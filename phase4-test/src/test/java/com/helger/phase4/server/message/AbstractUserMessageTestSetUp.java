/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.http.CHttp;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.mutable.MutableInt;
import com.helger.config.IConfig;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.phase4.util.AS4ResourceHelper;

import jakarta.mail.MessagingException;

public abstract class AbstractUserMessageTestSetUp extends AbstractAS4TestSetUp
{
  public static final String SETTINGS_SERVER_PROXY_ENABLED = "server.proxy.enabled";
  public static final String SETTINGS_SERVER_PROXY_ADDRESS = "server.proxy.address";
  public static final String SETTINGS_SERVER_PROXY_PORT = "server.proxy.port";

  protected static final String DEFAULT_PARTY_ID = "APP_MOCK_DUMMY_001";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractUserMessageTestSetUp.class);

  protected static AS4ResourceHelper s_aResMgr;

  protected final IAS4CryptoFactory m_aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();
  protected final AS4CryptParams m_aCryptParams = AS4CryptParams.createDefault ().setAlias ("ph-as4");
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
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT);
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (null);
    // s_aResMgr is closed by MockJettySetup
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  @Nonnull
  private static HttpPost _createMockPostToLocalJetty ()
  {
    final String sURL = MockJettySetup.getServerAddressFromSettings ();

    LOGGER.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    return new HttpPost (sURL);
  }

  @Nonnull
  private String _sendPlainMessage (@Nonnull final HttpPost aPost,
                                    @Nonnull final HttpEntity aHttpEntity,
                                    final boolean bExpectSuccess,
                                    @Nullable final String sExecptedResponseContent) throws IOException
  {
    // Debug code
    AS4HttpDebug.debug ( () -> {
      final StringBuilder aSB = new StringBuilder ();
      aSB.append ("TEST-SEND-START to ").append (aPost.toString ()).append ("\n");
      try
      {
        final Header [] aHeaders = aPost.getHeaders ();
        if (ArrayHelper.isNotEmpty (aHeaders))
        {
          for (final Header aHeader : aHeaders)
            aSB.append (aHeader.getName ()).append ('=').append (aHeader.getValue ()).append ('\n');
          aSB.append ('\n');
        }
        aSB.append (HttpClientHelper.entityToString (aHttpEntity, StandardCharsets.UTF_8));
      }
      catch (final IOException ex)
      {
        aSB.append (StackTraceHelper.getStackAsString (ex));
      }
      return aSB.toString ();
    });

    aPost.setEntity (aHttpEntity);

    final HttpClientSettings aHCS = new HttpClientSettings ();
    try
    {
      aHCS.setSSLContextTrustAll ();
    }
    catch (final GeneralSecurityException ex)
    {
      throw new IllegalStateException (ex);
    }
    aHCS.setResponseTimeout (Timeout.ofMinutes (5));
    aHCS.setRetryCount (m_nRetries);
    // Only required for "testEsens_TA10"
    aHCS.setRetryAlways (true);

    final IConfig aConfig = AS4Configuration.getConfig ();
    if (aConfig.getAsBoolean (SETTINGS_SERVER_PROXY_ENABLED, false))
    {
      // E.g. using little proxy for faking "no response"
      final HttpHost aProxyHost = new HttpHost (aConfig.getAsString (SETTINGS_SERVER_PROXY_ADDRESS),
                                                aConfig.getAsInt (SETTINGS_SERVER_PROXY_PORT));
      LOGGER.info ("Using proxy host " + aProxyHost.toString ());
      aHCS.setProxyHost (aProxyHost);
    }

    try (final CloseableHttpClient aHttpClient = new HttpClientFactory (aHCS).createHttpClient ())
    {
      // Response status code
      final MutableInt aSC = new MutableInt (-1);

      // Get response content as UTF-8 String
      final String sResponse = aHttpClient.execute (aPost, aHttpResponse -> {
        aSC.set (aHttpResponse.getCode ());

        final HttpEntity aEntity = aHttpResponse.getEntity ();
        if (aEntity == null)
          return "";

        // Consume independent of status code
        return HttpClientHelper.entityToString (aEntity, StandardCharsets.UTF_8);
      });

      final int nStatusCode = aSC.intValue ();

      if (bExpectSuccess)
      {
        assertTrue ("Server responded with an error.\nResponse: " + sResponse, !sResponse.contains ("Error"));
        assertTrue ("Server responded with an error code (" + nStatusCode + "). Content:\n" + sResponse,
                    nStatusCode == CHttp.HTTP_OK || nStatusCode == CHttp.HTTP_NO_CONTENT);
      }
      else
      {
        // 200, 400 or 500
        assertTrue ("Server responded with StatusCode=" +
                    nStatusCode +
                    ". Response:\n" +
                    sResponse,
                    nStatusCode == CHttp.HTTP_OK ||
                               nStatusCode == CHttp.HTTP_BAD_REQUEST ||
                               nStatusCode == CHttp.HTTP_INTERNAL_SERVER_ERROR);
        assertTrue ("Server responded with different error message than expected (" +
                    sExecptedResponseContent +
                    ")." +
                    " StatusCode=" +
                    nStatusCode +
                    "\nResponse: '" +
                    sResponse +
                    "'",
                    sResponse.contains (sExecptedResponseContent));
      }
      return sResponse;
    }
  }

  /**
   * Send a MIME message to the locally spawned Jetty
   *
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bExpectSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sExecptedResponseContent
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @return Response as String
   * @throws IOException
   *         In case HTTP sending fails
   * @throws MessagingException
   *         in case there is some error with the MIME message
   */
  @Nonnull
  protected final String sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                          final boolean bExpectSuccess,
                                          @Nullable final String sExecptedResponseContent) throws IOException,
                                                                                           MessagingException
  {
    final HttpPost aPost = _createMockPostToLocalJetty ();

    // Move all headers from MIME message to the HTTP POST
    AS4MimeMessageHelper.forEachHeaderAndRemoveAfterwards (aHttpEntity.getMimeMessage (), aPost::addHeader, true);

    // Ready to send
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sExecptedResponseContent);
  }

  /**
   * Send a non-MIME message to the locally spawned Jetty
   *
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bExpectSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sExecptedResponseContent
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @return Response as String
   * @throws IOException
   *         In case HTTP sending fails
   */
  @Nonnull
  protected final String sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                           final boolean bExpectSuccess,
                                           @Nullable final String sExecptedResponseContent) throws IOException
  {
    final HttpPost aPost = _createMockPostToLocalJetty ();

    // Ready to send
    return _sendPlainMessage (aPost, aHttpEntity, bExpectSuccess, sExecptedResponseContent);
  }
}
