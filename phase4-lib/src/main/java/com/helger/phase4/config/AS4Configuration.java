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
package com.helger.phase4.config;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resourceprovider.ClassPathResourceProvider;
import com.helger.commons.io.resourceprovider.FileSystemResourceProvider;
import com.helger.commons.io.resourceprovider.ReadableResourceProviderChain;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.config.Config;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.source.EConfigSourceType;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.res.ConfigurationSourceProperties;

/**
 * This class contains the central phase4 configuration. <br>
 * Note: this class should not depend on any other phase4 class to avoid startup
 * issues, and cyclic dependencies.
 *
 * @author Philip Helger
 * @since 0.11.0
 */
public final class AS4Configuration
{
  public static final long DEFAULT_RESET_MINUTES = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4Configuration.class);

  static
  {
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("phase4.server.configfile")))
      LOGGER.error ("The system property 'phase4.server.configfile' is no longer supported. See https://github.com/phax/ph-commons#ph-config for alternatives.");
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("as4.server.configfile")))
      LOGGER.error ("The system property 'as4.server.configfile' is no longer supported. See https://github.com/phax/ph-commons#ph-config for alternatives.");
    if (StringHelper.hasText (System.getenv ().get ("PHASE4_SERVER_CONFIG")))
      LOGGER.error ("The environment variable 'PHASE4_SERVER_CONFIG' is no longer supported. See https://github.com/phax/ph-commons#ph-config for alternatives.");
  }

  /**
   * @return The configuration value provider for phase4 that contains backward
   *         compatibility support.
   */
  @Phase4V1Tasks
  public static MultiConfigurationValueProvider createPhase4ValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    final int nResourceDefaultPrio = EConfigSourceType.RESOURCE.getDefaultPriority ();
    final ReadableResourceProviderChain aResourceProvider = new ReadableResourceProviderChain (new FileSystemResourceProvider ().setCanReadRelativePaths (true),
                                                                                               new ClassPathResourceProvider ());

    IReadableResource aRes;

    aRes = aResourceProvider.getReadableResourceIf ("private-phase4.properties", IReadableResource::exists);
    if (aRes != null)
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 4);

    aRes = aResourceProvider.getReadableResourceIf ("phase4.properties", IReadableResource::exists);
    if (aRes != null)
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 3);

    // Remove for 1.0
    aRes = aResourceProvider.getReadableResourceIf ("private-as4.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'private-as4.properties' is deprecated and will be removed for the 1.0 release. Use 'phase4.properties' or 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 2);
    }

    // Remove for 1.0
    aRes = aResourceProvider.getReadableResourceIf ("as4.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'as4.properties' is deprecated and will be removed for the 1.0 release. Use 'phase4.properties' or 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 1);
    }

    return ret;
  }

  private static final MultiConfigurationValueProvider VP = createPhase4ValueProvider ();
  private static final IConfig DEFAULT_INSTANCE = Config.create (VP);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  private static IConfig s_aConfig = DEFAULT_INSTANCE;

  private AS4Configuration ()
  {}

  /**
   * @return The current configuration. Never <code>null</code>.
   */
  @Nonnull
  public static IConfig getConfig ()
  {
    // Inline for performance
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aConfig;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * Overwrite the global configuration. This is only needed for testing.
   *
   * @return The old value of {@link IConfig}. Never <code>null</code>.
   */
  @Nonnull
  public static IConfig setConfig (@Nonnull final IConfig aNewConfig)
  {
    ValueEnforcer.notNull (aNewConfig, "NewConfig");
    final IConfig ret;
    s_aRWLock.writeLock ().lock ();
    try
    {
      ret = s_aConfig;
      s_aConfig = aNewConfig;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }

    if (!EqualsHelper.identityEqual (ret, aNewConfig))
      LOGGER.info ("The phase4 configuration provider was changed to " + aNewConfig);
    return ret;
  }

  public static boolean isGlobalDebug ()
  {
    final Boolean ret = getConfig ().getAsBooleanObj ("server.debug");
    if (ret != null)
    {
      LOGGER.warn ("Please change the configuration property 'server.debug' to 'global.debug'");
      return ret.booleanValue ();
    }
    return getConfig ().getAsBoolean ("gobal.debug", false);
  }

  public static boolean isGlobalProduction ()
  {
    final Boolean ret = getConfig ().getAsBooleanObj ("server.production");
    if (ret != null)
    {
      LOGGER.warn ("Please change the configuration property 'server.production' to 'global.production'");
      return ret.booleanValue ();
    }
    return getConfig ().getAsBoolean ("gobal.production", false);
  }

  public static boolean isNoStartupInfo ()
  {
    return getConfig ().getAsBoolean ("server.nostartupinfo", true);
  }

  public static boolean isUseInMemoryManagers ()
  {
    return getConfig ().getAsBoolean ("phase4.manager.inmemory", false);
  }

  @Nullable
  public static String getAS4ProfileID ()
  {
    return getConfig ().getAsString ("server.profile");
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "conf" relative to application startup directory
    return getConfig ().getAsString ("server.datapath", "conf");
  }

  @Nonnull
  public static String getDumpBasePath ()
  {
    // "conf" relative to application startup directory
    return getConfig ().getAsString ("server.datapath", "conf");
  }

  @Nonnull
  public static File getDumpBasePathFile ()
  {
    return new File (getDumpBasePath ()).getAbsoluteFile ();
  }

  /**
   * @return the number of minutes, the message IDs of incoming messages are
   *         stored for duplication check. By default this is
   *         {@value #DEFAULT_RESET_MINUTES} minutes.
   */
  public static long getIncomingDuplicateDisposalMinutes ()
  {
    return getConfig ().getAsLong ("server.incoming.duplicatedisposal.minutes", DEFAULT_RESET_MINUTES);
  }

  @Nullable
  public static String getThisEndpointAddress ()
  {
    return getConfig ().getAsString ("server.address");
  }
}
