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

import org.apache.wss4j.common.crypto.Crypto;

/**
 * The basic phase4 crypto interface.
 * <ul>
 * <li>See {@link AS4CryptoFactoryConfiguration} for an implementation of this
 * interface based on the global configuration</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 0.9.7
 */
public interface IAS4CryptoFactory
{
  /**
   * @param eCryptoMode
   *        The crypto mode to use. Never <code>null</code>.
   * @return A WSS4J {@link Crypto} instance and never <code>null</code>.
   */
  @Nonnull
  Crypto getCrypto (@Nonnull ECryptoMode eCryptoMode);

  /**
   * @return The underlying key store, or <code>null</code> if none is available
   *         (the reasons depend on the used implementation).
   */
  @Nullable
  KeyStore getKeyStore ();

  /**
   * @return The underlying private key entry from the keystore or
   *         <code>null</code> if none is available (the reasons depend on the
   *         used implementation).
   */
  @Nullable
  KeyStore.PrivateKeyEntry getPrivateKeyEntry ();

  /**
   * @return The keystore alias to resolve the private key entry. May be
   *         <code>null</code>.
   */
  @Nullable
  String getKeyAlias ();

  /**
   * Returns the password for the key represented by the provided alias.
   *
   * @param sSearchKeyAlias
   *        The alias of the key whose password is to be retrieved.
   * @return The password for the key represented by the provided by the alias
   *         or <code>null</code> if the factory doesn't have a password for the
   *         key.
   * @since 3.0.0
   */
  @Nullable
  char [] getKeyPasswordPerAliasCharArray (@Nullable String sSearchKeyAlias);

  /**
   * Returns the password for the key represented by the provided alias.
   *
   * @param sSearchKeyAlias
   *        The alias of the key whose password is to be retrieved.
   * @return The password for the key represented by the provided by the alias
   *         or <code>null</code> if the factory doesn't have a password for the
   *         key.
   * @since 1.4.1
   */
  @Nullable
  default String getKeyPasswordPerAlias (@Nullable final String sSearchKeyAlias)
  {
    final char [] ret = getKeyPasswordPerAliasCharArray (sSearchKeyAlias);
    return ret == null ? null : new String (ret);
  }

  /**
   * @return The trust store to be used or <code>null</code> if none is
   *         configured.
   * @since 0.12.0
   */
  @Nullable
  KeyStore getTrustStore ();
}
