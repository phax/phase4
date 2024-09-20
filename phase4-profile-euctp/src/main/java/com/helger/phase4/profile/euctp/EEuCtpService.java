/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.euctp;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * This enum contains all the EU CTP AS4 services.
 *
 * @author Jon Rios
 * @author Philip Helger
 */
public enum EEuCtpService
{
  TRADER_TO_CUSTOMS ("eu_ics2_t2c"),
  CUSTOMS_TO_TRADER ("eu_ics2_c2t");

  private final String m_sValue;

  EEuCtpService (@Nonnull @Nonempty final String sValue)
  {
    m_sValue = sValue;
  }

  @Nonnull
  @Nonempty
  public String getValue ()
  {
    return m_sValue;
  }
}
