/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class correlates the built source message with the HTTP response of the
 * passed type
 *
 * @author Philip Helger
 * @param <T>
 *        The response type
 */
public final class AS4ClientSentMessage <T>
{
  private final AS4ClientBuiltMessage m_aBuiltMsg;
  private final T m_aResponse;

  public AS4ClientSentMessage (@Nonnull final AS4ClientBuiltMessage aBuiltMsg, @Nullable final T aResponse)
  {
    m_aBuiltMsg = ValueEnforcer.notNull (aBuiltMsg, "BuiltMsg");
    m_aResponse = aResponse;
  }

  @Nonnull
  public AS4ClientBuiltMessage getBuiltMessage ()
  {
    return m_aBuiltMsg;
  }

  @Nonnull
  @Nonempty
  public String getMessageID ()
  {
    return m_aBuiltMsg.getMessageID ();
  }

  @Nullable
  public T getResponse ()
  {
    return m_aResponse;
  }

  public boolean hasResponse ()
  {
    return m_aResponse != null;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BuiltMsg", m_aBuiltMsg)
                                       .append ("Response", m_aResponse)
                                       .getToString ();
  }
}
