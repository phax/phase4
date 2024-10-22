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
import java.security.cert.X509Certificate;

import javax.annotation.Nullable;

import com.helger.security.keystore.KeyStoreHelper;

/**
 * Abstract implementation of {@link IAS4CryptoFactory}.
 *
 * @author Philip Helger
 * @since 2.6.0
 */
public abstract class AbstractAS4CryptoFactory implements IAS4CryptoFactory
{
  protected AbstractAS4CryptoFactory ()
  {}

  /**
   * @return The underlying private key entry from the keystore or
   *         <code>null</code> if none is available (the reasons depend on the
   *         used implementation).
   * @see #getKeyStore()
   * @see #getKeyAlias()
   * @see #getKeyPasswordPerAlias(String)
   */
  @Nullable
  public KeyStore.PrivateKeyEntry getPrivateKeyEntry ()
  {
    final KeyStore aKeyStore = getKeyStore ();
    if (aKeyStore == null)
      return null;

    final String sKeyAlias = getKeyAlias ();
    final char [] aKeyPassword = getKeyPasswordPerAliasCharArray (sKeyAlias);
    return KeyStoreHelper.loadPrivateKey (aKeyStore, "phase4 CryptoFactory KeyStore", sKeyAlias, aKeyPassword)
                         .getKeyEntry ();
  }

  /**
   * @return The public certificate of the private key entry or
   *         <code>null</code> if the private key entry could not be loaded.
   * @see #getPrivateKeyEntry()
   */
  @Nullable
  public X509Certificate getCertificate ()
  {
    final KeyStore.PrivateKeyEntry aPK = getPrivateKeyEntry ();
    return aPK == null ? null : (X509Certificate) aPK.getCertificate ();
  }
}
