package com.helger.phase4.peppol.server.reporting;

import java.time.YearMonth;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.helger.commons.lang.ClassHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.quartz.CronScheduleBuilder;
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
