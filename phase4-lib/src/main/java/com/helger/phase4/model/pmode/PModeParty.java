/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Party within a PMode
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementEqualsAndHashcode
public class PModeParty implements IHasID <String>, Serializable
{
  /** Optional ID type */
  private final String m_sIDType;

  /** Required ID value */
  private final String m_sIDValue;

  /** ID type and value combined */
  private String m_sID;

  /** Required role */
  private final String m_sRole;

  /** Authorization user name */
  private final String m_sUserName;

  /** Authorization password */
  private final String m_sPassword;

  public PModeParty (@Nullable final String sIDType,
                     @Nonnull @Nonempty final String sIDValue,
                     @Nonnull @Nonempty final String sRole,
                     @Nullable final String sUserName,
                     @Nullable final String sPassword)
  {
    m_sIDType = sIDType;
    m_sIDValue = ValueEnforcer.notEmpty (sIDValue, "IDValue");
    m_sRole = ValueEnforcer.notEmpty (sRole, "Role");
    m_sUserName = sUserName;
    m_sPassword = sPassword;

    // Combine once for performance
    if (StringHelper.hasText (m_sIDType))
      m_sID = m_sIDType + ':' + m_sIDValue;
    else
      m_sID = m_sIDValue;
  }

  /**
   * @return The ID type as passed in the constructor. May be <code>null</code>.
   */
  @Nullable
  public final String getIDType ()
  {
    return m_sIDType;
  }

  /**
   * @return <code>true</code> if an ID type is present, <code>false</code> if
   *         not.
   */
  public final boolean hasIDType ()
  {
    return StringHelper.hasText (m_sIDType);
  }

  /**
   * @return The ID value. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getIDValue ()
  {
    return m_sIDValue;
  }

  /**
   * Either <code>id-type:id-value</code> or just <code>id-value</code> if not
   * id-type is present.
   */
  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  /**
   * @return The party role. Never <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getRole ()
  {
    return m_sRole;
  }

  /**
   * @return The user name. May be <code>null</code>.
   */
  @Nullable
  public final String getUserName ()
  {
    return m_sUserName;
  }

  /**
   * @return <code>true</code> if a user name is present, <code>false</code> if
   *         not.
   */
  public final boolean hasUserName ()
  {
    return StringHelper.hasText (m_sUserName);
  }

  /**
   * @return The password in plain text. May be <code>null</code>.
   */
  @Nullable
  public final String getPassword ()
  {
    return m_sPassword;
  }

  /**
   * @return <code>true</code> if a password is present, <code>false</code> if
   *         not.
   */
  public final boolean hasPassword ()
  {
    return StringHelper.hasText (m_sPassword);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeParty rhs = (PModeParty) o;
    return EqualsHelper.equals (m_sIDType, rhs.m_sIDType) &&
           m_sIDValue.equals (rhs.m_sIDValue) &&
           m_sRole.equals (rhs.m_sRole) &&
           EqualsHelper.equals (m_sUserName, rhs.m_sUserName) &&
           EqualsHelper.equals (m_sPassword, rhs.m_sPassword);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sIDType)
                                       .append (m_sIDValue)
                                       .append (m_sRole)
                                       .append (m_sUserName)
                                       .append (m_sPassword)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("IDType", m_sIDType)
                                       .append ("IDValue", m_sIDValue)
                                       .append ("Role", m_sRole)
                                       .append ("UserName", m_sUserName)
                                       .appendPassword ("Password")
                                       .getToString ();
  }

  @Nonnull
  public static PModeParty createSimple (@Nonnull @Nonempty final String sIDValue,
                                         @Nonnull @Nonempty final String sRole)
  {
    return new PModeParty ((String) null, sIDValue, sRole, (String) null, (String) null);
  }
}
