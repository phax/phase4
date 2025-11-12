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
package com.helger.phase4.hredelivery.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Node;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
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
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHData;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHDataReadException;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHDataReader;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IAS4Attachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
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
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This is the SPI implementation to handle generic incoming AS4 requests. The main goal of this
 * class is to implement the HR eDelivery specific requirements of packaging data in SBDH. Users of
 * this package must implement {@link IPhase4HREDeliveryIncomingSBDHandlerSPI} instead which
 * provides a more HR eDelivery-style SPI handler. This class is instantiated only once, therefore
 * changing the state of this class may have unintended side effects.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4HREDeliveryServletMessageProcessorSPI implements IAS4IncomingMessageProcessorSPI
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

    @Nonnull
    @ReturnsMutableObject
    public byte [] payloadBytes ()
    {
      return m_aPayloadBytes;
    }

    @Nonnull
    @ReturnsMutableObject
    public StandardBusinessDocument standardBusinessDocument ()
    {
      return m_aSBDH;
    }
  }

  public static final ESMPTransportProfile DEFAULT_TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_ERACUN_AS4_V1;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4HREDeliveryServletMessageProcessorSPI.class);

  private List <IPhase4HREDeliveryIncomingSBDHandlerSPI> m_aHandlers;
  private ISMPTransportProfile m_aTransportProfile = DEFAULT_TRANSPORT_PROFILE;
  private Phase4HREDeliveryReceiverConfiguration m_aReceiverCheckData;

  /**
   * Constructor. Uses all SPI implementations of {@link IPhase4HREDeliveryIncomingSBDHandlerSPI} as
   * the handlers.
   */
  @UsedViaReflection
  public Phase4HREDeliveryServletMessageProcessorSPI ()
  {
    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IPhase4HREDeliveryIncomingSBDHandlerSPI.class);
    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("Found no instance of IPhase4HREDeliveryIncomingSBDHandlerSPI - this means incoming messages are only checked and afterwards discarded");
  }

  /**
   * @return A list of all contained HR eDelivery specific SBD handlers. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <IPhase4HREDeliveryIncomingSBDHandlerSPI> getAllHandler ()
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
  @Nonnull
  public final Phase4HREDeliveryServletMessageProcessorSPI setAllHandler (@Nonnull final Iterable <? extends IPhase4HREDeliveryIncomingSBDHandlerSPI> aHandlers)
  {
    ValueEnforcer.notNull (aHandlers, "Handlers");
    m_aHandlers = new CommonsArrayList <> (aHandlers);
    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("Phase4HREDeliveryServletMessageProcessorSPI has an empty handler list - this means incoming messages are only checked and afterwards discarded");
    return this;
  }

  /**
   * @return the transport profile to be handled. Never <code>null</code>. By default it is "HR
   *         eDelivery AS4 1.0" (see {@link #DEFAULT_TRANSPORT_PROFILE}).
   */
  @Nonnull
  public final ISMPTransportProfile getTransportProfile ()
  {
    return m_aTransportProfile;
  }

  /**
   * Set the transport profile to be used. By default it is HR eDelivery AS4 1.0.
   *
   * @param aTransportProfile
   *        The transport profile to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final Phase4HREDeliveryServletMessageProcessorSPI setTransportProfile (@Nonnull final ISMPTransportProfile aTransportProfile)
  {
    ValueEnforcer.notNull (aTransportProfile, "TransportProfile");
    m_aTransportProfile = aTransportProfile;
    return this;
  }

  /**
   * @return The receiver check data to be used. <code>null</code> by default.
   */
  @Nullable
  public final Phase4HREDeliveryReceiverConfiguration getReceiverCheckData ()
  {
    return m_aReceiverCheckData;
  }

  /**
   * Set the receiver check data to be used. If set, it overrides the global one defined by
   * {@link Phase4HREDeliveryDefaultReceiverConfiguration}.
   *
   * @param aReceiverCheckData
   *        The customer receiver check data to use. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final Phase4HREDeliveryServletMessageProcessorSPI setReceiverCheckData (@Nullable final Phase4HREDeliveryReceiverConfiguration aReceiverCheckData)
  {
    m_aReceiverCheckData = aReceiverCheckData;
    return this;
  }

  @Nullable
  private EndpointType _getReceiverEndpoint (@Nonnull final String sLogPrefix,
                                             @Nonnull final ISMPExtendedServiceMetadataProvider aSMPClient,
                                             @Nullable final IParticipantIdentifier aRecipientID,
                                             @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                             @Nullable final IProcessIdentifier aProcessID) throws Phase4HREDeliveryServletException
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
      throw new Phase4HREDeliveryServletException (sLogPrefix +
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

  private static void _checkIfReceiverEndpointURLMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull @Nonempty final String sOwnAPUrl,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws Phase4HREDeliveryServletException
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
      throw new Phase4HREDeliveryServletException (sErrorMsg);
    }
  }

  private static void _checkIfEndpointCertificateMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull final X509Certificate aOurCert,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws Phase4HREDeliveryServletException
  {
    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    X509Certificate aRecipientCert = null;
    try
    {
      aRecipientCert = CertificateHelper.convertStringToCertficate (sRecipientCertString);
    }
    catch (final CertificateException t)
    {
      throw new Phase4HREDeliveryServletException (sLogPrefix +
                                                   "Internal error: Failed to convert looked up endpoint certificate string '" +
                                                   sRecipientCertString +
                                                   "' to an X.509 certificate!",
                                                   t);
    }

    if (aRecipientCert == null)
    {
      // No certificate found - most likely because of invalid SMP entry
      throw new Phase4HREDeliveryServletException (sLogPrefix +
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
      throw new Phase4HREDeliveryServletException (sErrorMsg);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "The certificate of the SMP lookup matches our certificate");
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aSrcPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4IncomingMessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
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
    final Phase4HREDeliveryReceiverConfiguration aReceiverCheckData = m_aReceiverCheckData != null ? m_aReceiverCheckData
                                                                                                   : Phase4HREDeliveryDefaultReceiverConfiguration.getAsReceiverCheckData ();

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
      final String sDetails = "The received HR eDelivery message seems not to be encrypted (properly).";
      LOGGER.error (sLogPrefix + sDetails);
      aProcessingErrorMessages.add (EEbmsError.EBMS_FAILED_DECRYPTION.errorBuilder (aDisplayLocale)
                                                                     .refToMessageInError (sMessageID)
                                                                     .errorDetail (sDetails)
                                                                     .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    if (!aState.isSoapSignatureChecked ())
    {
      final String sDetails = "The received HR eDelivery message seems not to be signed (properly).";
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
        final String sDetails = "The received HR eDelivery message is signed with a HR eDelivery AP certificate invalid at " +
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
              final String sDetails = "HR eDelivery SBDH Issue: " + aError.getAsString (aDisplayLocale);
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
      // In HR eDelivery there must be exactly one payload
      final String sDetails = "In HR eDelivery exactly one payload attachment is expected. This request has " +
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

    // Extract HR eDelivery values from SBDH
    final HREDeliverySBDHData aHREDeliverySBDH;
    try
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Now evaluating the SBDH against HR eDelivery rules");

      // Interpret as HR eDelivery SBDH and eventually perform consistency checks
      final IIdentifierFactory aIdentifierFactory = aReceiverCheckData.getSBDHIdentifierFactory ();
      final boolean bPerformValueChecks = aReceiverCheckData.isPerformSBDHValueChecks ();
      final HREDeliverySBDHDataReader aReader = new HREDeliverySBDHDataReader (aIdentifierFactory).setPerformValueChecks (bPerformValueChecks);

      aHREDeliverySBDH = aReader.extractData (aReadAttachment.standardBusinessDocument ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix +
                      "The provided SBDH is valid according to HR eDelivery rules, with value checks being " +
                      (bPerformValueChecks ? "enabled" : "disabled"));
    }
    catch (final HREDeliverySBDHDataReadException ex)
    {
      final String sMsg = "Failed to extract the HR eDelivery data from SBDH.";
      LOGGER.error (sLogPrefix + sMsg, ex);
      aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                         .refToMessageInError (aState.getMessageID ())
                                                         .errorDetail (sMsg, ex)
                                                         .build ());
      return AS4MessageProcessorResult.createFailure ();
    }

    // Compared to Peppol the data is only contained in the AS4 header so we need to parse it
    // again. To avoid unexpected errors, we need to be as resilient as possible.
    final IDocumentTypeIdentifier aDocTypeID = SimpleIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (sAction);
    final IProcessIdentifier aProcessID = SimpleIdentifierFactory.INSTANCE.createProcessIdentifier (sServiceType,
                                                                                                    sService);

    // If the receiver checks are activated, run them now
    if (aReceiverCheckData.isReceiverCheckEnabled ())
    {
      LOGGER.info (sLogPrefix + "Performing checks if the received data is registered in our SMP");

      try
      {
        // Get the endpoint information required from the recipient
        // Check if an endpoint is registered
        final IParticipantIdentifier aReceiverID = aHREDeliverySBDH.getReceiverAsIdentifier ();
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
          // the errorDetail MUST be set according to HR eDelivery AS4 profile chapter 5.4
          aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                             .refToMessageInError (aState.getMessageID ())
                                                             .description (sMsg, aDisplayLocale)
                                                             .errorDetail ("ERACUN:NOT_SERVICED")
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
        aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                           .refToMessageInError (aState.getMessageID ())
                                                           .errorDetail (sMsg, ex)
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
                                                           .errorDetail ("The phase4 implementation is marked as in production, but has no capabilities to process an incoming HR eDelivery message." +
                                                                         " Unfortunately, the HR eDelivery message needs to be rejected for that reason.")
                                                           .build ());
        return AS4MessageProcessorResult.createFailure ();
      }
    }
    else
    {
      // Now start invoking SPI handlers
      for (final IPhase4HREDeliveryIncomingSBDHandlerSPI aHandler : m_aHandlers)
      {
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sLogPrefix + "Invoking HR eDelivery handler " + aHandler);
          aHandler.handleIncomingSBD (aMessageMetadata,
                                      aHttpHeaders.getClone (),
                                      aUserMessage.clone (),
                                      aReadAttachment.payloadBytes (),
                                      aReadAttachment.standardBusinessDocument (),
                                      aHREDeliverySBDH,
                                      aDocTypeID,
                                      aProcessID,
                                      aState,
                                      aProcessingErrorMessages);
        }
        catch (final Exception ex)
        {
          final String sDetails = "The incoming HR eDelivery message could not be processed.";
          LOGGER.error (sLogPrefix + sDetails, ex);
          aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                             .refToMessageInError (aState.getMessageID ())
                                                             .errorDetail (sDetails, ex)
                                                             .build ());
          return AS4MessageProcessorResult.createFailure ();
        }
      }
    }

    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  @UnsupportedOperation
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4IncomingMessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.error ("Invoking processAS4SignalMessage is not supported");
    throw new UnsupportedOperationException ();
  }

  public void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                         @Nonnull final IAS4IncomingMessageState aState,
                                         @Nonnull @Nonempty final String sResponseMessageID,
                                         @Nullable final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable)
  {
    // Now start invoking SPI handlers
    for (final IPhase4HREDeliveryIncomingSBDHandlerSPI aHandler : m_aHandlers)
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
