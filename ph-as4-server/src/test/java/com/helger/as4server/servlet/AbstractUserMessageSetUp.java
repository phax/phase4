package com.helger.as4server.servlet;

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

import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.message.MessageHelperMethods;
import com.helger.as4server.AbstractClientSetUp;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractUserMessageSetUp extends AbstractClientSetUp
{
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
   * @param sStatusCode
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @throws IOException
   */
  protected void sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                   @Nonnull final boolean bSuccess,
                                   @Nullable final String sStatusCode) throws IOException
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
        assertEquals ("Server respondedn with success message but failure was expected. Response: " +
                      m_sResponse,
                      new StringBuilder ().append (m_nStatusCode).toString (),
                      sStatusCode);

      }
    }
    catch (final HttpHostConnectException ex)
    {
      // No such server running
      fail ("No target AS4 server reachable: " + ex.getMessage () + " \n Check your properties!");
    }
  }
}
