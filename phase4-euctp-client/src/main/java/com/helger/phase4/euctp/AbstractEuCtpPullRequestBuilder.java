/*
 * Copyright (C) 2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.euctp;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.profile.euctp.AS4EuCtpProfileRegistarSPI;
import com.helger.phase4.sender.AbstractAS4PullRequestBuilder;

/**
 * Abstract EuCTP PullRequest builder class with sanity methods
 *
 * @param <IMPLTYPE>
 *        The implementation type
 * @author Ulrik Stehling
 * @author Philip Helger
 */
public abstract class AbstractEuCtpPullRequestBuilder <IMPLTYPE extends AbstractEuCtpPullRequestBuilder <IMPLTYPE>>
                                                      extends
                                                      AbstractAS4PullRequestBuilder <IMPLTYPE>
{
  public AbstractEuCtpPullRequestBuilder ()
  {
    try
    {
      as4ProfileID (AS4EuCtpProfileRegistarSPI.AS4_PROFILE_PULL_ID);

      // Other crypt parameters are located in the PMode security part
      cryptParams ().setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
      cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
      cryptParams ().setEncryptSymmetricSessionKey (false);

      // Other signing parameters are located in the PMode security part
      signingParams ().setKeyIdentifierType (AbstractEuCtpUserMessageBuilder.DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
      signingParams ().setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
      // Use the BST value type "#X509PKIPathv1"
      signingParams ().setUseSingleCertificate (false);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
    }
  }

  @Nonnull
  public IMPLTYPE httpClientFactory (@Nullable final KeyStore aKeyStore, @Nullable final char [] aKeyPassword)
                                                                                                               throws GeneralSecurityException
  {
    return httpClientFactory (new Phase4EuCtpHttpClientSettings (aKeyStore, aKeyPassword));
  }
}
