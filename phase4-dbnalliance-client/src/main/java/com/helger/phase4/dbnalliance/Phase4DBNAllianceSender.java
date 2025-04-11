/*
 * Copyright (C) 2024-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ETriState;
import com.helger.dbnalliance.commons.security.DBNAllianceTrustStores;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.xhe.DBNAlliancePayload;
import com.helger.peppol.xhe.DBNAllianceXHEData;
import com.helger.peppol.xhe.write.DBNAllianceXHEDocumentWriter;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.BDXR2IdentifierFactory;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.crypto.ICryptoSessionKeyProvider;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR2;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.dynamicdiscovery.IAS4EndpointDetailProvider;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.profile.dbnalliance.AS4DBNAllianceProfileRegistarSPI;
import com.helger.phase4.profile.dbnalliance.DBNAlliancePMode;
import com.helger.phase4.profile.dbnalliance.Phase4DBNAllianceHttpClientSettings;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.sender.IAS4SendingDateTimeConsumer;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.ERevocationCheckMode;
import com.helger.smpclient.bdxr2.IBDXR2ServiceMetadataProvider;
import com.helger.smpclient.url.DBNAURLProviderSMP;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.xhe.v10.CXHE10;
import com.helger.xhe.v10.XHE10Marshaller;
import com.helger.xhe.v10.XHE10XHEType;
import com.helger.xhe.v10.cac.XHE10PayloadType;

/**
 * This class contains all the specifics to send AS4 messages with the DBNAlliance profile. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with all potential
 * customization.
 *
 * @author Robinson Artemio Garcia Meléndez
 * @author Philip Helger
 */
@Immutable
public final class Phase4DBNAllianceSender
{
  public static final BDXR2IdentifierFactory IF = BDXR2IdentifierFactory.INSTANCE;
  public static final ISMPURLProvider URL_PROVIDER = DBNAURLProviderSMP.INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4DBNAllianceSender.class);
  private static final IMimeType MIME_TYPE = CMimeType.APPLICATION_XML;

  private Phase4DBNAllianceSender ()
  {}

  @Nullable
  private static XHE10XHEType _createDBNAllianceXHE (@Nonnull final IParticipantIdentifier aSenderID,
                                                     @Nonnull final IParticipantIdentifier aReceiverID,
                                                     @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                                     @Nonnull final IProcessIdentifier aProcID,
                                                     @Nonnull final Element aPayloadElement,
                                                     final boolean bClonePayloadElement) throws Phase4DBNAllianceException
  {
    final DBNAllianceXHEData aData = new DBNAllianceXHEData (IF);
    aData.setFromParty (aSenderID.getScheme (), aSenderID.getValue ());
    aData.setToParty (aReceiverID.getScheme (), aReceiverID.getValue ());
    aData.setID (UUID.randomUUID ().toString ());
    aData.setCreationDateAndTime (MetaAS4Manager.getTimestampMgr ().getCurrentXMLDateTime ());

    final DBNAlliancePayload aPayload = new DBNAlliancePayload (IF);
    // Content type code is mandatory
    aPayload.setContentTypeCode (MIME_TYPE.getAsString ());
    aPayload.setCustomizationID (null, aDocTypeID.getValue ());
    aPayload.setProfileID (aProcID.getScheme (), aProcID.getValue ());

    // Not cloning the payload element is for saving memory only (if it can be
    // ensured, the source payload element is not altered externally of course)
    if (bClonePayloadElement)
      aPayload.setPayloadContent (aPayloadElement);
    else
      aPayload.setPayloadContentNoClone (aPayloadElement);

    aData.addPayload (aPayload);

    // check with logging
    if (!aData.areAllFieldsSet (true))
      throw new Phase4DBNAllianceException ("The DBNAlliance XHE data is incomplete. See logs for details.");

    return DBNAllianceXHEDocumentWriter.createExchangeHeaderEnvelope (aData);
  }

  /**
   * Check if the provided certificate is a valid Peppol AP certificate.
   *
   * @param aCAChecker
   *        The Peppol CA checker to be used to verify the Peppol AP certificate. May not be
   *        <code>null</code>.
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
   * @throws Phase4DBNAllianceException
   *         in case of error
   */
  private static void _checkReceiverAPCert (@Nonnull final TrustedCAChecker aCAChecker,
                                            @Nullable final X509Certificate aReceiverCert,
                                            @Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer,
                                            @Nonnull final ETriState eCacheOSCResult,
                                            @Nullable final ERevocationCheckMode eCheckMode) throws Phase4DBNAllianceException
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
      throw new Phase4DBNAllianceException (sMsg);
    }
  }

  /**
   * @return Create a new Builder for AS4 messages if the XHE payload is present. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static DBNAllianceUserMessageBuilder builder ()
  {
    return new DBNAllianceUserMessageBuilder ();
  }

  /**
   * @return Create a new Builder for AS4 messages if the XHE message is present. Never
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nonnull
  public static DBNAllianceUserMessageXHEBuilder xheBuilder ()
  {
    return new DBNAllianceUserMessageXHEBuilder ();
  }

  /**
   * Abstract DBNAlliance UserMessage builder class with sanity methods.
   *
   * @author Robinson Artemio Garcia Meléndez
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  public abstract static class AbstractDBNAllianceUserMessageBuilder <IMPLTYPE extends AbstractDBNAllianceUserMessageBuilder <IMPLTYPE>>
                                                                     extends
                                                                     AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    // C1
    protected IParticipantIdentifier m_aSenderID;
    // C4
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;

    protected IAS4EndpointDetailProvider m_aEndpointDetailProvider;
    private IPhase4PeppolCertificateCheckResultHandler m_aCertificateConsumer;
    private Consumer <String> m_aAPEndpointURLConsumer;
    private boolean m_bCheckReceiverAPCertificate;
    protected TrustedCAChecker m_aCAChecker;

    // Status var
    private OffsetDateTime m_aEffectiveSendingDT;

    protected AbstractDBNAllianceUserMessageBuilder ()
    {
      // Override default values
      try
      {
        as4ProfileID (AS4DBNAllianceProfileRegistarSPI.AS4_PROFILE_ID);

        httpClientFactory (new Phase4DBNAllianceHttpClientSettings ());

        agreementRef (DBNAlliancePMode.DEFAULT_AGREEMENT_ID);

        fromPartyIDType (DBNAlliancePMode.DEFAULT_PARTY_TYPE_ID);
        fromRole (CAS4.DEFAULT_INITIATOR_URL);
        toPartyIDType (DBNAlliancePMode.DEFAULT_PARTY_TYPE_ID);
        toRole (CAS4.DEFAULT_RESPONDER_URL);

        // Other crypt parameters are located in the PMode security part
        cryptParams ().setSessionKeyProvider (ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_256);

        // The default is pilot
        apCAChecker (DBNAllianceTrustStores.Config2023.PILOT_CA);
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the sender participant ID (C1) of the message. The participant ID must be provided prior
     * to sending.
     *
     * @param aSenderID
     *        The sender participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE senderParticipantID (@Nonnull final IParticipantIdentifier aSenderID)
    {
      ValueEnforcer.notNull (aSenderID, "SenderID");
      if (m_aSenderID != null)
        LOGGER.warn ("An existing SenderParticipantID is overridden");
      m_aSenderID = aSenderID;
      return thisAsT ();
    }

    /**
     * Set the receiver participant ID (C4) of the message. The participant ID must be provided
     * prior to sending. This ends up in the "finalRecipient" UserMessage property.
     *
     * @param aReceiverID
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE receiverParticipantID (@Nonnull final IParticipantIdentifier aReceiverID)
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
    @Nonnull
    public final IMPLTYPE documentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
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
    @Nonnull
    public final IMPLTYPE processID (@Nonnull final IProcessIdentifier aProcessID)
    {
      ValueEnforcer.notNull (aProcessID, "ProcessID");
      if (m_aProcessID != null)
        LOGGER.warn ("An existing ProcessID is overridden");
      m_aProcessID = aProcessID;
      return service (aProcessID.getScheme (), aProcessID.getValue ());
    }

    /**
     * Set the abstract endpoint detail provider to be used. This can be an SMP lookup routine or in
     * certain test cases a predefined certificate and endpoint URL.
     *
     * @param aEndpointDetailProvider
     *        The endpoint detail provider to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #smpClient(IBDXR2ServiceMetadataProvider)
     */
    @Nonnull
    public final IMPLTYPE endpointDetailProvider (@Nonnull final IAS4EndpointDetailProvider aEndpointDetailProvider)
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
    @Nonnull
    public final IMPLTYPE smpClient (@Nonnull final IBDXR2ServiceMetadataProvider aSMPClient)
    {
      final AS4EndpointDetailProviderBDXR2 aEndpointDetailProvider = new AS4EndpointDetailProviderBDXR2 (aSMPClient);
      aEndpointDetailProvider.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_DBNA_AS4_v1);
      return endpointDetailProvider (aEndpointDetailProvider);
    }

    /**
     * Use this method to explicit set the AP certificate and AP endpoint URL that was retrieved
     * externally (e.g. via an SMP call or for a static test case).
     *
     * @param aCert
     *        The Peppol AP certificate that should be used to encrypt the message for the receiver.
     *        May not be <code>null</code>.
     * @param sDestURL
     *        The destination URL of the receiving AP to send the AS4 message to. Must be a valid
     *        URL and may neither be <code>null</code> nor empty.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE receiverEndpointDetails (@Nonnull final X509Certificate aCert,
                                                   @Nonnull @Nonempty final String sDestURL)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderConstant (aCert, sDestURL));
    }

    /**
     * Set an optional Consumer for the retrieved certificate from the endpoint details provider,
     * independent of its usability.
     *
     * @param aCertificateConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE certificateConsumer (@Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer)
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
    @Nonnull
    public final IMPLTYPE endpointURLConsumer (@Nullable final Consumer <String> aAPEndpointURLConsumer)
    {
      m_aAPEndpointURLConsumer = aAPEndpointURLConsumer;
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
     * @since 3.1.0
     */
    @Nonnull
    public final IMPLTYPE checkReceiverAPCertificate (final boolean bCheckReceiverAPCertificate)
    {
      m_bCheckReceiverAPCertificate = bCheckReceiverAPCertificate;
      return thisAsT ();
    }

    /**
     * Set a custom DBNAlliance AP certificate CA checker. This is e.g. needed when a non-standard
     * AP certificate is needed. This CA checker checks the certificate provided by the endpoint
     * detail provider (see below). This checker is only used, if
     * {@link #checkReceiverAPCertificate(boolean)} was called with <code>true</code>.
     *
     * @param aCAChecker
     *        The Certificate CA checker to be used. May not be <code>null</code>.
     * @return this for chaining
     * @since 3.1.0
     */
    @Nonnull
    public final IMPLTYPE apCAChecker (@Nonnull final TrustedCAChecker aCAChecker)
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
    protected ESuccess finishFields () throws Phase4Exception
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
        // Check if the received certificate is a valid Peppol AP certificate
        // Throws Phase4PeppolException in case of error
        _checkReceiverAPCert (m_aCAChecker, aReceiverCert, m_aCertificateConsumer, ETriState.UNDEFINED, null);
      }
      else
      {
        LOGGER.warn ("The check of the receiver's DBNAlliance AP certificate was explicitly disabled.");

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

      // From receiver certificate
      toPartyID (CertificateHelper.getSubjectCN (aReceiverCert));

      // Super at the end
      return super.finishFields ();
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
      // m_aCertificateConsumer may be null
      // m_aAPEndpointURLConsumer may be null

      // All valid
      return true;
    }

    @Override
    protected void customizeBeforeSending () throws Phase4Exception
    {
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
   * The builder class for sending AS4 messages using DBNAlliance profile specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Robinson Artemio Garcia Meléndez
   * @author Philip Helger
   */
  public static class DBNAllianceUserMessageBuilder extends
                                                    AbstractDBNAllianceUserMessageBuilder <DBNAllianceUserMessageBuilder>
  {
    private Element m_aPayloadElement;
    private Consumer <? super XHE10XHEType> m_aXHEDocumentConsumer;
    private Consumer <byte []> m_aXHEBytesConsumer;

    public DBNAllianceUserMessageBuilder ()
    {}

    /**
     * Set the payload element to be used, if it is available as a parsed DOM element. Internally
     * the DOM element will be cloned before sending it out. If this method is called, it overwrites
     * any other explicitly set payload.
     *
     * @param aPayloadElement
     *        The payload element to be used. They payload element MUST have a namespace URI. May
     *        not be <code>null</code>.
     * @return this for chaining
     * @deprecated in favour of {@link #payloadElement(Element)}
     */
    @Nonnull
    @Deprecated (forRemoval = true, since = "2.8.6")
    public DBNAllianceUserMessageBuilder payload (@Nonnull final Element aPayloadElement)
    {
      return payloadElement (aPayloadElement);
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
     * @since 2.8.6
     */
    @Nonnull
    public DBNAllianceUserMessageBuilder payloadElement (@Nonnull final Element aPayloadElement)
    {
      ValueEnforcer.notNull (aPayloadElement, "Payload");
      ValueEnforcer.notEmpty (aPayloadElement.getNamespaceURI (), "Payload.NamespaceURI");
      m_aPayloadElement = aPayloadElement;
      return this;
    }

    /**
     * Set an optional Consumer for the created StandardBusinessDocument (SBD).
     *
     * @param aXHEDocumentConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     * @since 3.1.0
     */
    @Nonnull
    public DBNAllianceUserMessageBuilder xheDocumentConsumer (@Nullable final Consumer <? super XHE10XHEType> aXHEDocumentConsumer)
    {
      m_aXHEDocumentConsumer = aXHEDocumentConsumer;
      return this;
    }

    /**
     * Set an optional Consumer for the created StandardBusinessDocument (SBD) bytes.
     *
     * @param aXHEBytesConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     * @since 3.1.0
     */
    @Nonnull
    public DBNAllianceUserMessageBuilder xheBytesConsumer (@Nullable final Consumer <byte []> aXHEBytesConsumer)
    {
      m_aXHEBytesConsumer = aXHEBytesConsumer;
      return this;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected ESuccess finishFields () throws Phase4Exception
    {
      // Perform SMP lookup
      if (super.finishFields ().isFailure ())
        return ESuccess.FAILURE;

      // Ensure a DOM element is present
      if (m_aPayloadElement != null)
      {
        // Ensure only one payload is present
        if (m_aPayload != null)
          throw new Phase4DBNAllianceException ("You cannot provide a payload element and a payload together - please pick one.");

        // Already provided as a DOM element
        final Element aPayloadElement = m_aPayloadElement;

        // Consistency check
        if (CXHE10.NAMESPACE_URI_XHE.equals (aPayloadElement.getNamespaceURI ()))
          throw new Phase4DBNAllianceException ("You cannot set an eXchange Header Envelope as the payload for the regular builder. The XHE is created automatically inside of this builder.");

        // Created SBDH
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Start creating XHE for AS4 message");

        final boolean bClonePayloadElement = true;
        final XHE10XHEType aXHE = _createDBNAllianceXHE (m_aSenderID,
                                                         m_aReceiverID,
                                                         m_aDocTypeID,
                                                         m_aProcessID,
                                                         aPayloadElement,
                                                         bClonePayloadElement);
        if (aXHE == null)
        {
          // A log message was already provided
          return ESuccess.FAILURE;
        }

        if (m_aXHEDocumentConsumer != null)
          m_aXHEDocumentConsumer.accept (aXHE);

        final byte [] aXHEBytes = new XHE10Marshaller ().getAsBytes (aXHE);
        if (m_aXHEBytesConsumer != null)
          m_aXHEBytesConsumer.accept (aXHEBytes);

        // Now we have the main payload
        payload (AS4OutgoingAttachment.builder ()
                                      .data (aXHEBytes)
                                      .compressionGZIP ()
                                      .mimeTypeXML ()
                                      .charset (StandardCharsets.UTF_8));
      }

      // Make sure a payload is present
      if (m_aPayload == null)
      {
        LOGGER.error ("No payload was provided to the DBNAlliance UserMessage builder");
        return ESuccess.FAILURE;
      }

      return ESuccess.SUCCESS;
    }
  }

  /**
   * A builder class for sending AS4 messages using DBNAlliance specifics. Use
   * {@link #sendMessage()} or {@link #sendMessageAndCheckForReceipt()} to trigger the main
   * transmission.<br>
   * This builder class assumes, that the XHE was created outside, therefore no validation can
   * occur.
   *
   * @author Philip Helger
   * @since 3.0.0
   */
  @NotThreadSafe
  public static class DBNAllianceUserMessageXHEBuilder extends
                                                       AbstractDBNAllianceUserMessageBuilder <DBNAllianceUserMessageXHEBuilder>
  {
    private byte [] m_aPayloadBytes;

    /**
     * Create a new builder with the defaults from
     * {@link AbstractDBNAllianceUserMessageBuilder#AbstractDBNAllianceUserMessageBuilder()}
     */
    public DBNAllianceUserMessageXHEBuilder ()
    {}

    /**
     * Set the XHE payload to be used as a byte array. This means, that you need to pass in all
     * other mandatory fields manually (sender participant ID, receiver participant ID, document
     * Type ID and process ID).
     *
     * @param aXHEBytes
     *        The XHE bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #senderParticipantID(IParticipantIdentifier)
     * @see #receiverParticipantID(IParticipantIdentifier)
     */
    @Nonnull
    public DBNAllianceUserMessageXHEBuilder payload (@Nonnull final byte [] aXHEBytes)
    {
      ValueEnforcer.notNull (aXHEBytes, "XHEBytes");
      m_aPayloadBytes = aXHEBytes;
      return this;
    }

    /**
     * Set the payload, the sender participant ID, the receiver participant ID, the document type ID
     * and the process ID.
     *
     * @param aXHE
     *        The XHE to use. May not be <code>null</code>.
     * @return this for chaining
     * @see #payload(byte[])
     * @see #senderParticipantID(IParticipantIdentifier)
     * @see #receiverParticipantID(IParticipantIdentifier)
     */
    @Nonnull
    public DBNAllianceUserMessageXHEBuilder payloadAndMetadata (@Nonnull final DBNAllianceXHEData aXHE)
    {
      ValueEnforcer.notNull (aXHE, "SBDH");

      // Check with logging
      if (!aXHE.areAllFieldsSet (true))
        throw new IllegalArgumentException ("The provided DBNAlliance XHE data is incomplete. See logs for details.");

      final XHE10XHEType aJaxbXHE = aXHE.getAsXHEDocument ();
      final XHE10PayloadType aJaxbPayload = aJaxbXHE.getPayloads ().hasPayloadEntries () ? aJaxbXHE.getPayloads ()
                                                                                                   .getPayloadAtIndex (0)
                                                                                         : null;

      senderParticipantID (aXHE.getFromPartyAsIdentifier ()).receiverParticipantID (aXHE.getToPartyAsIdentifier ());
      if (aJaxbPayload != null)
      {
        if (aJaxbPayload.getCustomizationID () != null)
          documentTypeID (new SimpleDocumentTypeIdentifier (null, aJaxbPayload.getCustomizationID ().getValue ()));

        if (aJaxbPayload.getProfileID () != null)
          processID (new SimpleProcessIdentifier (aJaxbPayload.getProfileID ().getSchemeID (),
                                                  aJaxbPayload.getProfileID ().getValue ()));
      }
      return payload (new XHE10Marshaller ().getAsBytes (aJaxbXHE));
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
    protected ESuccess finishFields () throws Phase4Exception
    {
      // Perform SMP lookup
      if (super.finishFields ().isFailure ())
        return ESuccess.FAILURE;

      // Now we have the main payload
      payload (AS4OutgoingAttachment.builder ().data (m_aPayloadBytes).mimeTypeXML ().compressionGZIP ());

      return ESuccess.SUCCESS;
    }
  }
}
