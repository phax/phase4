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
import com.helger.commons.string.ToStringGenerator;
import com.helger.security.keystore.IKeyStoreAndKeyDescriptor;
import com.helger.security.keystore.ITrustStoreDescriptor;

/**
 * This class contains an implementation of {@link IAS4CryptoFactory} in which
 * case the {@link KeyStore} is available in memory and the settings are NOT
 * read from a file. Created for https://github.com/phax/phase4/issues/28
 *
 * @author Philip Helger
 * @since 0.9.7
 */
@Immutable
public class AS4CryptoFactoryInMemoryKeyStore extends AbstractAS4CryptoFactory
{
  private final KeyStore m_aKeyStore;
  private final String m_sKeyAlias;
  private final char [] m_aKeyPassword;
  private final KeyStore m_aTrustStore;

  // Lazy initialized
  private Merlin m_aCrypto;

  /**
   * Constructor using the key store and trust store descriptors.
   *
   * @param aKeyStoreDesc
   *        The key store descriptor. May not be <code>null</code>.
   * @param aTrustStoreDesc
   *        The trust store descriptor. May be <code>null</code> in which case
   *        the global JRE CA certs list will be used.
   * @since 3.0.0
   */
  public AS4CryptoFactoryInMemoryKeyStore (@Nonnull final IKeyStoreAndKeyDescriptor aKeyStoreDesc,
                                           @Nullable final ITrustStoreDescriptor aTrustStoreDesc)
  {
    this (aKeyStoreDesc.loadKeyStore ().getKeyStore (),
          aKeyStoreDesc.getKeyAlias (),
          aKeyStoreDesc.getKeyPassword (),
          aTrustStoreDesc == null ? null : aTrustStoreDesc.loadTrustStore ().getKeyStore ());
  }

  /**
   * Default constructor.
   *
   * @param aKeyStore
   *        The key store to be used. May not be <code>null</code>.
   * @param sKeyAlias
   *        The key alias to be used. May neither be <code>null</code> nor
   *        empty.
   * @param aKeyPassword
   *        The key password to be used. May not be <code>null</code> but maybe
   *        empty.
   * @param aTrustStore
   *        The optional trust store to be used. If none is provided the default
   *        Java runtime truststore (cacerts) is used.
   */
  public AS4CryptoFactoryInMemoryKeyStore (@Nonnull final KeyStore aKeyStore,
                                           @Nonnull @Nonempty final String sKeyAlias,
                                           @Nonnull final char [] aKeyPassword,
                                           @Nullable final KeyStore aTrustStore)
  {
    ValueEnforcer.notNull (aKeyStore, "KeyStore");
    ValueEnforcer.notEmpty (sKeyAlias, "KeyAlias");
    ValueEnforcer.notNull (aKeyPassword, "KeyPassword");
    m_aKeyStore = aKeyStore;
    m_sKeyAlias = sKeyAlias;
    m_aKeyPassword = aKeyPassword;
    m_aTrustStore = aTrustStore;
  }

  /**
   * Lazily create a {@link Crypto} instance using the key store and trust store
   * from the constructor. Removed "final" in v3 to allow users to use a
   * different {@link Crypto} implementation if needed.
   */
  @Nonnull
  public Crypto getCrypto (@Nonnull final ECryptoMode eCryptoMode)
  {
    Merlin ret = m_aCrypto;
    if (ret == null)
    {
      // This constructor does not load anything from a file
      // Load cacerts only if no trust store is configured
      ret = m_aCrypto = new Merlin (m_aTrustStore == null, "changeit");
      ret.setKeyStore (m_aKeyStore);
      ret.setTrustStore (m_aTrustStore);
    }
    return ret;
  }

  @Nonnull
  public final KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  @Nonnull
  public final String getKeyAlias ()
  {
    return m_sKeyAlias;
  }

  @Nullable
  public char [] getKeyPasswordPerAliasCharArray (@Nullable final String sSearchKeyAlias)
  {
    // Use case insensitive compare, depends on the keystore type
    if (m_sKeyAlias != null && sSearchKeyAlias != null && m_sKeyAlias.equalsIgnoreCase (sSearchKeyAlias))
      return m_aKeyPassword;

    return null;
  }

  @Nullable
  public final KeyStore getTrustStore ()
  {
    return m_aTrustStore;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("KeyStore?", m_aKeyStore != null)
                                       .append ("KeyAlias", m_sKeyAlias)
                                       .appendPassword ("KeyPassword")
                                       .append ("TrustStore?", m_aTrustStore != null)
                                       .getToString ();
  }
}
