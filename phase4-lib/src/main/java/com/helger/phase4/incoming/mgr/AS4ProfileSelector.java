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
package com.helger.phase4.incoming.mgr;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.config.AS4Configuration;

/**
 * Static helper class for the fallback AS4 profile selection.
 *
 * @author Philip Helger
 * @since 0.9.13
 */
@ThreadSafe
public final class AS4ProfileSelector
{
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static String s_sAS4ProfileID;

  private AS4ProfileSelector ()
  {}

  /**
   * @return The custom default AS4 profile ID. Defaults to <code>null</code>.
   */
  @Nullable
  public static String getCustomDefaultAS4ProfileID ()
  {
    return RW_LOCK.readLockedGet ( () -> s_sAS4ProfileID);
  }

  /**
   * Set the custom default AS4 profile ID. This has precedence over the
   * configured AS4 default profile ID, to allow for a runtime change.
   *
   * @param sAS4ProfileID
   *        The AS4 profile ID to set. May be <code>null</code>.
   */
  public static void setCustomDefaultAS4ProfileID (@Nullable final String sAS4ProfileID)
  {
    RW_LOCK.writeLocked ( () -> s_sAS4ProfileID = sAS4ProfileID);
  }

  /**
   * Get the default AS4 profile ID to be used in the following order:
   * <ol>
   * <li>From {@link #getCustomDefaultAS4ProfileID()}</li>
   * <li>from the configuration properties.</li>
   * </ol>
   *
   * @return The AS4 profile ID to be used. May be <code>null</code>.
   */
  @Nullable
  public static String getDefaultAS4ProfileID ()
  {
    // Is a custom default provided?
    String ret = getCustomDefaultAS4ProfileID ();
    if (StringHelper.hasNoText (ret))
    {
      // Fall back to the configuration file
      // The profile ID from the configuration file is optional
      // This should be the only place, where this method is called for
      // evaluation - all other occurrences should use this method instead.
      ret = AS4Configuration.getDefaultAS4ProfileID ();
    }
    return ret;
  }
}
