/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.model.mpc.MPCManager;
import com.helger.as4lib.model.pmode.PModeConfigManager;
import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.as4lib.model.profile.AS4ProfileManager;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;

public final class MetaAS4Manager extends AbstractGlobalSingleton
{
  private static final String MPC_XML = "mpc.xml";
  private static final String PARTNER_XML = "partner.xml";
  private static final String PMODE_CONFIG_XML = "pmodeconfig.xml";
  private static final String PMODE_XML = "pmode.xml";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MetaAS4Manager.class);

  private MPCManager m_aMPCMgr;
  private PartnerManager m_aPartnerMgr;
  private PModeConfigManager m_aPModeConfigMgr;
  private PModeManager m_aPModeMgr;
  private AS4ProfileManager m_aProfileMgr;

  @Deprecated
  @UsedViaReflection
  public MetaAS4Manager ()
  {}

  private void _initCallbacks ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      // MPC manager before PMode manager
      m_aMPCMgr = new MPCManager (MPC_XML);
      m_aPartnerMgr = new PartnerManager (PARTNER_XML);
      m_aPModeConfigMgr = new PModeConfigManager (PMODE_CONFIG_XML);
      m_aPModeMgr = new PModeManager (PMODE_XML);
      m_aProfileMgr = new AS4ProfileManager ();

      _initCallbacks ();

      // Validate content
      m_aPModeConfigMgr.validateAllPModeConfigs ();
      m_aPModeMgr.validateAllPModes ();

      s_aLogger.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Throwable t)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), t);
    }
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed) throws Exception
  {}

  @Nonnull
  public static MetaAS4Manager getInstance ()
  {
    return getGlobalSingleton (MetaAS4Manager.class);
  }

  @Nonnull
  public static MPCManager getMPCMgr ()
  {
    return getInstance ().m_aMPCMgr;
  }

  @Nonnull
  public static PartnerManager getPartnerMgr ()
  {
    return getInstance ().m_aPartnerMgr;
  }

  @Nonnull
  public static PModeConfigManager getPModeConfigMgr ()
  {
    return getInstance ().m_aPModeConfigMgr;
  }

  @Nonnull
  public static PModeManager getPModeMgr ()
  {
    return getInstance ().m_aPModeMgr;
  }

  @Nonnull
  public static AS4ProfileManager getProfileMgr ()
  {
    return getInstance ().m_aProfileMgr;
  }
}
