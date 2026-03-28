/*
 * Copyright (C) 2015-2026 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.entsog;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.profile.entsog.AS4ENTSOGProfileRegistarSPI;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * This class contains all the specifics to send AS4 messages with the ENTSOG profile. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with all potential
 * customization.
 *
 * @author Pavel Rotek
 * @since 0.14.0
 */
@Immutable
public final class Phase4ENTSOGSender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4ENTSOGSender.class);

  private Phase4ENTSOGSender ()
  {}

  /**
   * @return Create a new Builder for AS4 messages using the ENTSOG v3.6 profile (RSA-based). Never
   *         <code>null</code>.
   */
  @NonNull
  public static ENTSOGUserMessageBuilder builder ()
  {
    return new ENTSOGUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 messages using the ENTSOG v4.0 EdDSA/X25519 profile
   *         (primary). Never <code>null</code>.
   * @since 4.4.2
   */
  @NonNull
  public static ENTSOG4EdDSAUserMessageBuilder builderEdDSA ()
  {
    return new ENTSOG4EdDSAUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 messages using the ENTSOG v4.0 ECDSA/ECDH-ES profile
   *         (alternative). Never <code>null</code>.
   * @since 4.4.2
   */
  @NonNull
  public static ENTSOG4ECDSAUserMessageBuilder builderECDSA ()
  {
    return new ENTSOG4ECDSAUserMessageBuilder ();
  }

  /**
   * Abstract ENTSOG UserMessage builder class with sanity methods
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  public abstract static class AbstractENTSOGUserMessageBuilder <IMPLTYPE extends AbstractENTSOGUserMessageBuilder <IMPLTYPE>>
                                                                extends
                                                                AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE = ECryptoKeyIdentifierType.ISSUER_SERIAL;

    private ENTSOGPayloadParams m_aPayloadParams;

    protected AbstractENTSOGUserMessageBuilder ()
    {
      // Override default values
      try
      {
        as4ProfileID (AS4ENTSOGProfileRegistarSPI.AS4_PROFILE_ID);
        httpClientFactory (new Phase4ENTSOGHttpClientSettings ());

        signingParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);
        cryptParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);

        conversationID ("");
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Encryption Key Identifier Type.
     *
     * @param eEncryptionKeyIdentifierType
     *        {@link ECryptoKeyIdentifierType}. Defaults to ISSUER_SERIAL. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE encryptionKeyIdentifierType (@Nullable final ECryptoKeyIdentifierType eEncryptionKeyIdentifierType)
    {
      if (eEncryptionKeyIdentifierType != null)
        cryptParams ().setKeyIdentifierType (eEncryptionKeyIdentifierType);
      return thisAsT ();
    }

    /**
     * Signing Key Identifier Type.
     *
     * @param eSigningKeyIdentifierType
     *        {@link ECryptoKeyIdentifierType}. Defaults to ISSUER_SERIAL. May be <code>null</code>.
     * @return this for chaining
     * @since 2.2.2
     */
    @NonNull
    public final IMPLTYPE signingKeyIdentifierType (@Nullable final ECryptoKeyIdentifierType eSigningKeyIdentifierType)
    {
      if (eSigningKeyIdentifierType != null)
        signingParams ().setKeyIdentifierType (eSigningKeyIdentifierType);
      return thisAsT ();
    }

    /**
     * Set the payload to be send out.
     *
     * @param aBuilder
     *        The payload builder to be used. GZip compression is automatically. enforced.
     * @param aPayloadParams
     *        The payload params to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE payload (final AS4OutgoingAttachment.@Nullable Builder aBuilder,
                                   @Nullable final ENTSOGPayloadParams aPayloadParams)
    {
      payload (aBuilder != null ? aBuilder.compressionGZIP ().build () : null);
      m_aPayloadParams = aPayloadParams;
      return thisAsT ();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayload == null)
      {
        LOGGER.warn ("The field 'payload' is not set");
        return false;
      }
      if (!"".equals (m_sConversationID))
      {
        LOGGER.warn ("The field 'conversationID' must not be changed");
        return false;
      }

      // All valid
      return true;
    }

    @Override
    protected WSS4JAttachment createMainAttachment (@NonNull final AS4OutgoingAttachment aPayload,
                                                    @NonNull final AS4ResourceHelper aResHelper) throws IOException
    {
      final WSS4JAttachment aPayloadAttachment = WSS4JAttachment.createOutgoingFileAttachment (aPayload, aResHelper);

      if (m_aPayloadParams != null)
      {
        if (m_aPayloadParams.getDocumentType () != null)
          aPayloadAttachment.customPartProperties ().put ("EDIGASDocumentType", m_aPayloadParams.getDocumentType ());
      }
      return aPayloadAttachment;
    }
  }

  /**
   * The builder class for sending AS4 messages using ENTSOG profile specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Philip Helger
   */
  public static class ENTSOGUserMessageBuilder extends AbstractENTSOGUserMessageBuilder <ENTSOGUserMessageBuilder>
  {
    public ENTSOGUserMessageBuilder ()
    {}
  }

  /**
   * Abstract ENTSOG 4.0 UserMessage builder class with EdDSA/ECDSA support.
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   * @since 4.4.2
   */
  public abstract static class AbstractENTSOG4UserMessageBuilder <IMPLTYPE extends AbstractENTSOG4UserMessageBuilder <IMPLTYPE>>
                                                                  extends
                                                                  AbstractENTSOGUserMessageBuilder <IMPLTYPE>
  {
    protected AbstractENTSOG4UserMessageBuilder (@NonNull final String sProfileID)
    {
      try
      {
        as4ProfileID (sProfileID);
        httpClientFactory (new Phase4ENTSOGHttpClientSettings ());

        signingParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);
        cryptParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);

        conversationID ("");
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }
  }

  /**
   * Builder for sending AS4 messages using ENTSOG 4.0 EdDSA/X25519 profile (primary). Use
   * {@link Phase4ENTSOGSender#builderEdDSA()} to create instances.
   *
   * @author Philip Helger
   * @since 4.4.2
   */
  public static class ENTSOG4EdDSAUserMessageBuilder extends AbstractENTSOG4UserMessageBuilder <ENTSOG4EdDSAUserMessageBuilder>
  {
    public ENTSOG4EdDSAUserMessageBuilder ()
    {
      super (AS4ENTSOGProfileRegistarSPI.AS4_PROFILE_ID_V4_EDDSA);

      // Set default crypto params for EdDSA/X25519
      signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.EDDSA_ED25519);
      cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM)
                    .setEDelivery2KeyAgreementX25519 ();
    }
  }

  /**
   * Builder for sending AS4 messages using ENTSOG 4.0 ECDSA/ECDH-ES profile (alternative). Use
   * {@link Phase4ENTSOGSender#builderECDSA()} to create instances.
   *
   * @author Philip Helger
   * @since 4.4.2
   */
  public static class ENTSOG4ECDSAUserMessageBuilder extends AbstractENTSOG4UserMessageBuilder <ENTSOG4ECDSAUserMessageBuilder>
  {
    public ENTSOG4ECDSAUserMessageBuilder ()
    {
      super (AS4ENTSOGProfileRegistarSPI.AS4_PROFILE_ID_V4_ECDSA);

      // Set default crypto params for ECDSA/ECDH-ES
      signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.ECDSA_SHA_256);
      cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM)
                    .setEDelivery2KeyAgreementECDHES ();
    }
  }

  /**
   * Additional parameters to add in the PayloadInfo part of AS4 UserMessage
   *
   * @author Pavel Rotek
   */
  @NotThreadSafe
  public static class ENTSOGPayloadParams
  {
    private String m_sDocumentType;

    /**
     * @return ENTSOG payload document type according to EDIG@S. Eg. "01G" for EDIG@S Nomination
     *         document.
     */
    @Nullable
    public String getDocumentType ()
    {
      return m_sDocumentType;
    }

    /**
     * ENTSOG payload document type
     *
     * @param sDocumentType
     *        Document type to use. May be <code>null</code>.
     */
    public void setDocumentType (@Nullable final String sDocumentType)
    {
      m_sDocumentType = sDocumentType;
    }
  }
}
