/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.reporting;

import java.time.YearMonth;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.helger.commons.lang.ClassHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.quartz.CronScheduleBuilder;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * A periodic job to run once a month to create, validate, send and store Peppol Reports.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class DoPeppolReportingJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (DoPeppolReportingJob.class);

  @Override
  protected void onExecute (final JobDataMap aJobDataMap, final IJobExecutionContext aContext)
                                                                                               throws JobExecutionException
  {
    if (APConfig.isSchedulePeppolReporting ())
    {
      LOGGER.info ("Running scheduled creation and sending of Peppol Reporting messages");
      // Use the previous month
      final YearMonth aYearMonth = YearMonth.now ().minusMonths (1);
      AppReportingHelper.createAndSendPeppolReports (aYearMonth);
    }
    else
      LOGGER.warn ("Creating and sending Peppol Reports is disabled in the configuration");
  }

  @Nullable
  public static TriggerKey scheduleMe ()
  {
    return GlobalQuartzScheduler.getInstance ()
                                .scheduleJob (ClassHelper.getClassLocalName (DoPeppolReportingJob.class),
                                              JDK8TriggerBuilder.newTrigger ()
                                                                .startNow ()
                                                                .withSchedule (CronScheduleBuilder.monthlyOnDayAndHourAndMinute (2,
                                                                                                                                 1,
                                                                                                                                 0)),
                                              DoPeppolReportingJob.class,
                                              null);
  }
}
