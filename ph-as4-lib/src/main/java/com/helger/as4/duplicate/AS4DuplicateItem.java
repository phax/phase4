/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.duplicate;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents a single "duplication check" item. It works for
 * incoming and outgoing duplication checks
 *
 * @author Philip Helger
 */
public class AS4DuplicateItem implements IHasID <String>, Serializable
{
  private final LocalDateTime m_aDT;
  private final String m_sMessageID;

  public AS4DuplicateItem (@Nonnull @Nonempty final String sMessageID)
  {
    this (PDTFactory.getCurrentLocalDateTime (), sMessageID);
  }

  AS4DuplicateItem (@Nonnull final LocalDateTime aDT, @Nonnull @Nonempty final String sMessageID)
  {
    m_aDT = ValueEnforcer.notNull (aDT, "DT");
    m_sMessageID = ValueEnforcer.notEmpty (sMessageID, "MessageID");
  }

  /**
   * @return The date time when the entry was created. Never <code>null</code>.
   */
  @Nonnull
  public LocalDateTime getDateTime ()
  {
    return m_aDT;
  }

  /**
   * @return The message ID. Neither <code>null</code> nor empty.
   * @see #getMessageID()
   */
  @Nonnull
  @Nonempty
  public String getID ()
  {
    return getMessageID ();
  }

  /**
   * @return The message ID. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getMessageID ()
  {
    return m_sMessageID;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final AS4DuplicateItem rhs = (AS4DuplicateItem) o;
    return m_sMessageID.equals (rhs.m_sMessageID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sMessageID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("DT", m_aDT).append ("MessageID", m_sMessageID).getToString ();
  }
}
