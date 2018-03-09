/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModePayloadService;
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
    {
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
  }

  @Override
  protected void initManagers ()
  {
    AS4ServerInitializer.initAS4Server ();
    DropFolderUserMessage.init ();

    // CEF conformance testing PModes

    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    {
      // SIMPLE_ONEWAY
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_SIMPLE_ONEWAY
      // 7. Action: ACT_SIMPLE_ONEWAY
      final PMode aPMode = ESENSPMode.createESENSPMode ("AnyInitiatorID",
                                                        "AnyResponderID",
                                                        "AnyResponderAddress",
                                                        (i, r) -> "SIMPLE_ONEWAY",
                                                        false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_SIMPLE_ONEWAY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_SIMPLE_ONEWAY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // SIMPLE_TWOWAY
      // 1. MEP: Two way push-and-push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_SIMPLE_TWOWAY
      // 7. Action: ACT_SIMPLE_TWOWAY
      final PMode aPMode = ESENSPMode.createESENSPModeTwoWay ("AnyInitiatorID",
                                                              "AnyResponderID",
                                                              "AnyResponderAddress",
                                                              (i, r) -> "SIMPLE_TWOWAY",
                                                              false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_SIMPLE_TWOWAY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_SIMPLE_TWOWAY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // ONEWAY_RETRY
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: 5 (the interval between retries must be less than 3 minutes)
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_ONEWAY_RETRY
      // 7. Action: ACT_ONEWAY_RETRY
      final PMode aPMode = ESENSPMode.createESENSPMode ("AnyInitiatorID",
                                                        "AnyResponderID",
                                                        "AnyResponderAddress",
                                                        (i, r) -> "ONEWAY_RETRY",
                                                        false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (true);
      aPMode.getReceptionAwareness ().setMaxRetries (5);
      aPMode.getReceptionAwareness ().setRetryIntervalMS (10_000);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_ONEWAY_RETRY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_ONEWAY_RETRY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // ONEWAY_ONLY_SIGN
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: No
      // 6. Service: SRV_ONEWAY_SIGNONLY
      // 7. Action: ACT_ONEWAY_SIGNONLY
      final PMode aPMode = ESENSPMode.createESENSPMode ("AnyInitiatorID",
                                                        "AnyResponderID",
                                                        "AnyResponderAddress",
                                                        (i, r) -> "ONEWAY_ONLY_SIGN",
                                                        false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getSecurity ().setX509EncryptionAlgorithm (null);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_ONEWAY_SIGNONLY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_ONEWAY_SIGNONLY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // PING
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service:
      // http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service
      // 7. Action:
      // http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test
      final PMode aPMode = ESENSPMode.createESENSPMode ("AnyInitiatorID",
                                                        "AnyResponderID",
                                                        "AnyResponderAddress",
                                                        (i, r) -> "PING",
                                                        false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService (CAS4.DEFAULT_SERVICE_URL);
      aPMode.getLeg1 ().getBusinessInfo ().setAction (CAS4.DEFAULT_ACTION_URL);
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
  }

  @Override
  protected void beforeContextDestroyed (@Nonnull final ServletContext aSC)
  {
    DropFolderUserMessage.destroy ();
  }
}
