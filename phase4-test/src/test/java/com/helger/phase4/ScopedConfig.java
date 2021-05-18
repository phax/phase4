/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.config.Config;
import com.helger.config.IConfig;
import com.helger.config.source.EConfigSourceType;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.config.source.res.ConfigurationSourceProperties;
import com.helger.phase4.config.AS4Configuration;

/**
 * Helper class to apply a certain {@link IConfig} for a certain code area. Use
 * via the try-with-resources idiom.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class ScopedConfig implements AutoCloseable
{
  private static final String TEST_CONFIG_FILE = "src/test/resources/test-phase4.properties";

  private final IConfig m_aOldConfig;

  private ScopedConfig (@Nonnull final IConfig aConfig)
  {
    m_aOldConfig = AS4Configuration.setConfig (aConfig);
  }

  public void close ()
  {
    // Restore the old config
    AS4Configuration.setConfig (m_aOldConfig);
  }

  @Nonnull
  public static ScopedConfig create (@Nonnull final IStringMap aMap)
  {
    final MultiConfigurationValueProvider aVP = AS4Configuration.createPhase4ValueProvider ();
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aMap::getAsString), EConfigSourceType.RESOURCE.getDefaultPriority () + 20);
    return new ScopedConfig (new Config (aVP));
  }

  @Nonnull
  public static ScopedConfig create (@Nonnull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Res");
    ValueEnforcer.isTrue (aRes.exists (), () -> "Resource does not exist: " + aRes);

    final MultiConfigurationValueProvider aVP = AS4Configuration.createPhase4ValueProvider ();
    // By default priority must be higher than the default
    aVP.addConfigurationSource (new ConfigurationSourceProperties (aRes), EConfigSourceType.RESOURCE.getDefaultPriority () + 10);
    return new ScopedConfig (new Config (aVP));
  }

  @Nonnull
  public static ScopedConfig createTestConfig ()
  {
    return create (new FileSystemResource (TEST_CONFIG_FILE));
  }

  @Nonnull
  public static ScopedConfig createTestConfig (@Nonnull final IStringMap aMap)
  {
    final MultiConfigurationValueProvider aVP = AS4Configuration.createPhase4ValueProvider ();
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aMap::getAsString), EConfigSourceType.RESOURCE.getDefaultPriority () + 20);
    aVP.addConfigurationSource (new ConfigurationSourceProperties (new FileSystemResource (TEST_CONFIG_FILE)),
                                EConfigSourceType.RESOURCE.getDefaultPriority () + 10);
    return new ScopedConfig (new Config (aVP));
  }
}
