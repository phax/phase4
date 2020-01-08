/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.system.SystemProperties;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.AS4ProfileManager;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Meta manager with all known managers.
 *
 * @author Philip Helger
 */
public final class MetaAS4Manager extends AbstractGlobalSingleton
{
  /** The boolean System property to enable in-memory managers */
  public static final String SYSTEM_PROPERTY_PHASE4_MANAGER_INMEMORY = "phase4.manager.inmemory";

  private static final Logger LOGGER = LoggerFactory.getLogger (MetaAS4Manager.class);

  private IMPCManager m_aMPCMgr;
  private IPModeManager m_aPModeMgr;
  private IAS4DuplicateManager m_aIncomingDuplicateMgr;
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
      final String sInMemory = SystemProperties.getPropertyValueOrNull (SYSTEM_PROPERTY_PHASE4_MANAGER_INMEMORY);
      final boolean bInMemory = StringParser.parseBool (sInMemory, false);
      final IManagerFactory aMgrFactory;
      if (bInMemory)
      {
        LOGGER.info (ClassHelper.getClassLocalName (this) + " is initialized with in-memory data structures");
        aMgrFactory = new ManagerFactoryInMemory ();
      }
      else
      {
        LOGGER.info (ClassHelper.getClassLocalName (this) + " is using file system persistence");
        aMgrFactory = new ManagerFactoryPersistingFileSystem ();
      }

      // MPC manager before PMode manager
      m_aMPCMgr = aMgrFactory.createMPCManager ();
      m_aPModeMgr = aMgrFactory.createPModeManager ();
      m_aIncomingDuplicateMgr = aMgrFactory.createDuplicateManager ();

      // profile mgr is always in-memory
      m_aProfileMgr = new AS4ProfileManager ();

      _initCallbacks ();

      // Validate content
      m_aPModeMgr.validateAllPModes ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
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
  public static IMPCManager getMPCMgr ()
  {
    return getInstance ().m_aMPCMgr;
  }

  @Nonnull
  public static IPModeManager getPModeMgr ()
  {
    return getInstance ().m_aPModeMgr;
  }

  @Nonnull
  public static IAS4DuplicateManager getIncomingDuplicateMgr ()
  {
    return getInstance ().m_aIncomingDuplicateMgr;
  }

  @Nonnull
  public static AS4ProfileManager getProfileMgr ()
  {
    return getInstance ().m_aProfileMgr;
  }
}
