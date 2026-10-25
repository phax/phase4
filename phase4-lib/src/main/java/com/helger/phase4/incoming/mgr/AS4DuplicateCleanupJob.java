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
package com.helger.phase4.incoming.mgr;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;

/**
 * A special job, that regularly removes all incoming duplicate message IDs that are older than
 * the disposal duration. It runs on a single daemon thread.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4DuplicateCleanupJob
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4DuplicateCleanupJob.class);
  private static final AtomicBoolean WAS_SCHEDULED = new AtomicBoolean (false);
  private static final AtomicReference <ScheduledExecutorService> EXECUTOR = new AtomicReference <> ();

  private AS4DuplicateCleanupJob ()
  {}

  /**
   * Remove all incoming duplicate message IDs that are older than the provided duration.
   *
   * @param aDisposalDuration
   *        The duration after which a message ID is removed. May not be <code>null</code>.
   */
  static void evictOldItems (@NonNull final Duration aDisposalDuration)
  {
    final OffsetDateTime aOldDT = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ().minus (aDisposalDuration);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to evit all incoming messages arrived before " + aOldDT.toString ());

    final ICommonsList <String> aEvicted = MetaAS4Manager.getIncomingDuplicateMgr ().evictAllItemsBefore (aOldDT);
    if (aEvicted.isNotEmpty ())
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Evicted " + aEvicted.size () + " incoming duplicate message IDs before " + aOldDT.toString ());
  }

  /**
   * Schedule the cleanup to run immediately and then every minute, or every disposal duration if
   * that is shorter than a minute.
   *
   * @param aDisposalDuration
   *        The duration after which a message ID is removed. Only positive durations schedule the
   *        cleanup. May not be <code>null</code>.
   * @return The scheduled cleanup, to be passed to {@link #unschedule(ScheduledFuture)}, or
   *         <code>null</code> if the cleanup is disabled or was already scheduled.
   */
  @Nullable
  public static ScheduledFuture <?> scheduleMe (@NonNull final Duration aDisposalDuration)
  {
    ScheduledFuture <?> ret = null;
    // Only for positive durations
    if (aDisposalDuration.compareTo (Duration.ZERO) > 0)
    {
      if (!WAS_SCHEDULED.getAndSet (true))
      {
        // For some tests the disposal is less then 1 minute - in this case the job needs to run
        // more often
        final Duration aInterval = aDisposalDuration.compareTo (Duration.ofMinutes (1)) < 0 ? aDisposalDuration
                                                                                             : Duration.ofMinutes (1);
        final ScheduledExecutorService aExecutor = Executors.newSingleThreadScheduledExecutor (r -> {
          final Thread aThread = new Thread (r, "phase4-duplicate-cleanup");
          aThread.setDaemon (true);
          return aThread;
        });
        EXECUTOR.set (aExecutor);
        ret = aExecutor.scheduleWithFixedDelay ( () -> {
          try
          {
            evictOldItems (aDisposalDuration);
          }
          catch (final Exception ex)
          {
            // An exception would cancel all further executions
            LOGGER.error ("Failed to evict incoming AS4 duplicate message IDs", ex);
          }
        }, 0, aInterval.toMillis (), TimeUnit.MILLISECONDS);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob was successfully scheduled");
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob is already scheduled");
      }
    }
    else
    {
      LOGGER.warn ("Incoming AS4 duplicate message cleanup is disabled!");
    }
    return ret;
  }

  /**
   * Stop the cleanup scheduled by {@link #scheduleMe(Duration)}.
   *
   * @param aScheduledCleanup
   *        The value returned by {@link #scheduleMe(Duration)}. May be <code>null</code>.
   */
  public static void unschedule (@Nullable final ScheduledFuture <?> aScheduledCleanup)
  {
    if (aScheduledCleanup != null)
    {
      // Was the job scheduled?
      if (WAS_SCHEDULED.getAndSet (false))
      {
        aScheduledCleanup.cancel (false);
        final ScheduledExecutorService aExecutor = EXECUTOR.getAndSet (null);
        if (aExecutor != null)
          aExecutor.shutdown ();

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob was successfully unscheduled");
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4DuplicateCleanupJob is not scheduled");
      }
    }
  }
}
