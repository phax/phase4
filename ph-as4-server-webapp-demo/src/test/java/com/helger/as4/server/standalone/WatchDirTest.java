/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.standalone;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.server.watchdir.WatchDir;
import com.helger.as4.server.watchdir.WatchDir.IWatchDirCallback;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.commons.thread.ThreadHelper;

/**
 * Test class for class {@link WatchDir}.
 *
 * @author Philip Helger
 */
public final class WatchDirTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (WatchDirTest.class);

  @Test
  public void testWatchCurrent () throws IOException
  {
    // register directory and process its events
    final Path aDir = Paths.get (AS4ServerConfiguration.getDataPath ());
    final boolean bRecursive = true;

    final IWatchDirCallback aCB = (eAction, aPath) -> s_aLogger.info ("CB: " + eAction + " - " + aPath);

    try (WatchDir d = new WatchDir (aDir, bRecursive))
    {
      d.callbacks ().addCallback (aCB);
      new Thread ( () -> d.processEvents (), "WatchDir-" + d.getStartDirectory ()).start ();
      ThreadHelper.sleep (10, TimeUnit.SECONDS);
    }

    // Simplified
    try (WatchDir d = WatchDir.createAsyncRunningWatchDir (aDir, bRecursive, aCB))
    {
      ThreadHelper.sleep (10, TimeUnit.SECONDS);
    }
  }

  @Test
  public void test () throws IOException
  {
    // Starting WatchDir
    final IWatchDirCallback aCB = (eAction, aPath) -> s_aLogger.info ("CB: " + eAction + " - " + aPath);

    try (final WatchDir aWatch = WatchDir.createAsyncRunningWatchDir (Paths.get (AS4ServerConfiguration.getDataPath ()),
                                                                      false,
                                                                      aCB))
    {
      ThreadHelper.sleep (10, TimeUnit.SECONDS);
    }
  }
}
