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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.state.EContinue;
import com.helger.phase4.AS4TestRule;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.mgr.MetaAS4Manager;

/**
 * Test class for class {@link AS4DuplicateCleanupJob}.
 *
 * @author GT
 */
public final class AS4DuplicateCleanupJobTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testDisabledForNonPositiveDuration ()
  {
    assertNull (AS4DuplicateCleanupJob.scheduleMe (Duration.ZERO));
    assertNull (AS4DuplicateCleanupJob.scheduleMe (Duration.ofSeconds (-1)));
  }

  @Test
  public void testScheduledCleanupEvictsOldItems () throws Exception
  {
    final IAS4DuplicateManager aMgr = MetaAS4Manager.getIncomingDuplicateMgr ();
    final String sMessageID = "cleanup-test-" + System.nanoTime ();
    assertSame (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, "profile", "pmode"));
    assertNotNull (aMgr.getItemOfMessageID (sMessageID));

    final ScheduledFuture <?> aScheduledCleanup = AS4DuplicateCleanupJob.scheduleMe (Duration.ofMillis (50));
    assertNotNull (aScheduledCleanup);
    try
    {
      // Scheduling twice does nothing
      assertNull (AS4DuplicateCleanupJob.scheduleMe (Duration.ofMillis (50)));

      // Wait for the item to become older than the disposal duration and be evicted
      final long nEnd = System.currentTimeMillis () + 10_000;
      while (aMgr.getItemOfMessageID (sMessageID) != null && System.currentTimeMillis () < nEnd)
        Thread.sleep (20);
      assertNull (aMgr.getItemOfMessageID (sMessageID));
    }
    finally
    {
      AS4DuplicateCleanupJob.unschedule (aScheduledCleanup);
    }

    // Can be scheduled again after unscheduling
    final ScheduledFuture <?> aScheduledAgain = AS4DuplicateCleanupJob.scheduleMe (Duration.ofMinutes (5));
    assertNotNull (aScheduledAgain);
    AS4DuplicateCleanupJob.unschedule (aScheduledAgain);
  }
}
