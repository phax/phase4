package com.helger.as4lib.message;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.helger.as4lib.soap.ESOAPVersion;
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
@RunWith (Parameterized.class)
public abstract class BaseUserMessageSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return Arrays.asList (new Object [] [] { { ESOAPVersion.SOAP_11 }, { ESOAPVersion.SOAP_12 } });
  }

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (BaseUserMessageSetUp.class);

  protected final ESOAPVersion m_eSOAPVersion;
  protected int m_nStatusCode;
  protected String m_sResponse;
  protected CloseableHttpClient aClient;
  protected HttpPost aPost;

  public BaseUserMessageSetUp (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Before
  public void setUp () throws KeyManagementException, NoSuchAlgorithmException
  {
    LOG.info ("The following test case will only work if there is a local holodeck server running, address http://127.0.0.1:8080/services/msh/!");
    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, RandomHelper.getSecureRandom ());

    aClient = new HttpClientFactory (aSSLContext).createHttpClient ();
    final HttpClientContext aContext = new HttpClientContext ();
    aContext.setRequestConfig (RequestConfig.custom ().setProxy (new HttpHost ("172.30.9.12", 8080)).build ());

    aPost = new HttpPost ("http://127.0.0.1:8080/services/msh/");
  }

  /**
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bSuccess
   *        specifies if the test case expects a positive or negativ response
   *        from the server
   * @param sErrorCode
   *        if you expect a negative response, you must give the expected
   *        errorcode as it will get searched for in the response.
   * @throws IOException
   */
  protected void _sendMessage (@Nonnull final HttpEntity aHttpEntity,
                               @Nonnull final boolean bSuccess,
                               @Nullable final String sErrorCode) throws IOException
  {
    aPost.setEntity (aHttpEntity);

    final CloseableHttpResponse aHttpResponse = aClient.execute (aPost);

    m_nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
    m_sResponse = EntityUtils.toString (aHttpResponse.getEntity ());

    if (bSuccess)
    {
      assertTrue ("Server responded with an error or error code." +
                  "StatusCode: " +
                  m_nStatusCode +
                  "\nResponse: " +
                  m_sResponse,
                  m_nStatusCode == 200 && !m_sResponse.contains ("Error"));
    }
    else
      assertTrue ("Server responded with success message but failure was expected." +
                  "StatusCode: " +
                  m_nStatusCode +
                  "\nResponse: " +
                  m_sResponse,
                  m_sResponse.contains (sErrorCode));
  }
}
