/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Party within a PMode
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeParty implements IHasID <String>, Serializable
{
  /** Optional ID type */
  private String m_sIDType;

  /** Required ID value */
  private String m_sIDValue;

  /** Required role */
  private String m_sRole;

  /** Authorization user name */
  private String m_sUserName;

  /** Authorization password */
  private String m_sPassword;

  public PModeParty (@Nullable final String sIDType,
                     @NonNull @Nonempty final String sIDValue,
                     @NonNull @Nonempty final String sRole,
                     @Nullable final String sUserName,
                     @Nullable final String sPassword)
  {
    setIDType (sIDType);
    setIDValue (sIDValue);
    setRole (sRole);
    setUserName (sUserName);
    setPassword (sPassword);
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
   * @return <code>true</code> if an ID type is present, <code>false</code> if not.
   */
  public final boolean hasIDType ()
  {
    return StringHelper.isNotEmpty (m_sIDType);
  }

  /**
   * Set the ID type to use.
   *
   * @param sIDType
   *        ID type to use. May be <code>null</code>.
   * @return {@link EChange}.
   * @since 0.12.0
   */
  @NonNull
  public final EChange setIDType (@Nullable final String sIDType)
  {
    if (EqualsHelper.equals (sIDType, m_sIDType))
      return EChange.UNCHANGED;
    m_sIDType = sIDType;
    return EChange.CHANGED;
  }

  /**
   * @return The ID value. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getIDValue ()
  {
    return m_sIDValue;
  }

  /**
   * Set the ID value to use.
   *
   * @param sIDValue
   *        ID value to use. May neither be <code>null</code> nor empty.
   * @return {@link EChange}.
   * @since 0.12.0
   */
  @NonNull
  public final EChange setIDValue (@NonNull @Nonempty final String sIDValue)
  {
    ValueEnforcer.notEmpty (sIDValue, "IDValue");
    if (sIDValue.equals (m_sIDValue))
      return EChange.UNCHANGED;
    m_sIDValue = sIDValue;
    return EChange.CHANGED;
  }

  /**
   * Either <code>id-type:id-value</code> or just <code>id-value</code> if not id-type is present.
   */
  @NonNull
  @Nonempty
  public final String getID ()
  {
    if (StringHelper.isNotEmpty (m_sIDType))
      return m_sIDType + ':' + m_sIDValue;
    return m_sIDValue;
  }

  /**
   * @return The party role. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getRole ()
  {
    return m_sRole;
  }

  /**
   * Set the role to use.
   *
   * @param sRole
   *        Role to use. May neither be <code>null</code> nor empty.
   * @return {@link EChange}.
   * @since 0.12.0
   */
  @NonNull
  public final EChange setRole (@NonNull @Nonempty final String sRole)
  {
    ValueEnforcer.notEmpty (sRole, "Role");
    if (sRole.equals (m_sRole))
      return EChange.UNCHANGED;
    m_sRole = sRole;
    return EChange.CHANGED;
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
   * @return <code>true</code> if a user name is present, <code>false</code> if not.
   */
  public final boolean hasUserName ()
  {
    return StringHelper.isNotEmpty (m_sUserName);
  }

  /**
   * Set the user name to use.
   *
   * @param sUserName
   *        User name to use. May be <code>null</code>.
   * @return {@link EChange}.
   * @since 0.12.0
   */
  @NonNull
  public final EChange setUserName (@Nullable final String sUserName)
  {
    if (EqualsHelper.equals (sUserName, m_sUserName))
      return EChange.UNCHANGED;
    m_sUserName = sUserName;
    return EChange.CHANGED;
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
   * @return <code>true</code> if a password is present, <code>false</code> if not.
   */
  public final boolean hasPassword ()
  {
    return StringHelper.isNotEmpty (m_sPassword);
  }

  /**
   * Set the password to use.
   *
   * @param sPassword
   *        Password to use. May be <code>null</code>.
   * @return {@link EChange}.
   * @since 0.12.0
   */
  @NonNull
  public final EChange setPassword (@Nullable final String sPassword)
  {
    if (EqualsHelper.equals (sPassword, m_sPassword))
      return EChange.UNCHANGED;
    m_sPassword = sPassword;
    return EChange.CHANGED;
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

  @NonNull
  public static PModeParty createSimple (@NonNull @Nonempty final String sIDValue,
                                         @NonNull @Nonempty final String sRole)
  {
    return new PModeParty ((String) null, sIDValue, sRole, (String) null, (String) null);
  }
}
