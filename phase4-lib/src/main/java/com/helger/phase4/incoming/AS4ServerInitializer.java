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
package com.helger.phase4.incoming;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.incoming.mgr.AS4DuplicateCleanupJob;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.quartz.TriggerKey;

/**
 * This class contains the init method for the server:
 * <ul>
 * <li>The {@link MetaAS4Manager} instance is ensured to be present</li>
 * <li>The duplicate cleanup job will also be started.</li>
 * </ul>
 *
 * @author bayerlma
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4ServerInitializer
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ServerInitializer.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static TriggerKey s_aTriggerKey;

  private AS4ServerInitializer ()
  {}

  /**
   * Call this method in your AS4 server to initialize everything that is
   * necessary to use initialize AS4 message receiving.
   */
  public static void initAS4Server ()
  {
    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();

    final long nDisposalMinutes = AS4Configuration.getIncomingDuplicateDisposalMinutes ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Scheduling AS4DuplicateCleanupJob to dispose incoming metadata that is older than " +
                    nDisposalMinutes +
                    " minutes");

    // Schedule jobs
    RW_LOCK.writeLocked ( () -> {
      // Consecutive calls return null
      final TriggerKey aTriggerKey = AS4DuplicateCleanupJob.scheduleMe (nDisposalMinutes);
      if (aTriggerKey != null)
      {
        if (s_aTriggerKey != null)
          throw new IllegalStateException ("Failed to schedule AS4DuplicateCleanupJob - seems like some cleanup is missing");
        s_aTriggerKey = aTriggerKey;
      }
    });
  }

  /**
   * Call this method to shutdown the AS4 server. This unschedules the jobs.
   *
   * @since 0.10.3
   */
  public static void shutdownAS4Server ()
  {
    RW_LOCK.writeLocked ( () -> {
      AS4DuplicateCleanupJob.unschedule (s_aTriggerKey);
      s_aTriggerKey = null;
    });
  }
}
