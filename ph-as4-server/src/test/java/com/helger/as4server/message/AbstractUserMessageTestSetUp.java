package com.helger.as4server.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.message.MessageHelperMethods;
import com.helger.as4server.AbstractClientSetUp;
import com.helger.commons.url.URLHelper;
import com.helger.photon.jetty.JettyStarter;
import com.helger.photon.jetty.JettyStopper;

public abstract class AbstractUserMessageTestSetUp extends AbstractClientSetUp
{
  private static final int PORT = URLHelper.getAsURL (PROPS.getAsString ("server.address")).getPort ();
  private static final int STOP_PORT = PORT + 1000;

  @BeforeClass
  public static void startServer () throws Exception
  {
    new Thread ( () -> {
      try
      {
        new JettyStarter (com.helger.as4server.standalone.RunInJettyAS4.class).setPort (PORT)
                                                                              .setStopPort (STOP_PORT)
                                                                              .run ();
      }
      catch (final Exception ex)
      {
        ex.printStackTrace ();
      }
    }).start ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    new JettyStopper ().setStopPort (STOP_PORT).run ();
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
        assertTrue ("Server responded with an error.\nResponse: " + m_sResponse, !m_sResponse.contains ("Error"));
        assertEquals ("Server responded with an error code.", 200, m_nStatusCode);
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
      fail ("No target AS4 server reachable: " + ex.getMessage () + " \n Check your properties!");
    }
  }
}
