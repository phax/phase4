package com.helger.phase4.euctp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.profile.AS4ProfileManager;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.euctp.EuCtpPMode;
import com.helger.phase4.sender.AbstractAS4PullRequestBuilder;

public class EuctpPullRequestBuilder extends AbstractAS4PullRequestBuilder<EuctpPullRequestBuilder>
{
	public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_SIGN = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
	// Default per section 2.2.6.2.2
	public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT = ECryptoKeyIdentifierType.X509_KEY_IDENTIFIER;

	public EuctpPullRequestBuilder()
	{
		try
		{
			// Other crypt parameters are located in the PMode security part
			cryptParams().setKeyIdentifierType(DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
			cryptParams().setKeyEncAlgorithm(ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
			cryptParams().setEncryptSymmetricSessionKey(false);

			// Other signing parameters are located in the PMode security part
			signingParams().setKeyIdentifierType(DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
			signingParams().setAlgorithmC14N(ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
			// Use the BST value type "#X509PKIPathv1"
			signingParams().setUseSingleCertificate(false);
		}
		catch (final Exception ex)
		{
			throw new IllegalStateException("Failed to init AS4 Client builder", ex);
		}
	}

	/**
	 * Encryption Key Identifier Type.
	 *
	 * @param eEncryptionKeyIdentifierType {@link ECryptoKeyIdentifierType}. Defaults to
	 *                                     BST_DIRECT_REFERENCE. May be <code>null</code>.
	 * @return this for chaining
	 */
	@Nonnull
	public final EuctpPullRequestBuilder encryptionKeyIdentifierType(@Nullable final ECryptoKeyIdentifierType eEncryptionKeyIdentifierType)
	{
		if (eEncryptionKeyIdentifierType != null)
			cryptParams().setKeyIdentifierType(eEncryptionKeyIdentifierType);
		return thisAsT();
	}

	/**
	 * Signing Key Identifier Type.
	 *
	 * @param eSigningKeyIdentifierType {@link ECryptoKeyIdentifierType}. Defaults to
	 *                                  BST_DIRECT_REFERENCE. May be <code>null</code>.
	 * @return this for chaining
	 */
	@Nonnull
	public final EuctpPullRequestBuilder signingKeyIdentifierType(@Nullable final ECryptoKeyIdentifierType eSigningKeyIdentifierType)
	{
		if (eSigningKeyIdentifierType != null)
			signingParams().setKeyIdentifierType(eSigningKeyIdentifierType);
		return thisAsT();
	}


}
