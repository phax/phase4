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
package com.helger.as4.model.pmode;

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

  /** Required role */
  private final String m_sRole;

  /** Authorization user name */
  private final String m_sUserName;

  /** Authorization password */
  private final String m_sPassword;

  // Status vars

  /** ID type and value combined */
  private transient String m_sStatusID;

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
  }

  @Nullable
  public String getIDType ()
  {
    return m_sIDType;
  }

  public boolean hasIDType ()
  {
    return StringHelper.hasText (m_sIDType);
  }

  @Nonnull
  @Nonempty
  public String getIDValue ()
  {
    return m_sIDValue;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    String ret = m_sStatusID;
    if (ret == null)
    {
      if (StringHelper.hasText (m_sIDType))
        ret = m_sIDType + ":" + m_sIDValue;
      else
        ret = m_sIDValue;
      m_sStatusID = ret;
    }
    return ret;
  }

  @Nonnull
  @Nonempty
  public String getRole ()
  {
    return m_sRole;
  }

  @Nullable
  public String getUserName ()
  {
    return m_sUserName;
  }

  public boolean hasUserName ()
  {
    return StringHelper.hasText (m_sUserName);
  }

  @Nullable
  public String getPassword ()
  {
    return m_sPassword;
  }

  public boolean hasPassword ()
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
