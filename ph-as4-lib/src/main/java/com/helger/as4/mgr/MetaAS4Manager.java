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
package com.helger.as4.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.duplicate.AS4DuplicateManager;
import com.helger.as4.model.mpc.MPCManager;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.profile.AS4ProfileManager;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Meta manager with all known managers.
 *
 * @author Philip Helger
 */
public final class MetaAS4Manager extends AbstractGlobalSingleton
{
  private static final String MPC_XML = "as4-mpc.xml";
  private static final String PMODE_XML = "as4-pmode.xml";
  private static final String INCOMING_DUPLICATE_XML = "as4-duplicate-incoming.xml";

  private static final Logger LOGGER = LoggerFactory.getLogger (MetaAS4Manager.class);

  private MPCManager m_aMPCMgr;
  private PModeManager m_aPModeMgr;
  private AS4ProfileManager m_aProfileMgr;
  private AS4DuplicateManager m_aIncomingDuplicateMgr;

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
      m_aPModeMgr = new PModeManager (PMODE_XML);
      m_aProfileMgr = new AS4ProfileManager ();
      m_aIncomingDuplicateMgr = new AS4DuplicateManager (INCOMING_DUPLICATE_XML);

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
  public static MPCManager getMPCMgr ()
  {
    return getInstance ().m_aMPCMgr;
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

  @Nonnull
  public static AS4DuplicateManager getIncomingDuplicateMgr ()
  {
    return getInstance ().m_aIncomingDuplicateMgr;
  }
}
