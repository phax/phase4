package com.helger.as4server.settings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

@Immutable
public final class AS4ServerConfiguration
{
  private static final ConfigFile PROPS = new ConfigFileBuilder ().addPath ("private-as4.properties")
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

  public static String getDataPath ()
  {
    return PROPS.getAsString ("server.datapath", "conf");
  }
}
