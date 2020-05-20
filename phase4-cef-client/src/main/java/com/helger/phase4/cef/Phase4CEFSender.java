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
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.HttpClientFactory;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.client.IAS4SignalMessageConsumer;
import com.helger.phase4.crypto.AS4CryptoFactoryPropertiesFile;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.servlet.AS4BidirectionalClientHelper;
import com.helger.phase4.soap.ESoapVersion;
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
  public static abstract class AbstractBaseBuilder <IMPLTYPE extends AbstractBaseBuilder <IMPLTYPE>> implements IGenericImplTrait <IMPLTYPE>
  {
    protected HttpClientFactory m_aHttpClientFactory;
    protected IAS4CryptoFactory m_aCryptoFactory;
    protected IPModeResolver m_aPModeResolver;
    protected IIncomingAttachmentFactory m_aIAF;
    protected IPMode m_aPMode;

    protected IParticipantIdentifier m_aSenderID;
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;
    protected String m_sAgreementRef;
    protected IParticipantIdentifier m_aFromPartyID;
    protected IParticipantIdentifier m_aToPartyID;
    protected String m_sMessageID;
    protected String m_sConversationID;

    protected IMimeType m_aPayloadMimeType;
    protected boolean m_bCompressPayload;

    protected IPhase4CEFEndpointDetailProvider m_aEndpointDetailProvider;

    protected IAS4ClientBuildMessageCallback m_aBuildMessageCallback;
    protected IAS4OutgoingDumper m_aOutgoingDumper;
    protected IAS4IncomingDumper m_aIncomingDumper;
    protected IAS4RetryCallback m_aRetryCallback;
    protected IAS4RawResponseConsumer m_aResponseConsumer;
    protected IAS4SignalMessageConsumer m_aSignalMsgConsumer;

    /**
     * Create a new builder, with the following fields already set:<br>
     * {@link #setHttpClientFactory(HttpClientFactory)}<br>
     * {@link #setCryptoFactory(IAS4CryptoFactory)}<br>
     * {@link #setPModeResolver(IPModeResolver)}<br>
     * {@link #setIncomingAttachmentFactory(IIncomingAttachmentFactory)}<br>
     * {@link #setPMode(IPMode)}<br>
     * {@link #setPayloadMimeType(IMimeType)}<br>
     * {@link #setCompressPayload(boolean)}<br>
     */
    protected AbstractBaseBuilder ()
    {
      // Set default values
      try
      {
        setHttpClientFactory (new HttpClientFactory (new Phase4CEFHttpClientSettings ()));
        setCryptoFactory (AS4CryptoFactoryPropertiesFile.getDefaultInstance ());
        final IPModeResolver aPModeResolver = DefaultPModeResolver.DEFAULT_PMODE_RESOLVER;
        setPModeResolver (aPModeResolver);
        setIncomingAttachmentFactory (IIncomingAttachmentFactory.DEFAULT_INSTANCE);
        setPMode (aPModeResolver.getPModeOfID (null, "s", "a", "i", "r", null));
        setPayloadMimeType (CMimeType.APPLICATION_XML);
        setCompressPayload (true);
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the HTTP client factory to be used. By default an instance of
     * {@link HttpClientFactory} is used and there is no need to invoke this
     * method.
     *
     * @param aHttpClientFactory
     *        The new HTTP client factory to be used. May not be
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setHttpClientFactory (@Nonnull final HttpClientFactory aHttpClientFactory)
    {
      ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
      m_aHttpClientFactory = aHttpClientFactory;
      return thisAsT ();
    }

    /**
     * Set the crypto factory to be used. The default crypto factory uses the
     * properties from the file "crypto.properties".
     *
     * @param aCryptoFactory
     *        The crypto factory to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setCryptoFactory (@Nonnull final IAS4CryptoFactory aCryptoFactory)
    {
      ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
      m_aCryptoFactory = aCryptoFactory;
      return thisAsT ();
    }

    /**
     * Set the PMode resolver to be used.
     *
     * @param aPModeResolver
     *        The PMode resolver to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setPModeResolver (@Nonnull final IPModeResolver aPModeResolver)
    {
      ValueEnforcer.notNull (aPModeResolver, "aPModeResolver");
      m_aPModeResolver = aPModeResolver;
      return thisAsT ();
    }

    /**
     * Set the incoming attachment factory to be used.
     *
     * @param aIAF
     *        The incoming attachment factory to be used. May not be
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setIncomingAttachmentFactory (@Nonnull final IIncomingAttachmentFactory aIAF)
    {
      ValueEnforcer.notNull (aIAF, "IAF");
      m_aIAF = aIAF;
      return thisAsT ();
    }

    /**
     * @return The used P-Mode. Never <code>null</code>.
     */
    @Nonnull
    public final IPMode getPMode ()
    {
      return m_aPMode;
    }

    /**
     * Set the PMode to be used. By default a generic PMode for CEF purposes is
     * used so there is no need to invoke this method.
     *
     * @param aPMode
     *        The PMode to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setPMode (@Nonnull final IPMode aPMode)
    {
      ValueEnforcer.notNull (aPMode, "PMode");
      m_aPMode = aPMode;
      return thisAsT ();
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
    public final IMPLTYPE setSenderParticipantID (@Nonnull final IParticipantIdentifier aSenderID)
    {
      ValueEnforcer.notNull (aSenderID, "SenderID");
      m_aSenderID = aSenderID;
      return thisAsT ();
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must
     * be provided prior to sending. This ends up in the "finalRecipient"
     * UserMessage property.
     *
     * @param aReceiverID
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setReceiverParticipantID (@Nonnull final IParticipantIdentifier aReceiverID)
    {
      ValueEnforcer.notNull (aReceiverID, "ReceiverID");
      m_aReceiverID = aReceiverID;
      return thisAsT ();
    }

    /**
     * Set the document type ID to be send. The document type must be provided
     * prior to sending.
     *
     * @param aDocTypeID
     *        The document type ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
    {
      ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
      m_aDocTypeID = aDocTypeID;
      return thisAsT ();
    }

    /**
     * Set the process ID to be send. The process ID must be provided prior to
     * sending.
     *
     * @param aProcessID
     *        The process ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setProcessID (@Nonnull final IProcessIdentifier aProcessID)
    {
      ValueEnforcer.notNull (aProcessID, "ProcessID");
      m_aProcessID = aProcessID;
      return thisAsT ();
    }

    /**
     * Set the "AgreementRef" value. It's optional.
     * 
     * @param sAgreementRef
     *        Agreement reference. May be <code>null</code>.
     * @return this for chaining.
     */
    @Nonnull
    public final IMPLTYPE setAgreementRef (@Nullable final String sAgreementRef)
    {
      ValueEnforcer.notNull (sAgreementRef, "AgreementRef");
      m_sAgreementRef = sAgreementRef;
      return thisAsT ();
    }

    /**
     * Set the "from party ID". This is mandatory
     *
     * @param aFromPartyID
     *        The from party ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setFromPartyID (@Nonnull @Nonempty final IParticipantIdentifier aFromPartyID)
    {
      ValueEnforcer.notNull (aFromPartyID, "FromPartyID");
      m_aFromPartyID = aFromPartyID;
      return thisAsT ();
    }

    /**
     * Set the "to party ID". This is mandatory
     *
     * @param aToPartyID
     *        The to party ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setToPartyID (@Nonnull @Nonempty final IParticipantIdentifier aToPartyID)
    {
      ValueEnforcer.notNull (aToPartyID, "ToPartyID");
      m_aToPartyID = aToPartyID;
      return thisAsT ();
    }

    /**
     * Set the optional AS4 message ID. If this field is not set, a random
     * message ID is created.
     *
     * @param sMessageID
     *        The optional AS4 message ID to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setMessageID (@Nullable final String sMessageID)
    {
      m_sMessageID = sMessageID;
      return thisAsT ();
    }

    /**
     * Set the optional AS4 conversation ID. If this field is not set, a random
     * conversation ID is created.
     *
     * @param sConversationID
     *        The optional AS4 conversation ID to be used. May be
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setConversationID (@Nullable final String sConversationID)
    {
      m_sConversationID = sConversationID;
      return thisAsT ();
    }

    /**
     * Set the abstract endpoint detail provider to be used. This can be an SMP
     * lookup routine or in certain test cases a predefined certificate and
     * endpoint URL.
     *
     * @param aEndpointDetailProvider
     *        The endpoint detail provider to be used. May not be
     *        <code>null</code>.
     * @return this for chaining
     * @see #setSMPClient(IBDXRServiceMetadataProvider)
     */
    @Nonnull
    public final IMPLTYPE setEndpointDetailProvider (@Nonnull final IPhase4CEFEndpointDetailProvider aEndpointDetailProvider)
    {
      ValueEnforcer.notNull (aEndpointDetailProvider, "EndpointDetailProvider");
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
     * @see #setEndpointDetailProvider(IPhase4CEFEndpointDetailProvider)
     */
    @Nonnull
    public final IMPLTYPE setSMPClient (@Nonnull final IBDXRServiceMetadataProvider aSMPClient)
    {
      return setEndpointDetailProvider (new Phase4CEFEndpointDetailProviderBDXR (aSMPClient));
    }

    @Nonnull
    public final IMPLTYPE setReceiverEndpointDetails (@Nonnull final X509Certificate aCert, @Nonnull @Nonempty final String sDestURL)
    {
      return setEndpointDetailProvider (new Phase4CEFEndpointDetailProviderConstant (aCert, sDestURL));
    }

    /**
     * Set the MIME type of the payload. By default it is
     * <code>application/xml</code> and MUST usually not be changed. This value
     * is required for sending.
     *
     * @param aPayloadMimeType
     *        The payload MIME type. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setPayloadMimeType (@Nonnull final IMimeType aPayloadMimeType)
    {
      ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
      m_aPayloadMimeType = aPayloadMimeType;
      return thisAsT ();
    }

    /**
     * Enable or disable the AS4 compression of the payload. By default
     * compression is disabled.
     *
     * @param bCompressPayload
     *        <code>true</code> to compress the payload, <code>false</code> to
     *        not compress it.
     * @return this for chaining.
     */
    @Nonnull
    public final IMPLTYPE setCompressPayload (final boolean bCompressPayload)
    {
      m_bCompressPayload = bCompressPayload;
      return thisAsT ();
    }

    /**
     * Set a internal message callback. Usually this method is NOT needed. Use
     * only when you know what you are doing.
     *
     * @param aBuildMessageCallback
     *        An internal to be used for the created message. Maybe
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setBuildMessageCallback (@Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback)
    {
      m_aBuildMessageCallback = aBuildMessageCallback;
      return thisAsT ();
    }

    /**
     * Set a specific outgoing dumper for this builder.
     *
     * @param aOutgoingDumper
     *        An outgoing dumper to be used. Maybe <code>null</code>. If
     *        <code>null</code> the global outgoing dumper is used.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setOutgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
    {
      m_aOutgoingDumper = aOutgoingDumper;
      return thisAsT ();
    }

    /**
     * Set a specific incoming dumper for this builder.
     *
     * @param aIncomingDumper
     *        An incoming dumper to be used. Maybe <code>null</code>. If
     *        <code>null</code> the global incoming dumper is used.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setIncomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
    {
      m_aIncomingDumper = aIncomingDumper;
      return thisAsT ();
    }

    /**
     * Set an optional handler that is notified if an http sending will be
     * retried. This method is optional and must not be called prior to sending.
     *
     * @param aRetryCallback
     *        The optional retry callback. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setRetryCallback (@Nullable final IAS4RetryCallback aRetryCallback)
    {
      m_aRetryCallback = aRetryCallback;
      return thisAsT ();
    }

    /**
     * Set an optional handler for the synchronous result message received from
     * the other side. This method is optional and must not be called prior to
     * sending.
     *
     * @param aResponseConsumer
     *        The optional response consumer. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setRawResponseConsumer (@Nullable final IAS4RawResponseConsumer aResponseConsumer)
    {
      m_aResponseConsumer = aResponseConsumer;
      return thisAsT ();
    }

    /**
     * Set an optional Ebms3 Signal Message Consumer. If this consumer is set,
     * the response is trying to be parsed as a Signal Message. This method is
     * optional and must not be called prior to sending.
     *
     * @param aSignalMsgConsumer
     *        The optional signal message consumer. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setSignalMsgConsumer (@Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer)
    {
      m_aSignalMsgConsumer = aSignalMsgConsumer;
      return thisAsT ();
    }

    @OverridingMethodsMustInvokeSuper
    public boolean isEveryRequiredFieldSet ()
    {
      if (m_aHttpClientFactory == null)
        return false;
      // m_aCryptoFactory may be null
      if (m_aPMode == null)
        return false;

      if (m_aSenderID == null)
        return false;
      if (m_aReceiverID == null)
        return false;
      if (m_aDocTypeID == null)
        return false;
      if (m_aProcessID == null)
        return false;
      if (m_aFromPartyID == null)
        return false;
      if (m_aToPartyID == null)
        return false;
      // m_sMessageID is optional
      // m_sConversationID is optional
      if (m_aEndpointDetailProvider == null)
        return false;

      if (m_aPayloadMimeType == null)
        return false;
      // m_bCompressPayload cannot be null

      // m_aBuildMessageCallback may be null
      // m_aOutgoingDumper may be null
      // m_aIncomingDumper may be null
      // m_aResponseConsumer may be null
      // m_aSignalMsgConsumer may be null

      return true;
    }

    /**
     * Synchronously send the AS4 message. Before sending,
     * {@link #isEveryRequiredFieldSet()} is called to check that the mandatory
     * elements are set.
     *
     * @return {@link ESuccess#FAILURE} if not all mandatory parameters are set
     *         or if sending failed, {@link ESuccess#SUCCESS} upon success.
     *         Never <code>null</code>.
     * @throws Phase4CEFException
     *         In case of any error
     * @see #isEveryRequiredFieldSet()
     */
    @Nonnull
    public abstract ESuccess sendMessage () throws Phase4CEFException;
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
    private byte [] m_aPayloadBytes;
    private Consumer <X509Certificate> m_aCertificateConsumer;
    private Consumer <String> m_aAPEndointURLConsumer;

    /**
     * Create a new builder, with the following fields already set:<br>
     * {@link #setHttpClientFactory(HttpClientFactory)}<br>
     * {@link #setCryptoFactory(IAS4CryptoFactory)}<br>
     * {@link #setPModeResolver(IPModeResolver)}<br>
     * {@link #setIncomingAttachmentFactory(IIncomingAttachmentFactory)}<br>
     * {@link #setPMode(IPMode)}<br>
     * {@link #setPayloadMimeType(IMimeType)}<br>
     * {@link #setCompressPayload(boolean)}<br>
     */
    public Builder ()
    {}

    /**
     * Set the payload to be used as a byte array. It will be parsed internally
     * to a DOM element. If this method is called, it overwrites any other
     * explicitly set payload.
     *
     * @param aPayloadBytes
     *        The payload bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setPayload (@Nonnull final byte [] aPayloadBytes)
    {
      ValueEnforcer.notNull (aPayloadBytes, "PayloadBytes");
      m_aPayloadBytes = aPayloadBytes;
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
    public Builder setCertificateConsumer (@Nullable final Consumer <X509Certificate> aCertificateConsumer)
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
    public Builder setAPEndointURLConsumer (@Nullable final Consumer <String> aAPEndointURLConsumer)
    {
      m_aAPEndointURLConsumer = aAPEndointURLConsumer;
      return this;
    }

    @Override
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayloadBytes == null)
        return false;

      // All valid
      return true;
    }

    @Override
    @Nonnull
    public ESuccess sendMessage () throws Phase4CEFException
    {
      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // e.g. SMP lookup (may throw an exception)
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup (may throw an exception)
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      if (m_aCertificateConsumer != null)
        m_aCertificateConsumer.accept (aReceiverCert);

      // URL from e.g. SMP lookup (may throw an exception)
      final String sReceiverEndpointURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sReceiverEndpointURL);

      // Temporary file manager
      try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
      {
        final ICommonsList <WSS4JAttachment> aPayloads = new CommonsArrayList <> ();
        aPayloads.add (WSS4JAttachment.createOutgoingFileAttachment (m_aPayloadBytes,
                                                                     null,
                                                                     "document.xml",
                                                                     m_aPayloadMimeType,
                                                                     m_bCompressPayload ? EAS4CompressionMode.GZIP : null,
                                                                     aResHelper));

        // Start building AS4 User Message
        final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
        aUserMsg.setHttpClientFactory (m_aHttpClientFactory);

        // Otherwise Oxalis dies
        aUserMsg.setQuoteHttpHeaders (false);
        aUserMsg.setSoapVersion (ESoapVersion.SOAP_12);
        // Set the keystore/truststore parameters
        aUserMsg.setAS4CryptoFactory (m_aCryptoFactory);
        aUserMsg.setPMode (m_aPMode, true);

        // Set after PMode
        aUserMsg.cryptParams ().setCertificate (aReceiverCert);

        // Explicit parameters have precedence over PMode
        aUserMsg.setAgreementRefValue (m_sAgreementRef);
        // The eb3:AgreementRef element also includes an optional attribute
        // pmode
        // which can be used to include the PMode.ID. This attribute MUST NOT be
        // used as Access Points may use just one generic P-Mode for receiving
        // messages.
        aUserMsg.setPModeIDFactory (x -> null);
        aUserMsg.setServiceType (m_aProcessID.getScheme ());
        aUserMsg.setServiceValue (m_aProcessID.getValue ());
        aUserMsg.setAction (m_aDocTypeID.getURIEncoded ());
        if (StringHelper.hasText (m_sMessageID))
          aUserMsg.setMessageID (m_sMessageID);
        aUserMsg.setConversationID (StringHelper.hasText (m_sConversationID) ? m_sConversationID : UUID.randomUUID ().toString ());

        // Backend or gateway?
        aUserMsg.setFromPartyIDType (m_aFromPartyID.getScheme ());
        aUserMsg.setFromPartyID (m_aFromPartyID.getValue ());
        aUserMsg.setToPartyIDType (m_aToPartyID.getScheme ());
        aUserMsg.setToPartyID (m_aToPartyID.getValue ());

        aUserMsg.ebms3Properties ()
                .add (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER, m_aSenderID.getScheme (), m_aSenderID.getValue ()));
        aUserMsg.ebms3Properties ()
                .add (MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT,
                                                                m_aReceiverID.getScheme (),
                                                                m_aReceiverID.getValue ()));

        // No payload - only one attachment
        aUserMsg.setPayload (null);

        // Add main attachment
        for (final WSS4JAttachment aAttachment : aPayloads)
          aUserMsg.addAttachment (aAttachment);

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
