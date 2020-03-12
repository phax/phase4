/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet.mgr;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import com.helger.commons.concurrent.SimpleReadWriteLock;

/**
 * Static helper class to make the AS4 profile selection more deterministic and
 * flexible.
 *
 * @author Philip Helger
 * @since 0.9.13
 */
public final class AS4ProfileSelector
{
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static String s_sAS4ProfileID;

  private AS4ProfileSelector ()
  {}

  @Nullable
  public static String getCustomAS4ProfileID ()
  {
    return s_aRWLock.readLocked ( () -> s_sAS4ProfileID);
  }

  public static void setCustomAS4ProfileID (@Nullable final String sAS4ProfileID)
  {
    s_aRWLock.writeLocked ( () -> s_sAS4ProfileID = sAS4ProfileID);
  }

  /**
   * Get the AS4 profile ID to be used in the following order:
   * <ol>
   * <li>From {@link #getCustomAS4ProfileID()}</li>
   * <li>from the configuration file</li>
   * </ol>
   *
   * @return The AS4 profile ID to be used. May be <code>null</code>.
   */
  @Nullable
  public static String getAS4ProfileID ()
  {
    String ret = getCustomAS4ProfileID ();
    if (ret == null)
    {
      // Fall back to the configuration file
      // The profile ID from the configuration file is optional
      ret = AS4ServerConfiguration.getAS4ProfileID ();
    }
    return ret;
  }
}
