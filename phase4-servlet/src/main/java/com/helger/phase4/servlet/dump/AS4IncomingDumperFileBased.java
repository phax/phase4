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
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.file.FileHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;

/**
 * Simple file based version of {@link IAS4IncomingDumper}
 *
 * @author Philip Helger
 * @since 0.9.3
 */
public class AS4IncomingDumperFileBased implements IAS4IncomingDumper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4IncomingDumperFileBased.class);

  private final Supplier <File> m_aFileProvider;

  public AS4IncomingDumperFileBased ()
  {
    this ( () -> new File (AS4ServerConfiguration.getDataPath (),
                           "incoming/" + PDTIOHelper.getCurrentLocalDateTimeForFilename () + ".dat"));
  }

  public AS4IncomingDumperFileBased (@Nonnull final Supplier <File> aFileProvider)
  {
    ValueEnforcer.notNull (aFileProvider, "FileProvider");
    m_aFileProvider = aFileProvider;
  }

  @Nonnull
  public OutputStream onNewRequest (@Nonnull final HttpServletRequest aHttpServletRequest) throws IOException
  {
    final File aResponseFile = m_aFileProvider.get ();
    LOGGER.info ("Logging incoming AS4 request to '" + aResponseFile.getAbsolutePath () + "'");
    final OutputStream ret = FileHelper.getBufferedOutputStream (aResponseFile);
    // Add all incoming headers
    int nHeader = 0;
    for (final String sHeader : CollectionHelper.newList (aHttpServletRequest.getHeaderNames ()))
      for (final String sValue : CollectionHelper.newList (aHttpServletRequest.getHeaders (sHeader)))
      {
        nHeader++;
        ret.write ((sHeader + ": " + sValue + CHttp.EOL).getBytes (CHttp.HTTP_CHARSET));
      }
    if (nHeader > 0)
      ret.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    return ret;
  }
}
