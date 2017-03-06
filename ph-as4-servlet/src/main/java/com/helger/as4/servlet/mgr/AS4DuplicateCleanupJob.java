package com.helger.as4.servlet.mgr;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.lang.ClassHelper;
import com.helger.photon.core.app.CApplication;
import com.helger.photon.core.job.AbstractPhotonJob;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;

@DisallowConcurrentExecution
public final class AS4DuplicateCleanupJob extends AbstractPhotonJob
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4DuplicateCleanupJob.class);
  private static final String KEY_MINUTES = "mins";

  public AS4DuplicateCleanupJob ()
  {
    // Fixed ID
    super (CApplication.APP_ID_SECURE);
  }

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    final long nMins = aJobDataMap.getLong (KEY_MINUTES);
    final LocalDateTime aOldDT = PDTFactory.getCurrentLocalDateTime ().minusMinutes (nMins);

    final ICommonsList <String> aEvicted = MetaAS4Manager.getIncomingDuplicateMgr ().evictAllItemsBefore (aOldDT);
    if (aEvicted.isNotEmpty ())
      s_aLogger.info ("Evicted " + aEvicted.size () + " incoming duplicate message IDs");
  }

  public static void scheduleMe (final long nDisposalMinutes)
  {
    if (nDisposalMinutes > 0)
    {
      final ICommonsMap <String, Object> aJobDataMap = new CommonsHashMap<> ();
      aJobDataMap.put (KEY_MINUTES, Long.valueOf (nDisposalMinutes));
      GlobalQuartzScheduler.getInstance ().scheduleJob (ClassHelper.getClassLocalName (AS4DuplicateCleanupJob.class),
                                                        JDK8TriggerBuilder.newTrigger ()
                                                                          .startNow ()
                                                                          .withSchedule (SimpleScheduleBuilder.repeatMinutelyForever (5)),
                                                        AS4DuplicateCleanupJob.class,
                                                        aJobDataMap);
    }
    else
    {
      s_aLogger.warn ("Incoming duplicate message cleaning is disabled!");
    }
  }
}
