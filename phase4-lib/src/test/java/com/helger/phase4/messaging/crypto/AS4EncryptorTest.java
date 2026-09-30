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
package com.helger.phase4.messaging.crypto;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;

/**
 * Test class for class {@link AS4Encryptor}.
 *
 * @author Dino Patti
 */
public final class AS4EncryptorTest
{
  @Test
  public void testSessionKeySizeMatches ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM);
    AS4Encryptor.checkSessionKeySize (aParams, new SecretKeySpec (new byte [32], "AES"));
  }

  @Test
  public void testSessionKeyTooShortIsRejected ()
  {
    final AS4CryptParams aParams = new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_256_GCM);
    try
    {
      AS4Encryptor.checkSessionKeySize (aParams, new SecretKeySpec (new byte [16], "AES"));
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("128 bits"));
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("aes256-gcm"));
    }
  }
}
