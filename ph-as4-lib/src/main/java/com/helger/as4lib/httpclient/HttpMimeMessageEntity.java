package com.helger.as4lib.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.entity.AbstractHttpEntity;

import com.helger.commons.ValueEnforcer;

/**
 * Special HTTP entity that reads and writes to a {@link MimeMessage}.
 *
 * @author Philip Helger
 * @author bayerlma
 */
public class HttpMimeMessageEntity extends AbstractHttpEntity
{
  private final MimeMessage m_aMsg;

  public HttpMimeMessageEntity (@Nonnull final MimeMessage aMsg)
  {
    m_aMsg = ValueEnforcer.notNull (aMsg, "Msg");
  }

  /**
   * @return The mime message passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public MimeMessage getMimeMessage ()
  {
    return m_aMsg;
  }

  public boolean isRepeatable ()
  {
    return true;
  }

  public long getContentLength ()
  {
    return -1;
  }

  public boolean isStreaming ()
  {
    return true;
  }

  public InputStream getContent () throws IOException
  {
    try
    {
      return m_aMsg.getInputStream ();
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Error reading MIME message", ex);
    }
  }

  public void writeTo (@Nonnull final OutputStream aOS) throws IOException
  {
    try
    {
      m_aMsg.writeTo (aOS);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Error writing MIME message", ex);
    }
  }
}
