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
package com.helger.as4lib.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Defines the labels to be used with the MEPs.
 *
 * @author Philip Helger
 */
public enum EMEPLabel
{
  /**
   * Use for the one way MEP.
   */
  ONE_WAY ("oneway"),
  /**
   * Request part of the two way MEP.
   */
  REQUEST ("request"),
  /**
   * Response part of the two way MEP.
   */
  REPLY ("reply");

  private final String m_sLabel;

  private EMEPLabel (@Nonnull @Nonempty final String sLabel)
  {
    m_sLabel = sLabel;
  }

  public boolean isSuitableForMEP (@Nonnull final EMEP eMEP)
  {
    switch (eMEP.getMessageCount ())
    {
      case 1:
        return this == ONE_WAY;
      case 2:
        return this != ONE_WAY;
    }

    throw new IllegalArgumentException ("Unsupported MEP: " + eMEP);
  }

  @Nonnull
  @Nonempty
  public String getLabel ()
  {
    return m_sLabel;
  }

  @Nullable
  public static EMEPLabel getFromLabelOrNull (@Nullable final String sLabel)
  {
    if (StringHelper.hasText (sLabel))
      for (final EMEPLabel eLabel : values ())
        if (sLabel.equals (eLabel.getLabel ()))
          return eLabel;
    return null;
  }
}
