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
package com.helger.phase4.model;

import javax.annotation.Nullable;

import com.helger.phase4.CAS4;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;

/**
 * Generic AS4 helper for specification related things.
 *
 * @author Philip Helger
 */
public final class AS4Helper
{
  private AS4Helper ()
  {}

  /**
   * Check if the combination of Action and Service resembles an EBMS Ping (aka
   * test) Message as described in chapter 5.2.2.9.
   *
   * @param sAction
   *        Action value. May be <code>null</code>.
   * @param sService
   *        Service value. May be <code>null</code>.
   * @return <code>true</code> if the action is {@value CAS4#DEFAULT_ACTION_URL}
   *         and service is {@value CAS4#DEFAULT_SERVICE_URL},
   *         <code>false</code> otherwise.
   */
  public static boolean isPingMessage (@Nullable final String sAction, @Nullable final String sService)
  {
    // See Chapter 5.2.2.9 of the ebMS 3 Core Spec
    return CAS4.DEFAULT_ACTION_URL.equals (sAction) && CAS4.DEFAULT_SERVICE_URL.equals (sService);
  }

  /**
   * Check if the combination of Action and Service resembles an EBMS Ping (aka
   * test) Message as described in chapter 5.2.2.9.
   *
   * @param aBusinessInfo
   *        PMode business information. May be <code>null</code>.
   * @return <code>true</code> if the action is {@value CAS4#DEFAULT_ACTION_URL}
   *         and service is {@value CAS4#DEFAULT_SERVICE_URL},
   *         <code>false</code> otherwise.
   */
  public static boolean isPingMessage (@Nullable final PModeLegBusinessInformation aBusinessInfo)
  {
    return aBusinessInfo != null && isPingMessage (aBusinessInfo.getAction (), aBusinessInfo.getService ());
  }

  /**
   * Check if the combination of Action and Service resembles an EBMS Ping (aka
   * test) Message as described in chapter 5.2.2.9.
   *
   * @param aPMode
   *        to check. May be <code>null</code>
   * @return <code>true</code> if the default values to ping are not used else
   *         <code>false</code>
   */
  public static boolean isPingMessage (@Nullable final IPMode aPMode)
  {
    // Leg 2 wouldn't make sense... Only leg 1 can be pinged
    return aPMode != null && isPingMessage (aPMode.getLeg1 ().getBusinessInfo ());
  }
}
