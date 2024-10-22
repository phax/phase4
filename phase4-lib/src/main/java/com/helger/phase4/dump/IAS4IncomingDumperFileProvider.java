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
package com.helger.phase4.dump;

import java.io.File;
import java.time.OffsetDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * Callback interface to create a file based on the provided metadata.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
@FunctionalInterface
public interface IAS4IncomingDumperFileProvider
{
  /** The default file extension to be used */
  String DEFAULT_FILE_EXTENSION = ".as4in";

  /**
   * Get the {@link File} to write the dump to. The filename must be globally
   * unique. The resulting file should be an absolute path.
   *
   * @param aMessageMetadata
   *        The message metadata of the incoming message. Never
   *        <code>null</code>.
   * @param aHttpHeaderMap
   *        The HTTP headers of the incoming message. Never <code>null</code>.
   * @return A non-<code>null</code> {@link File}.
   * @see AS4Configuration#getDumpBasePath()
   */
  @Nonnull
  File createFile (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata, @Nonnull HttpHeaderMap aHttpHeaderMap);

  @Nonnull
  static String getDefaultDirectoryName (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    final OffsetDateTime aLDT = aMessageMetadata.getIncomingDT ();
    return aLDT.getYear () +
           "/" +
           StringHelper.getLeadingZero (aLDT.getMonthValue (), 2) +
           "/" +
           StringHelper.getLeadingZero (aLDT.getDayOfMonth (), 2);
  }

  @Nonnull
  static String getDefaultFilename (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    final OffsetDateTime aLDT = aMessageMetadata.getIncomingDT ();
    return PDTIOHelper.getTimeForFilename (aLDT.toLocalTime ()) +
           '-' +
           FilenameHelper.getAsSecureValidASCIIFilename (aMessageMetadata.getIncomingUniqueID ()) +
           DEFAULT_FILE_EXTENSION;
  }

  @Nonnull
  static String getDefaultDirectoryAndFilename (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    return getDefaultDirectoryName (aMessageMetadata) + "/" + getDefaultFilename (aMessageMetadata);
  }
}
