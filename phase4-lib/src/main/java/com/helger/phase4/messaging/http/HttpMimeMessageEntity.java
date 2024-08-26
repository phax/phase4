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
package com.helger.phase4.messaging.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.messaging.mime.AS4MimeMessage;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Special HTTP entity that reads and writes to a {@link MimeMessage}.
 *
 * @author Philip Helger
 * @author bayerlma
 */
public class HttpMimeMessageEntity extends AbstractHttpEntity
{
  private final AS4MimeMessage m_aMsg;

  protected HttpMimeMessageEntity (@Nonnull @Nonempty final String sContentType, @Nonnull final AS4MimeMessage aMsg)
  {
    super (sContentType, null);
    m_aMsg = aMsg;
  }

  @Override
  public final void close () throws IOException
  {
    // nothing to do
  }

  /**
   * @return The mime message passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final MimeMessage getMimeMessage ()
  {
    return m_aMsg;
  }

  @Override
  public boolean isRepeatable ()
  {
    return m_aMsg.isRepeatable ();
  }

  public long getContentLength ()
  {
    // length unknown - negative number
    return -1;
  }

  public boolean isStreaming ()
  {
    // Self contained? Depends on the underlying DataHandler
    return false;
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

  @Override
  public void writeTo (@Nonnull final OutputStream aOS) throws IOException
  {
    ValueEnforcer.notNull (aOS, "OutputStream");
    try
    {
      m_aMsg.writeTo (aOS);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Error writing MIME message", ex);
    }
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("MimeMsg", m_aMsg).getToString ();
  }

  @Nonnull
  public static HttpMimeMessageEntity create (@Nonnull final AS4MimeMessage aMsg)
  {
    ValueEnforcer.notNull (aMsg, "Msg");
    try
    {
      return new HttpMimeMessageEntity (aMsg.getContentType (), aMsg);
    }
    catch (final MessagingException ex)
    {
      throw new IllegalArgumentException ("Failed to get the Content-Type from " + aMsg, ex);
    }
  }
}
