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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Defines the available Message Exchange Patterns (MEPs).
 *
 * @author Philip Helger
 */
public enum EMEP
{
  /**
   * The One-Way MEP which governs the exchange of a single User Message Unit
   * unrelated to other User Messages.
   */
  ONE_WAY (1, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay"),
  /**
   * The Two-Way MEP which governs the exchange of two User Message Units in
   * opposite directions, the first one to occur is labeled "request", the other
   * one "reply". In an actual instance, the "reply" must reference the
   * "request" using eb:RefToMessageId.
   */
  TWO_WAY (2, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay"),
  /**
   * The Two-Way/Push-and-Push MEP composes the choreographies of two
   * One-Way/Push MEPs in opposite directions, the User Message unit of the
   * second referring to the User Message unit of the first via
   * eb:RefToMessageId.
   */
  TWO_WAY_PUSH_PUSH (2, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush"),
  /**
   * The Two-Way/Push-and-Pull MEP composes the choreography of a One-Way/Push
   * MEP followed by the choreography of a One-Way/Pull MEP, both initiated from
   * the same MSH (Initiator). The User Message unit in the "pulled" message
   * must refer to the previously "pushed" User Message unit.
   */
  TWO_WAY_PUSH_PULL (2, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull"),
  /**
   * The Two-Way/Pull-and-Push MEP composes the choreography of a One-Way/Pull
   * MEP followed by the choreography of a One-Way/Push MEP, with both MEPs
   * initiated from the same MSH. The User Message unit in the "pushed" message
   * must refer to the previously "pulled" User Message unit.
   */
  TWO_WAY_PULL_PUSH (2, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush");

  private final int m_nMsgCount;
  private final String m_sURI;

  private EMEP (@Nonnegative final int nMsgCount, @Nonnull @Nonempty final String sURI)
  {
    m_nMsgCount = nMsgCount;
    m_sURI = sURI;
  }

  @Nonnegative
  public int getMessageCount ()
  {
    return m_nMsgCount;
  }

  @Nonnull
  @Nonempty
  public String getURI ()
  {
    return m_sURI;
  }

  @Nullable
  public static EMEP getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasText (sURI))
      for (final EMEP e : values ())
        if (sURI.equals (e.getURI ()))
          return e;
    return null;
  }
}
