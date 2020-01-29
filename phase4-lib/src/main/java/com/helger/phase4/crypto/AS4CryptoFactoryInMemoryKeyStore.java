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
package com.helger.phase4.crypto;

import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * This class contains an implementation of {@link IAS4CryptoFactory} in which
 * case the {@link KeyStore} is available in memory and the settings are NOT
 * read from a file. Created for https://github.com/phax/phase4/issues/28
 *
 * @author Philip Helger
 * @since 0.9.7
 */
@Immutable
public class AS4CryptoFactoryInMemoryKeyStore implements IAS4CryptoFactory
{
  static
  {
    // Init once - must be present!
    WSSConfig.init ();
  }

  private final KeyStore m_aKeyStore;
  private final String m_sKeyAlias;
  private final String m_sKeyPassword;
  private final KeyStore m_aTrustStore;

  // Lazy initialized
  private Merlin m_aCrypto;
  private KeyStore.PrivateKeyEntry m_aPK;

  /**
   * Default constructor.
   *
   * @param aKeyStore
   *        The key store to be used. May not be <code>null</code>.
   * @param sKeyAlias
   *        The key alias to be used. May neither be <code>null</code> nor
   *        empty.
   * @param sKeyPassword
   *        The key password to be used. May not be <code>null</code> but maybe
   *        empty.
   * @param aTrustStore
   *        The optional trust store to be used. If none is provided the default
   *        Java runtime truststore (cacerts) is used.
   */
  public AS4CryptoFactoryInMemoryKeyStore (@Nonnull final KeyStore aKeyStore,
                                           @Nonnull @Nonempty final String sKeyAlias,
                                           @Nonnull final String sKeyPassword,
                                           @Nullable final KeyStore aTrustStore)
  {
    ValueEnforcer.notNull (aKeyStore, "KeyStore");
    ValueEnforcer.notEmpty (sKeyAlias, "KeyAlias");
    ValueEnforcer.notNull (sKeyPassword, "KeyPassword");
    m_aKeyStore = aKeyStore;
    m_sKeyAlias = sKeyAlias;
    m_sKeyPassword = sKeyPassword;
    m_aTrustStore = aTrustStore;
  }

  /**
   * Lazily create a {@link Crypto} instance using the key store and trust store
   * from the constructor.
   *
   * @return A {@link Crypto} instance and never <code>null</code>.
   */
  @Nonnull
  public final Crypto getCrypto ()
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

  @Nullable
  public final KeyStore.PrivateKeyEntry getPrivateKeyEntry ()
  {
    KeyStore.PrivateKeyEntry ret = m_aPK;
    if (ret == null)
    {
      ret = m_aPK = KeyStoreHelper.loadPrivateKey (m_aKeyStore,
                                                   "in-memory KeyStore",
                                                   m_sKeyAlias,
                                                   m_sKeyPassword.toCharArray ())
                                  .getKeyEntry ();
    }
    return ret;
  }

  @Nonnull
  public final String getKeyAlias ()
  {
    return m_sKeyAlias;
  }

  @Nonnull
  public final String getKeyPassword ()
  {
    return m_sKeyPassword;
  }

  @Nullable
  public final KeyStore getTrustStore ()
  {
    return m_aTrustStore;
  }
}
