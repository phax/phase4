/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.state.ESuccess;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHData;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHDataWriter;
import com.helger.hredelivery.commons.smp.HRMPSClientReadOnly;
import com.helger.hredelivery.commons.url.HREDeliveryNaptrURLProvider;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifierParts;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.dynamicdiscovery.IAS4EndpointDetailProvider;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.MessageProperty;
import com.helger.phase4.profile.hredelivery.AS4HREDeliveryProfileRegistarSPI;
import com.helger.phase4.profile.hredelivery.HREDeliveryPMode;
import com.helger.phase4.profile.hredelivery.Phase4HREDeliveryHttpClientSettings;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.sender.IAS4SendingDateTimeConsumer;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;
import com.helger.phive.api.executorset.IValidationExecutorSetRegistry;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.sbdh.CSBDH;
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.ERevocationCheckMode;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xsds.bdxr.smp1.EndpointType;

/**
 * This class contains all the specifics to send AS4 messages to HR eDelivery. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with all potential
 * customization.
 *
 * @author Philip Helger
 * @since 4.0.2
 */
@Immutable
public final class Phase4HREdeliverySender
{
  // Seems to be fine to use the Peppol IDs for now
  public static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  public static final ISMPURLProvider URL_PROVIDER = HREDeliveryNaptrURLProvider.INSTANCE;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4HREdeliverySender.class);

  private Phase4HREdeliverySender ()
  {}

  @Nullable
  private static HREDeliverySBDHData _createHREDeliverySBDHData (@NonNull final IParticipantIdentifier aSenderID,
                                                                 @NonNull final IParticipantIdentifier aReceiverID,
                                                                 @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                                 @Nullable final String sInstanceIdentifier,
                                                                 @Nullable final String sStandard,
                                                                 @Nullable final String sTypeVersion,
                                                                 @Nullable final String sType,
                                                                 @NonNull final Element aPayloadElement,
                                                                 final boolean bClonePayloadElement)
  {
    final HREDeliverySBDHData aData = new HREDeliverySBDHData (IF);
    aData.setSender (aSenderID);
    aData.setReceiver (aReceiverID);

    String sRealStandard = sStandard;
    if (StringHelper.isEmpty (sRealStandard))
    {
      sRealStandard = aPayloadElement.getNamespaceURI ();
    }
    if (StringHelper.isEmpty (sRealStandard))
    {
      LOGGER.warn ("No Standard was provided and none could be deduced from the payload element (XML payloads without namespace URI are not permitted)");
      return null;
    }

    String sRealTypeVersion = sTypeVersion;
    if (StringHelper.isEmpty (sRealTypeVersion))
    {
      // Determine from document type
      try
      {
        final IPeppolDocumentTypeIdentifierParts aParts = PeppolDocumentTypeIdentifierParts.extractFromIdentifier (aDocTypeID);
        sRealTypeVersion = aParts.getVersion ();
      }
      catch (final IllegalArgumentException ex)
      {
        // failure
      }
    }
    if (StringHelper.isEmpty (sRealTypeVersion))
    {
      LOGGER.warn ("No TypeVersion was provided and none could be deduced from the document type identifier '" +
                   aDocTypeID.getURIEncoded () +
                   "'");
      return null;
    }

    String sRealType = sType;
    if (StringHelper.isEmpty (sRealType))
    {
      sRealType = aPayloadElement.getLocalName ();
    }
    if (StringHelper.isEmpty (sRealType))
    {
      LOGGER.warn ("No Type was provided and none could be deduced from the payload element");
      return null;
    }

    String sRealInstanceIdentifier = sInstanceIdentifier;
    if (StringHelper.isEmpty (sRealInstanceIdentifier))
    {
      // Create a new random UUID as the SBDH Instance Identifier
      sRealInstanceIdentifier = UUID.randomUUID ().toString ();
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("As no SBDH InstanceIdentifier was provided, a random one was created: '" +
                      sRealInstanceIdentifier +
                      "'");
    }

    aData.setDocumentIdentification (sRealStandard,
                                     sRealTypeVersion,
                                     sRealType,
                                     sRealInstanceIdentifier,
                                     MetaAS4Manager.getTimestampMgr ().getCurrentXMLDateTime ());

    // Not cloning the payload element is for saving memory only (if it can be
    // ensured, the source payload element is not altered externally of course)
    if (bClonePayloadElement)
      aData.setBusinessMessage (aPayloadElement);
    else
      aData.setBusinessMessageNoClone (aPayloadElement);

    // Check with logging
    if (!aData.areAllFieldsSet (true))
      throw new IllegalArgumentException ("The HR eDelivery SBDH data is incomplete. See logs for details.");

    return aData;
  }

  @Nullable
  private static StandardBusinessDocument _createSBD (@NonNull final IParticipantIdentifier aSenderID,
                                                      @NonNull final IParticipantIdentifier aReceiverID,
                                                      @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                      @Nullable final String sInstanceIdentifier,
                                                      @Nullable final String sStandard,
                                                      @Nullable final String sTypeVersion,
                                                      @Nullable final String sType,
                                                      @NonNull final Element aPayloadElement,
                                                      final boolean bClonePayloadElement)
  {
    final HREDeliverySBDHData aData = _createHREDeliverySBDHData (aSenderID,
                                                                  aReceiverID,
                                                                  aDocTypeID,
                                                                  sInstanceIdentifier,
                                                                  sStandard,
                                                                  sTypeVersion,
                                                                  sType,
                                                                  aPayloadElement,
                                                                  bClonePayloadElement);
    if (aData == null)
      return null;

    // We never need to clone the payload element here because it was evtl.
    // cloned before
    return new HREDeliverySBDHDataWriter ().setFavourSpeed (true).createStandardBusinessDocument (aData);
  }

  /**
   * @param aSenderID
   *        Sender participant ID. May not be <code>null</code>.
   * @param aReceiverID
   *        Receiver participant ID. May not be <code>null</code>.
   * @param aDocTypeID
   *        Document type ID. May not be <code>null</code>.
   * @param sInstanceIdentifier
   *        SBDH instance identifier. May be <code>null</code> to create a random ID.
   * @param sStandard
   *        SBDH standard (e.g. the XML payload root element namespace URI). May be
   *        <code>null</code> to use the default.
   * @param sTypeVersion
   *        SBDH syntax version ID (e.g. "2.1" for OASIS UBL 2.1). May be <code>null</code> to use
   *        the default.
   * @param sType
   *        SBDH type (e.g. the XML payload root element local name). May be <code>null</code> to
   *        use the default.
   * @param aPayloadElement
   *        Payload element to be wrapped. May not be <code>null</code>.
   * @return The domain object representation of the created SBDH or <code>null</code> if not all
   *         parameters are present.
   */
  @Nullable
  public static StandardBusinessDocument createSBDH (@NonNull final IParticipantIdentifier aSenderID,
                                                     @NonNull final IParticipantIdentifier aReceiverID,
                                                     @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                     @Nullable final String sInstanceIdentifier,
                                                     @Nullable final String sStandard,
                                                     @Nullable final String sTypeVersion,
                                                     @Nullable final String sType,
                                                     @NonNull final Element aPayloadElement)
  {
    return _createSBD (aSenderID,
                       aReceiverID,
                       aDocTypeID,
                       sInstanceIdentifier,
                       sStandard,
                       sTypeVersion,
                       sType,
                       aPayloadElement,
                       true);
  }

  /**
   * @param aPayloadElement
   *        The payload element to be validated. May not be <code>null</code>.
   * @param aRegistry
   *        The validation registry to be used. May be <code>null</code> to indicate to use the
   *        default one.
   * @param aVESID
   *        The VESID to validate against. May be <code>null</code>.
   * @param aValidationResultHandler
   *        The validation result handler to be used. May be <code>null</code>.
   * @throws Phase4Exception
   *         If the validation result handler decides to do so....
   */
  private static void _validatePayload (@NonNull final Element aPayloadElement,
                                        @Nullable final IValidationExecutorSetRegistry <IValidationSourceXML> aRegistry,
                                        @Nullable final DVRCoordinate aVESID,
                                        @Nullable final IPhase4HREdeliveryValidationResultHandler aValidationResultHandler) throws Phase4Exception
  {
    // Client side validation
    if (aRegistry != null && aVESID != null && aValidationResultHandler != null)
    {
      // Custom registry
      Phase4HREdeliveryValidation.validateOutgoingBusinessDocument (aPayloadElement,
                                                                    aRegistry,
                                                                    aVESID,
                                                                    aValidationResultHandler);
    }
    else
    {
      if (aValidationResultHandler != null)
        LOGGER.warn ("A ValidationResultHandler is present but VESID or VES Registry is missing - therefore no validation is performed");
    }
  }

  /**
   * Check if the provided certificate is a valid HR eDelivery AP certificate.
   *
   * @param aCAChecker
   *        The HR eDelivery CA checker to be used to verify the HR eDelivery AP certificate. May
   *        not be <code>null</code>.
   * @param aReceiverCert
   *        The determined receiver AP certificate to check. Never <code>null</code>.
   * @param aCertificateConsumer
   *        An optional consumer that is invoked with the received AP certificate to be used for the
   *        transmission. The certification check result must be considered when used. May be
   *        <code>null</code>.
   * @param eCacheOSCResult
   *        Possibility to override the usage of OSCP caching flag on a per query basis. Use
   *        {@link ETriState#UNDEFINED} to solely use the global flag.
   * @param eCheckMode
   *        Possibility to override the OSCP checking flag on a per query basis. May be
   *        <code>null</code> to use the global flag from
   *        {@link CertificateRevocationChecker#getRevocationCheckMode()}.
   * @throws Phase4Exception
   *         in case of error
   */
  private static void _checkReceiverAPCert (@NonNull final TrustedCAChecker aCAChecker,
                                            @Nullable final X509Certificate aReceiverCert,
                                            @Nullable final IPhase4HREdeliveryCertificateCheckResultHandler aCertificateConsumer,
                                            @NonNull final ETriState eCacheOSCResult,
                                            @Nullable final ERevocationCheckMode eCheckMode) throws Phase4Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Using the following receiver AP certificate from the SMP: " + aReceiverCert);

    final OffsetDateTime aNow = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
    final ECertificateCheckResult eCertCheckResult = aCAChecker.checkCertificate (aReceiverCert,
                                                                                  aNow,
                                                                                  eCacheOSCResult,
                                                                                  eCheckMode);

    // Interested in the certificate?
    if (aCertificateConsumer != null)
      aCertificateConsumer.onCertificateCheckResult (aReceiverCert, aNow, eCertCheckResult);

    if (eCertCheckResult.isInvalid ())
    {
      final String sMsg = "The configured receiver AP certificate is not valid (at " +
                          aNow +
                          ") and cannot be used for sending towards. Aborting. Reason: " +
                          eCertCheckResult.getReason ();
      LOGGER.error (sMsg);
      throw new Phase4HREdeliveryException (sMsg).setRetryFeasible (false);
    }
  }

  /**
   * @return Create a new Builder for AS4 messages if the payload is present and the SBDH is always
   *         created internally by phase4. Never <code>null</code>.
   * @see #sbdhBuilder() if you already have a ready Standard Business Document
   */
  @NonNull
  public static HREDeliveryUserMessageBuilder builder ()
  {
    return new HREDeliveryUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 messages if the SBDH payload is already present. This
   *         builder is slightly more limited, because it doesn't offer validation, as it is
   *         expected to be done before. Never <code>null</code>.
   * @see #builder() if you want phase4 to create the Standard Business Document
   */
  @NonNull
  public static HREDeliveryUserMessageSBDHBuilder sbdhBuilder ()
  {
    return new HREDeliveryUserMessageSBDHBuilder ();
  }

  /**
   * Abstract builder base class with the minimum requirements configuration
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  @NotThreadSafe
  public abstract static class AbstractHREDeliveryUserMessageBuilder <IMPLTYPE extends AbstractHREDeliveryUserMessageBuilder <IMPLTYPE>>
                                                                     extends
                                                                     AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    public static final boolean DEFAULT_COMPRESS_PAYLOAD = true;
    public static final boolean DEFAULT_CHECK_RECEIVER_AP_CERTIFICATE = true;

    // C1
    protected IParticipantIdentifier m_aSenderID;
    // C4
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;

    protected IMimeType m_aPayloadMimeType;
    protected boolean m_bCompressPayload;
    protected String m_sPayloadContentID;

    // This value is set for backwards compatibility reasons
    protected IAS4EndpointDetailProvider m_aEndpointDetailProvider;
    private IPhase4HREdeliveryCertificateCheckResultHandler m_aCertificateConsumer;
    private Consumer <String> m_aAPEndpointURLConsumer;
    private Consumer <String> m_aAPTechnicalContactConsumer;
    private boolean m_bCheckReceiverAPCertificate;
    protected TrustedCAChecker m_aCAChecker;

    // Status var
    private OffsetDateTime m_aEffectiveSendingDT;

    /**
     * Create a new builder, with the defaults from
     * {@link AbstractAS4UserMessageBuilderMIMEPayload#AbstractAS4UserMessageBuilderMIMEPayload()}
     */
    public AbstractHREDeliveryUserMessageBuilder ()
    {
      // Override default values
      try
      {
        as4ProfileID (AS4HREDeliveryProfileRegistarSPI.AS4_PROFILE_ID);

        // Use the HR eDelivery specific timeout settings
        httpClientFactory (new Phase4HREDeliveryHttpClientSettings ());
        fromPartyIDType (HREDeliveryPMode.DEFAULT_PARTY_TYPE_ID);
        fromRole (CAS4.DEFAULT_INITIATOR_URL);
        toPartyIDType (HREDeliveryPMode.DEFAULT_PARTY_TYPE_ID);
        toRole (CAS4.DEFAULT_RESPONDER_URL);
        compressPayload (DEFAULT_COMPRESS_PAYLOAD);

        checkReceiverAPCertificate (DEFAULT_CHECK_RECEIVER_AP_CERTIFICATE);

        // Source: AS4 spec, Domibus example config
        payloadContentID ("message");
        payloadMimeType (CMimeType.TEXT_XML);

        // Explicitly NOT needed in message
        if (false)
        {
          agreementRef (HREDeliveryPMode.DEFAULT_AGREEMENT_ID);
          agreementType (HREDeliveryPMode.DEFAULT_AGREEMENT_TYPE);
        }

        // HR eDelivery uses its own root certificate, so no checks needed - this is
        // only to quiet the warning
        signingParams ().setSubjectCertConstraints (HREDeliveryPMode.CERTIFICATE_SUBJECT_CONSTRAINT_PATTERN);
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the sender participant ID of the message. The participant ID must be provided prior to
     * sending.
     *
     * @param aSenderID
     *        The sender participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE senderParticipantID (@NonNull final IParticipantIdentifier aSenderID)
    {
      ValueEnforcer.notNull (aSenderID, "SenderID");
      if (m_aSenderID != null)
        LOGGER.warn ("An existing SenderParticipantID is overridden");
      m_aSenderID = aSenderID;
      return thisAsT ();
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must be provided prior to
     * sending. This ends up in the "finalRecipient" UserMessage property.
     *
     * @param aReceiverID
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE receiverParticipantID (@NonNull final IParticipantIdentifier aReceiverID)
    {
      ValueEnforcer.notNull (aReceiverID, "ReceiverID");
      if (m_aReceiverID != null)
        LOGGER.warn ("An existing ReceiverParticipantID is overridden");
      m_aReceiverID = aReceiverID;
      return thisAsT ();
    }

    /**
     * @return The currently set Document Type ID. May be <code>null</code>.
     */
    @Nullable
    public final IDocumentTypeIdentifier documentTypeID ()
    {
      return m_aDocTypeID;
    }

    /**
     * Set the document type ID to be send. The document type must be provided prior to sending.
     * This is a shortcut to the {@link #action(String)} method.
     *
     * @param aDocTypeID
     *        The document type ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE documentTypeID (@NonNull final IDocumentTypeIdentifier aDocTypeID)
    {
      ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
      if (m_aDocTypeID != null)
        LOGGER.warn ("An existing DocumentTypeID is overridden");
      m_aDocTypeID = aDocTypeID;
      return action (aDocTypeID.getURIEncoded ());
    }

    /**
     * @return The currently set Process ID. May be <code>null</code>.
     */
    @Nullable
    public final IProcessIdentifier processID ()
    {
      return m_aProcessID;
    }

    /**
     * Set the process ID to be send. The process ID must be provided prior to sending. This is a
     * shortcut to the {@link #service(String, String)} method.
     *
     * @param aProcessID
     *        The process ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE processID (@NonNull final IProcessIdentifier aProcessID)
    {
      ValueEnforcer.notNull (aProcessID, "ProcessID");
      if (m_aProcessID != null)
        LOGGER.warn ("An existing ProcessID is overridden");
      m_aProcessID = aProcessID;
      return service (aProcessID.getScheme (), aProcessID.getValue ());
    }

    /**
     * Set the "sender party ID" which is the CN part of the HR eDelivery AP certificate. An example
     * value is e.g. "POP000123" but it MUST match the certificate you are using. This must be
     * provided prior to sending. This is a shortcut to the {@link #fromPartyID(String)} method.
     *
     * @param sSenderPartyID
     *        The sender party ID. May neither be <code>null</code> nor empty.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE senderPartyID (@NonNull @Nonempty final String sSenderPartyID)
    {
      ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
      return fromPartyID (sSenderPartyID);
    }

    /**
     * Set the MIME type of the payload. By default it is <code>application/xml</code> and MUST
     * usually not be changed. This value is required for sending.
     *
     * @param aPayloadMimeType
     *        The payload MIME type. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE payloadMimeType (@NonNull final IMimeType aPayloadMimeType)
    {
      ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
      m_aPayloadMimeType = aPayloadMimeType;
      return thisAsT ();
    }

    /**
     * Enable or disable the AS4 compression of the payload. By default compression is disabled.
     *
     * @param bCompressPayload
     *        <code>true</code> to compress the payload, <code>false</code> to not compress it.
     * @return this for chaining.
     */
    @NonNull
    public final IMPLTYPE compressPayload (final boolean bCompressPayload)
    {
      m_bCompressPayload = bCompressPayload;
      return thisAsT ();
    }

    /**
     * Set an optional payload "Content-ID". This method is usually not needed, because in HR
     * eDelivery there are currently no rules on the Content-ID. By default a random Content-ID is
     * created.
     *
     * @param sPayloadContentID
     *        The new payload content ID. May be null.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE payloadContentID (@Nullable final String sPayloadContentID)
    {
      m_sPayloadContentID = sPayloadContentID;
      return thisAsT ();
    }

    /**
     * Set the abstract endpoint detail provider to be used. This can be an SMP lookup routine or in
     * certain test cases a predefined certificate and endpoint URL.
     *
     * @param aEndpointDetailProvider
     *        The endpoint detail provider to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #smpClient(HRMPSClientReadOnly)
     */
    @NonNull
    public final IMPLTYPE endpointDetailProvider (@NonNull final IAS4EndpointDetailProvider aEndpointDetailProvider)
    {
      ValueEnforcer.notNull (aEndpointDetailProvider, "EndpointDetailProvider");
      if (m_aEndpointDetailProvider != null)
        LOGGER.warn ("An existing EndpointDetailProvider is overridden");
      m_aEndpointDetailProvider = aEndpointDetailProvider;
      return thisAsT ();
    }

    /**
     * Set the SMP client to be used. This is the point where e.g. the differentiation between SMK
     * and SML can be done. This must be set prior to sending. If the endpoint information are
     * already known you can also use {@link #receiverEndpointDetails(X509Certificate, String)}
     * instead.
     *
     * @param aSMPClient
     *        The SMP client to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #receiverEndpointDetails(X509Certificate, String)
     * @see #endpointDetailProvider(IAS4EndpointDetailProvider)
     */
    @NonNull
    public final IMPLTYPE smpClient (@NonNull final HRMPSClientReadOnly aSMPClient)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderBDXR (aSMPClient).setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_ERACUN_AS4_V1));
    }

    /**
     * Use this method to explicit set the AP certificate and AP endpoint URL that was retrieved
     * from a previous SMP query.
     *
     * @param aEndpoint
     *        The HR eDelivery SMP Endpoint instance. May not be <code>null</code>.
     * @return this for chaining
     * @throws CertificateException
     *         In case the conversion from byte array to X509 certificate failed
     */
    @NonNull
    public final IMPLTYPE receiverEndpointDetails (@NonNull final EndpointType aEndpoint) throws CertificateException
    {
      return receiverEndpointDetails (BDXRClientReadOnly.getEndpointCertificate (aEndpoint),
                                      BDXRClientReadOnly.getEndpointAddress (aEndpoint));
    }

    /**
     * Use this method to explicit set the AP certificate and AP endpoint URL that was retrieved
     * externally (e.g. via an SMP call or for a static test case).
     *
     * @param aCert
     *        The HR eDelivery AP certificate that should be used to encrypt the message for the
     *        receiver. May not be <code>null</code>.
     * @param sDestURL
     *        The destination URL of the receiving AP to send the AS4 message to. Must be a valid
     *        URL and may neither be <code>null</code> nor empty.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE receiverEndpointDetails (@NonNull final X509Certificate aCert,
                                                   @NonNull @Nonempty final String sDestURL)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderConstant (aCert, sDestURL));
    }

    /**
     * Set an optional Consumer for the retrieved certificate from the endpoint details provider,
     * independent of its usability.
     *
     * @param aCertificateConsumer
     *        The consumer to be used. The first parameter is the certificate itself and the second
     *        parameter is the internal check result. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE certificateConsumer (@Nullable final IPhase4HREdeliveryCertificateCheckResultHandler aCertificateConsumer)
    {
      m_aCertificateConsumer = aCertificateConsumer;
      return thisAsT ();
    }

    /**
     * Set an optional Consumer for the destination AP address retrieved from the endpoint details
     * provider, independent of its usability.
     *
     * @param aAPEndpointURLConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE endpointURLConsumer (@Nullable final Consumer <String> aAPEndpointURLConsumer)
    {
      m_aAPEndpointURLConsumer = aAPEndpointURLConsumer;
      return thisAsT ();
    }

    /**
     * Set an optional Consumer for the technical contact retrieved from the endpoint details
     * provider, independent of its usability.
     *
     * @param aAPTechnicalContactConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE technicalContactConsumer (@Nullable final Consumer <String> aAPTechnicalContactConsumer)
    {
      m_aAPTechnicalContactConsumer = aAPTechnicalContactConsumer;
      return thisAsT ();
    }

    /**
     * Enable or disable the check of the receiver AP certificate. This checks the validity of the
     * certificate as well as the revocation status. It is strongly recommended to enable this
     * check.
     *
     * @param bCheckReceiverAPCertificate
     *        <code>true</code> to enable it, <code>false</code> to disable it.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE checkReceiverAPCertificate (final boolean bCheckReceiverAPCertificate)
    {
      m_bCheckReceiverAPCertificate = bCheckReceiverAPCertificate;
      return thisAsT ();
    }

    /**
     * Set a custom HR eDelivery AP certificate CA checker. This CA checker checks the certificate
     * provided by the endpoint detail provider (see below). This checker is only used, if
     * {@link #checkReceiverAPCertificate(boolean)} was called with <code>true</code>.
     *
     * @param aCAChecker
     *        The Certificate CA checker to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final IMPLTYPE apCAChecker (@NonNull final TrustedCAChecker aCAChecker)
    {
      ValueEnforcer.notNull (aCAChecker, "CAChecker");
      m_aCAChecker = aCAChecker;
      return thisAsT ();
    }

    /**
     * The effective sending date time of the message. That is set only if message sending takes
     * place.
     *
     * @return The effective sending date time or <code>null</code> if the messages was not sent
     *         yet.
     */
    @Nullable
    public final OffsetDateTime effectiveSendingDateTime ()
    {
      return m_aEffectiveSendingDT;
    }

    protected final boolean isEndpointDetailProviderUsable ()
    {
      // Sender ID doesn't matter here
      if (m_aReceiverID == null)
      {
        LOGGER.warn ("The field 'receiverID' is not set");
        return false;
      }
      if (m_aDocTypeID == null)
      {
        LOGGER.warn ("The field 'docTypeID' is not set");
        return false;
      }
      if (m_aProcessID == null)
      {
        LOGGER.warn ("The field 'processID' is not set");
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
    @OverridingMethodsMustInvokeSuper
    protected ESuccess finishFields (@NonNull final AS4ResourceHelper aResHelper) throws Phase4Exception
    {
      if (!isEndpointDetailProviderUsable ())
      {
        LOGGER.error ("At least one mandatory field for endpoint discovery is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // e.g. SMP lookup. Throws an Phase4Exception in case of error
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup (may throw an exception)
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      if (m_bCheckReceiverAPCertificate)
      {
        if (m_aCAChecker == null)
        {
          LOGGER.error ("The verification of the receiver AP certificate is enabled, but no CA checker is installed.");
          return ESuccess.FAILURE;
        }

        // Check if the received certificate is a valid HR eDelivery AP certificate
        // Throws Phase4HREDeliveryException in case of error
        _checkReceiverAPCert (m_aCAChecker, aReceiverCert, m_aCertificateConsumer, ETriState.UNDEFINED, null);
      }
      else
      {
        LOGGER.warn ("The check of the receiver's HR eDelivery AP certificate was explicitly disabled.");

        // Interested in the certificate?
        if (m_aCertificateConsumer != null)
        {
          final OffsetDateTime aNow = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
          m_aCertificateConsumer.onCertificateCheckResult (aReceiverCert, aNow, ECertificateCheckResult.NOT_CHECKED);
        }
      }
      receiverCertificate (aReceiverCert);

      // URL from e.g. SMP lookup (may throw an exception)
      final String sDestURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndpointURLConsumer != null)
        m_aAPEndpointURLConsumer.accept (sDestURL);
      endpointURL (sDestURL);

      // For informational (logging) purposes only
      if (m_aAPTechnicalContactConsumer != null)
        m_aAPTechnicalContactConsumer.accept (m_aEndpointDetailProvider.getReceiverTechnicalContact ());

      // From receiver certificate
      toPartyID (CertificateHelper.getSubjectCN (aReceiverCert));

      // Super at the end
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
      if (m_aDocTypeID == null)
      {
        LOGGER.warn ("The field 'docTypeID' is not set");
        return false;
      }
      if (m_aProcessID == null)
      {
        LOGGER.warn ("The field 'processID' is not set");
        return false;
      }

      // m_aPayloadMimeType may be null
      // m_bCompressPayload may be null
      // m_sPayloadContentID may be null

      if (m_aEndpointDetailProvider == null)
      {
        LOGGER.warn ("The field 'endpointDetailProvider' is not set");
        return false;
      }
      // m_aCertificateConsumer may be null
      // m_aAPEndpointURLConsumer may be null

      // All valid
      return true;
    }

    @Override
    protected void customizeBeforeSending () throws Phase4Exception
    {
      // Add mandatory properties
      addMessageProperty (MessageProperty.builder ()
                                         .name (CAS4.ORIGINAL_SENDER)
                                         .type (m_aSenderID.getScheme ())
                                         .value (m_aSenderID.getValue ()));
      addMessageProperty (MessageProperty.builder ()
                                         .name (CAS4.FINAL_RECIPIENT)
                                         .type (m_aReceiverID.getScheme ())
                                         .value (m_aReceiverID.getValue ()));

      // Explicitly remember the old handler
      // Try to do this as close to sending as possible, to avoid that another
      // sendingDateTimConsumer is used
      final IAS4SendingDateTimeConsumer aExistingSendingDTConsumer = m_aSendingDTConsumer;
      sendingDateTimeConsumer (aSendingDT -> {
        // Store in this instance
        m_aEffectiveSendingDT = aSendingDT;

        // Call the original handler
        if (aExistingSendingDTConsumer != null)
          aExistingSendingDTConsumer.onEffectiveSendingDateTime (aSendingDT);
      });
    }
  }

  /**
   * The builder class for sending AS4 messages using HR eDelivery specifics. Use
   * {@link #sendMessage()} or {@link #sendMessageAndCheckForReceipt()} to trigger the main
   * transmission.<br>
   * This builder class assumes, that only the payload (e.g. the Invoice) is present, and that both
   * validation and SBDH creation happens inside.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class HREDeliveryUserMessageBuilder extends
                                                    AbstractHREDeliveryUserMessageBuilder <HREDeliveryUserMessageBuilder>
  {
    private String m_sSBDHInstanceIdentifier;
    private String m_sSBDHStandard;
    private String m_sSBDHTypeVersion;
    private String m_sSBDHType;
    private Element m_aPayloadElement;
    private byte [] m_aPayloadBytes;
    private IHasInputStream m_aPayloadHasIS;
    private Consumer <? super StandardBusinessDocument> m_aSBDDocumentConsumer;

    private IValidationExecutorSetRegistry <IValidationSourceXML> m_aVESRegistry;
    private DVRCoordinate m_aVESID;
    private IPhase4HREdeliveryValidationResultHandler m_aValidationResultHandler;

    /**
     * Create a new builder, with the defaults from
     * {@link AbstractHREDeliveryUserMessageBuilder#AbstractHREDeliveryUserMessageBuilder()}
     */
    public HREDeliveryUserMessageBuilder ()
    {}

    /**
     * Set the SBDH instance identifier. If none is provided, a random ID is used. Usually this must
     * NOT be set. In case of a retry, the same Instance Identifier should be used. Also an MLR
     * should always refer to the Instance Identifier of the original transmission.
     *
     * @param sSBDHInstanceIdentifier
     *        The SBDH instance identifier to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder sbdhInstanceIdentifier (@Nullable final String sSBDHInstanceIdentifier)
    {
      m_sSBDHInstanceIdentifier = sSBDHInstanceIdentifier;
      return this;
    }

    /**
     * Set the SBDH document standard. If none is provided, the value is extracted from the business
     * document root element. This is specifically needed for non-XML payloads.
     *
     * @param sSBDHStandard
     *        The SBDH document standard to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder sbdhStandard (@Nullable final String sSBDHStandard)
    {
      m_sSBDHStandard = sSBDHStandard;
      return this;
    }

    /**
     * Set the SBDH document identification type version. If none is provided, the value is
     * extracted from the document type identifier. This is specifically needed for non-XML
     * payloads.
     *
     * @param sSBDHTypeVersion
     *        The SBDH document identification type version to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder sbdhTypeVersion (@Nullable final String sSBDHTypeVersion)
    {
      m_sSBDHTypeVersion = sSBDHTypeVersion;
      return this;
    }

    /**
     * Set the SBDH document identification type. If none is provided, the value is extracted from
     * the business document root element. This is specifically needed for non-XML payloads.
     *
     * @param sSBDHType
     *        The SBDH document identification type to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder sbdhType (@Nullable final String sSBDHType)
    {
      m_sSBDHType = sSBDHType;
      return this;
    }

    /**
     * Set the payload element to be used, if it is available as a parsed DOM element. Internally
     * the DOM element will be cloned before sending it out. If this method is called, it overwrites
     * any other explicitly set payload.
     *
     * @param aPayloadElement
     *        The payload element to be used. They payload element MUST have a namespace URI. May
     *        not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder payload (@NonNull final Element aPayloadElement)
    {
      ValueEnforcer.notNull (aPayloadElement, "Payload");
      ValueEnforcer.notNull (aPayloadElement.getNamespaceURI (), "Payload.NamespaceURI");
      m_aPayloadElement = aPayloadElement;
      m_aPayloadBytes = null;
      m_aPayloadHasIS = null;
      return this;
    }

    /**
     * Set the XML payload to be used as a byte array. It will be parsed internally to a DOM
     * element. Compared to {@link #payload(Element)} the read DOM element will not be cloned
     * internally, so this option is less memory intensive. If this method is called, it overwrites
     * any other explicitly set payload.
     *
     * @param aPayloadBytes
     *        The payload bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder payload (final byte @NonNull [] aPayloadBytes)
    {
      ValueEnforcer.notNull (aPayloadBytes, "PayloadBytes");
      m_aPayloadElement = null;
      m_aPayloadBytes = aPayloadBytes;
      m_aPayloadHasIS = null;
      return this;
    }

    /**
     * Set the XML payload to be used as an InputStream provider. It will be parsed internally to a
     * DOM element. Compared to {@link #payload(Element)} the read DOM element will not be cloned
     * internally, so this option is less memory intensive. If this method is called, it overwrites
     * any other explicitly set payload.
     *
     * @param aPayloadHasIS
     *        The payload input stream provider to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder payload (@NonNull final IHasInputStream aPayloadHasIS)
    {
      ValueEnforcer.notNull (aPayloadHasIS, "PayloadHasIS");
      m_aPayloadElement = null;
      m_aPayloadBytes = null;
      m_aPayloadHasIS = aPayloadHasIS;
      return this;
    }

    /**
     * Set an optional Consumer for the created StandardBusinessDocument (SBD).
     *
     * @param aSBDDocumentConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder sbdDocumentConsumer (@Nullable final Consumer <? super StandardBusinessDocument> aSBDDocumentConsumer)
    {
      m_aSBDDocumentConsumer = aSBDDocumentConsumer;
      return this;
    }

    /**
     * Set the client side validation to be used. If this method is not invoked, than it's the
     * responsibility of the caller to validate the document prior to sending it. This method uses a
     * default "do nothing validation result handler".
     *
     * @param aVESRegistry
     *        The registry to use. May not be <code>null</code>.
     * @param aVESID
     *        The Validation Execution Set ID to validate against. May be <code>null</code>.
     * @return this for chaining
     * @see #validationConfiguration(IValidationExecutorSetRegistry,DVRCoordinate,
     *      IPhase4HREdeliveryValidationResultHandler)
     */
    @NonNull
    public HREDeliveryUserMessageBuilder validationConfiguration (@Nullable final IValidationExecutorSetRegistry <IValidationSourceXML> aVESRegistry,
                                                                  @Nullable final DVRCoordinate aVESID)
    {
      final IPhase4HREdeliveryValidationResultHandler aHdl = aVESID == null ? null
                                                                            : new Phase4HREdeliveryValidatonResultHandler ();
      return validationConfiguration (aVESRegistry, aVESID, aHdl);
    }

    /**
     * Set the client side validation to be used. If this method is not invoked, than it's the
     * responsibility of the caller to validate the document prior to sending it. If the validation
     * should happen internally, both the VESID AND the result handler must be set.
     *
     * @param aVESRegistry
     *        The registry to use. May not be <code>null</code>.
     * @param aVESID
     *        The Validation Execution Set ID to validate against. May be <code>null</code>.
     * @param aValidationResultHandler
     *        The validation result handler for positive and negative response handling. May be
     *        <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder validationConfiguration (@Nullable final IValidationExecutorSetRegistry <IValidationSourceXML> aVESRegistry,
                                                                  @Nullable final DVRCoordinate aVESID,
                                                                  @Nullable final IPhase4HREdeliveryValidationResultHandler aValidationResultHandler)
    {
      m_aVESRegistry = aVESRegistry;
      m_aVESID = aVESID;
      m_aValidationResultHandler = aValidationResultHandler;
      return this;
    }

    /**
     * Disable the validation for the outbound call.
     *
     * @return this for chaining
     */
    @NonNull
    public HREDeliveryUserMessageBuilder disableValidation ()
    {
      return validationConfiguration (null, null);
    }

    @Override
    protected ESuccess finishFields (@NonNull final AS4ResourceHelper aResHelper) throws Phase4Exception
    {
      // Ensure a DOM element is present
      final Element aPayloadElement;
      final boolean bClonePayloadElement;
      if (m_aPayloadElement != null)
      {
        // Already provided as a DOM element
        aPayloadElement = m_aPayloadElement;
        bClonePayloadElement = true;
      }
      else
      {
        if (m_aPayloadBytes != null)
        {
          // Parse it
          final Document aDoc = DOMReader.readXMLDOM (m_aPayloadBytes);
          if (aDoc == null)
            throw new Phase4HREdeliveryException ("Failed to parse payload bytes to a DOM node").setRetryFeasible (false);
          aPayloadElement = aDoc.getDocumentElement ();
        }
        else
          if (m_aPayloadHasIS != null)
          {
            // Parse it
            final InputStream aIS = m_aPayloadHasIS.getBufferedInputStream ();
            if (aIS == null)
              throw new Phase4HREdeliveryException ("Failed to create payload InputStream from provider").setRetryFeasible (false);
            final Document aDoc = DOMReader.readXMLDOM (aIS);
            if (aDoc == null)
              throw new Phase4HREdeliveryException ("Failed to parse payload InputStream to a DOM node").setRetryFeasible (false);
            aPayloadElement = aDoc.getDocumentElement ();
          }
          else
            throw new IllegalStateException ("Unexpected - neither element nor bytes nor InputStream provider are present");
        if (aPayloadElement == null)
          throw new Phase4HREdeliveryException ("The parsed XML document must have a root element").setRetryFeasible (false);
        if (aPayloadElement.getNamespaceURI () == null)
          throw new Phase4HREdeliveryException ("The root element of the parsed XML document does not have a namespace URI").setRetryFeasible (false);
        bClonePayloadElement = false;
      }

      // Consistency check
      if (CSBDH.SBDH_NS.equals (aPayloadElement.getNamespaceURI ()))
        throw new Phase4HREdeliveryException ("You cannot set a Standard Business Document as the payload for the regular builder." +
                                              " The SBD is created automatically inside of this builder." +
                                              " Use Phase4HREDeliverySender.sbdhBuilder() if you have a pre-build SBD.").setRetryFeasible (false);

      // Optional payload validation
      _validatePayload (aPayloadElement, m_aVESRegistry, m_aVESID, m_aValidationResultHandler);

      // Perform SMP lookup
      if (super.finishFields (aResHelper).isFailure ())
        return ESuccess.FAILURE;

      // Created SBDH
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Start creating SBDH for AS4 message");

      final HREDeliverySBDHData aHREDeliverySBDH = _createHREDeliverySBDHData (m_aSenderID,
                                                                               m_aReceiverID,
                                                                               m_aDocTypeID,
                                                                               m_sSBDHInstanceIdentifier,
                                                                               m_sSBDHStandard,
                                                                               m_sSBDHTypeVersion,
                                                                               m_sSBDHType,
                                                                               aPayloadElement,
                                                                               bClonePayloadElement);

      if (aHREDeliverySBDH == null)
      {
        // A log message was already provided
        return ESuccess.FAILURE;
      }

      // We never need to clone the payload element here because it was evtl.
      // cloned before
      final StandardBusinessDocument aSBD = new HREDeliverySBDHDataWriter ().setFavourSpeed (true)
                                                                            .createStandardBusinessDocument (aHREDeliverySBDH);

      if (m_aSBDDocumentConsumer != null)
        m_aSBDDocumentConsumer.accept (aSBD);

      {
        // Serialize the SBDH to a temporary file
        // Advantage: works with large files as well because it consumes less
        // memory
        try
        {
          final File aTempSBDFile = aResHelper.createTempFile ();
          new SBDMarshaller ().write (aSBD, aTempSBDFile);

          // Now we have the main payload
          payload (AS4OutgoingAttachment.builder ()
                                        .data (aTempSBDFile)
                                        .mimeType (m_aPayloadMimeType)
                                        .compression (m_bCompressPayload ? EAS4CompressionMode.GZIP : null)
                                        .contentID (m_sPayloadContentID));
        }
        catch (final IOException ex)
        {
          throw new Phase4HREdeliveryException ("Failed to create temporary file for SBDH", ex);
        }
      }

      return ESuccess.SUCCESS;
    }
  }

  /**
   * A builder class for sending AS4 messages using HR eDelivery specifics. Use
   * {@link #sendMessage()} or {@link #sendMessageAndCheckForReceipt()} to trigger the main
   * transmission.<br>
   * This builder class assumes, that the SBDH was created outside, therefore no validation can
   * occur.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class HREDeliveryUserMessageSBDHBuilder extends
                                                        AbstractHREDeliveryUserMessageBuilder <HREDeliveryUserMessageSBDHBuilder>
  {
    private byte [] m_aPayloadBytes;

    /**
     * Create a new builder with the defaults from
     * {@link AbstractHREDeliveryUserMessageBuilder#AbstractHREDeliveryUserMessageBuilder()}
     */
    public HREDeliveryUserMessageSBDHBuilder ()
    {}

    /**
     * Set the SBDH payload to be used as a byte array. This means, that you need to pass in all
     * other mandatory fields manually (sender participant ID, receiver participant ID, document
     * Type ID, process ID and country C1).
     *
     * @param aSBDHBytes
     *        The SBDH bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #senderParticipantID(IParticipantIdentifier)
     * @see #receiverParticipantID(IParticipantIdentifier)
     * @see #documentTypeID(IDocumentTypeIdentifier)
     * @see #processID(IProcessIdentifier)
     */
    @NonNull
    public HREDeliveryUserMessageSBDHBuilder payload (final byte @NonNull [] aSBDHBytes)
    {
      ValueEnforcer.notNull (aSBDHBytes, "SBDHBytes");
      m_aPayloadBytes = aSBDHBytes;
      return this;
    }

    /**
     * Set the payload, the sender participant ID, the receiver participant ID, the document type ID
     * and the process ID.
     *
     * @param aSBDH
     *        The SBDH to use. May not be <code>null</code>.
     * @return this for chaining
     * @see #payload(byte[])
     * @see #senderParticipantID(IParticipantIdentifier)
     * @see #receiverParticipantID(IParticipantIdentifier)
     */
    @NonNull
    public HREDeliveryUserMessageSBDHBuilder payloadAndMetadata (@NonNull final HREDeliverySBDHData aSBDH)
    {
      ValueEnforcer.notNull (aSBDH, "SBDH");

      // Check with logging
      if (!aSBDH.areAllFieldsSet (true))
        throw new IllegalArgumentException ("The provided HR eDelivery SBDH data is incomplete. See logs for details.");

      final StandardBusinessDocument aJaxbSbdh = new HREDeliverySBDHDataWriter ().createStandardBusinessDocument (aSBDH);
      return senderParticipantID (aSBDH.getSenderAsIdentifier ()).receiverParticipantID (aSBDH.getReceiverAsIdentifier ())
                                                                 .payload (new SBDMarshaller ().getAsBytes (aJaxbSbdh));
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayloadBytes == null)
      {
        LOGGER.warn ("The field 'payloadBytes' is not set");
        return false;
      }
      // All valid
      return true;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected ESuccess finishFields (@NonNull final AS4ResourceHelper aResHelper) throws Phase4Exception
    {
      // Perform SMP lookup
      if (super.finishFields (aResHelper).isFailure ())
        return ESuccess.FAILURE;

      // Now we have the main payload
      payload (AS4OutgoingAttachment.builder ()
                                    .data (m_aPayloadBytes)
                                    .mimeType (m_aPayloadMimeType)
                                    .compression (m_bCompressPayload ? EAS4CompressionMode.GZIP : null)
                                    .contentID (m_sPayloadContentID));

      return ESuccess.SUCCESS;
    }
  }
}
