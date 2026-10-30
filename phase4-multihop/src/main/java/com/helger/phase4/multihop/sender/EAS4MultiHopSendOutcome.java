/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.sender;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.state.ISuccessIndicator;

/**
 * The outcome of sending an AS4 User Message through an I-Cloud. In a multi-hop setup the absence
 * of a synchronous signal message is not an error - the edge intermediary may simply have accepted
 * the message for later delivery (D10).
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public enum EAS4MultiHopSendOutcome implements IHasID <String>, ISuccessIndicator
{
  /** A synchronous signal message was received and it was a Receipt */
  SIGNAL_RECEIVED_SYNC ("syncsignal", true),
  /** The edge intermediary accepted the message; the Receipt will arrive asynchronously */
  ACCEPTED_BY_ICLOUD_ASYNC ("asyncaccepted", true),
  /** Sending failed */
  FAILED ("failed", false);

  private final String m_sID;
  private final boolean m_bSuccess;

  EAS4MultiHopSendOutcome (@NonNull @Nonempty final String sID, final boolean bSuccess)
  {
    m_sID = sID;
    m_bSuccess = bSuccess;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isSuccess ()
  {
    return m_bSuccess;
  }

  @Nullable
  public static EAS4MultiHopSendOutcome getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4MultiHopSendOutcome.class, sID);
  }
}
