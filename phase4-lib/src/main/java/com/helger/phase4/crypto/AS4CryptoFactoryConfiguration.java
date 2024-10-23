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

import java.security.KeyStore.PrivateKeyEntry;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Merlin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * phase4 crypto factory settings based on {@link IConfig}. It can do the same
 * as {@link AS4CryptoFactoryProperties} except that the configuration elements
 * are solely taken from the global configuration and not from arbitrary files.
 * Multiple different crypto factory configurations can be handled uses
 * different configuration property prefixes. This class only supports
 * {@link Merlin} as the crypto implementation.<br>
 * Note: the default instance of this class should be a replacement for the
 * default instance of {@link AS4CryptoFactoryProperties}, except that the
 * support for the specific properties files
 * <code>private-crypto.properties</code> and <code>crypto.properties</code> was
 * removed.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
@SuppressWarnings ("javadoc")
@Immutable
public class AS4CryptoFactoryConfiguration extends AS4CryptoFactoryInMemoryKeyStore
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4CryptoFactoryConfiguration.class);
  private static final AS4CryptoFactoryConfiguration DEFAULT_INSTANCE = new AS4CryptoFactoryConfiguration (AS4Configuration.getConfig ());

  /**
   * @return The default instance, created by reading the default properties
   *         from the configuration sources.
   */
  @Nonnull
  public static AS4CryptoFactoryConfiguration getDefaultInstance ()
  {
    return DEFAULT_INSTANCE;
  }

  private final IAS4KeyStoreDescriptor m_aKeyStoreDesc;
  private final IAS4TrustStoreDescriptor m_aTrustStorDesc;

  /**
   * This constructor takes the configuration object and uses the default prefix
   * for backwards compatibility. This is kind of the default constructor.
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfigWithFallback aConfig)
  {
    this (aConfig, CAS4Crypto.DEFAULT_CONFIG_PREFIX);
  }

  @Nonnull
  private static IAS4KeyStoreDescriptor _loadKeyStore (@Nonnull final IConfigWithFallback aConfig,
                                                       @Nonnull @Nonempty final String sConfigPrefix)
  {
    final IAS4KeyStoreDescriptor aDescriptor = AS4KeyStoreDescriptor.createFromConfig (aConfig, sConfigPrefix, null);
    final LoadedKeyStore aLKS = aDescriptor.loadKeyStore ();
    if (aLKS.getKeyStore () == null)
    {
      LOGGER.error ("Failed to load the key store from the properties starting with '" +
                    sConfigPrefix +
                    "': " +
                    aLKS.getErrorText (Locale.ROOT));
    }
    else
    {
      final LoadedKey <PrivateKeyEntry> aLK = aDescriptor.loadKey ();
      if (aLK.getKeyEntry () == null)
      {
        LOGGER.error ("Failed to load the prvate key from the key store properties starting with '" +
                      sConfigPrefix +
                      "': " +
                      aLK.getErrorText (Locale.ROOT));
      }
    }
    return aDescriptor;
  }

  @Nullable
  private static IAS4TrustStoreDescriptor _loadTrustStore (@Nonnull final IConfigWithFallback aConfig,
                                                           @Nonnull @Nonempty final String sConfigPrefix)
  {
    // Load the trust store - may be null
    final IAS4TrustStoreDescriptor aDescriptor = AS4TrustStoreDescriptor.createFromConfig (aConfig,
                                                                                           sConfigPrefix,
                                                                                           null);
    if (aDescriptor != null)
    {
      final LoadedKeyStore aLTS = aDescriptor.loadTrustStore ();
      if (aLTS.getKeyStore () == null)
      {
        LOGGER.error ("Failed to load the trust store from the properties starting with '" +
                      sConfigPrefix +
                      "': " +
                      aLTS.getErrorText (Locale.ROOT));
      }
    }
    return aDescriptor;
  }

  /**
   * This constructor takes the configuration object and uses the provided
   * configuration prefix. This is kind of the default constructor.
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   * @param sConfigPrefix
   *        The configuration prefix to be used. May neither be
   *        <code>null</code> nor empty and must end with a dot ('.').
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfigWithFallback aConfig,
                                        @Nonnull @Nonempty final String sConfigPrefix)
  {
    this (_loadKeyStore (aConfig, sConfigPrefix), _loadTrustStore (aConfig, sConfigPrefix));
  }

  /**
   * Constructor using the key store and trust store descriptors.
   *
   * @param aKeyStoreDesc
   *        The key store descriptor. May not be <code>null</code>.
   * @param aTrustStoreDesc
   *        The trust store descriptor. May be <code>null</code> in which case
   *        the global JRE CA certs list will be used.
   */
  private AS4CryptoFactoryConfiguration (@Nonnull final IAS4KeyStoreDescriptor aKeyStoreDesc,
                                         @Nonnull final IAS4TrustStoreDescriptor aTrustStorDesc)
  {
    super (aKeyStoreDesc, aTrustStorDesc);
    m_aKeyStoreDesc = aKeyStoreDesc;
    m_aTrustStorDesc = aTrustStorDesc;
  }

  /**
   * @return The descriptor used to load the key store. Never <code>null</code>.
   */
  @Nonnull
  public IAS4KeyStoreDescriptor getKeyStoreDescriptor ()
  {
    return m_aKeyStoreDesc;
  }

  /**
   * @return The descriptor used to load the trust store. Never
   *         <code>null</code>.
   */
  @Nonnull
  public IAS4TrustStoreDescriptor getTrustStoreDescriptor ()
  {
    return m_aTrustStorDesc;
  }
}
