package com.helger.phase4.euctp;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.profile.euctp.EEuCtpAction;
import com.helger.phase4.profile.euctp.EEuCtpService;
import com.helger.phase4.profile.euctp.EuCtpPMode;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;

/**
 * Abstract EuCTP UserMessage builder class with sanity methods
 *
 * @param <IMPLTYPE>
 *        The implementation type
 * @author Ulrik Stehling
 * @author Philip Helger
 */
public abstract class AbstractEuCtpUserMessageBuilder <IMPLTYPE extends AbstractEuCtpUserMessageBuilder <IMPLTYPE>>
                                                      extends
                                                      AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractEuCtpUserMessageBuilder.class);

  // Default per section 2.2.6.2.1
  public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_SIGN = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
  // Default per section 2.2.6.2.2
  public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT = ECryptoKeyIdentifierType.X509_KEY_IDENTIFIER;

  protected AbstractEuCtpUserMessageBuilder ()
  {
    // Override default values
    try
    {
      // Other crypt parameters are located in the PMode security part
      cryptParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
      cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
      cryptParams ().setEncryptSymmetricSessionKey (false);

      // Other signing parameters are located in the PMode security part
      signingParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
      signingParams ().setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
      // Use the BST value type "#X509PKIPathv1"
      signingParams ().setUseSingleCertificate (false);

      conversationID (UUID.randomUUID ().toString ());

      agreementRef (EuCtpPMode.DEFAULT_AGREEMENT_ID);
      agreementType(EuCtpPMode.DEFAULT_AGREEMENT_TYPE);

      forceMimeMessage (true);
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

  @Nonnull
  public IMPLTYPE service (@Nullable final String sServiceType, @Nonnull final EEuCtpService eEuCtpService)
  {
    ValueEnforcer.notNull (eEuCtpService, "EuCtpService");

    return service (sServiceType, eEuCtpService.getValue ());
  }

  @Nonnull
  public IMPLTYPE action (@Nonnull final EEuCtpAction eEuCtpAction)
  {
    ValueEnforcer.notNull (eEuCtpAction, "EuCtpAction");

    return action (eEuCtpAction.getValue ());
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (!super.isEveryRequiredFieldSet ())
      return false;

    if (m_aPayload == null)
    {
      if (EuCtpPMode.ACTION_TEST.equals (m_sAction))
      {
        // Payload is optional
      }
      else
      {
        // Payload is mandatory
        LOGGER.warn ("The field 'payload' is not set");
        return false;
      }
    }

    // All valid
    return true;
  }
}
