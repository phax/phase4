/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
import com.helger.commons.io.resourceprovider.ReadableResourceProviderChain;
import com.helger.commons.string.StringParser;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
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
  /**
   * The boolean property to enable in-memory managers.
   */
  public static final String PROPERTY_PHASE4_MANAGER_INMEMORY = "phase4.manager.inmemory";
  public static final boolean DEFAULT_PHASE4_MANAGER_INMEMORY = true;

  /**
   * The boolean property to enable synchronization of sign/verify and
   * encrypt/decrypt.
   */
  public static final String PROPERTY_PHASE4_WSS4J_SYNCSECURITY = "phase4.wss4j.syncsecurity";
  public static final boolean DEFAULT_PHASE4_WSS4J_SYNCSECURITY = false;

  public static final long DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4Configuration.class);

  /**
   * The configuration value provider created in here uses the default lookup
   * scheme defined by {@link ConfigFactory#createDefaultValueProvider()} but
   * adds support for AS4 specific files. For a sustainable solution use one of
   * the following files that have higher precedence than
   * <code>application.properties</code>:
   * <ul>
   * <li>private-phase4.properties - priority 204</li>
   * <li>phase4.properties - priority 203</li>
   * </ul>
   *
   * @return The configuration value provider for phase4 that contains backward
   *         compatibility support.
   */
  @Nonnull
  public static MultiConfigurationValueProvider createPhase4ValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    final int nResourceDefaultPrio = EConfigSourceType.RESOURCE.getDefaultPriority ();
    final ReadableResourceProviderChain aResourceProvider = ConfigFactory.createDefaultResourceProviderChain ();

    // Phase4 files
    IReadableResource aRes = aResourceProvider.getReadableResourceIf ("private-phase4.properties",
                                                                      IReadableResource::exists);
    if (aRes != null)
    {
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8),
                                  nResourceDefaultPrio + 4);
    }

    aRes = aResourceProvider.getReadableResourceIf ("phase4.properties", IReadableResource::exists);
    if (aRes != null)
    {
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8),
                                  nResourceDefaultPrio + 3);
    }

    return ret;
  }

  private static final MultiConfigurationValueProvider VP = createPhase4ValueProvider ();
  private static final IConfigWithFallback DEFAULT_INSTANCE = new ConfigWithFallback (VP);
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  private static IConfigWithFallback s_aConfig = DEFAULT_INSTANCE;

  private AS4Configuration ()
  {}

  /**
   * @return The current global configuration. Never <code>null</code>.
   */
  @Nonnull
  public static IConfigWithFallback getConfig ()
  {
    // Inline for performance
    RW_LOCK.readLock ().lock ();
    try
    {
      return s_aConfig;
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
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
  public static IConfigWithFallback setConfig (@Nonnull final IConfigWithFallback aNewConfig)
  {
    ValueEnforcer.notNull (aNewConfig, "NewConfig");
    final IConfigWithFallback ret;
    RW_LOCK.writeLock ().lock ();
    try
    {
      ret = s_aConfig;
      s_aConfig = aNewConfig;
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }

    if (!EqualsHelper.identityEqual (ret, aNewConfig))
      LOGGER.info ("The phase4 configuration provider was changed to " + aNewConfig);
    return ret;
  }

  /**
   * @return <code>true</code> to enable the global debugging mode.
   */
  public static boolean isGlobalDebug ()
  {
    return getConfig ().getAsBoolean ("global.debug", false);
  }

  /**
   * @return <code>true</code> to enable the global production mode.
   */
  public static boolean isGlobalProduction ()
  {
    return getConfig ().getAsBoolean ("global.production", false);
  }

  /**
   * @return <code>true</code> if no startup info should be logged.
   */
  public static boolean isNoStartupInfo ()
  {
    return getConfig ().getAsBoolean ("global.nostartupinfo", true);
  }

  @Nonnull
  public static String getDataPath ()
  {
    // "phase4-data" relative to application startup directory
    return getConfig ().getAsString ("global.datapath", "phase4-data");
  }

  /**
   * @return Use in-memory managers, taken from the configuration item
   *         <code>phase4.manager.inmemory</code>. Defaults to <code>true</code>
   *         since 0.11.0.
   */
  public static boolean isUseInMemoryManagers ()
  {
    if (false)
    {
      // This should work, but doesn't
      return getConfig ().getAsBoolean (PROPERTY_PHASE4_MANAGER_INMEMORY, DEFAULT_PHASE4_MANAGER_INMEMORY);
    }

    // Parse manually
    final String sValue = getConfig ().getAsString (PROPERTY_PHASE4_MANAGER_INMEMORY);
    return StringParser.parseBool (sValue, DEFAULT_PHASE4_MANAGER_INMEMORY);
  }

  /**
   * @return <code>true</code> if all WSS4J actions should be explicitly
   *         synchronized. This is needed if multiple workers sending/receiving
   *         AS4 messages from the same JVM. The configuration item is
   *         <code>phase4.wss4j.syncsecurity</code>.
   */
  public static boolean isWSS4JSynchronizedSecurity ()
  {
    if (false)
    {
      // This should work, but doesn't in all cases
      return getConfig ().getAsBoolean (PROPERTY_PHASE4_WSS4J_SYNCSECURITY, DEFAULT_PHASE4_WSS4J_SYNCSECURITY);
    }

    // Parse manually
    final String sValue = getConfig ().getAsString (PROPERTY_PHASE4_WSS4J_SYNCSECURITY);
    return StringParser.parseBool (sValue, DEFAULT_PHASE4_WSS4J_SYNCSECURITY);
  }

  /**
   * @return The AS4 profile to use, taken from the configuration item
   *         <code>phase4.default.profile</code> (changed in 3.0.0; was
   *         <code>phase4.profile</code> before). May be <code>null</code>.
   */
  @Nullable
  public static String getDefaultAS4ProfileID ()
  {
    return getConfig ().getAsStringOrFallback ("phase4.default.profile", "phase4.profile");
  }

  /**
   * @return the number of minutes, the message IDs of incoming messages are
   *         stored for duplication check. Taken from the configuration item
   *         <code>phase4.incoming.duplicatedisposal.minutes</code>. By default
   *         this is {@value #DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES}
   *         minutes.
   */
  public static long getIncomingDuplicateDisposalMinutes ()
  {
    return getConfig ().getAsLong ("phase4.incoming.duplicatedisposal.minutes",
                                   DEFAULT_PHASE4_INCOMING_DUPLICATEDISPOSAL_MINUTES);
  }

  /**
   * @return The dumping base path. Taken from the configuration item
   *         <code>phase4.dump.path</code>.
   * @see #getDumpBasePathFile() for the same data as a {@link File}
   */
  @Nonnull
  public static String getDumpBasePath ()
  {
    // "phase4-dumps" relative to application startup directory
    return getConfig ().getAsString ("phase4.dump.path", "phase4-dumps");
  }

  /**
   * @return The dumping base path as a {@link File}. Taken from the
   *         configuration item <code>phase4.dump.path</code>.
   * @see #getDumpBasePath() for the plain String
   */
  @Nonnull
  public static File getDumpBasePathFile ()
  {
    return new File (getDumpBasePath ()).getAbsoluteFile ();
  }

  /**
   * @return The endpoint address of "our" AP for comparison. Taken from the
   *         configuration item <code>phase4.endpoint.address</code>. For Peppol
   *         this is e.g. required to have the `https` protocol in production
   *         mode.
   */
  @Nullable
  public static String getThisEndpointAddress ()
  {
    return getConfig ().getAsString ("phase4.endpoint.address");
  }

  /**
   * @return <code>true</code> if stack traces should be contained in error
   *         messages, <code>false</code> if not. Defaults to <code>true</code>.
   * @since 2.7.5
   */
  public static boolean isIncludeStackTraceInErrorMessages ()
  {
    // Defaults to true for backwards compatibility reason
    return getConfig ().getAsBoolean ("phase4.errormsg.include.stacktraces", true);
  }
}
