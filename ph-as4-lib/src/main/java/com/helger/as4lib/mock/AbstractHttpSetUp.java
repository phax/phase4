package com.helger.as4lib.mock;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;

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
public abstract class AbstractHttpSetUp
{
  protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (AbstractHttpSetUp.class);
  protected static final ConfigFile PROPS = new ConfigFileBuilder ().addPath ("as4.properties").build ();

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

    m_aClient = new HttpClientFactory (aSSLContext).createHttpClient ();

    m_aPost = new HttpPost (sURL);

    if (PROPS.getAsBoolean ("server.proxy.enabled", false))
    {
      m_aPost.setConfig (RequestConfig.custom ()
                                      .setProxy (new HttpHost (PROPS.getAsString ("server.proxy.address"),
                                                               PROPS.getAsInt ("server.proxy.port")))
                                      .build ());
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
