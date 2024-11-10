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
package com.helger.phase4.crypto;

import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreAndKeyDescriptor;

/**
 * A specific helper for {@link KeyStoreAndKeyDescriptor}
 *
 * @author Philip Helger
 * @since 3.0.0
 */
@Immutable
public final class AS4KeyStoreDescriptor
{
  private AS4KeyStoreDescriptor ()
  {}

  /**
   * Create the key store descriptor from the default configuration item. The
   * following configuration properties are used, relative to the default
   * configuration prefix:
   * <ul>
   * <li><code>keystore.type</code> - the key store type</li>
   * <li><code>keystore.file</code> - the key store path</li>
   * <li><code>keystore.password</code> - the key store password</li>
   * <li><code>keystore.alias</code> - the key store alias</li>
   * <li><code>keystore.private.password</code> - the key store key
   * password</li>
   * </ul>
   *
   * @return A new {@link KeyStoreAndKeyDescriptor} object and never
   *         <code>null</code>.
   */
  @Nonnull
  public static KeyStoreAndKeyDescriptor createFromConfig ()
  {
    return createFromConfig (AS4Configuration.getConfig (), CAS4Crypto.DEFAULT_CONFIG_PREFIX, null);
  }

  /**
   * Create the key store descriptor from the provided configuration item. The
   * following configuration properties are used, relative to the configuration
   * prefix:
   * <ul>
   * <li><code>keystore.type</code> - the key store type</li>
   * <li><code>keystore.file</code> - the key store path</li>
   * <li><code>keystore.password</code> - the key store password</li>
   * <li><code>keystore.alias</code> - the key store alias</li>
   * <li><code>keystore.private.password</code> - the key store key
   * password</li>
   * </ul>
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   * @param sConfigPrefix
   *        The configuration prefix to be used. May neither be
   *        <code>null</code> nor empty and must end with a dot ('.').
   * @param aProvider
   *        The Java security provider for loading the key store. May be
   *        <code>null</code> to use the default.
   * @return A new {@link KeyStoreAndKeyDescriptor} object and never
   *         <code>null</code>.
   */
  @Nonnull
  public static KeyStoreAndKeyDescriptor createFromConfig (@Nonnull final IConfigWithFallback aConfig,
                                                           @Nonnull @Nonempty final String sConfigPrefix,
                                                           @Nullable final Provider aProvider)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigPrefix, "ConfigPrefix");
    ValueEnforcer.isTrue ( () -> StringHelper.endsWith (sConfigPrefix, '.'), "ConfigPrefix must end with a dot");

    // Key Store
    final String sType = aConfig.getAsString (sConfigPrefix + "keystore.type");
    final EKeyStoreType aType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType,
                                                                                 CAS4Crypto.DEFAULT_KEY_STORE_TYPE);
    final String sPath = aConfig.getAsString (sConfigPrefix + "keystore.file");
    final char [] aPassword = aConfig.getAsCharArray (sConfigPrefix + "keystore.password");

    // Key Store Key
    final String sKeyAlias = aConfig.getAsString (sConfigPrefix + "keystore.alias");
    final char [] aKeyPassword = aConfig.getAsCharArray (sConfigPrefix + "keystore.private.password");

    return new KeyStoreAndKeyDescriptor (aType, sPath, aPassword, aProvider, sKeyAlias, aKeyPassword);
  }
}
