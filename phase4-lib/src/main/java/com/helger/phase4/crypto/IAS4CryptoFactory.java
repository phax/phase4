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

import java.io.Serializable;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.string.StringHelper;
import com.helger.phase4.messaging.domain.MessageHelperMethods;

/**
 * The basic phase4 crypto interface.
 * <ul>
 * <li>See {@link AS4CryptoFactory} for an implementation of this interface
 * using a properties based approach</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 0.9.7
 */
public interface IAS4CryptoFactory extends Serializable
{
  /**
   * @return A WSS4J {@link Crypto} instance and never <code>null</code>.
   */
  @Nonnull
  Crypto getCrypto ();

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
   * @return The password to access the private key entry denoted by the key
   *         alias. May be <code>null</code>.
   */
  @Nullable
  String getKeyPassword ();

  /**
   * @return WSS4J configuration to be used.
   */
  @Nonnull
  default WSSConfig createWSSConfig ()
  {
    final WSSConfig ret = WSSConfig.getNewInstance ();
    ret.setIdAllocator (new WsuIdAllocator ()
    {
      public String createId (@Nullable final String sPrefix, final Object o)
      {
        return createSecureId (sPrefix, o);
      }

      public String createSecureId (final String sPrefix, final Object o)
      {
        return StringHelper.getConcatenatedOnDemand (sPrefix, "-", MessageHelperMethods.createRandomWSUID ());
      }
    });
    return ret;
  }
}
