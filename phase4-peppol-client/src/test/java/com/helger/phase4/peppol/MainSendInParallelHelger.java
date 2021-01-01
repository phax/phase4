/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;

public class MainSendInParallelHelger
{
  public static void main (final String [] args) throws Exception
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    try
    {
      // https://accap.mypeppol.app/as4
      final ExecutorService aES = Executors.newFixedThreadPool (10);

      for (int i = 0; i < 500; ++i)
        aES.submit (MainPhase4PeppolSenderHelger::send);

      aES.shutdown ();
      ExecutorServiceHelper.waitUntilAllTasksAreFinished (aES);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
