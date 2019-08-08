/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.apache.http.HttpEntity;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.http.HttpMimeMessageEntity;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.messaging.domain.MessageHelperMethods;

/**
 * The client HTTP message and some metadata
 *
 * @author Philip Helger
 */
public final class AS4ClientBuiltMessage
{
  private final String m_sMessageID;
  private final HttpEntity m_aHttpEntity;
  private final HttpHeaderMap m_aCustomHeaders;

  public AS4ClientBuiltMessage (@Nonnull @Nonempty final String sMessageID, @Nonnull final HttpXMLEntity aHttpEntity)
  {
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
    m_aHttpEntity = ValueEnforcer.notNull (aHttpEntity, "HttpEntity");
    m_aCustomHeaders = null;
  }

  public AS4ClientBuiltMessage (@Nonnull @Nonempty final String sMessageID,
                                @Nonnull final HttpMimeMessageEntity aHttpEntity) throws MessagingException
  {
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
    m_aHttpEntity = ValueEnforcer.notNull (aHttpEntity, "HttpEntity");
    m_aCustomHeaders = MessageHelperMethods.getAndRemoveAllHeaders (aHttpEntity.getMimeMessage ());
  }

  @Nonnull
  @Nonempty
  public String getMessageID ()
  {
    return m_sMessageID;
  }

  /**
   * @return The {@link HttpEntity} provided in the constructor. Always the same
   *         object.
   */
  @Nonnull
  @ReturnsMutableCopy
  public HttpEntity getHttpEntity ()
  {
    return m_aHttpEntity;
  }

  @Nullable
  @ReturnsMutableCopy
  public HttpHeaderMap getCustomHeaders ()
  {
    return m_aCustomHeaders == null ? null : m_aCustomHeaders.getClone ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MessageID", m_sMessageID)
                                       .append ("HttpEntity", m_aHttpEntity)
                                       .appendIfNotNull ("CustomHeaders", m_aCustomHeaders)
                                       .getToString ();
  }
}
