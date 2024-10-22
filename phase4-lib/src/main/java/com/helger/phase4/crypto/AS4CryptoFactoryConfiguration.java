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

import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.phase4.config.AS4Configuration;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;

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
public class AS4CryptoFactoryConfiguration extends AbstractAS4CryptoFactory
{
  public static final EKeyStoreType DEFAULT_KEYSTORE_TYPE = EKeyStoreType.JKS;
  public static final EKeyStoreType DEFAULT_TRUSTSTORE_TYPE = EKeyStoreType.JKS;

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

  private final EKeyStoreType m_eKeyStoreType;
  private final String m_sKeyStorePath;
  private final String m_sKeyStorePassword;

  private final String m_sKeyAlias;
  private final String m_sKeyPassword;

  private final EKeyStoreType m_eTrustStoreType;
  private final String m_sTrustStorePath;
  private final String m_sTrustStorePassword;

  // Lazy initialized
  private Merlin m_aCrypto;
  private KeyStore m_aKeyStore;
  private KeyStore m_aTrustStore;

  /**
   * This constructor takes the configuration object and uses the default prefix
   * for backwards compatibility. This is kind of the default constructor.
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   */
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfig aConfig)
  {
    this (aConfig, "org.apache.wss4j.crypto.merlin.");
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
  public AS4CryptoFactoryConfiguration (@Nonnull final IConfig aConfig, @Nonnull @Nonempty final String sConfigPrefix)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigPrefix, "ConfigPrefix");
    ValueEnforcer.isTrue ( () -> StringHelper.endsWith (sConfigPrefix, '.'), "ConfigPrefix must end with a dot");

    // Key Store
    m_eKeyStoreType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (aConfig.getAsString (sConfigPrefix +
                                                                                            "keystore.type"),
                                                                       DEFAULT_KEYSTORE_TYPE);
    m_sKeyStorePath = aConfig.getAsString (sConfigPrefix + "keystore.file");
    m_sKeyStorePassword = aConfig.getAsString (sConfigPrefix + "keystore.password");

    // Key Store Key
    m_sKeyAlias = aConfig.getAsString (sConfigPrefix + "keystore.alias");
    m_sKeyPassword = aConfig.getAsString (sConfigPrefix + "keystore.private.password");

    // Trust Store
    m_eTrustStoreType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (aConfig.getAsString (sConfigPrefix +
                                                                                              "truststore.type"),
                                                                         DEFAULT_KEYSTORE_TYPE);
    m_sTrustStorePath = aConfig.getAsString (sConfigPrefix + "truststore.file");
    m_sTrustStorePassword = aConfig.getAsString (sConfigPrefix + "truststore.password");
  }

  /**
   * Helper method to create a WSS4J {@link Merlin} instance based on the
   * configured keystore and truststore.
   *
   * @return A new {@link Merlin} object.
   * @throws IllegalStateException
   *         if creation failed
   */
  @Nonnull
  public Merlin createMerlin ()
  {
    try
    {
      final Merlin ret = new Merlin ();
      ret.setKeyStore (getKeyStore ());
      ret.setTrustStore (getTrustStore ());
      return ret;
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to create Merlin object", ex);
    }
  }

  /**
   * Lazily create a {@link Merlin} instance using the configured keystore and
   * truststore.
   */
  @Nonnull
  public final Crypto getCrypto (@Nonnull final ECryptoMode eCryptoMode)
  {
    Merlin ret = m_aCrypto;
    if (ret == null)
    {
      // Create only once and cache
      ret = m_aCrypto = createMerlin ();
    }
    return ret;
  }

  @Nullable
  public final KeyStore getKeyStore ()
  {
    KeyStore ret = m_aKeyStore;
    if (ret == null)
    {
      ret = m_aKeyStore = KeyStoreHelper.loadKeyStore (m_eKeyStoreType, m_sKeyStorePath, m_sKeyStorePassword)
                                        .getKeyStore ();
    }
    return ret;
  }

  @Nullable
  public final String getKeyAlias ()
  {
    return m_sKeyAlias;
  }

  @Nullable
  public String getKeyPasswordPerAlias (@Nullable final String sSearchKeyAlias)
  {
    final String sKeyAlias = m_sKeyAlias;

    // Use case insensitive compare, depends on the keystore type
    if (sKeyAlias != null && sSearchKeyAlias != null && sKeyAlias.equalsIgnoreCase (sSearchKeyAlias))
      return m_sKeyPassword;

    return null;
  }

  @Nullable
  public final KeyStore getTrustStore ()
  {
    KeyStore ret = m_aTrustStore;
    if (ret == null)
    {
      // Load only once and cache then
      ret = m_aTrustStore = KeyStoreHelper.loadKeyStore (m_eTrustStoreType, m_sTrustStorePath, m_sTrustStorePassword)
                                          .getKeyStore ();
    }
    return ret;
  }
}
