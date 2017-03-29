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
package com.helger.as4.servlet.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.exception.InitializationException;
import com.helger.settings.ISettings;
import com.helger.settings.Settings;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * Manages the AS4 server configuration file. The files are read in the
 * following order:
 * <ol>
 * <li>System property <code>as4.server.configfile</code></li>
 * <li>private-as4.properties</li>
 * <li>as4.properties</li>
 * </ol>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class AS4ServerConfiguration
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4ServerConfiguration.class);
  private static final Settings SETTINGS = new Settings ("as4-server");
  private static boolean s_bTestMode = false;
  private static final long DEFAULT_RESET_MINUTES = 10;

  public static void reinit (final boolean bForTest)
  {
    s_bTestMode = bForTest;
    final ConfigFileBuilder aBuilder = new ConfigFileBuilder ();
    if (bForTest)
    {
      aBuilder.addPathFromSystemProperty ("as4.server.test.configfile")
              .addPath ("private-test-as4.properties")
              .addPath ("test-as4.properties");
    }
    else
    {
      aBuilder.addPathFromSystemProperty ("as4.server.configfile")
              .addPath ("private-as4.properties")
              .addPath ("as4.properties");
    }
    final ConfigFile aCF = aBuilder.build ();
    if (!aCF.isRead ())
      throw new InitializationException ("Failed to read AS4 server configuration file!");
    s_aLogger.info ("Successfully read AS4 configuration file from " + aCF.getReadResource ().getPath ());
    SETTINGS.clear ();
    SETTINGS.setValues (aCF.getSettings ());
  }

  @VisibleForTesting
  public static void internalReinitForTestOnly ()
  {
    // Re-read the config file with precedence to "private-test-as4.properties"
    // file but only if it wasn't read in test mode before.
    // This is necessary to avoid that the dymnamic properties from the test
    // suites are overwritten with each new test
    if (!s_bTestMode)
      reinit (true);
  }

  static
  {
    reinit (false);
  }

  private AS4ServerConfiguration ()
  {}

  @Nonnull
  public static ISettings getSettings ()
  {
    return SETTINGS;
  }

  @Nonnull
  public static Settings getMutableSettings ()
  {
    return SETTINGS;
  }

  @Nullable
  public static String getAS4ProfileName ()
  {
    return getSettings ().getAsString ("server.profile");
  }

  @Nonnull
  public static boolean isGlobalDebug ()
  {
    return getSettings ().getAsBoolean ("server.debug", true);
  }

  @Nonnull
  public static boolean isGlobalProduction ()
  {
    return getSettings ().getAsBoolean ("server.production", false);
  }

  @Nonnull
  public static boolean isNoStartupInfo ()
  {
    return getSettings ().getAsBoolean ("server.nostartupinfo", false);
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "conf" relative to application startup directory
    return getSettings ().getAsString ("server.datapath", "conf");
  }

  public static long getIncomingDuplicateDisposalMinutes ()
  {
    final String sFieldName = "server.incoming.duplicatedisposal.minutes";
    return getSettings ().getAsLong (sFieldName, DEFAULT_RESET_MINUTES);
  }

  public static String getResponderAddress ()
  {
    return getSettings ().getAsString ("server.address");
  }
}
