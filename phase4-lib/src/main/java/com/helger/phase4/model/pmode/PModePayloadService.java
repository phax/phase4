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
package com.helger.phase4.model.pmode;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.attachment.EAS4CompressionMode;

@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModePayloadService implements Serializable
{
  private EAS4CompressionMode m_eCompressionMode;

  public PModePayloadService (@Nullable final EAS4CompressionMode eCompressionMode)
  {
    setCompressionMode (eCompressionMode);
  }

  /**
   * @return The compression mode to use. May be <code>null</code>.
   */
  @Nullable
  public final EAS4CompressionMode getCompressionMode ()
  {
    return m_eCompressionMode;
  }

  /**
   * @return <code>true</code> if a compression mode is set, <code>false</code>
   *         if not.
   */
  public final boolean hasCompressionMode ()
  {
    return m_eCompressionMode != null;
  }

  /**
   * @return The ID of the used compression mode or <code>null</code> if no
   *         compression mode is set.
   */
  @Nullable
  public final String getCompressionModeID ()
  {
    return m_eCompressionMode == null ? null : m_eCompressionMode.getID ();
  }

  /**
   * Set the compression mode to use.
   *
   * @param eCompressionMode
   *        Compression mode to use. May be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setCompressionMode (@Nullable final EAS4CompressionMode eCompressionMode)
  {
    if (EqualsHelper.equals (eCompressionMode, m_eCompressionMode))
      return EChange.UNCHANGED;
    m_eCompressionMode = eCompressionMode;
    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModePayloadService rhs = (PModePayloadService) o;
    return EqualsHelper.equals (m_eCompressionMode, rhs.m_eCompressionMode);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eCompressionMode).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("CompressionMode", m_eCompressionMode).getToString ();
  }
}
