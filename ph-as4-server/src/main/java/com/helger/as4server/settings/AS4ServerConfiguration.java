package com.helger.as4server.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

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
@Immutable
public final class AS4ServerConfiguration
{
  private static final ConfigFile PROPS = new ConfigFileBuilder ().addPathFromSystemProperty ("as4.server.configfile")
                                                                  .addPath ("private-as4.properties")
                                                                  .addPath ("as4.properties")
                                                                  .build ();

  private AS4ServerConfiguration ()
  {}

  @Nullable
  public static String getAS4Profile ()
  {
    return PROPS.getAsString ("server.profile");
  }

  public static boolean isGlobalDebug ()
  {
    return PROPS.getAsBoolean ("server.debug", true);
  }

  public static boolean isGlobalProduction ()
  {
    return PROPS.getAsBoolean ("server.production", false);
  }

  public static boolean hasStartupInfo ()
  {
    return PROPS.getAsBoolean ("server.nostartupinfo", false);
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "conf" relative to application startup directory
    return PROPS.getAsString ("server.datapath", "conf");
  }
}
