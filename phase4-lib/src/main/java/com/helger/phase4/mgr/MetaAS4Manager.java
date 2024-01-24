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
package com.helger.phase4.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4ProfileManager;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Meta manager with all known managers.
 *
 * @author Philip Helger
 */
public final class MetaAS4Manager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MetaAS4Manager.class);

  private static IAS4ManagerFactory s_aFactory;
  private static IAS4TimestampManager s_aTimestampMgr;
  static
  {
    if (AS4Configuration.isUseInMemoryManagers ())
    {
      LOGGER.info ("MetaAS4Manager is initialized with in-memory data structures");
      s_aFactory = new AS4ManagerFactoryInMemory ();
    }
    else
    {
      LOGGER.info ("MetaAS4Manager is initialized using file system persistence");
      s_aFactory = new AS4ManagerFactoryPersistingFileSystem ();
    }
    s_aTimestampMgr = s_aFactory.createTimestampManager ();
  }

  /**
   * @return The current manager factory. Never <code>null</code>.
   * @since 0.9.14
   */
  @Nonnull
  public static IAS4ManagerFactory getFactory ()
  {
    return s_aFactory;
  }

  /**
   * Set the manager factory to be used. This must be called before the first
   * invocation of {@link #getInstance()} to have an effect.
   *
   * @param aFactory
   *        The new factory. May not be <code>null</code>.
   * @since 0.9.14
   */
  public static void setFactory (@Nonnull final IAS4ManagerFactory aFactory)
  {
    ValueEnforcer.notNull (aFactory, "Factory");
    s_aFactory = aFactory;
    s_aTimestampMgr = s_aFactory.createTimestampManager ();
  }

  private IMPCManager m_aMPCMgr;
  private IPModeManager m_aPModeMgr;
  private IAS4DuplicateManager m_aIncomingDuplicateMgr;
  private IAS4ProfileManager m_aProfileMgr;

  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public MetaAS4Manager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      LOGGER.info ("Creating AS4 managers using factory class " + s_aFactory.getClass ().getName ());

      // MPC manager before PMode manager
      m_aMPCMgr = s_aFactory.createMPCManager ();
      m_aPModeMgr = s_aFactory.createPModeManager ();
      m_aIncomingDuplicateMgr = s_aFactory.createDuplicateManager ();
      m_aProfileMgr = s_aFactory.createProfileManager ();

      // Validate content
      m_aPModeMgr.validateAllPModes ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

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
  public static IAS4ProfileManager getProfileMgr ()
  {
    return getInstance ().m_aProfileMgr;
  }

  @Nonnull
  public static IAS4TimestampManager getTimestampMgr ()
  {
    // The timestamp manager may be needed during initialization of a singleton,
    // as such it must be static
    return s_aTimestampMgr;
  }
}
