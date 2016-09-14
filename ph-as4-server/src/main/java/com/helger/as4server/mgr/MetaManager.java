package com.helger.as4server.mgr;

import java.io.File;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4server.attachment.DefaultIncomingAttachmentFactory;
import com.helger.as4server.attachment.IIncomingAttachmentFactory;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.io.file.LoggingFileOperationCallback;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;

public final class MetaManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MetaManager.class);
  private static final FileOperationManager s_aFOP = new FileOperationManager (new LoggingFileOperationCallback ());

  private IIncomingAttachmentFactory m_aIncomingAttachmentFactory;

  @Deprecated
  @UsedViaReflection
  public MetaManager ()
  {}

  private void _initCallbacks ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      m_aIncomingAttachmentFactory = new DefaultIncomingAttachmentFactory ();

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
  {
    if (m_aIncomingAttachmentFactory != null)
      for (final File aFile : m_aIncomingAttachmentFactory.getAllTempFiles ())
      {
        s_aLogger.info ("Deleting temporary file " + aFile.getAbsolutePath ());
        s_aFOP.deleteFileIfExisting (aFile);
      }
  }

  @Nonnull
  public static MetaManager getInstance ()
  {
    return getGlobalSingleton (MetaManager.class);
  }

  @Nonnull
  public static IIncomingAttachmentFactory getIncomingAttachmentFactory ()
  {
    return getInstance ().m_aIncomingAttachmentFactory;
  }
}
