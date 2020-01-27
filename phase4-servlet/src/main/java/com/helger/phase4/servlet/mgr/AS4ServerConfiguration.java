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
package com.helger.phase4.servlet.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.VisibleForTesting;
import com.helger.settings.ISettings;
import com.helger.settings.Settings;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * Manages the AS4 server configuration file. The files are read in the
 * following order:
 * <ol>
 * <li>Environment variable <code>PHASE4_SERVER_CONFIG</code></li>
 * <li>System property <code>phase4.server.configfile</code></li>
 * <li>System property <code>as4.server.configfile</code></li>
 * <li>private-phase4.properties</li>
 * <li>phase4.properties</li>
 * <li>private-as4.properties</li>
 * <li>as4.properties</li>
 * </ol>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class AS4ServerConfiguration
{
  public static final long DEFAULT_RESET_MINUTES = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ServerConfiguration.class);
  private static final Settings SETTINGS = new Settings ("phase4-server");
  private static boolean s_bUnitTestMode = false;

  public static void reinit (final boolean bForUnitTest)
  {
    s_bUnitTestMode = bForUnitTest;
    final ConfigFileBuilder aBuilder = new ConfigFileBuilder ();
    if (bForUnitTest)
    {
      aBuilder.addPathFromEnvVar ("PHASE4_SERVER_TEST_CONFIG")
              .addPathFromSystemProperty ("phase4.server.test.configfile")
              .addPath ("private-test-phase4.properties")
              .addPath ("test-phase4.properties");
    }
    else
    {
      aBuilder.addPathFromEnvVar ("PHASE4_SERVER_CONFIG")
              .addPathFromSystemProperty ("phase4.server.configfile")
              .addPathFromSystemProperty ("as4.server.configfile")
              .addPath ("private-phase4.properties")
              .addPath ("phase4.properties")
              .addPath ("private-as4.properties")
              .addPath ("as4.properties");
    }
    final ConfigFile aCF = aBuilder.build ();
    if (!aCF.isRead ())
      LOGGER.warn ("Failed to read phase4 server configuration file! All values will be default values");
    else
      LOGGER.info ("Successfully read phase4 configuration file from " + aCF.getReadResource ().getPath ());
    // Can handle null
    SETTINGS.setAll (aCF.getSettings ());
  }

  @VisibleForTesting
  public static void internalReinitForTestOnly ()
  {
    // Re-read the config file with precedence to "private-test-as4.properties"
    // file but only if it wasn't read in test mode before.
    // This is necessary to avoid that the dynamic properties from the test
    // suites are overwritten with each new test
    if (!s_bUnitTestMode)
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
  public static String getAS4ProfileID ()
  {
    return getSettings ().getAsString ("server.profile");
  }

  public static boolean isGlobalDebug ()
  {
    return getSettings ().getAsBoolean ("server.debug", false);
  }

  public static boolean isGlobalProduction ()
  {
    return getSettings ().getAsBoolean ("server.production", false);
  }

  public static boolean isNoStartupInfo ()
  {
    return getSettings ().getAsBoolean ("server.nostartupinfo", true);
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "conf" relative to application startup directory
    return getSettings ().getAsString ("server.datapath", "conf");
  }

  /**
   * @return the number of minutes, the message IDs of incoming messages are
   *         stored for duplication check. By default this is
   *         {@value #DEFAULT_RESET_MINUTES} minutes.
   */
  public static long getIncomingDuplicateDisposalMinutes ()
  {
    return getSettings ().getAsLong ("server.incoming.duplicatedisposal.minutes", DEFAULT_RESET_MINUTES);
  }

  @Nullable
  public static String getServerAddress ()
  {
    return getSettings ().getAsString ("server.address");
  }
}
