package com.helger.as4server.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.entity.AbstractHttpEntity;

import com.helger.commons.ValueEnforcer;

public class HttpMimeMessageEntity extends AbstractHttpEntity
{
  private final MimeMessage m_aMsg;

  public HttpMimeMessageEntity (@Nonnull final MimeMessage aMsg)
  {
    m_aMsg = ValueEnforcer.notNull (aMsg, "Msg");
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
    return false;
  }

  public InputStream getContent () throws IOException
  {
    // Should be implemented as well but is irrelevant for this case
    throw new UnsupportedOperationException ();
  }

  public void writeTo (final OutputStream outstream) throws IOException
  {
    try
    {
      m_aMsg.writeTo (outstream);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Error writing MIME message", ex);
    }
  }
}
