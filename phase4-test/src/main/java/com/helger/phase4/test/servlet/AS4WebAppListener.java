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
package com.helger.phase4.test.servlet;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.httpclient.HttpDebugger;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.incoming.AS4ServerInitializer;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.servlet.ServletContext;

/**
 * Internal web app listener to get the test requirements up and running
 *
 * @author Philip Helger
 */
public final class AS4WebAppListener extends WebAppListener
{
  public AS4WebAppListener ()
  {
    setHandleStatisticsOnEnd (false);
  }

  @Override
  @Nullable
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isGlobalDebug ());
  }

  @Override
  @Nullable
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isGlobalProduction ());
  }

  @Override
  @Nullable
  protected String getInitParameterNoStartupInfo (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isNoStartupInfo ());
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AS4Configuration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return false;
  }

  @Override
  protected void initGlobalSettings ()
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Disable some bloat ware ;-)
    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    HttpDebugger.setEnabled (false);
  }

  @Override
  protected void initSecurity ()
  {
    // Ensure user exists
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    if (!aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
      aUserMgr.createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
                                     CSecurity.USER_ADMINISTRATOR_LOGIN,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_PASSWORD,
                                     "Admin",
                                     "istrator",
                                     null,
                                     Locale.US,
                                     null,
                                     false);
  }

  @Override
  protected void initManagers ()
  {
    AS4ServerInitializer.initAS4Server ();
  }

  @Override
  protected void beforeContextDestroyed (final ServletContext aSC)
  {
    AS4ServerInitializer.shutdownAS4Server ();
  }
}
