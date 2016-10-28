/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.GenericReflection;
import com.helger.commons.type.ObjectType;

public class AS4Profile implements IAS4Profile
{
  public static final ObjectType OT = new ObjectType ("as4.profile");

  private final String m_sID;
  private final String m_sDisplayName;
  private final Class <? extends IAS4ProfileValidator> m_aValidatorClass;

  public AS4Profile (@Nonnull @Nonempty final String sID,
                     @Nonnull @Nonempty final String sDisplayName,
                     @Nullable final Class <? extends IAS4ProfileValidator> aValidatorClass)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_sDisplayName = ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    m_aValidatorClass = aValidatorClass;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  @Nullable
  public String getValidatorClassName ()
  {
    return m_aValidatorClass == null ? null : m_aValidatorClass.getName ();
  }

  @Nullable
  public IAS4ProfileValidator getValidator ()
  {
    return m_aValidatorClass == null ? null : GenericReflection.newInstance (m_aValidatorClass);
  }
}
