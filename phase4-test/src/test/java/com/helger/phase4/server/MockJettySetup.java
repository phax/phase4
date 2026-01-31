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
package com.helger.phase4.server;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.io.id.factory.FileIntIDFactory;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.photon.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

public final class MockJettySetup extends AbstractAS4TestSetUp
{
  public static final String SETTINGS_SERVER_JETTY_ENABLED = "server.jetty.enabled";
  public static final String SETTINGS_SERVER_ADDRESS = "server.jetty.address";

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MockJettySetup.class);

  private static AS4JettyRunner s_aJetty;
  private static AS4ResourceHelper s_aResMgr;

  static
  {
    Thread.setDefaultUncaughtExceptionHandler ( (t, e) -> LOGGER.error ("Thread " + t.getId () + " oopsed", e));
  }

  private MockJettySetup ()
  {}

  @NonNull
  @Nonempty
  public static String getServerAddressFromSettings ()
  {
    final String ret = AS4Configuration.getConfig ()
                                       .getAsString (MockJettySetup.SETTINGS_SERVER_ADDRESS,
                                                     AS4TestConstants.DEFAULT_SERVER_ADDRESS);
    if (StringHelper.isEmpty (ret))
      throw new IllegalStateException ("Configuration property '" +
                                       MockJettySetup.SETTINGS_SERVER_ADDRESS +
                                       "' is missing");
    return ret;
  }

  private static boolean _isRunJetty ()
  {
    return AS4Configuration.getConfig ().getAsBoolean (SETTINGS_SERVER_JETTY_ENABLED, false);
  }

  private static int _getJettyPort ()
  {
    return URLHelper.getAsURL (getServerAddressFromSettings ()).getPort ();
  }

  @BeforeClass
  public static void startServer () throws Exception
  {
    LOGGER.info ("MockJettySetup - starting");
    if (_isRunJetty ())
    {
      final int nPort = _getJettyPort ();
      s_aJetty = new AS4JettyRunner ();
      s_aJetty.setPort (nPort).setStopPort (nPort + 1000).setAllowAnnotationBasedConfig (false);
      s_aJetty.startServer ();
    }
    else
    {
      // No Jetty needed for this class
      s_aJetty = null;
      WebScopeManager.onGlobalBegin (MockServletContext.create ());
      final File aSCPath = new File ("target/junittest").getAbsoluteFile ();
      WebFileIO.initPaths (new File (AS4Configuration.getDataPath ()).getAbsoluteFile (),
                           aSCPath.getAbsolutePath (),
                           false);
      GlobalIDFactory.setPersistentIntIDFactory (new FileIntIDFactory (WebFileIO.getDataIO ().getFile ("ids.dat")));
    }

    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    s_aResMgr = new AS4ResourceHelper ();

    LOGGER.info ("MockJettySetup - started");
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    LOGGER.info ("MockJettySetup - stopping");

    if (s_aResMgr != null)
      s_aResMgr.close ();
    if (_isRunJetty ())
    {
      if (s_aJetty != null)
      {
        s_aJetty.shutDownServer ();
        // Wait a little until shutdown happened
        ThreadHelper.sleep (500);
      }
      s_aJetty = null;
    }
    else
    {
      // No Jetty needed for this class
      WebFileIO.resetPaths ();
      WebScopeManager.onGlobalEnd ();
    }
    LOGGER.info ("MockJettySetup - stopped");
  }

  @NonNull
  public static AS4ResourceHelper getResourceManagerInstance ()
  {
    return s_aResMgr;
  }
}
