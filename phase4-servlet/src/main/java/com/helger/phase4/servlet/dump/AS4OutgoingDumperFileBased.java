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
package com.helger.phase4.servlet.dump;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;

/**
 * File based implementation if {@link IAS4OutgoingDumper}
 *
 * @author Philip Helger
 * @since 0.9.3
 */
public class AS4OutgoingDumperFileBased implements IAS4OutgoingDumper
{
  public static interface IFileProvider
  {
    @Nonnull
    File getFile (@Nonnull @Nonempty String sMessageID, @Nonnegative int nTry);
  }

  private final IFileProvider m_aFileProvider;

  public AS4OutgoingDumperFileBased ()
  {
    this ( (sMessageID,
            nTry) -> new File (AS4ServerConfiguration.getDataPath (),
                               "outgoing/" +
                                                                      PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                                                                      "-" +
                                                                      FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
                                                                      "-" +
                                                                      nTry +
                                                                      ".dat"));
  }

  public AS4OutgoingDumperFileBased (@Nonnull final IFileProvider aFileProvider)
  {
    ValueEnforcer.notNull (aFileProvider, "FileProvider");
    m_aFileProvider = aFileProvider;
  }

  @Nullable
  public OutputStream onBeginRequest (@Nonnull @Nonempty final String sMessageID,
                                      @Nullable final HttpHeaderMap aCustomHeaders,
                                      @Nonnegative final int nTry) throws IOException
  {
    final File aResponseFile = m_aFileProvider.getFile (sMessageID, nTry);
    final OutputStream ret = FileHelper.getBufferedOutputStream (aResponseFile);
    if (aCustomHeaders != null && aCustomHeaders.isNotEmpty ())
    {
      for (final Map.Entry <String, ICommonsList <String>> aEntry : aCustomHeaders)
      {
        final String sHeader = aEntry.getKey ();
        for (final String sValue : aEntry.getValue ())
          ret.write ((sHeader +
                      ": " +
                      HttpHeaderMap.getUnifiedValue (sValue) +
                      CHttp.EOL).getBytes (CHttp.HTTP_CHARSET));
      }
      ret.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
    return ret;
  }
}
