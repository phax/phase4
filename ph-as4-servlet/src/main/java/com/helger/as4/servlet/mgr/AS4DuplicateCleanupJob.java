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
package com.helger.as4.servlet.mgr;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.lang.ClassHelper;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.web.scope.util.AbstractScopeAwareJob;

@DisallowConcurrentExecution
public final class AS4DuplicateCleanupJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4DuplicateCleanupJob.class);
  private static final String KEY_MINUTES = "mins";

  public AS4DuplicateCleanupJob ()
  {}

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    final long nMins = aJobDataMap.getAsLong (KEY_MINUTES);
    final LocalDateTime aOldDT = PDTFactory.getCurrentLocalDateTime ().minusMinutes (nMins);

    final ICommonsList <String> aEvicted = MetaAS4Manager.getIncomingDuplicateMgr ().evictAllItemsBefore (aOldDT);
    if (aEvicted.isNotEmpty ())
      LOGGER.info ("Evicted " + aEvicted.size () + " incoming duplicate message IDs");
  }

  private static final AtomicBoolean s_aScheduled = new AtomicBoolean (false);

  public static void scheduleMe (final long nDisposalMinutes)
  {
    if (nDisposalMinutes > 0)
    {
      if (!s_aScheduled.getAndSet (true))
      {
        final JobDataMap aJobDataMap = new JobDataMap ();
        aJobDataMap.putIn (KEY_MINUTES, nDisposalMinutes);
        GlobalQuartzScheduler.getInstance ()
                             .scheduleJob (ClassHelper.getClassLocalName (AS4DuplicateCleanupJob.class) +
                                           "-" +
                                           nDisposalMinutes,
                                           JDK8TriggerBuilder.newTrigger ()
                                                             .startNow ()
                                                             .withSchedule (SimpleScheduleBuilder.repeatMinutelyForever (5)),
                                           AS4DuplicateCleanupJob.class,
                                           aJobDataMap);
      }
      // else already scheduled
    }
    else
    {
      LOGGER.warn ("Incoming duplicate message cleaning is disabled!");
    }
  }
}
