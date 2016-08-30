package com.helger.as4lib.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;

import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractUserMessageSetUp
{
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (AbstractUserMessageSetUp.class);

  protected final ESOAPVersion m_eSOAPVersion;
  protected int m_nStatusCode;
  protected String m_sResponse;
  private CloseableHttpClient m_aClient;
  private HttpPost m_aPost;

  public AbstractUserMessageSetUp (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Before
  public void setUp () throws KeyManagementException, NoSuchAlgorithmException
  {
    // TODO read from config
    final String sURL = "http://127.0.0.1:8080/services/msh/";

    LOG.info ("The following test case will only work if there is a local AS4 server running @ " + sURL);
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, RandomHelper.getSecureRandom ());

    m_aClient = new HttpClientFactory (aSSLContext).createHttpClient ();

    m_aPost = new HttpPost (sURL);

    // TODO read from config
    if (false)
    {
      m_aPost.setConfig (RequestConfig.custom ().setProxy (new HttpHost ("10.0.0.0", 8080)).build ());
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

  protected void sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                  @Nonnull final boolean bSuccess,
                                  @Nullable final String sErrorCode) throws IOException, MessagingException
  {
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aHttpEntity.getMimeMessage (), m_aPost);
    sendPlainMessage (aHttpEntity, bSuccess, sErrorCode);
  }

  /**
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sErrorCode
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @throws IOException
   */
  protected void sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                   @Nonnull final boolean bSuccess,
                                   @Nullable final String sErrorCode) throws IOException
  {
    m_aPost.setEntity (aHttpEntity);

    try
    {
      final CloseableHttpResponse aHttpResponse = m_aClient.execute (m_aPost);

      m_nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
      m_sResponse = EntityUtils.toString (aHttpResponse.getEntity ());

      if (bSuccess)
      {
        assertEquals ("Server responded with an error code.", 200, m_nStatusCode);
        assertTrue ("Server responded with an error.\nResponse: " + m_sResponse, !m_sResponse.contains ("Error"));
      }
      else
      {
        // Status code may by 20x but may be an error anyway
        assertTrue ("Server responded with success message but failure was expected." +
                    "StatusCode: " +
                    m_nStatusCode +
                    "\nResponse: " +
                    m_sResponse,
                    m_sResponse.contains (sErrorCode));
      }
    }
    catch (final HttpHostConnectException ex)
    {
      // No such server running
      LOG.info ("No target AS4 server reachable: " + ex.getMessage ());
    }
  }
}
