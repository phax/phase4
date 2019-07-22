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
package com.helger.as4.server.servlet;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.as4.model.pmode.resolve.IPModeResolver;
import com.helger.as4.servlet.AS4ServerInitializer;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.commons.debug.GlobalDebug;
import com.helger.httpclient.HttpDebugger;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.UserManager;
import com.helger.xservlet.requesttrack.RequestTracker;

public final class AS4WebAppListener extends WebAppListener
{
  @Override
  @Nullable
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isGlobalDebug ());
  }

  @Override
  @Nullable
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isGlobalProduction ());
  }

  @Override
  @Nullable
  protected String getInitParameterNoStartupInfo (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isNoStartupInfo ());
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AS4ServerConfiguration.getDataPath ();
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

    if (GlobalDebug.isDebugMode ())
      RequestTracker.getInstance ().getRequestTrackingMgr ().setLongRunningCheckEnabled (false);

    HttpDebugger.setEnabled (false);
  }

  @Override
  protected void initSecurity ()
  {
    // Ensure user exists
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
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
    final IPModeResolver aPModeResolver = DefaultPModeResolver.DEFAULT_PMODE_RESOLVER;
    final AS4CryptoFactory aCryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;
    AS4ServerInitializer.initAS4Server (aPModeResolver, aCryptoFactory);
  }
}
