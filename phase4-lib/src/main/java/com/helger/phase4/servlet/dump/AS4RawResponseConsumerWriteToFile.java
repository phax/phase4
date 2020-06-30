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
package com.helger.phase4.servlet.dump;

import java.io.File;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.util.Phase4Exception;

/**
 * Example implementation of {@link IAS4RawResponseConsumer} writing to a file.
 * The base directory is determined by
 * {@link AS4ServerConfiguration#getDataPath()}.
 *
 * @author Philip Helger
 */
public class AS4RawResponseConsumerWriteToFile implements IAS4RawResponseConsumer
{
  /**
   * Callback interface to create a file based on the provided metadata.
   *
   * @author Philip Helger
   * @since 0.10.2
   */
  @FunctionalInterface
  public static interface IFileProvider
  {
    @Nonnull
    File createFile (@Nonnull String sMessageID);

    @Nonnull
    static String getFilename (@Nonnull final String sMessageID)
    {
      return PDTIOHelper.getCurrentLocalDateTimeForFilename () +
             "-" +
             FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
             "-response.xml";
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4RawResponseConsumerWriteToFile.class);

  private final IFileProvider m_aFileProvider;

  /**
   * Default constructor. Writes the files to the AS4 configured data path +
   * {@link AS4OutgoingDumperFileBased#DEFAULT_BASE_PATH}.
   *
   * @see AS4ServerConfiguration#getDataPath()
   */
  public AS4RawResponseConsumerWriteToFile ()
  {
    this (sMessageID -> new File (AS4ServerConfiguration.getDataPath (),
                                  AS4OutgoingDumperFileBased.DEFAULT_BASE_PATH + IFileProvider.getFilename (sMessageID)));
  }

  /**
   * Constructor with a custom file provider.
   *
   * @param aFileProvider
   *        The file provider to be used. May not be <code>null</code>.
   * @since 0.10.2
   */
  public AS4RawResponseConsumerWriteToFile (@Nonnull final IFileProvider aFileProvider)
  {
    ValueEnforcer.notNull (aFileProvider, "FileProvider");
    m_aFileProvider = aFileProvider;
  }

  public void handleResponse (@Nonnull final AS4ClientSentMessage <byte []> aResponseEntity) throws Phase4Exception
  {
    if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
    {
      final String sMessageID = aResponseEntity.getMessageID ();

      // Use the configured data path as the base
      final File aResponseFile = m_aFileProvider.createFile (sMessageID);
      LOGGER.info ("Logging AS4 response to '" + aResponseFile.getAbsolutePath () + "'");

      if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isFailure ())
        LOGGER.error ("Error writing AS4 response file to '" + aResponseFile.getAbsolutePath () + "'");
    }
  }

  /**
   * Create a new instance for the provided directory.
   *
   * @param aBaseDirectory
   *        The absolute directory to be used. May not be <code>null</code>.
   * @return The created instance. Never <code>null</code>.
   * @since 0.10.2
   */
  @Nonnull
  public static AS4RawResponseConsumerWriteToFile createForDirectory (@Nonnull final File aBaseDirectory)
  {
    ValueEnforcer.notNull (aBaseDirectory, "BaseDirectory");
    return new AS4RawResponseConsumerWriteToFile (sMessageID -> new File (aBaseDirectory, IFileProvider.getFilename (sMessageID)));
  }
}
