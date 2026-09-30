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
import static org.junit.Assert.assertSame;
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
    assertEquals (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE, aParams.getKeyIdentifierType ());
    assertFalse (aParams.hasCertificate ());
    assertFalse (aParams.hasAlias ());
    assertNull (aParams.getCertificate ());
    assertNull (aParams.getAlias ());
    assertNull (aParams.getSecurityProviderEncrypt ());
    assertNull (aParams.getSecurityProviderDecrypt ());
    assertTrue (aParams.isEncryptSymmetricSessionKey ());
    assertFalse (aParams.hasWSSecEncryptCustomizer ());
    assertNull (aParams.getWSSecEncryptCustomizer ());
  }

  @Test
  public void testCryptNotEnabledWithoutAlgorithm ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    assertFalse (aParams.isCryptEnabled (null));
  }

  @Test
  public void testCryptNotEnabledWithoutCertOrAlias ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);
    // Algorithm set, but no certificate or alias
    assertFalse (aParams.isCryptEnabled (null));
  }

  @Test
  public void testCryptEnabledWithAlias ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM).setAlias ("test-alias");
    assertTrue (aParams.isCryptEnabled (null));
    assertTrue (aParams.hasAlias ());
    assertEquals ("test-alias", aParams.getAlias ());
  }

  @Test
  public void testEDelivery2X25519 ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM).setEDelivery2KeyAgreementX25519 ();

    assertTrue (aParams.hasKeyAgreementMethod ());
    assertEquals (ECryptoKeyAgreementMethod.X25519, aParams.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aParams.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testEDelivery2ECDHES ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM).setEDelivery2KeyAgreementECDHES ();

    assertTrue (aParams.hasKeyAgreementMethod ());
    assertEquals (ECryptoKeyAgreementMethod.ECDH_ES, aParams.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aParams.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testResetKeyAgreement ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setEDelivery2KeyAgreementX25519 ();
    assertTrue (aParams.hasKeyAgreementMethod ());

    // Reset key agreement to null (back to key transport mode)
    aParams.setKeyAgreementMethod (null).setKeyDerivationMethod (null).setKeyWrapAlgorithm (null);
    assertFalse (aParams.hasKeyAgreementMethod ());
    assertNull (aParams.getKeyAgreementMethod ());
    assertNull (aParams.getKeyDerivationMethod ());
    assertNull (aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testIndividualKeyAgreementSetters ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();

    aParams.setKeyAgreementMethod (ECryptoKeyAgreementMethod.X448);
    assertSame (ECryptoKeyAgreementMethod.X448, aParams.getKeyAgreementMethod ());

    aParams.setKeyDerivationMethod (ECryptoKeyDerivationMethod.CONCAT_KDF);
    assertSame (ECryptoKeyDerivationMethod.CONCAT_KDF, aParams.getKeyDerivationMethod ());

    aParams.setKeyWrapAlgorithm (ECryptoKeyWrapAlgorithm.AES_256);
    assertSame (ECryptoKeyWrapAlgorithm.AES_256, aParams.getKeyWrapAlgorithm ());
  }

  @Test
  public void testCloneWithKeyAgreement ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM).setEDelivery2KeyAgreementX25519 ();

    final AS4CryptParams aClone = aParams.getClone ();
    assertNotNull (aClone);
    assertEquals (ECryptoKeyAgreementMethod.X25519, aClone.getKeyAgreementMethod ());
    assertEquals (ECryptoKeyDerivationMethod.HKDF, aClone.getKeyDerivationMethod ());
    assertEquals (ECryptoKeyWrapAlgorithm.AES_128, aClone.getKeyWrapAlgorithm ());
    assertEquals (ECryptoAlgorithmCrypt.AES_128_GCM, aClone.getAlgorithmCrypt ());
  }

  @Test
  public void testCloneWithoutKeyAgreement ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM)
           .setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.RSA_OAEP)
           .setAlias ("my-alias")
           .setEncryptSymmetricSessionKey (false);

    final AS4CryptParams aClone = aParams.getClone ();
    assertNotNull (aClone);
    assertNull (aClone.getKeyAgreementMethod ());
    assertEquals (ECryptoAlgorithmCrypt.AES_256_GCM, aClone.getAlgorithmCrypt ());
    assertEquals (ECryptoKeyEncryptionAlgorithm.RSA_OAEP, aClone.getKeyEncAlgorithm ());
    assertEquals ("my-alias", aClone.getAlias ());
    assertFalse (aClone.isEncryptSymmetricSessionKey ());
  }

  @Test
  public void testCreateDefault ()
  {
    final AS4CryptParams aParams = AS4CryptParams.createDefault ();
    assertNotNull (aParams);
    assertEquals (ECryptoAlgorithmCrypt.AES_128_GCM, aParams.getAlgorithmCrypt ());
  }

  @Test
  public void testToString ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    aParams.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM).setEDelivery2KeyAgreementX25519 ();
    final String sToString = aParams.toString ();
    assertNotNull (sToString);
    assertTrue (sToString.contains ("KeyAgreementMethod"));
    assertTrue (sToString.contains ("KeyDerivationMethod"));
    assertTrue (sToString.contains ("KeyWrapAlgorithm"));
  }

  @Test
  public void testToStringWithoutKeyAgreement ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    final String sToString = aParams.toString ();
    assertNotNull (sToString);
    // Key agreement fields should not appear when null
    assertFalse (sToString.contains ("KeyAgreementMethod"));
  }

  @Test
  public void testSessionKeyMatchesAlgorithmByDefault ()
  {
    for (final ECryptoAlgorithmCrypt e : ECryptoAlgorithmCrypt.values ())
    {
      final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (e);
      assertFalse (aParams.hasExplicitSessionKeyProvider ());
      final int nBits = aParams.getSessionKeyProvider ().getSessionKey ().getEncoded ().length * 8;
      assertEquals (e.getID (), e.getKeySizeBits (), nBits);
    }
  }

  @Test
  public void testSessionKeyWithoutAlgorithmUsesDefault ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ();
    assertSame (AS4CryptParams.DEFAULT_SESSION_KEY_PROVIDER, aParams.getSessionKeyProvider ());
  }

  @Test
  public void testExplicitSessionKeyProviderWins ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM)
                                                        .setSessionKeyProvider (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_128);
    assertTrue (aParams.hasExplicitSessionKeyProvider ());
    assertSame (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_128, aParams.getSessionKeyProvider ());
  }

  @Test
  public void testCloneKeepsDerivedSessionKeyProvider ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_CBC);
    final AS4CryptParams aClone = aParams.getClone ();
    assertFalse (aClone.hasExplicitSessionKeyProvider ());
    // Changing the algorithm of the clone must change the derived key size
    aClone.setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);
    assertEquals (16, aClone.getSessionKeyProvider ().getSessionKey ().getEncoded ().length);
    assertEquals (32, aParams.getSessionKeyProvider ().getSessionKey ().getEncoded ().length);
  }

  @Test
  public void testSessionKeyProviderCanBeResetToDerived ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM)
                                                        .setSessionKeyProvider (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_128);
    assertTrue (aParams.hasExplicitSessionKeyProvider ());
    aParams.setSessionKeyProvider (null);
    assertFalse (aParams.hasExplicitSessionKeyProvider ());
    assertEquals (32, aParams.getSessionKeyProvider ().getSessionKey ().getEncoded ().length);
  }

  @Test
  public void testCloneKeepsExplicitSessionKeyProvider ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM)
                                                        .setSessionKeyProvider (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_256);
    final AS4CryptParams aClone = aParams.getClone ();
    assertTrue (aClone.hasExplicitSessionKeyProvider ());
    assertSame (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_256, aClone.getSessionKeyProvider ());
  }
}
