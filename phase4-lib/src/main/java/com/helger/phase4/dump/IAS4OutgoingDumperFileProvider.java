/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.string.StringHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FilenameHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.mgr.MetaAS4Manager;

@FunctionalInterface
public interface IAS4OutgoingDumperFileProvider
{
  /** The default file extension to be used */
  String DEFAULT_FILE_EXTENSION = ".as4out";

  /**
   * Get the {@link File} to write the dump to. The filename must be globally unique. The resulting
   * file should be an absolute path.
   *
   * @param eMsgMode
   *        Are we dumping a request or a response? Never <code>null</code>. Added in v1.2.0.
   * @param sAS4MessageID
   *        The AS4 message ID that was send out. Neither <code>null</code> nor empty.
   * @param nTry
   *        The number of the try to send the message. The initial try has value 0, the first retry
   *        has value 1 etc.
   * @return A non-<code>null</code> {@link File}.
   * @see AS4Configuration#getDumpBasePath()
   */
  @NonNull
  File getFile (@NonNull EAS4MessageMode eMsgMode, @NonNull @Nonempty String sAS4MessageID, @Nonnegative int nTry);

  @NonNull
  static String getDefaultDirectoryName (@NonNull final OffsetDateTime aNow)
  {
    return aNow.getYear () +
           "/" +
           StringHelper.getLeadingZero (aNow.getMonthValue (), 2) +
           "/" +
           StringHelper.getLeadingZero (aNow.getDayOfMonth (), 2);
  }

  @NonNull
  static String getDefaultFilename (@NonNull final OffsetDateTime aNow,
                                    @NonNull @Nonempty final String sAS4MessageID,
                                    @Nonnegative final int nTry)
  {
    return PDTIOHelper.getTimeForFilename (aNow.toLocalTime ()) +
           "-" +
           FilenameHelper.getAsSecureValidASCIIFilename (sAS4MessageID) +
           "-" +
           nTry +
           DEFAULT_FILE_EXTENSION;
  }

  @NonNull
  static String getDefaultDirectoryAndFilename (@NonNull @Nonempty final String sAS4MessageID,
                                                @Nonnegative final int nTry)
  {
    final OffsetDateTime aNow = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
    return getDefaultDirectoryName (aNow) + '/' + getDefaultFilename (aNow, sAS4MessageID, nTry);
  }
}
