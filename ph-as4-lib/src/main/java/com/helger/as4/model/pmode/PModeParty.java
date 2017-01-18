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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.string.StringHelper;

@Immutable
public class PModeParty implements IHasID <String>
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
  private final String m_sID;

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
    m_sID = StringHelper.getNotNull (m_sIDType) + ":" + m_sIDValue;
  }

  @Nullable
  public String getIDType ()
  {
    return m_sIDType;
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
    return m_sID;
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

  @Nullable
  public String getPassword ()
  {
    return m_sPassword;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeParty rhs = (PModeParty) o;
    return m_sIDType.equals (rhs.m_sIDType) &&
           EqualsHelper.equals (m_sIDValue, rhs.m_sIDValue) &&
           EqualsHelper.equals (m_sPassword, rhs.m_sPassword) &&
           EqualsHelper.equals (m_sRole, rhs.m_sRole) &&
           EqualsHelper.equals (m_sUserName, rhs.m_sUserName);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sIDType)
                                       .append (m_sIDValue)
                                       .append (m_sPassword)
                                       .append (m_sRole)
                                       .append (m_sUserName)
                                       .getHashCode ();
  }
}
