package com.helger.phase4.euctp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.profile.euctp.EuCtpPMode;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract euctp UserMessage builder class with sanity methods
 *
 * @param <IMPLTYPE> The implementation type
 * @author Ulrik Stehling
 */
public abstract class AbstractEuctpUserMessageBuilder<IMPLTYPE extends AbstractEuctpUserMessageBuilder<IMPLTYPE>>
		extends
		AbstractAS4UserMessageBuilderMIMEPayload<IMPLTYPE>
{
	private static final Logger LOGGER = LoggerFactory.getLogger (AbstractEuctpUserMessageBuilder.class);

	// Default per section 2.2.6.2.1
	public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_SIGN = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
	// Default per section 2.2.6.2.2
	public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT = ECryptoKeyIdentifierType.SKI_KEY_IDENTIFIER;

	protected AbstractEuctpUserMessageBuilder()
	{
		// Override default values
		try
		{
//        httpClientFactory(new Phase4EuCtpHttpClientSettings());

			// Other crypt parameters are located in the PMode security part
			cryptParams().setKeyIdentifierType(DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
			cryptParams().setKeyEncAlgorithm(ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
			cryptParams().setEncryptSymmetricSessionKey(false);

			/*
			 * Assumption: the BST "ValueType" attribute is set to
			 * "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1"
			 * by WSS4J automatically (see WSSecSignature#addBST)
			 */

			// Other signing parameters are located in the PMode security part
			signingParams().setKeyIdentifierType(DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
			signingParams().setAlgorithmC14N(ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
			// Use the BST value type "#X509PKIPathv1"
			signingParams().setUseSingleCertificate(false);

			conversationID("1");

			agreementRef(EuCtpPMode.DEFAULT_AGREEMENT_ID);

			// According to #186
			forceMimeMessage(true);
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
	public final IMPLTYPE encryptionKeyIdentifierType(@Nullable final ECryptoKeyIdentifierType eEncryptionKeyIdentifierType)
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
	public final IMPLTYPE signingKeyIdentifierType(@Nullable final ECryptoKeyIdentifierType eSigningKeyIdentifierType)
	{
		if (eSigningKeyIdentifierType != null)
			signingParams().setKeyIdentifierType(eSigningKeyIdentifierType);
		return thisAsT();
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public boolean isEveryRequiredFieldSet()
	{
		if (!super.isEveryRequiredFieldSet())
			return false;

		if (m_aPayload == null)
		{
			// todo uli
			if (m_sAction.equals(EuCtpPMode.ACTION_TEST))
			{
				// Payload is optional
			}
			else
			{
				// Payload is mandatory
				LOGGER.warn("The field 'payload' is not set");
				return false;
			}
		}

		// All valid
		return true;
	}
}
