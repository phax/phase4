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
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resourceprovider.ClassPathResourceProvider;
import com.helger.commons.io.resourceprovider.FileSystemResourceProvider;
import com.helger.commons.io.resourceprovider.ReadableResourceProviderChain;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
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
@Phase4V1Tasks
public final class AS4Configuration
{
  /**
   * The boolean property to enable in-memory managers.
   */
  public static final String PROPERTY_PHASE4_MANAGER_INMEMORY = "phase4.manager.inmemory";
  /**
   * The boolean property to enable synchronization of sign/verify and
   * encrypt/decrypt.
   */
  public static final String PROPERTY_PHASE4_WSS4J_SYNCSECURITY = "phase4.wss4j.syncsecurity";

  public static final long DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4Configuration.class);

  static
  {
    // Since 0.11.0 - remove in 1.0
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("phase4.server.configfile")))
      throw new InitializationException ("The system property 'phase4.server.configfile' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("as4.server.configfile")))
      throw new InitializationException ("The system property 'as4.server.configfile' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (System.getenv ().get ("PHASE4_SERVER_CONFIG")))
      throw new InitializationException ("The environment variable 'PHASE4_SERVER_CONFIG' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");
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

    // Remove for 1.0
    aRes = aResourceProvider.getReadableResourceIf ("private-crypto.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'private-crypto.properties' is deprecated and will be removed for the 1.0 release. Place the properties in 'phase4.properties' or 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 6);
    }

    // Remove for 1.0
    aRes = aResourceProvider.getReadableResourceIf ("crypto.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'crypto.properties' is deprecated and will be removed for the 1.0 release. Place the properties in 'phase4.properties' or 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 5);
    }

    // Phase 4 files
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
      LOGGER.warn ("The support for the properties file 'private-as4.properties' is deprecated and will be removed for the 1.0 release. Place the properties in 'phase4.properties' or 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nResourceDefaultPrio + 2);
    }

    // Remove for 1.0
    aRes = aResourceProvider.getReadableResourceIf ("as4.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'as4.properties' is deprecated and will be removed for the 1.0 release. Place the properties in 'phase4.properties' or 'application.properties' instead.");
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
   * @return The current global configuration. Never <code>null</code>.
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
   * @param aNewConfig
   *        The configuration to use globally. May not be <code>null</code>.
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

  private static void _logRenamedConfig (@Nonnull final String sOld, @Nonnull final String sNew)
  {
    LOGGER.warn ("Please rename the configuration property '" +
                 sOld +
                 "' to '" +
                 sNew +
                 "'. Support for the old property name will be removed in v1.0.");
  }

  /**
   * @return <code>true</code> to enable the global debugging mode.
   */
  @Phase4V1Tasks
  public static boolean isGlobalDebug ()
  {
    final Boolean ret = getConfig ().getAsBooleanObj ("server.debug");
    if (ret != null)
    {
      _logRenamedConfig ("server.debug", "global.debug");
      return ret.booleanValue ();
    }
    return getConfig ().getAsBoolean ("global.debug", false);
  }

  /**
   * @return <code>true</code> to enable the global production mode.
   */
  @Phase4V1Tasks
  public static boolean isGlobalProduction ()
  {
    final Boolean ret = getConfig ().getAsBooleanObj ("server.production");
    if (ret != null)
    {
      _logRenamedConfig ("server.production", "global.production");
      return ret.booleanValue ();
    }
    return getConfig ().getAsBoolean ("global.production", false);
  }

  /**
   * @return <code>true</code> if no startup info should be logged.
   */
  @Phase4V1Tasks
  public static boolean isNoStartupInfo ()
  {
    final Boolean ret = getConfig ().getAsBooleanObj ("server.nostartupinfo");
    if (ret != null)
    {
      _logRenamedConfig ("server.nostartupinfo", "global.nostartupinfo");
      return ret.booleanValue ();
    }
    return getConfig ().getAsBoolean ("global.nostartupinfo", true);
  }

  @Nonnull
  public static String getDataPath ()
  {
    final String ret = getConfig ().getAsString ("server.datapath");
    if (StringHelper.hasText (ret))
    {
      _logRenamedConfig ("server.datapath", "global.datapath");
      return ret;
    }
    // "phase4-data" relative to application startup directory
    return getConfig ().getAsString ("global.datapath", "phase4-data");
  }

  /**
   * @return Use in-memory managers? Defaults to <code>true</code> since 0.11.0.
   */
  public static boolean isUseInMemoryManagers ()
  {
    if (false)
    {
      // This should work, but doesn't
      return getConfig ().getAsBoolean (PROPERTY_PHASE4_MANAGER_INMEMORY, true);
    }

    // Parse manually
    final String sValue = getConfig ().getAsString (PROPERTY_PHASE4_MANAGER_INMEMORY);
    return StringParser.parseBool (sValue, true);
  }

  public static boolean isWSS4JSynchronizedSecurity ()
  {
    if (false)
    {
      // This should work, but doesn't in all cases
      return getConfig ().getAsBoolean (PROPERTY_PHASE4_WSS4J_SYNCSECURITY, false);
    }

    // Parse manually
    final String sValue = getConfig ().getAsString (PROPERTY_PHASE4_WSS4J_SYNCSECURITY);
    return StringParser.parseBool (sValue, false);
  }

  @Nullable
  @Phase4V1Tasks
  public static String getAS4ProfileID ()
  {
    final String ret = getConfig ().getAsString ("server.profile");
    if (StringHelper.hasText (ret))
    {
      _logRenamedConfig ("server.profile", "phase4.profile");
      return ret;
    }
    return getConfig ().getAsString ("phase4.profile");
  }

  /**
   * @return the number of minutes, the message IDs of incoming messages are
   *         stored for duplication check. By default this is
   *         {@value #DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES}
   *         minutes.
   */
  @Phase4V1Tasks
  public static long getIncomingDuplicateDisposalMinutes ()
  {
    final Long ret = getConfig ().getAsLongObj ("server.incoming.duplicatedisposal.minutes");
    if (ret != null)
    {
      _logRenamedConfig ("server.incoming.duplicatedisposal.minutes", "phase4.incoming.duplicatedisposal.minutes");
      return ret.longValue ();
    }
    return getConfig ().getAsLong ("phase4.incoming.duplicatedisposal.minutes", DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES);
  }

  @Nonnull
  @Phase4V1Tasks
  public static String getDumpBasePath ()
  {
    String ret = getConfig ().getAsString ("phase4.dump.path");
    if (StringHelper.hasNoText (ret))
    {
      // Check without default here
      ret = getConfig ().getAsString ("server.datapath");
      if (StringHelper.hasText (ret))
        LOGGER.warn ("Since 0.11.0 the base path to dump files can be configured globally via the property 'phase4.dump.path'." +
                     " For backwards compatibility this value is currently taken from the property 'server.datapath'." +
                     " This fallback mechanism will be removed for the 1.0 release.");
    }
    if (StringHelper.hasNoText (ret))
      ret = "phase4-dumps";
    return ret;
  }

  @Nonnull
  public static File getDumpBasePathFile ()
  {
    return new File (getDumpBasePath ()).getAbsoluteFile ();
  }

  @Nullable
  @Phase4V1Tasks
  public static String getThisEndpointAddress ()
  {
    final String ret = getConfig ().getAsString ("server.address");
    if (StringHelper.hasText (ret))
    {
      _logRenamedConfig ("server.address", "phase4.endpoint.address");
      return ret;
    }
    return getConfig ().getAsString ("phase4.endpoint.address");
  }
}
