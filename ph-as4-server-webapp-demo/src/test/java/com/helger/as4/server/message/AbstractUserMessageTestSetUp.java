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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.helger.as4.client.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.server.AbstractClientSetUp;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;

public abstract class AbstractUserMessageTestSetUp extends AbstractClientSetUp
{
  protected static AS4ResourceManager s_aResMgr;

  @BeforeClass
  public static void startServer () throws Exception
  {
    AS4ServerConfiguration.reinitForTestOnly ();
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  protected void sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                  final boolean bExpectSuccess,
                                  @Nullable final String sErrorCode) throws IOException, MessagingException
  {
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aHttpEntity.getMimeMessage (), m_aPost);
    sendPlainMessage (aHttpEntity, bExpectSuccess, sErrorCode);
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
   * @throws IOException
   */
  protected void sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                   final boolean bExpectSuccess,
                                   @Nullable final String sErrorCode) throws IOException
  {
    m_aPost.setEntity (aHttpEntity);

    try
    {
      final CloseableHttpResponse aHttpResponse = m_aClient.execute (m_aPost);

      m_nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
      final HttpEntity aEntity = aHttpResponse.getEntity ();
      m_sResponse = aEntity == null ? "" : EntityUtils.toString (aEntity);

      if (bExpectSuccess)
      {
        assertTrue ("Server responded with an error.\nResponse: " + m_sResponse, !m_sResponse.contains ("Error"));
        assertTrue ("Server responded with an error code (" +
                    m_nStatusCode +
                    "). Content:\n" +
                    m_sResponse,
                    m_nStatusCode == HttpServletResponse.SC_OK || m_nStatusCode == HttpServletResponse.SC_NO_CONTENT);
      }
      else
      {
        if (sErrorCode.equals ("500"))
        {
          // Expecting Internal Servlet error
          assertEquals ("Server responded with internal servlet error", 500, m_nStatusCode);
        }
        else
        {
          // Status code may by 20x but may be an error anyway
          assertTrue ("Server responded with success or different error message but failure was expected." +
                      "StatusCode: " +
                      m_nStatusCode +
                      "\nResponse: " +
                      m_sResponse,
                      m_sResponse.contains (sErrorCode));
        }
      }
    }
    catch (final HttpHostConnectException ex)
    {
      // No such server running
      fail ("No target AS4 server reachable: " + ex.getMessage () + " \n Check your properties!");
    }
  }
}
