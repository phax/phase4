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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

/**
 * Predefines transport channel bindings.
 *
 * @author Philip Helger
 */
public enum EMEPBinding implements IHasID <String>
{
  /**
   * maps an MEP User message to the 1st leg of an underlying 2-way transport
   * protocol, or of a 1-way protocol.
   */
  PUSH ("push", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push", 1),
  /**
   * maps an MEP User message to the second leg of an underlying two-way
   * transport protocol, as a result of an ebMS Pull Signal sent over the first
   * leg.
   */
  PULL ("pull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull", 1),
  /**
   * maps an exchange of two User messages respectively to the first and second
   * legs of a two-way underlying transport protocol.
   */
  SYNC ("sync", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sync", 2),
  /**
   * The Two-Way/Push-and-Push MEP composes the choreographies of two
   * One-Way/Push MEPs in opposite directions, the User Message unit of the
   * second referring to the User Message unit of the first via
   * eb:RefToMessageId.
   */
  PUSH_PUSH ("pushpush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush", 2),
  /**
   * The Two-Way/Push-and-Pull MEP composes the choreography of a One-Way/Push
   * MEP followed by the choreography of a One-Way/Pull MEP, both initiated from
   * the same MSH (Initiator). The User Message unit in the "pulled" message
   * must refer to the previously "pushed" User Message unit.
   */
  PUSH_PULL ("pushpull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull", 2),
  /**
   * The Two-Way/Pull-and-Push MEP composes the choreography of a One-Way/Pull
   * MEP followed by the choreography of a One-Way/Push MEP, with both MEPs
   * initiated from the same MSH. The User Message unit in the "pushed" message
   * must refer to the previously "pulled" User Message unit.
   */
  PULL_PUSH ("pullpush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush", 2);

  public static final EMEPBinding DEFAULT_EBMS = PUSH;

  private final String m_sID;
  private final String m_sURI;
  private final int m_nRequiredLegs;

  EMEPBinding (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sURI, final int nRequiredLegs)
  {
    m_sID = sID;
    m_sURI = sURI;
    m_nRequiredLegs = nRequiredLegs;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getURI ()
  {
    return m_sURI;
  }

  @Nonnegative
  public int getRequiredLegs ()
  {
    return m_nRequiredLegs;
  }

  /**
   * @return <code>true</code> if the processing is synchronous (PUSH, PULL or
   *         SYNC), <code>false</code> otherwise.
   * @see #isAsynchronous()
   */
  public boolean isSynchronous ()
  {
    return this == PUSH || this == PULL || this == SYNC;
  }

  /**
   * @return <code>true</code> if the processing is asynchronous (PUSH_PUSH,
   *         PUSH_PULL or PULL_PUSH), <code>false</code> otherwise.
   * @see #isSynchronous()
   */
  public boolean isAsynchronous ()
  {
    return this == PUSH_PUSH || this == PUSH_PULL || this == PULL_PUSH;
  }

  /**
   * @return <code>true</code> if initiator side has the asynchronous part of
   *         the transfer (this == PUSH_PULL || this == PULL_PUSH)
   */
  public boolean isAsynchronousInitiator ()
  {
    return this == PUSH_PULL || this == PULL_PUSH;
  }

  public boolean canSendUserMessageBack ()
  {
    return this == PULL || this == SYNC || this == PULL_PUSH || this == PUSH_PULL;
  }

  @Nullable
  public static EMEPBinding getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EMEPBinding.class, sID);
  }

  @Nullable
  public static EMEPBinding getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasNoText (sURI))
      return null;
    return EnumHelper.findFirst (EMEPBinding.class, x -> sURI.equals (x.getURI ()));
  }
}
