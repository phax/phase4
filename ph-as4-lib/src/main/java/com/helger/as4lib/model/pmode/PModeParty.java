/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public class PModeParty
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

  @Nonnull
  @Nonempty
  public String getIDValue ()
  {
    return m_sIDValue;
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
}
