/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;

/**
 * Interface for a "session key" provider, that is used for encrypting documents. Default instances
 * for AES-128 and AES-256 are provided for simplicity, and {@link #createRandomFor(ECryptoAlgorithmCrypt)}
 * creates a provider whose key size matches a given encryption algorithm.
 *
 * @author Philip Helger
 */
public interface ICryptoSessionKeyProvider
{
  /**
   * Get or create a new symmetric session key. This method may only throw unchecked exceptions.
   *
   * @return A new session key. Must not be <code>null</code>.
   */
  @NonNull
  SecretKey getSessionKey ();

  /**
   * Session key provider for AES-128 keys that can be used e.g. for AES-128-CBC or AES-128-GCM
   */
  ICryptoSessionKeyProvider INSTANCE_RANDOM_AES_128 = () -> {
    try
    {
      final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_128);
      return aKeyGen.generateKey ();
    }
    catch (final WSSecurityException ex)
    {
      throw new IllegalStateException ("Failed to create session key (AES-128)", ex);
    }
  };

  /**
   * Session key provider for AES-256 keys that can be used e.g. for AES-256-CBC or AES-256-GCM
   */
  ICryptoSessionKeyProvider INSTANCE_RANDOM_AES_256 = () -> {
    try
    {
      final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_256);
      return aKeyGen.generateKey ();
    }
    catch (final WSSecurityException ex)
    {
      throw new IllegalStateException ("Failed to create session key (AES-256)", ex);
    }
  };

  /**
   * Create a session key provider that creates random keys of exactly the size the provided
   * encryption algorithm requires (e.g. 256 bits for AES-256-GCM).
   *
   * @param eAlgorithmCrypt
   *        The encryption algorithm the keys are used for. May not be <code>null</code>.
   * @return A new session key provider. Never <code>null</code>.
   * @since 4.7.1
   */
  @NonNull
  static ICryptoSessionKeyProvider createRandomFor (@NonNull final ECryptoAlgorithmCrypt eAlgorithmCrypt)
  {
    ValueEnforcer.notNull (eAlgorithmCrypt, "AlgorithmCrypt");
    return () -> {
      try
      {
        // Plain JCE, so that this also works before WSS4J/XMLSec was initialized
        final KeyGenerator aKeyGen;
        if (eAlgorithmCrypt == ECryptoAlgorithmCrypt.CRYPT_3DES)
          aKeyGen = KeyGenerator.getInstance ("DESede");
        else
        {
          aKeyGen = KeyGenerator.getInstance ("AES");
          aKeyGen.init (eAlgorithmCrypt.getKeySizeBits ());
        }
        return aKeyGen.generateKey ();
      }
      catch (final NoSuchAlgorithmException ex)
      {
        throw new IllegalStateException ("Failed to create session key (" + eAlgorithmCrypt.getID () + ")", ex);
      }
    };
  }
}
