package com.helger.as4server.encrypt;

import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

public class CryptoConfigBuilder
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("crypto.properties").build ();
}
