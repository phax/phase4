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
 * Test class for class {@link AS4SigningParams}.
 *
 * @author Philip Helger
 */
public final class AS4SigningParamsTest
{
  @Test
  public void testDefaultValues ()
  {
    final AS4SigningParams aParams = new AS4SigningParams ();
    assertNull (aParams.getAlgorithmSign ());
    assertNull (aParams.getAlgorithmSignDigest ());
    // Default params have no signing algorithm set
    assertFalse (aParams.isSigningEnabled ());
    assertEquals (ECryptoAlgorithmC14N.C14N_ALGORITHM_DEFAULT, aParams.getAlgorithmC14N ());
    assertEquals (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE, aParams.getKeyIdentifierType ());
    assertTrue (aParams.isUseSingleCertificate ());
    assertNull (aParams.getSecurityProviderSign ());
    assertNull (aParams.getSecurityProviderVerify ());
    assertFalse (aParams.hasWSSecSignatureCustomizer ());
    assertNull (aParams.getWSSecSignatureCustomizer ());
  }

  @Test
  public void testSetAlgorithms ()
  {
    final AS4SigningParams aParams = new AS4SigningParams ();
    aParams.setAlgorithmSign (ECryptoAlgorithmSign.EDDSA_ED25519);
    assertEquals (ECryptoAlgorithmSign.EDDSA_ED25519, aParams.getAlgorithmSign ());

    aParams.setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    assertEquals (ECryptoAlgorithmSignDigest.DIGEST_SHA_256, aParams.getAlgorithmSignDigest ());

    aParams.setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
    assertEquals (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS, aParams.getAlgorithmC14N ());
  }

  @Test
  public void testClone ()
  {
    final AS4SigningParams aParams = new AS4SigningParams ();
    aParams.setAlgorithmSign (ECryptoAlgorithmSign.ECDSA_SHA_384)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_384)
           .setUseSingleCertificate (false);

    final AS4SigningParams aClone = aParams.getClone ();
    assertNotNull (aClone);
    assertEquals (ECryptoAlgorithmSign.ECDSA_SHA_384, aClone.getAlgorithmSign ());
    assertEquals (ECryptoAlgorithmSignDigest.DIGEST_SHA_384, aClone.getAlgorithmSignDigest ());
    assertFalse (aClone.isUseSingleCertificate ());
  }

  @Test
  public void testToString ()
  {
    final AS4SigningParams aParams = new AS4SigningParams ();
    final String s = aParams.toString ();
    assertNotNull (s);
    assertTrue (s.contains ("AlgorithmSign"));
  }
}
