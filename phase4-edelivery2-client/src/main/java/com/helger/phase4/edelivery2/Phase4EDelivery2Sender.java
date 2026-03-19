/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.edelivery2;

import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR2;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.dynamicdiscovery.IAS4EndpointDetailProvider;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.MessageProperty;
import com.helger.phase4.profile.edelivery2.AS4EDelivery2ProfileRegistarSPI;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;
import com.helger.smpclient.bdxr1.IBDXRExtendedServiceMetadataProvider;
import com.helger.smpclient.bdxr2.IBDXR2ServiceMetadataProvider;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.IBDXLURLProvider;

/**
 * This class contains all the specifics to send AS4 messages with the eDelivery AS4 2.0 profile.
 * See <code>sendAS4Message</code> as the main method to trigger the sending, with all potential
 * customization.<br>
 * Supports both the Common Usage Profile (EdDSA/X25519) and the Alternative Elliptic Curve Profile
 * (ECDSA/ECDH-ES with secp256r1).
 *
 * @author Philip Helger
 * @since 4.4.0
 */
@Immutable
public final class Phase4EDelivery2Sender
{
  public static final SimpleIdentifierFactory IF = SimpleIdentifierFactory.INSTANCE;
  public static final IBDXLURLProvider URL_PROVIDER = BDXLURLProvider.INSTANCE;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4EDelivery2Sender.class);

  private Phase4EDelivery2Sender ()
  {}

  /**
   * @return Create a new Builder for AS4 messages using the Common Usage Profile (EdDSA/X25519).
   *         Never <code>null</code>.
   */
  @NonNull
  public static EDelivery2EdDSAUserMessageBuilder builderEdDSA ()
  {
    return new EDelivery2EdDSAUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 messages using the Alternative EC Profile (ECDSA/ECDH-ES).
   *         Never <code>null</code>.
   */
  @NonNull
  public static EDelivery2ECDSAUserMessageBuilder builderECDSA ()
  {
    return new EDelivery2ECDSAUserMessageBuilder ();
  }

  /**
   * Abstract eDelivery 2.0 UserMessage builder class with sanity methods
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  @NotThreadSafe
  public abstract static class AbstractEDelivery2UserMessageBuilder <IMPLTYPE extends AbstractEDelivery2UserMessageBuilder <IMPLTYPE>>
                                                                    extends
                                                                    AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    public static final boolean DEFAULT_USE_ORIGINAL_SENDER_FINAL_RECIPIENT_TYPE_ATTR = true;

    protected IParticipantIdentifier m_aSenderID;
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;
    protected IAS4EndpointDetailProvider m_aEndpointDetailProvider;
    protected Consumer <X509Certificate> m_aCertificateConsumer;
    protected Consumer <String> m_aAPEndointURLConsumer;
    protected boolean m_bUseOriginalSenderFinalRecipientTypeAttr = DEFAULT_USE_ORIGINAL_SENDER_FINAL_RECIPIENT_TYPE_ATTR;

    protected AbstractEDelivery2UserMessageBuilder (@NonNull final String sProfileID)
    {
      try
      {
        as4ProfileID (sProfileID);
        httpClientFactory (new Phase4EDelivery2HttpClientSettings ());
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the sender participant ID of the message. The participant ID must be provided prior to
     * sending. This ends up in the "originalSender" UserMessage property.
     *
     * @param a
     *        The sender participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE senderParticipantID (@Nullable final IParticipantIdentifier a)
    {
      m_aSenderID = a;
      return thisAsT ();
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must be provided prior to
     * sending. This ends up in the "finalRecipient" UserMessage property.
     *
     * @param a
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE receiverParticipantID (@Nullable final IParticipantIdentifier a)
    {
      m_aReceiverID = a;
      return thisAsT ();
    }

    /**
     * Set the document type ID to be send. The document type must be provided prior to sending.
     *
     * @param a
     *        The document type ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE documentTypeID (@Nullable final IDocumentTypeIdentifier a)
    {
      m_aDocTypeID = a;
      return action (a == null ? null : a.getURIEncoded ());
    }

    /**
     * Set the process ID to be send. The process ID must be provided prior to sending.
     *
     * @param a
     *        The process ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE processID (@Nullable final IProcessIdentifier a)
    {
      m_aProcessID = a;
      return service (a == null ? null : a.getScheme (), a == null ? null : a.getValue ());
    }

    /**
     * Set the "from party ID". This is mandatory
     *
     * @param a
     *        The from party ID. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE fromPartyID (@Nullable final IParticipantIdentifier a)
    {
      return fromPartyIDType (a == null ? null : a.getScheme ()).fromPartyID (a == null ? null : a.getValue ());
    }

    /**
     * Set the "to party ID". This is mandatory
     *
     * @param a
     *        The to party ID. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE toPartyID (@Nullable final IParticipantIdentifier a)
    {
      return toPartyIDType (a == null ? null : a.getScheme ()).toPartyID (a == null ? null : a.getValue ());
    }

    @NonNull
    public final IMPLTYPE endpointDetailProvider (@Nullable final IAS4EndpointDetailProvider a)
    {
      m_aEndpointDetailProvider = a;
      return thisAsT ();
    }

    @NonNull
    public final IMPLTYPE smpClient (@NonNull final IBDXRExtendedServiceMetadataProvider a)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderBDXR (a));
    }

    @NonNull
    public final IMPLTYPE smpClient (@NonNull final IBDXR2ServiceMetadataProvider a)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderBDXR2 (a));
    }

    @NonNull
    public final IMPLTYPE receiverEndpointDetails (@NonNull final X509Certificate aCert,
                                                   @NonNull @Nonempty final String sDestURL)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderConstant (aCert, sDestURL));
    }

    @NonNull
    public final IMPLTYPE certificateConsumer (@Nullable final Consumer <X509Certificate> a)
    {
      m_aCertificateConsumer = a;
      return thisAsT ();
    }

    @NonNull
    public final IMPLTYPE endointURLConsumer (@Nullable final Consumer <String> a)
    {
      m_aAPEndointURLConsumer = a;
      return thisAsT ();
    }

    @NonNull
    public final IMPLTYPE useOriginalSenderFinalRecipientTypeAttr (final boolean b)
    {
      m_bUseOriginalSenderFinalRecipientTypeAttr = b;
      return thisAsT ();
    }

    protected final boolean isEndpointDetailProviderUsable ()
    {
      if (m_aEndpointDetailProvider instanceof AS4EndpointDetailProviderConstant)
        return true;

      if (m_aReceiverID == null)
        return false;
      if (m_aDocTypeID == null)
        return false;
      if (m_aProcessID == null)
        return false;
      if (m_aEndpointDetailProvider == null)
        return false;

      return true;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected ESuccess finishFields (@NonNull final AS4ResourceHelper aResHelper) throws Phase4Exception
    {
      if (!isEndpointDetailProviderUsable ())
      {
        LOGGER.error ("At least one mandatory field for endpoint discovery is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      if (m_aCertificateConsumer != null)
        m_aCertificateConsumer.accept (aReceiverCert);
      receiverCertificate (aReceiverCert);

      final String sReceiverEndpointURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sReceiverEndpointURL);
      endpointURL (sReceiverEndpointURL);

      return super.finishFields (aResHelper);
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
      if (m_aSenderID == null)
      {
        LOGGER.warn ("The field 'senderID' is not set");
        return false;
      }
      if (m_aReceiverID == null)
      {
        LOGGER.warn ("The field 'receiverID' is not set");
        return false;
      }
      if (m_aDocTypeID == null && StringHelper.isEmpty (m_sAction))
      {
        LOGGER.warn ("Neither the field 'docTypeID' nor the field 'action' is set");
        return false;
      }
      if (m_aProcessID == null && StringHelper.isEmpty (m_sService))
      {
        LOGGER.warn ("Neither the field 'processID' nor the field 'service' is set");
        return false;
      }
      if (m_aEndpointDetailProvider == null)
      {
        LOGGER.warn ("The field 'endpointDetailProvider' is not set");
        return false;
      }

      return true;
    }

    @Override
    protected void customizeBeforeSending () throws Phase4Exception
    {
      // Add mandatory properties
      if (m_bUseOriginalSenderFinalRecipientTypeAttr)
      {
        addMessageProperty (MessageProperty.builder ()
                                           .name (CAS4.ORIGINAL_SENDER)
                                           .type (m_aSenderID.getScheme ())
                                           .value (m_aSenderID.getValue ()));
        addMessageProperty (MessageProperty.builder ()
                                           .name (CAS4.FINAL_RECIPIENT)
                                           .type (m_aReceiverID.getScheme ())
                                           .value (m_aReceiverID.getValue ()));
      }
      else
      {
        addMessageProperty (MessageProperty.builder ()
                                           .name (CAS4.ORIGINAL_SENDER)
                                           .value (m_aSenderID.getURIEncoded ()));
        addMessageProperty (MessageProperty.builder ()
                                           .name (CAS4.FINAL_RECIPIENT)
                                           .value (m_aReceiverID.getURIEncoded ()));
      }
    }
  }

  /**
   * The builder class for sending AS4 messages using the eDelivery AS4 2.0 Common Usage Profile
   * (EdDSA/X25519). Use {@link #sendMessage()} or {@link #sendMessageAndCheckForReceipt()} to
   * trigger the main transmission.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class EDelivery2EdDSAUserMessageBuilder extends
                                                        AbstractEDelivery2UserMessageBuilder <EDelivery2EdDSAUserMessageBuilder>
  {
    public EDelivery2EdDSAUserMessageBuilder ()
    {
      super (AS4EDelivery2ProfileRegistarSPI.AS4_PROFILE_ID_EDDSA_FOUR_CORNER);

      // Set default crypto params for EdDSA/X25519
      signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.EDDSA_ED25519);
      cryptParams ().setAlgorithmCrypt (com.helger.phase4.crypto.ECryptoAlgorithmCrypt.AES_128_GCM)
                    .setEDelivery2KeyAgreementX25519 ();
    }
  }

  /**
   * The builder class for sending AS4 messages using the eDelivery AS4 2.0 Alternative EC Profile
   * (ECDSA/ECDH-ES with secp256r1). Use {@link #sendMessage()} or
   * {@link #sendMessageAndCheckForReceipt()} to trigger the main transmission.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class EDelivery2ECDSAUserMessageBuilder extends
                                                        AbstractEDelivery2UserMessageBuilder <EDelivery2ECDSAUserMessageBuilder>
  {
    public EDelivery2ECDSAUserMessageBuilder ()
    {
      super (AS4EDelivery2ProfileRegistarSPI.AS4_PROFILE_ID_ECDSA_FOUR_CORNER);

      // Set default crypto params for ECDSA/ECDH-ES
      signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.ECDSA_SHA_256);
      cryptParams ().setAlgorithmCrypt (com.helger.phase4.crypto.ECryptoAlgorithmCrypt.AES_128_GCM)
                    .setEDelivery2KeyAgreementECDHES ();
    }
  }
}
