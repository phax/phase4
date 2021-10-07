/*
 * Copyright (C) 2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.springboot.enumeration;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

public enum ESBDHHandlerServiceSelector
{
  CUSTOM_PEPPOL_INCOMING (Constants.CUSTOM_PEPPOL_INCOMING_VALUE);

  public static class Constants
  {
    public static final String CUSTOM_PEPPOL_INCOMING_VALUE = "CustomPeppolIncomingSBDHandlerServiceImpl";
  }

  private final String m_sLabel;

  ESBDHHandlerServiceSelector (@Nonnull @Nonempty final String sLabel)
  {
    m_sLabel = sLabel;
  }

  @Nonnull
  @Nonempty
  public String getLabel ()
  {
    return m_sLabel;
  }
}
