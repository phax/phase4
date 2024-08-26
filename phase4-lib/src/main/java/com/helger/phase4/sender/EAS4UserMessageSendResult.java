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
package com.helger.phase4.sender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.state.ISuccessIndicator;

/**
 * Specific enumeration with the result error codes of the
 * {@link AbstractAS4UserMessageBuilder#sendMessageAndCheckForReceipt()}
 * method.<br />
 * Old name before v3: <code>ESimpleUserMessageSendResult</code>
 *
 * @author Philip Helger
 */
public enum EAS4UserMessageSendResult implements IHasID <String>, ISuccessIndicator
{
  /**
   * Programming error, because not all mandatory fields are filled.
   */
  INVALID_PARAMETERS ("invalid-parameters"),
  /**
   * Something failed on the network or HTTP(S) level
   */
  TRANSPORT_ERROR ("transport-error"),
  /**
   * Some answer was received, but it was no valid AS4 Signal Message
   */
  NO_SIGNAL_MESSAGE_RECEIVED ("no-signal-msg-received"),
  /**
   * An AS4 Error Message was received
   */
  AS4_ERROR_MESSAGE_RECEIVED ("as4-error-msg-received"),
  /**
   * An AS4 Signal Message was received, but it was neither a Receipt nor an
   * Error Message but something else.
   */
  INVALID_SIGNAL_MESSAGE_RECEIVED ("invalid-signal-message-received"),
  /**
   * Everything worked according to plan. The message was successfully
   * delivered.
   */
  SUCCESS ("success");

  private final String m_sID;

  EAS4UserMessageSendResult (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  /**
   * @return The ID of the of the error message.
   * @since 1.0.0-rc1
   */
  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isSuccess ()
  {
    return this == SUCCESS;
  }

  /**
   * @return A recommendation whether a retry might be feasible in case the
   *         internal retries were disabled.
   * @since 1.0.0-rc1
   */
  public boolean isRetryFeasible ()
  {
    return this == TRANSPORT_ERROR || this == NO_SIGNAL_MESSAGE_RECEIVED || this == INVALID_SIGNAL_MESSAGE_RECEIVED;
  }

  @Nullable
  public static EAS4UserMessageSendResult getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4UserMessageSendResult.class, sID);
  }
}
