/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.entity.AbstractHttpEntity;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;

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
    // Is it always repeatable? Depends on the underlying DataHandler
    return true;
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

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("MimeMsg", m_aMsg).getToString ();
  }
}
