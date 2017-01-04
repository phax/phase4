package com.helger.as4server.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

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
  private static final Settings SETTINGS = new Settings ("as4-server");

  public static void reinit (final boolean bForTest)
  {
    final ConfigFileBuilder aBuilder = new ConfigFileBuilder ();
    if (bForTest)
      aBuilder.addPath ("private-test-as4.properties");
    final ConfigFile aCF = aBuilder.addPathFromSystemProperty ("as4.server.configfile")
                                   .addPath ("private-as4.properties")
                                   .addPath ("as4.properties")
                                   .build ();
    if (!aCF.isRead ())
      throw new InitializationException ("Failed to read AS4 server configuration file!");
    SETTINGS.clear ();
    SETTINGS.setValues (aCF.getSettings ());
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
  public static String getAS4Profile ()
  {
    return getSettings ().getAsString ("server.profile");
  }

  public static boolean isGlobalDebug ()
  {
    return getSettings ().getAsBoolean ("server.debug", true);
  }

  public static boolean isGlobalProduction ()
  {
    return getSettings ().getAsBoolean ("server.production", false);
  }

  public static boolean hasStartupInfo ()
  {
    return getSettings ().getAsBoolean ("server.nostartupinfo", false);
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "conf" relative to application startup directory
    return getSettings ().getAsString ("server.datapath", "conf");
  }
}
