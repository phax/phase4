/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Node;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.annotation.style.UnsupportedOperation;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.CollectionFind;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IAS4Attachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4Error;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.util.Phase4Exception;
import com.helger.phase4.util.Phase4IncomingException;
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;

/**
 * This is the SPI implementation to handle generic incoming AS4 requests. The main goal of this
 * class is to implement the Peppol specific requirements of packaging data in SBDH. Users of this
 * package must implement {@link IPhase4PeppolIncomingSBDHandlerSPI} instead which provides a more
 * Peppol-style SPI handler. This class is instantiated only once, therefore changing the state of
 * this class may have unintended side effects.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4PeppolServletMessageProcessorSPI implements IAS4IncomingMessageProcessorSPI
{
  /**
   * This class represents the data of a single AS4 attachment.
   *
   * @author Philip Helger
   */
  private static final class ReadAttachment
  {
    private String m_sID;
    private String m_sMimeType;
    private String m_sUncompressedMimeType;
    private Charset m_aCharset;
    private EAS4CompressionMode m_eCompressionMode;
    private byte [] m_aPayloadBytes;
    private StandardBusinessDocument m_aSBDH;

    private ReadAttachment ()
    {}

    @Nullable
    public String getID ()
    {
      return m_sID;
    }

    @Nullable
    public String getMimeType ()
    {
      return m_sMimeType;
    }

    @Nullable
    public String getUncompressedMimeType ()
    {
      return m_sUncompressedMimeType;
    }

    @Nullable
    public Charset getCharset ()
    {
      return m_aCharset;
    }

    @Nullable
    public EAS4CompressionMode getCompressionMode ()
    {
      return m_eCompressionMode;
    }

    @NonNull
    @ReturnsMutableObject
    public byte [] payloadBytes ()
    {
      return m_aPayloadBytes;
    }

    @NonNull
    @ReturnsMutableObject
    public StandardBusinessDocument standardBusinessDocument ()
    {
      return m_aSBDH;
    }
  }

  public static final ESMPTransportProfile DEFAULT_TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4PeppolServletMessageProcessorSPI.class);

  @CodingStyleguideUnaware
  private List <IPhase4PeppolIncomingSBDHandlerSPI> m_aHandlers;
  private ISMPTransportProfile m_aTransportProfile = DEFAULT_TRANSPORT_PROFILE;
  private Phase4PeppolReceiverConfiguration m_aReceiverCheckData;

  /**
   * Constructor. Uses all SPI implementations of {@link IPhase4PeppolIncomingSBDHandlerSPI} as the
   * handlers.
   */
  @UsedViaReflection
  public Phase4PeppolServletMessageProcessorSPI ()
  {
    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IPhase4PeppolIncomingSBDHandlerSPI.class);
    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("Found no instance of IPhase4PeppolIncomingSBDHandlerSPI - this means incoming messages are only checked and afterwards discarded");
  }

  /**
   * @return A list of all contained Peppol specific SBD handlers. Never <code>null</code> but maybe
   *         empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public final ICommonsList <IPhase4PeppolIncomingSBDHandlerSPI> getAllHandler ()
  {
    return new CommonsArrayList <> (m_aHandlers);
  }

  /**
   * Set all handler to be used. This is helpful, if this message processor is not used as an SPI
   * but as a manually configured handler.
   *
   * @param aHandlers
   *        The handler to be set. May not be <code>null</code> but maybe empty (in which case the
   *        message is basically discarded).
   * @return this for chaining
   */
  @NonNull
  public final Phase4PeppolServletMessageProcessorSPI setAllHandler (@NonNull final Iterable <? extends IPhase4PeppolIncomingSBDHandlerSPI> aHandlers)
  {
    ValueEnforcer.notNull (aHandlers, "Handlers");
    m_aHandlers = new CommonsArrayList <> (aHandlers);
    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("Phase4PeppolServletMessageProcessorSPI has an empty handler list - this means incoming messages are only checked and afterwards discarded");
    return this;
  }

  /**
   * @return the transport profile to be handled. Never <code>null</code>. By default it is "Peppol
   *         AS4 v2" (see {@link #DEFAULT_TRANSPORT_PROFILE}).
   */
  @NonNull
  public final ISMPTransportProfile getTransportProfile ()
  {
    return m_aTransportProfile;
  }

  /**
   * Set the transport profile to be used. By default it is Peppol AS4 v2.
   *
   * @param aTransportProfile
   *        The transport profile to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public final Phase4PeppolServletMessageProcessorSPI setTransportProfile (@NonNull final ISMPTransportProfile aTransportProfile)
  {
    ValueEnforcer.notNull (aTransportProfile, "TransportProfile");
    m_aTransportProfile = aTransportProfile;
    return this;
  }

  /**
   * @return The receiver check data to be used. <code>null</code> by default.
   * @since 0.9.13
   */
  @Nullable
  public final Phase4PeppolReceiverConfiguration getReceiverCheckData ()
  {
    return m_aReceiverCheckData;
  }

  /**
   * Set the receiver check data to be used. If set, it overrides the global one defined by
   * {@link Phase4PeppolDefaultReceiverConfiguration}.
   *
   * @param aReceiverCheckData
   *        The customer receiver check data to use. May be <code>null</code>.
   * @return this for chaining
   * @since 0.9.13
   */
  @NonNull
  public final Phase4PeppolServletMessageProcessorSPI setReceiverCheckData (@Nullable final Phase4PeppolReceiverConfiguration aReceiverCheckData)
  {
    m_aReceiverCheckData = aReceiverCheckData;
    return this;
  }

  @Nullable
  private EndpointType _getReceiverEndpoint (@NonNull final String sLogPrefix,
                                             @NonNull final ISMPExtendedServiceMetadataProvider aSMPClient,
                                             @Nullable final IParticipantIdentifier aRecipientID,
                                             @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                             @Nullable final IProcessIdentifier aProcessID) throws Phase4PeppolServletException
  {
    if (aRecipientID == null || aDocTypeID == null || aProcessID == null)
      return null;

    try
    {
      if (LOGGER.isDebugEnabled ())
      {
        LOGGER.debug (sLogPrefix +
                      "Looking up the endpoint of recipient " +
                      aRecipientID.getURIEncoded () +
                      " for " +
                      aRecipientID.getURIEncoded () +
                      " and " +
                      aDocTypeID.getURIEncoded () +
                      " and " +
                      aProcessID.getURIEncoded () +
                      " and " +
                      m_aTransportProfile.getID ());
      }

      // PFUOI 4.3.0
      final SignedServiceMetadataType aSSM = aSMPClient.getSchemeSpecificServiceMetadataOrNull (aRecipientID,
                                                                                                aDocTypeID);
      return aSSM == null ? null : SMPClientReadOnly.getEndpoint (aSSM, aProcessID, m_aTransportProfile);
    }
    catch (final Exception ex)
    {
      throw new Phase4PeppolServletException (sLogPrefix +
                                              "Failed to retrieve endpoint of (" +
                                              aRecipientID.getURIEncoded () +
                                              ", " +
                                              aDocTypeID.getURIEncoded () +
                                              ", " +
                                              aProcessID.getURIEncoded () +
                                              ", " +
                                              m_aTransportProfile.getID () +
                                              ")",
                                              ex);
    }
  }

  private static void _checkIfReceiverEndpointURLMatches (@NonNull final String sLogPrefix,
                                                          @NonNull @Nonempty final String sOwnAPUrl,
                                                          @NonNull final EndpointType aRecipientEndpoint) throws Phase4PeppolServletException
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Our AP URL is " + sOwnAPUrl);

    final String sRecipientAPUrl = SMPClientReadOnly.getEndpointAddress (aRecipientEndpoint);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Recipient AP URL from SMP is " + sRecipientAPUrl);

    // Is it for us?
    if (sRecipientAPUrl == null || !sRecipientAPUrl.contains (sOwnAPUrl))
    {
      final String sErrorMsg = sLogPrefix +
                               "Internal error: The request is targeted for '" +
                               sRecipientAPUrl +
                               "' and is not for us (" +
                               sOwnAPUrl +
                               ")";
      LOGGER.error (sErrorMsg);
      throw new Phase4PeppolServletException (sErrorMsg);
    }
  }

  private static void _checkIfEndpointCertificateMatches (@NonNull final String sLogPrefix,
                                                          @NonNull final X509Certificate aOurCert,
                                                          @NonNull final EndpointType aRecipientEndpoint) throws Phase4PeppolServletException
  {
    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    final X509Certificate aRecipientCert;
    try
    {
      aRecipientCert = new CertificateDecodeHelper ().source (sRecipientCertString)
                                                     .pemEncoded (true)
                                                     .getDecodedOrThrow ();
    }
    catch (final IllegalArgumentException | CertificateException t)
    {
      throw new Phase4PeppolServletException (sLogPrefix +
                                              "Internal error: Failed to convert looked up endpoint certificate string '" +
                                              sRecipientCertString +
                                              "' to an X.509 certificate!",
                                              t);
    }

    if (aRecipientCert == null)
    {
      // No certificate found - most likely because of invalid SMP entry
      throw new Phase4PeppolServletException (sLogPrefix +
                                              "No certificate found in looked up endpoint! Is this AP maybe NOT contained in an SMP?");
    }

    // Certificate found
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Conformant recipient certificate present: " + aRecipientCert.toString ());

    // Compare serial numbers
    if (!aOurCert.getSerialNumber ().equals (aRecipientCert.getSerialNumber ()))
    {
      final String sErrorMsg = sLogPrefix +
                               "Certificate retrieved from SMP lookup (" +
                               aRecipientCert +
                               ") does not match this APs configured Certificate (" +
                               aOurCert +
                               ") - different serial numbers - ignoring document";
      LOGGER.error (sErrorMsg);
      throw new Phase4PeppolServletException (sErrorMsg);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "The certificate of the SMP lookup matches our certificate");
  }

  /**
   * Method to create a new {@link PeppolReportingItem} for a received message.
   *
   * @param aUserMessage
   *        The current AS4 UserMessage. May not be <code>null</code>.
   * @param aPeppolSBD
   *        The parsed Peppol SBDH object. May not be <code>null</code>.
   * @param aState
   *        The processing state of the incoming message. May not be <code>null</code>.
   * @param sC3ID
   *        The Peppol Service Provider Seat ID (in the format PXX000000). May neither be
   *        <code>null</code> nor empty.
   * @param sC4CountryCode
   *        The country code of the End User that is the business receiver of the document. Must
   *        neither be <code>null</code> nor empty.
   * @param sEndUserID
   *        The internal (local) ID of the End User that is the business receiver of the document.
   *        This ID is NOT part of the reporting towards OpenPeppol, it is just for created
   *        aggregating counts. Must neither be <code>null</code> nor empty.
   * @return <code>null</code> if not all necessary elements are present. Check logs for details.
   * @since 2.2.2
   */
  @Nullable
  public static PeppolReportingItem createPeppolReportingItemForReceivedMessage (@NonNull final Ebms3UserMessage aUserMessage,
                                                                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                                                                 @NonNull final IAS4IncomingMessageState aState,
                                                                                 @NonNull @Nonempty final String sC3ID,
                                                                                 @NonNull @Nonempty final String sC4CountryCode,
                                                                                 @NonNull @Nonempty final String sEndUserID)
  {
    ValueEnforcer.notNull (aUserMessage, "UserMessage");
    ValueEnforcer.notNull (aPeppolSBD, "PeppolSBD");
    ValueEnforcer.notNull (aState, "State");
    ValueEnforcer.notEmpty (sC3ID, "C3ID");
    ValueEnforcer.notEmpty (sC4CountryCode, "C4CountryCode");
    ValueEnforcer.notEmpty (sEndUserID, "EndUserID");

    // In case of success start building reporting item
    final XMLOffsetDateTime aUserMsgDT = aUserMessage.getMessageInfo ().getTimestamp ();
    final OffsetDateTime aExchangeDT;
    if (aUserMsgDT != null)
    {
      // Take AS4 sending date time
      if (aUserMsgDT.getOffset () != null)
        aExchangeDT = aUserMsgDT.toOffsetDateTime ();
      else
        aExchangeDT = OffsetDateTime.of (aUserMsgDT.toLocalDateTime (), ZoneOffset.UTC);
    }
    else
    {
      // Try SBDH
      final XMLOffsetDateTime aSbdhDT = aPeppolSBD.getCreationDateAndTime ();
      if (aSbdhDT != null)
      {
        // Take SBDH creation date time
        if (aSbdhDT.getOffset () != null)
          aExchangeDT = aSbdhDT.toOffsetDateTime ();
        else
          aExchangeDT = OffsetDateTime.of (aSbdhDT.toLocalDateTime (), ZoneOffset.UTC);
      }
      else
      {
        // Neither in AS4 nor in SBDH
        LOGGER.warn ("Incoming messages does not contain a UserMessage/MessageInfo/Timestamp value and no SBDH CreationDateTime. Using current date time");
        aExchangeDT = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
      }
    }

    // Incoming message signed by C2
    final String sC2ID = CertificateHelper.getSubjectCN (aState.getSigningCertificate ());

    try
    {
      String sC1CountryCode = aPeppolSBD.getCountryC1 ();
      if (StringHelper.isEmpty (sC1CountryCode))
      {
        // Fallback to ZZ to make sure the item can be created
        sC1CountryCode = CPeppolReporting.REPLACEMENT_COUNTRY_CODE;
      }

      return PeppolReportingItem.builder ()
                                .exchangeDateTime (aExchangeDT)
                                .directionReceiving ()
                                .c2ID (sC2ID)
                                .c3ID (sC3ID)
                                .docTypeID (aPeppolSBD.getDocumentTypeAsIdentifier ())
                                .processID (aPeppolSBD.getProcessAsIdentifier ())
                                .transportProtocolPeppolAS4v2 ()
                                .c1CountryCode (sC1CountryCode)
                                .c4CountryCode (sC4CountryCode)
                                .endUserID (sEndUserID)
                                .build ();
    }
    catch (final IllegalStateException ex)
    {
      LOGGER.error ("Not all mandatory fields are set. Cannot create Peppol Reporting Item", ex);
      return null;
    }
  }

  /**
   * Method that is invoked after the message was successfully processed with at least one handler,
   * and before a Receipt is returned. By default this method does nothing. The idea was to override
   * this method to allow for remembering the created transaction for Peppol Reporting.
   *
   * @param aUserMessage
   *        The current AS4 UserMessage. Never <code>null</code>.
   * @param aPeppolSBD
   *        The parsed Peppol SBDH object. Never <code>null</code>.
   * @param aState
   *        The processing state of the incoming message. Never <code>null</code>.
   * @since 2.2.2
   * @see #createPeppolReportingItemForReceivedMessage(Ebms3UserMessage, PeppolSBDHData,
   *      IAS4IncomingMessageState, String, String, String)
   */
  @OverrideOnDemand
  protected void afterSuccessfulPeppolProcessing (@NonNull final Ebms3UserMessage aUserMessage,
                                                  @NonNull final PeppolSBDHData aPeppolSBD,
                                                  @NonNull final IAS4IncomingMessageState aState)
  {}

  @NonNull
  public AS4MessageProcessorResult processAS4UserMessage (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @NonNull final HttpHeaderMap aHttpHeaders,
                                                          @NonNull final Ebms3UserMessage aUserMessage,
                                                          @NonNull final IPMode aSrcPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @NonNull final IAS4IncomingMessageState aState,
                                                          @NonNull final AS4ErrorList aProcessingErrorMessages)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Invoking processAS4UserMessage");

    final String sMessageID = aUserMessage.getMessageInfo ().getMessageId ();
    final String sServiceType = aUserMessage.getCollaborationInfo ().getService ().getType ();
    final String sService = aUserMessage.getCollaborationInfo ().getServiceValue ();
    final String sAction = aUserMessage.getCollaborationInfo ().getAction ();
    final String sConversationID = aUserMessage.getCollaborationInfo ().getConversationId ();
    final Locale aDisplayLocale = aState.getLocale ();
    final String sLogPrefix = "[" + sMessageID + "] ";

    // Start consistency checks if the receiver is supported or not
    final Phase4PeppolReceiverConfiguration aReceiverCheckData = m_aReceiverCheckData != null ? m_aReceiverCheckData
                                                                                              : Phase4PeppolDefaultReceiverConfiguration.getAsReceiverCheckData ();

    // Debug log
    if (LOGGER.isDebugEnabled ())
    {
      if (aSrcPMode == null)
        LOGGER.debug (sLogPrefix + "  No Source PMode present");
      else
        LOGGER.debug (sLogPrefix + "  Source PMode = " + aSrcPMode.getID ());
      LOGGER.debug (sLogPrefix + "  AS4 Message ID = '" + sMessageID + "'");
      LOGGER.debug (sLogPrefix + "  AS4 Service Type = '" + sServiceType + "'");
      LOGGER.debug (sLogPrefix + "  AS4 Service = '" + sService + "'");
      LOGGER.debug (sLogPrefix + "  AS4 Action = '" + sAction + "'");
      LOGGER.debug (sLogPrefix + "  AS4 ConversationId = '" + sConversationID + "'");

      // Log User Message Message Properties
      if (aUserMessage.getMessageProperties () != null && aUserMessage.getMessageProperties ().hasPropertyEntries ())
      {
        LOGGER.debug (sLogPrefix + "  AS4 MessageProperties:");
        for (final Ebms3Property p : aUserMessage.getMessageProperties ().getProperty ())
          LOGGER.debug (sLogPrefix + "    [" + p.getName () + "] = [" + p.getValue () + "]");
      }
      else
        LOGGER.debug (sLogPrefix + "  No AS4 Mesage Properties present");

      if (aPayload == null)
        LOGGER.debug (sLogPrefix + "  No SOAP Body Payload present");
      else
        LOGGER.debug (sLogPrefix + "  SOAP Body Payload = " + XMLWriter.getNodeAsString (aPayload));
    }

    // Check preconditions
    if (!aState.isSoapDecrypted ())
    {
      final String sDetails = "The received Peppol message seems not to be encrypted (properly).";
      LOGGER.error (sLogPrefix + sDetails);
      aProcessingErrorMessages.add (EEbmsError.EBMS_FAILED_DECRYPTION.errorBuilder (aDisplayLocale)
                                                                     .refToMessageInError (sMessageID)
                                                                     .errorDetail (sDetails)
                                                                     .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    if (!aState.isSoapSignatureChecked ())
    {
      final String sDetails = "The received Peppol message seems not to be signed (properly).";
      LOGGER.error (sLogPrefix + sDetails);
      aProcessingErrorMessages.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.errorBuilder (aDisplayLocale)
                                                                         .refToMessageInError (sMessageID)
                                                                         .errorDetail (sDetails)
                                                                         .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    if (aReceiverCheckData.isCheckSigningCertificateRevocation ())
    {
      final OffsetDateTime aNow = MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ();
      final X509Certificate aSenderSigningCert = aState.getSigningCertificate ();
      // Check if signing AP certificate is revoked
      // * Use global caching setting
      // * Use global certificate check mode
      final ECertificateCheckResult eCertCheckResult = aReceiverCheckData.getAPCAChecker ()
                                                                         .checkCertificate (aSenderSigningCert, aNow);
      if (eCertCheckResult.isInvalid ())
      {
        final String sDetails = "The received Peppol message is signed with a Peppol AP certificate invalid at " +
                                aNow +
                                ". Rejecting incoming message. Reason: " +
                                eCertCheckResult.getReason ();
        LOGGER.error (sLogPrefix + sDetails);
        aProcessingErrorMessages.add (EEbmsError.EBMS_FAILED_AUTHENTICATION.errorBuilder (aDisplayLocale)
                                                                           .refToMessageInError (sMessageID)
                                                                           .errorDetail (sDetails)
                                                                           .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }
    else
    {
      LOGGER.warn (sLogPrefix + "The revocation check of the received signing certificate is disabled.");
    }

    // Read all attachments and copy them into local objects. That eventually
    // includes decompressing them.
    final ICommonsList <ReadAttachment> aReadAttachments = new CommonsArrayList <> ();
    if (aIncomingAttachments != null)
    {
      int nAttachmentIndex = 0;
      for (final IAS4Attachment aIncomingAttachment : aIncomingAttachments)
      {
        final ReadAttachment a = new ReadAttachment ();
        a.m_sID = aIncomingAttachment.getId ();
        a.m_sMimeType = aIncomingAttachment.getMimeType ();
        a.m_sUncompressedMimeType = aIncomingAttachment.getUncompressedMimeType ();
        a.m_aCharset = aIncomingAttachment.getCharset ();
        a.m_eCompressionMode = aIncomingAttachment.getCompressionMode ();

        // This stream is decompressing if needed
        try (final InputStream aSIS = aIncomingAttachment.getSourceStream ())
        {
          // Get a decompressed copy
          // And yes, for very large files, this is not a good idea
          final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
          if (StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aSIS, aBAOS).isSuccess ())
          {
            a.m_aPayloadBytes = aBAOS.getBufferOrCopy ();
          }
        }
        catch (final IOException | AS4DecompressException ex)
        {
          // Fall through
        }
        if (a.m_aPayloadBytes == null)
        {
          final String sDetails = "Failed to decompress the payload of attachment #" + nAttachmentIndex;
          LOGGER.error (sLogPrefix + sDetails);
          aProcessingErrorMessages.add (EEbmsError.EBMS_DECOMPRESSION_FAILURE.errorBuilder (aDisplayLocale)
                                                                             .refToMessageInError (sMessageID)
                                                                             .errorDetail (sDetails)
                                                                             .build ());
          return AS4MessageProcessorResult.createFailure ();
        }

        // Read data as SBDH
        // Hint for production systems: this may take a huge amount of memory,
        // if the payload is large
        final ErrorList aSBDHErrors = new ErrorList ();
        a.m_aSBDH = new SBDMarshaller ().setCollectErrors (aSBDHErrors).read (a.m_aPayloadBytes);

        // Only fail if the first attachment is not an SBDH. The check for
        // exactly 1 attachment comes below
        if (nAttachmentIndex == 0 && a.m_aSBDH == null)
        {
          if (aSBDHErrors.isEmpty ())
          {
            final String sDetails = "Failed to read the provided SBDH document";
            LOGGER.error (sLogPrefix + sDetails);
            aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                               .refToMessageInError (sMessageID)
                                                               .errorDetail (sDetails)
                                                               .build ());
          }
          else
          {
            // Add all SBDH errors to the output
            for (final IError aError : aSBDHErrors)
            {
              final String sDetails = "Peppol SBDH Issue: " + aError.getAsString (aDisplayLocale);
              LOGGER.error (sLogPrefix + sDetails);
              aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                 .refToMessageInError (sMessageID)
                                                                 .errorDetail (sDetails)
                                                                 .build ());
            }
          }

          return AS4MessageProcessorResult.createFailure ();
        }

        // We found a valid attachment - remember it
        aReadAttachments.add (a);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug (sLogPrefix +
                        "AS4 Attachment " +
                        nAttachmentIndex +
                        " with ID [" +
                        a.m_sID +
                        "] uses [" +
                        a.m_sMimeType +
                        (a.m_sUncompressedMimeType == null ? null : " - uncompressed " + a.m_sUncompressedMimeType) +
                        "] and [" +
                        StringHelper.getToString (a.m_aCharset, "no charset") +
                        "] and length is " +
                        (a.m_aPayloadBytes == null ? "<error>" : Integer.toString (a.m_aPayloadBytes.length)) +
                        " bytes" +
                        (a.m_eCompressionMode == null ? "" : " of compressed payload"));
        nAttachmentIndex++;
      }
    }

    if (aReadAttachments.size () <= 0)
    {
      // In Peppol there must be exactly one payload
      final String sDetails = "In Peppol exactly one payload attachment is expected. This request has " +
                              aReadAttachments.size () +
                              " attachments";
      LOGGER.error (sLogPrefix + sDetails);
      aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                         .refToMessageInError (aState.getMessageID ())
                                                         .errorDetail (sDetails)
                                                         .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    // The one and only
    final ReadAttachment aReadAttachment = aReadAttachments.getFirstOrNull ();

    // Extract Peppol values from SBDH
    final PeppolSBDHData aPeppolSBDH;
    try
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Now evaluating the SBDH against Peppol rules");

      // Interpret as Peppol SBDH and eventually perform consistency checks
      final IIdentifierFactory aIdentifierFactory = aReceiverCheckData.getSBDHIdentifierFactory ();
      final boolean bPerformValueChecks = aReceiverCheckData.isPerformSBDHValueChecks ();
      final boolean bCheckForCountryC1 = aReceiverCheckData.isCheckSBDHForMandatoryCountryC1 ();
      final PeppolSBDHDataReader aReader = new PeppolSBDHDataReader (aIdentifierFactory).setPerformValueChecks (bPerformValueChecks)
                                                                                        .setCheckForCountryC1 (bCheckForCountryC1);

      aPeppolSBDH = aReader.extractData (aReadAttachment.standardBusinessDocument ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix +
                      "The provided SBDH is valid according to Peppol rules, with value checks being " +
                      (bPerformValueChecks ? "enabled" : "disabled"));
    }
    catch (final PeppolSBDHDataReadException ex)
    {
      final String sMsg = "Failed to extract the Peppol data from SBDH.";
      LOGGER.error (sLogPrefix + sMsg, ex);
      aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                         .refToMessageInError (aState.getMessageID ())
                                                         .errorDetail (sMsg, ex)
                                                         .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    // Compare C1 ID from SBDH with the one of AS4
    {
      // Theoretically the MessageProperties must be present, but if someone disables the phase4
      // PMode Validation it may be null
      final Ebms3Property aOriginalSender = aUserMessage.getMessageProperties () == null ? null
                                                                                         : CollectionFind.findFirst (aUserMessage.getMessageProperties ()
                                                                                                                                 .getProperty (),
                                                                                                                     x -> CAS4.ORIGINAL_SENDER.equals (x.getName ()));
      final PeppolParticipantIdentifier aSBDHC1PID = aOriginalSender == null ? null
                                                                             : PeppolIdentifierFactory.INSTANCE.createParticipantIdentifier (aOriginalSender.getType (),
                                                                                                                                             aOriginalSender.getValue ());
      if (!aPeppolSBDH.getSenderAsIdentifier ().hasSameContent (aSBDHC1PID))
      {
        final String sMsg = "The AS4 originalSender (" +
                            (aSBDHC1PID == null ? "not provided" : aSBDHC1PID.getURIEncoded ()) +
                            " ) is different from the SBDH Sender Identifier (" +
                            aPeppolSBDH.getSenderAsIdentifier ().getURIEncoded () +
                            ")";
        LOGGER.error (sLogPrefix + sMsg);
        aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                           .refToMessageInError (aState.getMessageID ())
                                                           .errorDetail (sMsg)
                                                           .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }

    // Compare C4 ID from SBDH with the one of AS4
    {
      // Theoretically the MessageProperties must be present, but if someone disables the phase4
      // PMode Validation it may be null
      final Ebms3Property aFinalRecipient = aUserMessage.getMessageProperties () == null ? null
                                                                                         : CollectionFind.findFirst (aUserMessage.getMessageProperties ()
                                                                                                                                 .getProperty (),
                                                                                                                     x -> CAS4.FINAL_RECIPIENT.equals (x.getName ()));
      final PeppolParticipantIdentifier aSBDHC4PID = aFinalRecipient == null ? null
                                                                             : PeppolIdentifierFactory.INSTANCE.createParticipantIdentifier (aFinalRecipient.getType (),
                                                                                                                                             aFinalRecipient.getValue ());
      if (!aPeppolSBDH.getReceiverAsIdentifier ().hasSameContent (aSBDHC4PID))
      {
        final String sMsg = "The AS4 finalRecipient (" +
                            (aSBDHC4PID == null ? "not provided" : aSBDHC4PID.getURIEncoded ()) +
                            " ) is different from the SBDH Sender Identifier (" +
                            aPeppolSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                            ")";
        LOGGER.error (sLogPrefix + sMsg);
        aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                           .refToMessageInError (aState.getMessageID ())
                                                           .errorDetail (sMsg)
                                                           .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }

    // If the receiver checks are activated, run them now
    if (aReceiverCheckData.isReceiverCheckEnabled ())
    {
      LOGGER.info (sLogPrefix + "Performing checks if the received data is registered in our SMP");

      try
      {
        // Get the endpoint information required from the recipient
        // Check if an endpoint is registered
        final IParticipantIdentifier aReceiverID = aPeppolSBDH.getReceiverAsIdentifier ();
        final IDocumentTypeIdentifier aDocTypeID = aPeppolSBDH.getDocumentTypeAsIdentifier ();
        final IProcessIdentifier aProcessID = aPeppolSBDH.getProcessAsIdentifier ();
        final EndpointType aReceiverEndpoint = _getReceiverEndpoint (sLogPrefix,
                                                                     aReceiverCheckData.getSMPClient (),
                                                                     aReceiverID,
                                                                     aDocTypeID,
                                                                     aProcessID);
        if (aReceiverEndpoint == null)
        {
          final String sMsg = "Failed to resolve SMP endpoint for provided receiver ID (" +
                              (aReceiverID == null ? "null" : aReceiverID.getURIEncoded ()) +
                              ")/documentType ID (" +
                              (aDocTypeID == null ? "null" : aDocTypeID.getURIEncoded ()) +
                              ")/process ID (" +
                              (aProcessID == null ? "null" : aProcessID.getURIEncoded ()) +
                              ")/transport profile (" +
                              m_aTransportProfile.getID () +
                              ") - not handling incoming AS4 document";
          LOGGER.error (sLogPrefix + sMsg);
          // the errorDetail MUST be set according to Peppol AS4 profile 2.2
          aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                             .refToMessageInError (aState.getMessageID ())
                                                             .description (sMsg, aDisplayLocale)
                                                             .errorDetail ("PEPPOL:NOT_SERVICED")
                                                             .build ());
          return AS4MessageProcessorResult.createFailure ();
        }

        // Check if the message is for us
        _checkIfReceiverEndpointURLMatches (sLogPrefix, aReceiverCheckData.getAS4EndpointURL (), aReceiverEndpoint);

        // Get the recipient certificate from the SMP
        _checkIfEndpointCertificateMatches (sLogPrefix, aReceiverCheckData.getAPCertificate (), aReceiverEndpoint);
      }
      catch (final Phase4Exception ex)
      {
        final String sMsg = "The addressing data contained in the SBDH could not be verified";
        LOGGER.error (sLogPrefix + sMsg, ex);

        final int nHttpStatusCode = ex instanceof final Phase4IncomingException pix ? pix.getHttpStatusCode ()
                                                                                    : CAS4.HTTP_STATUS_UNDEFINED;

        aProcessingErrorMessages.add (AS4Error.builder ()
                                              .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                               .refToMessageInError (aState.getMessageID ())
                                                                               .errorDetail (sMsg, ex))
                                              .httpStatusCode (nHttpStatusCode)
                                              .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }
    else
    {
      LOGGER.info (sLogPrefix + "Endpoint checks for incoming AS4 messages are disabled");
    }

    // Receiving checks are positively done

    if (m_aHandlers.isEmpty ())
    {
      // Oops - programming error
      LOGGER.error (sLogPrefix + "No SPI handler is present - the message is unhandled and discarded!");
      if (GlobalDebug.isProductionMode ())
      {
        // This error is only in production mode
        // It will trigger a rejection on AS4 level
        aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                           .refToMessageInError (aState.getMessageID ())
                                                           .errorDetail ("The phase4 implementation is marked as in production, but has no capabilities to process an incoming Peppol message." +
                                                                         " Unfortunately, the Peppol message needs to be rejected for that reason.")
                                                           .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }
    else
    {
      // Now start invoking SPI handlers
      for (final IPhase4PeppolIncomingSBDHandlerSPI aHandler : m_aHandlers)
      {
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sLogPrefix + "Invoking Peppol handler " + aHandler);
          aHandler.handleIncomingSBD (aMessageMetadata,
                                      aHttpHeaders.getClone (),
                                      aUserMessage.clone (),
                                      aReadAttachment.payloadBytes (),
                                      aReadAttachment.standardBusinessDocument (),
                                      aPeppolSBDH,
                                      aState,
                                      aProcessingErrorMessages);
        }
        catch (final Exception ex)
        {
          final String sDetails = "The incoming Peppol message could not be processed.";
          LOGGER.error (sLogPrefix + sDetails, ex);

          final int nHttpStatusCode = ex instanceof final Phase4IncomingException pix ? pix.getHttpStatusCode ()
                                                                                      : CAS4.HTTP_STATUS_UNDEFINED;

          aProcessingErrorMessages.add (AS4Error.builder ()
                                                .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                                 .refToMessageInError (aState.getMessageID ())
                                                                                 .errorDetail (sDetails, ex))
                                                .httpStatusCode (nHttpStatusCode)
                                                .build ());
          return AS4MessageProcessorResult.createFailure ();
        }
      }

      // Trigger post-processing, e.g. for reporting
      afterSuccessfulPeppolProcessing (aUserMessage, aPeppolSBDH, aState);
    }

    return AS4MessageProcessorResult.createSuccess ();
  }

  @NonNull
  @UnsupportedOperation
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @NonNull final HttpHeaderMap aHttpHeaders,
                                                                  @NonNull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @NonNull final IAS4IncomingMessageState aState,
                                                                  @NonNull final AS4ErrorList aProcessingErrorMessages)
  {
    LOGGER.error ("Invoking processAS4SignalMessage is not supported");
    throw new UnsupportedOperationException ();
  }

  public void processAS4ResponseMessage (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                         @NonNull final IAS4IncomingMessageState aState,
                                         @NonNull @Nonempty final String sResponseMessageID,
                                         @Nullable final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable)
  {
    // Now start invoking SPI handlers
    for (final IPhase4PeppolIncomingSBDHandlerSPI aHandler : m_aHandlers)
    {
      // Just pass it through
      aHandler.processAS4ResponseMessage (aMessageMetadata,
                                          aState,
                                          sResponseMessageID,
                                          aResponseBytes,
                                          bResponsePayloadIsAvailable);
    }
  }
}
