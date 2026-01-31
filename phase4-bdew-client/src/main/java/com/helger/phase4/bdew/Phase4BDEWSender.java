/*
 * Copyright (C) 2023-2026 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.bdew;

import java.io.IOException;
import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.profile.bdew.AS4BDEWProfileRegistarSPI;
import com.helger.phase4.profile.bdew.BDEWPMode;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * This class contains all the specifics to send AS4 messages with the BDEW profile. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with all potential
 * customization.
 *
 * @author Gregor Scholtysik
 * @author Philip Helger
 */
@Immutable
public final class Phase4BDEWSender
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4BDEWSender.class);

  private Phase4BDEWSender ()
  {}

  /**
   * @return Create a new Builder for AS4 messages if the payload is present. Never
   *         <code>null</code>.
   */
  @NonNull
  public static BDEWUserMessageBuilder builder ()
  {
    return new BDEWUserMessageBuilder ();
  }

  /**
   * Abstract BDEW UserMessage builder class with sanity methods
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  public abstract static class AbstractBDEWUserMessageBuilder <IMPLTYPE extends AbstractBDEWUserMessageBuilder <IMPLTYPE>>
                                                              extends
                                                              AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    // Default per section 2.2.6.2.1
    public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_SIGN = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
    // Default per section 2.2.6.2.2
    public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT = ECryptoKeyIdentifierType.X509_SKI_KEY_IDENTIFIER;

    private BDEWPayloadParams m_aPayloadParams;

    protected AbstractBDEWUserMessageBuilder ()
    {
      // Override default values
      try
      {
        as4ProfileID (AS4BDEWProfileRegistarSPI.AS4_PROFILE_ID);

        httpClientFactory (new Phase4BDEWHttpClientSettings ());

        // Other crypt parameters are located in the PMode security part
        cryptParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE_CRYPT);
        cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
        cryptParams ().setEncryptSymmetricSessionKey (false);

        // See BDEW specs 2.2.6.2
        // Other signing parameters are located in the PMode security part
        signingParams ().setKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE_SIGN);
        signingParams ().setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_EXCL_OMIT_COMMENTS);
        // Use the BST value type "#X509PKIPathv1"
        signingParams ().setUseSingleCertificate (false);

        // Must be empty
        conversationID ("");

        agreementRef (BDEWPMode.DEFAULT_AGREEMENT_ID);

        // According to #186
        forceMimeMessage (true);
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
     *        {@link ECryptoKeyIdentifierType}. Defaults to BST_DIRECT_REFERENCE. May be
     *        <code>null</code>.
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
     *        {@link ECryptoKeyIdentifierType}. Defaults to BST_DIRECT_REFERENCE. May be
     *        <code>null</code>.
     * @return this for chaining
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
                                   @Nullable final BDEWPayloadParams aPayloadParams)
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
        // See #180 for the specifics
        if (!m_sAction.equals (BDEWPMode.ACTION_REQUEST_SWITCH) && !m_sAction.equals (BDEWPMode.ACTION_CONFIRM_SWITCH))
        {
          // Payload is mandatory
          LOGGER.warn ("The field 'payload' is not set");
          return false;
        }
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
    @Nullable
    protected WSS4JAttachment createMainAttachment (@NonNull final AS4OutgoingAttachment aPayload,
                                                    @NonNull final AS4ResourceHelper aResHelper) throws IOException
    {
      final WSS4JAttachment aPayloadAttachment = WSS4JAttachment.createOutgoingFileAttachment (aPayload, aResHelper);

      if (m_aPayloadParams != null)
      {
        if (StringHelper.isNotEmpty (m_aPayloadParams.getDocumentType ()))
          aPayloadAttachment.customPartProperties ().put ("BDEWDocumentType", m_aPayloadParams.getDocumentType ());

        if (m_aPayloadParams.getDocumentDate () != null)
          aPayloadAttachment.customPartProperties ()
                            .put ("BDEWDocumentDate", m_aPayloadParams.getDocumentDate ().toString ());

        if (StringHelper.isNotEmpty (m_aPayloadParams.getDocumentNumber ()))
          aPayloadAttachment.customPartProperties ().put ("BDEWDocumentNo", m_aPayloadParams.getDocumentNumber ());

        if (m_aPayloadParams.getFulfillmentDate () != null)
          aPayloadAttachment.customPartProperties ()
                            .put ("BDEWFulfillmentDate", m_aPayloadParams.getFulfillmentDate ().toString ());

        if (StringHelper.isNotEmpty (m_aPayloadParams.getSubjectPartyId ()))
          aPayloadAttachment.customPartProperties ().put ("BDEWSubjectPartyID", m_aPayloadParams.getSubjectPartyId ());

        if (StringHelper.isNotEmpty (m_aPayloadParams.getSubjectPartyRole ()))
          aPayloadAttachment.customPartProperties ()
                            .put ("BDEWSubjectPartyRole", m_aPayloadParams.getSubjectPartyRole ());

        if (StringHelper.isNotEmpty (m_aPayloadParams.getApplicationReference ()))
          aPayloadAttachment.customPartProperties ()
                            .put ("BDEWApplicationReference", m_aPayloadParams.getApplicationReference ());
      }
      return aPayloadAttachment;
    }
  }

  /**
   * The builder class for sending AS4 messages using BDEW profile specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Philip Helger
   */
  public static class BDEWUserMessageBuilder extends AbstractBDEWUserMessageBuilder <BDEWUserMessageBuilder>
  {
    public BDEWUserMessageBuilder ()
    {}
  }

  /**
   * Additional parameters to add in the PayloadInfo part of AS4 UserMessage
   *
   * @author Gregor Scholtysik
   */
  @NotThreadSafe
  public static class BDEWPayloadParams
  {
    private String m_sDocumentType;
    private LocalDate m_aDocumentDate;
    private String m_sDocumentNumber;
    private LocalDate m_aFulfillmentDate;
    private String m_sSubjectPartyID;
    private String m_sSubjectPartyRole;
    private String m_sApplicationReference;

    /**
     * @return BDEW payload document type for payload identifier <code>BDEWDocumentType</code>
     */
    @Nullable
    public String getDocumentType ()
    {
      return m_sDocumentType;
    }

    /**
     * BDEW payload document type
     *
     * @param sDocumentType
     *        Document type to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setDocumentType (@Nullable final String sDocumentType)
    {
      m_sDocumentType = sDocumentType;
      return this;
    }

    /**
     * @return BDEW payload document date for payload identifier <code>BDEWDocumentDate</code>
     */
    @Nullable
    public LocalDate getDocumentDate ()
    {
      return m_aDocumentDate;
    }

    /**
     * BDEW payload document date
     *
     * @param sDocumentDate
     *        Document date to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setDocumentDate (@Nullable final LocalDate sDocumentDate)
    {
      m_aDocumentDate = sDocumentDate;
      return this;
    }

    /**
     * Note: type change in 2.1.3 from <code>Integer</code> to <code>String</code>
     *
     * @return BDEW payload document number for payload identifier <code>BDEWDocumentNo</code>
     */
    @Nullable
    public String getDocumentNumber ()
    {
      return m_sDocumentNumber;
    }

    /**
     * BDEW payload document number
     *
     * @param nDocumentNumber
     *        Document number to use. May be <code>null</code>.
     * @return this for chaining
     * @since 2.1.2
     */
    @NonNull
    public BDEWPayloadParams setDocumentNumber (final int nDocumentNumber)
    {
      return setDocumentNumber (Integer.toString (nDocumentNumber));
    }

    /**
     * BDEW payload document number.<br>
     * Note: type change in 2.1.3 from <code>Integer</code> to <code>String</code>
     *
     * @param sDocumentNumber
     *        Document number to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setDocumentNumber (@Nullable final String sDocumentNumber)
    {
      m_sDocumentNumber = sDocumentNumber;
      return this;
    }

    /**
     * @return BDEW payload fulfillment date for payload identifier <code>BDEWFulfillmentDate</code>
     */
    @Nullable
    public LocalDate getFulfillmentDate ()
    {
      return m_aFulfillmentDate;
    }

    /**
     * BDEW payload fulfillment date
     *
     * @param sFulfillmenttDate
     *        Fulfillment date to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setFulfillmentDate (@Nullable final LocalDate sFulfillmenttDate)
    {
      m_aFulfillmentDate = sFulfillmenttDate;
      return this;
    }

    /**
     * @return BDEW payload subject party ID for payload identifier <code>BDEWSubjectPartyID</code>
     */
    @Nullable
    public String getSubjectPartyId ()
    {
      return m_sSubjectPartyID;
    }

    /**
     * BDEW payload subject party ID
     *
     * @param sSubjectPartyId
     *        Subject party ID to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setSubjectPartyId (@Nullable final String sSubjectPartyId)
    {
      m_sSubjectPartyID = sSubjectPartyId;
      return this;
    }

    /**
     * @return BDEW payload subject party ID for payload identifier
     *         <code>BDEWSubjectPartyRole</code>
     */
    @Nullable
    public String getSubjectPartyRole ()
    {
      return m_sSubjectPartyRole;
    }

    /**
     * BDEW payload subject party role
     *
     * @param sSubjectPartyRole
     *        Subject party role to use. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public BDEWPayloadParams setSubjectPartyRole (@Nullable final String sSubjectPartyRole)
    {
      m_sSubjectPartyRole = sSubjectPartyRole;
      return this;
    }

    /**
     * @return BDEW payload application reference for payload identifier
     *         <code>BDEWApplicationReference</code>
     * @since 3.0.1
     */
    @Nullable
    public String getApplicationReference ()
    {
      return m_sApplicationReference;
    }

    /**
     * BDEW payload application reference
     *
     * @param sApplicationReference
     *        Application reference to use. May be <code>null</code>.
     * @return this for chaining
     * @since 3.0.1
     */
    @NonNull
    public BDEWPayloadParams setApplicationReference (@Nullable final String sApplicationReference)
    {
      m_sApplicationReference = sApplicationReference;
      return this;
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("DocumentType", m_sDocumentType)
                                         .append ("DocumentDate", m_aDocumentDate)
                                         .append ("DocumentNumber", m_sDocumentNumber)
                                         .append ("FulfillmentDate", m_aFulfillmentDate)
                                         .append ("SubjectPartyID", m_sSubjectPartyID)
                                         .append ("SubjectPartyRole", m_sSubjectPartyRole)
                                         .append ("ApplicationReference", m_sApplicationReference)
                                         .getToString ();
    }
  }
}
