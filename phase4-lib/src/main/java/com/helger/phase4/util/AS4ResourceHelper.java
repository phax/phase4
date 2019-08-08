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
package com.helger.phase4.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.io.file.FileIOError;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.phase4.CAS4;

/**
 * A resource manager that keeps track of temporary files and other closables
 * that will be closed when this manager is closed. When calling
 * {@link #createTempFile()} a new filename is created and added to the list.
 * When using {@link #addCloseable(Closeable)} the Closable is added for
 * postponed closing.
 *
 * @author Philip Helger
 */
public class AS4ResourceHelper implements Closeable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ResourceHelper.class);
  private static File s_aTempDir;

  /**
   * @return The temp file directory to use, or <code>null</code> for the system
   *         default.
   */
  @Nullable
  public static File getTempDir ()
  {
    return s_aTempDir;
  }

  /**
   * Set a temporary directory to use.
   *
   * @param aTempDir
   *        The directory to use. It must be an existing directory. May be
   *        <code>null</code> to use the system default.
   * @throws IllegalArgumentException
   *         If the directory does not exist
   */
  public static void setTempDir (@Nullable final File aTempDir)
  {
    if (aTempDir != null)
      if (!aTempDir.isDirectory ())
        throw new IllegalArgumentException ("Temporary directory '" +
                                            aTempDir.getAbsolutePath () +
                                            "' is not a directory");
    s_aTempDir = aTempDir;
  }

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final AtomicBoolean m_aInClose = new AtomicBoolean (false);
  @GuardedBy ("m_aRWLock")
  private final ICommonsList <File> m_aTempFiles = new CommonsArrayList <> ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsList <Closeable> m_aCloseables = new CommonsArrayList <> ();

  public AS4ResourceHelper ()
  {}

  /**
   * @return A new temporary {@link File} that will be deleted when
   *         {@link #close()} is called.
   * @throws IOException
   *         When temp file creation fails.
   * @throws IllegalStateException
   *         If {@link #close()} was already called before
   */
  @Nonnull
  public File createTempFile () throws IOException
  {
    if (m_aInClose.get ())
      throw new IllegalStateException ("ResourceManager is already closing/closed!");

    // Create
    final File ret = File.createTempFile ("phase4-res-", ".tmp", s_aTempDir);
    // And remember
    m_aRWLock.writeLocked ( () -> m_aTempFiles.add (ret));
    return ret;
  }

  /**
   * @return A list of all known temp files. Never <code>null</code> but maybe
   *         empty.
   * @since 0.8.3
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <File> getAllTempFiles ()
  {
    return m_aRWLock.readLocked (m_aTempFiles::getClone);
  }

  /**
   * Add a new closable for later closing.
   *
   * @param aCloseable
   *        The closable to be closed later. May not be <code>null</code>.
   * @throws IllegalStateException
   *         If {@link #close()} was already called before
   */
  public void addCloseable (@Nonnull final Closeable aCloseable)
  {
    ValueEnforcer.notNull (aCloseable, "Closeable");

    if (m_aInClose.get ())
      throw new IllegalStateException ("AS4ResourceHelper is already closing/closed!");

    m_aCloseables.add (aCloseable);
  }

  /**
   * @return A list of all known closables. Never <code>null</code> but maybe
   *         empty.
   * @since 0.8.3
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Closeable> getAllCloseables ()
  {
    return m_aRWLock.readLocked (m_aCloseables::getClone);
  }

  public void close ()
  {
    // Avoid taking new objects
    // close only once
    if (!m_aInClose.getAndSet (true))
    {
      // Close all closeables before deleting files, because the closables might
      // be the files to be deleted :)
      final ICommonsList <Closeable> aCloseables = m_aRWLock.writeLocked ( () -> {
        final ICommonsList <Closeable> ret = m_aCloseables.getClone ();
        m_aCloseables.clear ();
        return ret;
      });
      if (aCloseables.isNotEmpty ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Closing " + aCloseables.size () + " " + CAS4.LIB_NAME + " stream handles");

        for (final Closeable aCloseable : aCloseables)
          StreamHelper.close (aCloseable);
      }

      // Get and delete all temp files
      final ICommonsList <File> aFiles = m_aRWLock.writeLocked ( () -> {
        final ICommonsList <File> ret = m_aTempFiles.getClone ();
        m_aTempFiles.clear ();
        return ret;
      });
      if (aFiles.isNotEmpty ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Deleting " + aFiles.size () + " temporary " + CAS4.LIB_NAME + " files");

        for (final File aFile : aFiles)
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Deleting temporary file '" + aFile.getAbsolutePath () + "'");

          final FileIOError aError = AS4IOHelper.getFileOperationManager ().deleteFileIfExisting (aFile);
          if (aError.isFailure ())
            LOGGER.warn ("  Failed to delete temporary " +
                         CAS4.LIB_NAME +
                         " file " +
                         aFile.getAbsolutePath () +
                         ": " +
                         aError.toString ());
        }
      }
    }
  }
}
