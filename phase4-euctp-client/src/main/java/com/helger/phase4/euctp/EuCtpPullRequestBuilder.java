package com.helger.phase4.euctp;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.profile.euctp.AS4EuCtpProfileRegistarSPI;
import com.helger.phase4.sender.AbstractAS4PullRequestBuilder;

public class EuCtpPullRequestBuilder extends AbstractAS4PullRequestBuilder <EuCtpPullRequestBuilder>
{
  public EuCtpPullRequestBuilder ()
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
  public EuCtpPullRequestBuilder httpClientFactory (@Nullable final KeyStore aKeyStore,
                                                    @Nullable final char [] aKeyPassword) throws GeneralSecurityException
  {
    return httpClientFactory (new Phase4EuCtpHttpClientSettings (aKeyStore, aKeyPassword));
  }
}
