/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.message;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

/**
 * Defines the meta message types.
 *
 * @author Philip Helger
 */
public enum EAS4MessageType implements IHasID <String>, IHasDisplayName
{
  ERROR_MESSAGE ("errormsg", "Error Message"),
  PULL_REQUEST ("pullreq", "Pull Request"),
  RECEIPT ("receipt", "Receipt"),
  USER_MESSAGE ("usermsg", "User Message");

  private final String m_sID;
  private final String m_sDisplayName;

  EAS4MessageType (@NonNull @Nonempty final String sID, @NonNull @Nonempty final String sDisplayName)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  public boolean isUserMessage ()
  {
    return this == USER_MESSAGE;
  }

  public boolean isSignalMessage ()
  {
    return this != USER_MESSAGE;
  }

  public boolean isReceiptOrError ()
  {
    return this == RECEIPT || this == ERROR_MESSAGE;
  }

  @Nullable
  public static EAS4MessageType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4MessageType.class, sID);
  }
}
