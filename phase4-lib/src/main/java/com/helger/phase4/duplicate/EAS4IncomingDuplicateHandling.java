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
package com.helger.phase4.duplicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Defines how an incoming duplicate message (a message with an AS4 message ID that was already
 * received) is handled.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public enum EAS4IncomingDuplicateHandling implements IHasID <String>
{
  /**
   * Answer the duplicate with the ebMS Error <code>EBMS:4001</code>. The SPIs are not invoked.
   */
  RETURN_ERROR ("error"),
  /**
   * Silently dismiss the duplicate: the SPIs are not invoked, but the usual Receipt is returned.
   */
  DISMISS ("dismiss"),
  /**
   * Process the duplicate again like a new message: the SPIs are invoked and the usual Receipt is
   * returned. The SPIs can check for duplicates via
   * {@link com.helger.phase4.incoming.IAS4IncomingMessageState#isDuplicate()}.
   */
  PROCESS_AGAIN ("process");

  /** The default handling */
  public static final EAS4IncomingDuplicateHandling DEFAULT = RETURN_ERROR;

  private final String m_sID;

  EAS4IncomingDuplicateHandling (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static EAS4IncomingDuplicateHandling getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4IncomingDuplicateHandling.class, sID);
  }

  @Nullable
  public static EAS4IncomingDuplicateHandling getFromIDOrDefault (@Nullable final String sID,
                                                                  @Nullable final EAS4IncomingDuplicateHandling eDefault)
  {
    return EnumHelper.getFromIDOrDefault (EAS4IncomingDuplicateHandling.class, sID, eDefault);
  }
}
