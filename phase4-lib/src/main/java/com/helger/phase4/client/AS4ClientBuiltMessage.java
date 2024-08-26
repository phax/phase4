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
package com.helger.phase4.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.hc.core5.http.HttpEntity;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.xsds.xmldsig.ReferenceType;

import jakarta.mail.MessagingException;

/**
 * The client HTTP message and some metadata.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class AS4ClientBuiltMessage
{
  private final String m_sMessageID;
  private final HttpEntity m_aHttpEntity;
  private final HttpHeaderMap m_aCustomHttpHeaders;
  private final ICommonsList <ReferenceType> m_aDSReferences;

  public AS4ClientBuiltMessage (@Nonnull @Nonempty final String sMessageID,
                                @Nonnull final HttpXMLEntity aHttpEntity,
                                @Nullable final ICommonsList <ReferenceType> aCreatedDSReferences)
  {
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
    m_aHttpEntity = ValueEnforcer.notNull (aHttpEntity, "HttpEntity");
    m_aCustomHttpHeaders = null;
    m_aDSReferences = aCreatedDSReferences;
  }

  public AS4ClientBuiltMessage (@Nonnull @Nonempty final String sMessageID,
                                @Nonnull final HttpMimeMessageEntity aHttpEntity,
                                @Nullable final ICommonsList <ReferenceType> aCreatedDSReferences) throws MessagingException
  {
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
    m_aHttpEntity = ValueEnforcer.notNull (aHttpEntity, "HttpEntity");
    m_aCustomHttpHeaders = AS4MimeMessageHelper.getAndRemoveAllHeaders (aHttpEntity.getMimeMessage ());
    m_aDSReferences = aCreatedDSReferences;
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
  public HttpHeaderMap getAllCustomHttpHeaders ()
  {
    return m_aCustomHttpHeaders == null ? null : m_aCustomHttpHeaders.getClone ();
  }

  public boolean hasCustomHttpHeaders ()
  {
    return m_aCustomHttpHeaders != null && m_aCustomHttpHeaders.isNotEmpty ();
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <ReferenceType> getAllDSReferences ()
  {
    return m_aDSReferences == null ? null : m_aDSReferences.getClone ();
  }

  public boolean hasDSReferences ()
  {
    return m_aDSReferences != null && m_aDSReferences.isNotEmpty ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MessageID", m_sMessageID)
                                       .append ("HttpEntity", m_aHttpEntity)
                                       .appendIfNotNull ("CustomHttpHeaders", m_aCustomHttpHeaders)
                                       .appendIfNotNull ("DSReferences", m_aDSReferences)
                                       .getToString ();
  }
}
