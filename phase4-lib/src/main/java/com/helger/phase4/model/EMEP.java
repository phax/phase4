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
package com.helger.phase4.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.string.StringHelper;

/**
 * Defines the available Message Exchange Patterns (MEPs).
 *
 * @author Philip Helger
 */
public enum EMEP implements IHasID <String>
{
  /**
   * The One-Way MEP which governs the exchange of a single User Message Unit unrelated to other
   * User Messages.
   */
  ONE_WAY ("oneway", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay"),
  /**
   * The Two-Way MEP which governs the exchange of two User Message Units in opposite directions,
   * the first one to occur is labeled "request", the other one "reply". In an actual instance, the
   * "reply" must reference the "request" using eb:RefToMessageId. Or referenced to as The
   * Two-Way/Sync MEP.
   */
  TWO_WAY ("twoway", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay");

  public static final EMEP DEFAULT_EBMS = ONE_WAY;

  private final String m_sID;
  private final String m_sURI;

  EMEP (@NonNull @Nonempty final String sID, @NonNull @Nonempty final String sURI)
  {
    m_sID = sID;
    m_sURI = sURI;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnegative
  public int getMessageCount ()
  {
    if (isOneWay ())
      return 1;
    if (isTwoWay ())
      return 2;
    throw new IllegalStateException ();
  }

  public boolean isOneWay ()
  {
    return this == ONE_WAY;
  }

  public boolean isTwoWay ()
  {
    return this == TWO_WAY;
  }

  @NonNull
  @Nonempty
  public String getURI ()
  {
    return m_sURI;
  }

  @Nullable
  public static EMEP getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EMEP.class, sID);
  }

  @Nullable
  public static EMEP getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.isEmpty (sURI))
      return null;
    return EnumHelper.findFirst (EMEP.class, x -> sURI.equals (x.getURI ()));
  }
}
