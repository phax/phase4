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
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.dump.AbstractAS4IncomingDumperWithHeaders;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;

/**
 * Simple file based version of {@link IAS4IncomingDumper}
 *
 * @author Philip Helger
 * @since 0.9.3
 */
public class AS4IncomingDumperFileBased extends AbstractAS4IncomingDumperWithHeaders
{
  /**
   * Callback interface to create a file based on the provided metadata.
   *
   * @author Philip Helger
   * @since 0.9.8
   */
  @FunctionalInterface
  public static interface IFileProvider
  {
    @Nonnull
    File createFile (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata, @Nonnull HttpHeaderMap aHttpHeaderMap);
  }

  public static final String DEFAULT_BASE_PATH = "incoming/";
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4IncomingDumperFileBased.class);

  private final IFileProvider m_aFileProvider;

  public AS4IncomingDumperFileBased ()
  {
    this ( (aMessageMetadata,
            aHttpHeaderMap) -> new File (AS4ServerConfiguration.getDataPath (),
                                         DEFAULT_BASE_PATH +
                                                                                PDTIOHelper.getLocalDateTimeForFilename (aMessageMetadata.getIncomingDT ()) +
                                                                                ".as4in"));
  }

  public AS4IncomingDumperFileBased (@Nonnull final IFileProvider aFileProvider)
  {
    ValueEnforcer.notNull (aFileProvider, "FileProvider");
    m_aFileProvider = aFileProvider;
  }

  @Override
  @Nullable
  protected OutputStream openOutputStream (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                           @Nonnull final HttpHeaderMap aHttpHeaderMap) throws IOException
  {
    final File aResponseFile = m_aFileProvider.createFile (aMessageMetadata, aHttpHeaderMap);
    LOGGER.info ("Logging incoming AS4 request to '" + aResponseFile.getAbsolutePath () + "'");
    return FileHelper.getBufferedOutputStream (aResponseFile);
  }
}
