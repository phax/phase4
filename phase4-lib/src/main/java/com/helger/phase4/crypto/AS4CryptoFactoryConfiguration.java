/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import com.helger.commons.annotation.Nonempty;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.util.Phase4RuntimeException;
import com.helger.security.keystore.IKeyStoreAndKeyDescriptor;
import com.helger.security.keystore.ITrustStoreDescriptor;
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
@Immutable
public class AS4CryptoFactoryConfiguration extends AS4CryptoFactoryInMemoryKeyStore
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4CryptoFactoryConfiguration.class);

  /**
   * @return The default instance, created by reading the default properties
   *         from the configuration sources (application.properties, environment
   *         variables and Java system properties).
   * @throws Phase4RuntimeException
   *         if one of the mandatory configuration parameters is not present.
   */
  @Nonnull
  public static AS4CryptoFactoryConfiguration getDefaultInstance () throws Phase4RuntimeException
  {
    // Don't store this in a static variable, because it may fail if the
    // respective configuration properties are not present
    return new AS4CryptoFactoryConfiguration (AS4Configuration.getConfig (), CAS4Crypto.DEFAULT_CONFIG_PREFIX, false);
  }

  /**
   * Same as {@link #getDefaultInstance()} just that it returns
   * <code>null</code> instead of throwing a RuntimeException.
   *
   * @return <code>null</code> in case of error.
   */
  @Nullable
  public static AS4CryptoFactoryConfiguration getDefaultInstanceOrNull ()
  {
    try
    {
      return getDefaultInstance ();
    }
    catch (final Phase4RuntimeException ex)
    {
      // Use debug level only, as this is used in many default scenarios
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to create AS4CryptoFactoryConfiguration default instance", ex);
      return null;
    }
  }

  private final IKeyStoreAndKeyDescriptor m_aKeyStoreDesc;
  private final ITrustStoreDescriptor m_aTrustStorDesc;

  /**
   * This constructor takes the configuration object and uses the default prefix
   * for backwards compatibility. This is kind of the default constructor.
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   * @throws Phase4RuntimeException
   *         If loading the key store configuration from configuration fails.
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfigWithFallback aConfig) throws Phase4RuntimeException
  {
    this (aConfig, CAS4Crypto.DEFAULT_CONFIG_PREFIX);
  }

  @Nonnull
  private static IKeyStoreAndKeyDescriptor _loadKeyStore (@Nonnull final IConfigWithFallback aConfig,
                                                          @Nonnull @Nonempty final String sConfigPrefix,
                                                          final boolean bLogError) throws Phase4RuntimeException
  {
    // Load the keystore - may be null
    final IKeyStoreAndKeyDescriptor aDescriptor = AS4KeyStoreDescriptor.createFromConfig (aConfig, sConfigPrefix, null);
    if (aDescriptor == null)
    {
      final String sMsg = "Failed to load the key store configuration from properties starting with '" +
                          sConfigPrefix +
                          "'";
      if (bLogError)
        LOGGER.error (sMsg);
      throw new Phase4RuntimeException (sMsg);
    }

    final LoadedKeyStore aLKS = aDescriptor.loadKeyStore ();
    if (aLKS.getKeyStore () == null)
    {
      final String sMsg = "Failed to load the key store from the properties starting with '" +
                          sConfigPrefix +
                          "': " +
                          aLKS.getErrorText (Locale.ROOT);
      if (bLogError)
        LOGGER.error (sMsg);
      throw new Phase4RuntimeException (sMsg);
    }

    final LoadedKey <PrivateKeyEntry> aLK = aDescriptor.loadKey ();
    if (aLK.getKeyEntry () == null)
    {
      final String sMsg = "Failed to load the private key from the key store properties starting with '" +
                          sConfigPrefix +
                          "': " +
                          aLK.getErrorText (Locale.ROOT);
      if (bLogError)
        LOGGER.error (sMsg);
      throw new Phase4RuntimeException (sMsg);
    }

    return aDescriptor;
  }

  @Nullable
  private static ITrustStoreDescriptor _loadTrustStore (@Nonnull final IConfigWithFallback aConfig,
                                                        @Nonnull @Nonempty final String sConfigPrefix,
                                                        final boolean bLogError)
  {
    // Load the trust store - may be null
    final ITrustStoreDescriptor aDescriptor = AS4TrustStoreDescriptor.createFromConfig (aConfig, sConfigPrefix, null);
    if (aDescriptor != null)
    {
      final LoadedKeyStore aLTS = aDescriptor.loadTrustStore ();
      if (aLTS.getKeyStore () == null)
      {
        if (bLogError)
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
   * @throws Phase4RuntimeException
   *         If loading the key store configuration from configuration fails.
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfigWithFallback aConfig,
                                        @Nonnull @Nonempty final String sConfigPrefix) throws Phase4RuntimeException
  {
    // Log warning for backward compatibility reasons
    this (aConfig, sConfigPrefix, true);
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
   * @param bLogError
   *        <code>true</code> if errors should be logged if loading fails.
   * @throws Phase4RuntimeException
   *         If loading the key store configuration from configuration fails.
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfigWithFallback aConfig,
                                        @Nonnull @Nonempty final String sConfigPrefix,
                                        final boolean bLogError) throws Phase4RuntimeException
  {
    this (_loadKeyStore (aConfig, sConfigPrefix, bLogError), _loadTrustStore (aConfig, sConfigPrefix, bLogError));
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
  private AS4CryptoFactoryConfiguration (@Nonnull final IKeyStoreAndKeyDescriptor aKeyStoreDesc,
                                         @Nullable final ITrustStoreDescriptor aTrustStoreDesc)
  {
    super (aKeyStoreDesc, aTrustStoreDesc);
    m_aKeyStoreDesc = aKeyStoreDesc;
    m_aTrustStorDesc = aTrustStoreDesc;
  }

  /**
   * @return The descriptor used to load the key store. Never <code>null</code>.
   */
  @Nonnull
  public IKeyStoreAndKeyDescriptor getKeyStoreDescriptor ()
  {
    return m_aKeyStoreDesc;
  }

  /**
   * @return The descriptor used to load the trust store. Never
   *         <code>null</code>.
   */
  @Nonnull
  public ITrustStoreDescriptor getTrustStoreDescriptor ()
  {
    return m_aTrustStorDesc;
  }
}
