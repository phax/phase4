/**
 * Copyright (C) 2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.cef;

import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ESuccess;
import com.helger.httpclient.HttpClientFactory;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.model.MessageProperty;
import com.helger.phase4.sender.AS4BidirectionalClientHelper;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.smpclient.bdxr1.IBDXRServiceMetadataProvider;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.IBDXLURLProvider;

/**
 * This class contains all the specifics to send AS4 messages with the CEF
 * profile. See <code>sendAS4Message</code> as the main method to trigger the
 * sending, with all potential customization.
 *
 * @author Philip Helger
 * @since 0.9.15
 */
@Immutable
public final class Phase4CEFSender
{
  public static final SimpleIdentifierFactory IF = SimpleIdentifierFactory.INSTANCE;
  public static final IBDXLURLProvider URL_PROVIDER = BDXLURLProvider.INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4CEFSender.class);

  private Phase4CEFSender ()
  {}

  /**
   * @return Create a new Builder for AS4 messages if the payload is present and
   *         the SBDH should be created internally. Never <code>null</code>.
   */
  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * Abstract builder base class with the minimum requirements configuration
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  public static abstract class AbstractBaseBuilder <IMPLTYPE extends AbstractBaseBuilder <IMPLTYPE>> extends
                                                   AbstractAS4UserMessageBuilder <IMPLTYPE>
  {
    protected IParticipantIdentifier m_aSenderID;
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;
    protected IPhase4CEFEndpointDetailProvider m_aEndpointDetailProvider;

    protected AbstractBaseBuilder ()
    {
      // Override default values
      try
      {
        httpClientFactory (new HttpClientFactory (new Phase4CEFHttpClientSettings ()));
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the sender participant ID of the message. The participant ID must be
     * provided prior to sending. This ends up in the "originalSender"
     * UserMessage property.
     *
     * @param aSenderID
     *        The sender participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE senderParticipantID (@Nullable final IParticipantIdentifier aSenderID)
    {
      m_aSenderID = aSenderID;
      return thisAsT ();
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must
     * be provided prior to sending. This ends up in the "finalRecipient"
     * UserMessage property.
     *
     * @param a
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE receiverParticipantID (@Nullable final IParticipantIdentifier a)
    {
      m_aReceiverID = a;
      return thisAsT ();
    }

    /**
     * Set the document type ID to be send. The document type must be provided
     * prior to sending.
     *
     * @param a
     *        The document type ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE documentTypeID (@Nullable final IDocumentTypeIdentifier a)
    {
      m_aDocTypeID = a;
      return action (a == null ? null : a.getURIEncoded ());
    }

    /**
     * Set the process ID to be send. The process ID must be provided prior to
     * sending.
     *
     * @param a
     *        The process ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
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
    @Nonnull
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
    @Nonnull
    public final IMPLTYPE toPartyID (@Nullable final IParticipantIdentifier a)
    {
      return toPartyIDType (a == null ? null : a.getScheme ()).toPartyID (a == null ? null : a.getValue ());
    }

    /**
     * Set the abstract endpoint detail provider to be used. This can be an SMP
     * lookup routine or in certain test cases a predefined certificate and
     * endpoint URL.
     *
     * @param aEndpointDetailProvider
     *        The endpoint detail provider to be used. May be <code>null</code>.
     * @return this for chaining
     * @see #smpClient(IBDXRServiceMetadataProvider)
     */
    @Nonnull
    public final IMPLTYPE endpointDetailProvider (@Nullable final IPhase4CEFEndpointDetailProvider aEndpointDetailProvider)
    {
      m_aEndpointDetailProvider = aEndpointDetailProvider;
      return thisAsT ();
    }

    /**
     * Set the SMP client to be used. This is the point where e.g. the
     * differentiation between SMK and SML can be done. This must be set prior
     * to sending.
     *
     * @param aSMPClient
     *        The SMP client to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #endpointDetailProvider(IPhase4CEFEndpointDetailProvider)
     */
    @Nonnull
    public final IMPLTYPE smpClient (@Nonnull final IBDXRServiceMetadataProvider aSMPClient)
    {
      return endpointDetailProvider (new Phase4CEFEndpointDetailProviderBDXR (aSMPClient));
    }

    @Nonnull
    public final IMPLTYPE receiverEndpointDetails (@Nonnull final X509Certificate aCert, @Nonnull @Nonempty final String sDestURL)
    {
      return endpointDetailProvider (new Phase4CEFEndpointDetailProviderConstant (aCert, sDestURL));
    }

    protected final boolean isEndpointDetailProviderUsable ()
    {
      // Sender ID doesn't matter here
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
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aSenderID == null)
        return false;
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
  }

  /**
   * The builder class for sending AS4 messages using CEF profile specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.<br>
   * This builder class assumes, that only the payload (e.g. the Invoice) is
   * present, and that both validation and SBDH creation happens inside.
   *
   * @author Philip Helger
   */
  public static class Builder extends AbstractBaseBuilder <Builder>
  {
    private Phase4OutgoingAttachment m_aPayload;
    private Consumer <X509Certificate> m_aCertificateConsumer;
    private Consumer <String> m_aAPEndointURLConsumer;

    public Builder ()
    {}

    /**
     * Set the payload to be send out.
     *
     * @param a
     *        The payload builder to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder payload (@Nullable final Phase4OutgoingAttachment.Builder a)
    {
      return payload (a == null ? null : a.build ());
    }

    /**
     * Set the payload to be send out.
     *
     * @param a
     *        The payload to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder payload (@Nullable final Phase4OutgoingAttachment a)
    {
      m_aPayload = a;
      return this;
    }

    /**
     * Set an optional Consumer for the retrieved certificate, independent of
     * its usability.
     *
     * @param aCertificateConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder certificateConsumer (@Nullable final Consumer <X509Certificate> aCertificateConsumer)
    {
      m_aCertificateConsumer = aCertificateConsumer;
      return this;
    }

    /**
     * Set an optional Consumer for the destination AP address, independent of
     * its usability.
     *
     * @param aAPEndointURLConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder endointURLConsumer (@Nullable final Consumer <String> aAPEndointURLConsumer)
    {
      m_aAPEndointURLConsumer = aAPEndointURLConsumer;
      return this;
    }

    @Override
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayload == null)
        return false;
      // m_aCertificateConsumer is optional
      // m_aAPEndointURLConsumer is optional

      // All valid
      return true;
    }

    @Override
    @Nonnull
    public ESuccess sendMessage () throws Phase4CEFException
    {
      if (!isEndpointDetailProviderUsable ())
      {
        LOGGER.error ("At least one mandatory field for endpoint discovery is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // e.g. SMP lookup (may throw an exception)
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup (may throw an exception)
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      if (m_aCertificateConsumer != null)
        m_aCertificateConsumer.accept (aReceiverCert);
      receiverCertificate (aReceiverCert);

      // URL from e.g. SMP lookup (may throw an exception)
      final String sReceiverEndpointURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sReceiverEndpointURL);
      endpointURL (sReceiverEndpointURL);

      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // Add mandatory properties
      addMessageProperty (MessageProperty.builder ()
                                         .name (CAS4.ORIGINAL_SENDER)
                                         .type (m_aSenderID.getScheme ())
                                         .value (m_aSenderID.getValue ()));
      addMessageProperty (MessageProperty.builder ()
                                         .name (CAS4.FINAL_RECIPIENT)
                                         .type (m_aReceiverID.getScheme ())
                                         .value (m_aReceiverID.getValue ()));

      // Temporary file manager
      try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
      {
        // Start building AS4 User Message
        final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
        applyToUserMessage (aUserMsg);

        // No payload - only one attachment
        aUserMsg.setPayload (null);

        // Add main attachment
        aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (m_aPayload, aResHelper));

        // Add other attachments
        for (final Phase4OutgoingAttachment aAttachment : m_aAttachments)
          aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aAttachment, aResHelper));

        // Main sending
        AS4BidirectionalClientHelper.sendAS4AndReceiveAS4 (m_aCryptoFactory,
                                                           m_aPModeResolver,
                                                           m_aIAF,
                                                           aUserMsg,
                                                           Locale.US,
                                                           sReceiverEndpointURL,
                                                           m_aBuildMessageCallback,
                                                           m_aOutgoingDumper,
                                                           m_aIncomingDumper,
                                                           m_aRetryCallback,
                                                           m_aResponseConsumer,
                                                           m_aSignalMsgConsumer);
      }
      catch (final Phase4CEFException ex)
      {
        // Re-throw
        throw ex;
      }
      catch (final Exception ex)
      {
        // wrap
        throw new Phase4CEFException ("Wrapped Phase4CEFException", ex);
      }
      return ESuccess.SUCCESS;
    }
  }
}
