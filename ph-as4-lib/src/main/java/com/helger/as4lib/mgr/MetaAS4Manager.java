package com.helger.as4lib.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;

public final class MetaAS4Manager extends AbstractGlobalSingleton
{
  private static final String PMODE_XML = "pmode.xml";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MetaAS4Manager.class);

  private PModeManager m_aPModeMgr;

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
      // Non business-logic managers
      m_aPModeMgr = new PModeManager (PMODE_XML);

      _initCallbacks ();

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
  public static PModeManager getPModeMgr ()
  {
    return getInstance ().m_aPModeMgr;
  }
}
