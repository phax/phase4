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
package com.helger.phase4.model.soapfault;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Defines the retry disposition of a received SOAP Fault.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public enum EAS4FaultDisposition implements IHasID <String>
{
  /**
   * The fault indicates a definitive rejection by the receiving side. Retrying the same message
   * will not succeed.
   */
  PERMANENT ("permanent"),
  /**
   * The fault indicates a temporary problem on the receiving side. Retrying the same message may
   * succeed.
   */
  TRANSIENT ("transient");

  private final String m_sID;

  EAS4FaultDisposition (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isPermanent ()
  {
    return this == PERMANENT;
  }

  public boolean isTransient ()
  {
    return this == TRANSIENT;
  }

  @Nullable
  public static EAS4FaultDisposition getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4FaultDisposition.class, sID);
  }
}
