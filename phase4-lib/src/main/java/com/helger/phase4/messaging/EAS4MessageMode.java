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
package com.helger.phase4.messaging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Message mode.<br>
 * Was originally called <code>EAS4IncomingMessageMode</code> until v1.2.0.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public enum EAS4MessageMode implements IHasID <String>
{
  /** A request is a message that initiated an interaction */
  REQUEST ("request"),
  /** A response can only exist in relation to a previous request */
  RESPONSE ("response");

  private final String m_sID;

  EAS4MessageMode (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isRequest ()
  {
    return this == REQUEST;
  }

  public boolean isResponse ()
  {
    return this == RESPONSE;
  }

  @Nullable
  public static EAS4MessageMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4MessageMode.class, sID);
  }
}
