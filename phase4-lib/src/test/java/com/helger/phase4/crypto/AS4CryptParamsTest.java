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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link AS4CryptParams}.
 *
 * @author Philip Helger
 */
public final class AS4CryptParamsTest
{
  @Test
  public void testDefaultValues ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    assertNull (aParams.getAlgorithmCrypt ());
    assertNull (aParams.getKeyAgreementMethod ());
    assertNull (aParams.getKeyDerivationMethod ());
    assertNull (aParams.getKeyWrapAlgorithm ());
    assertFalse (aParams.hasKeyAgreementMethod ());
    assertEquals (ECryptoKeyEncryptionAlgorithm.RSA_OAEP_XENC11, aParams.getKeyEncAlgorithm ());
  }

  @Test
  public void testEDelivery2X25519 ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM)
           .setEDelivery2KeyAgreementX25519 ();

    assertTrue (aParams.hasKeyAgreementMethod ());
    assertEquals (ECryptoKeyAgreementMethod.X25519, aParams.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aParams.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testEDelivery2ECDHES ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM)
           .setEDelivery2KeyAgreementECDHES ();

    assertTrue (aParams.hasKeyAgreementMethod ());
    assertEquals (ECryptoKeyAgreementMethod.ECDH_ES, aParams.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aParams.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testCloneWithKeyAgreement ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM)
           .setEDelivery2KeyAgreementX25519 ();

    final AS4CryptParams aClone = aParams.getClone ();
    assertNotNull (aClone);
    assertEquals (ECryptoKeyAgreementMethod.X25519, aClone.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aClone.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aClone.getKeyWrapAlgorithm ());
    assertEquals (ECryptoAlgorithmCrypt.AES_128_GCM, aClone.getAlgorithmCrypt ());
  }
}
