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
package com.helger.phase4.peppol.receivers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

public class MainSendInParallelHelger
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainSendInParallelHelger.class);

  public static void main (final String [] args) throws Exception
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // No dumping

    try
    {
      // https://accap.mypeppol.app/as4
      final StopWatch aSW = StopWatch.createdStarted ();
      final ExecutorService aES = Executors.newFixedThreadPool (10);

      for (int i = 0; i < 50; ++i)
        aES.submit (MainPhase4PeppolSenderHelger::send);

      aES.shutdown ();
      ExecutorServiceHelper.waitUntilAllTasksAreFinished (aES);
      LOGGER.info ("Sending out took " + aSW.stopAndGetMillis () + "ms");
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
