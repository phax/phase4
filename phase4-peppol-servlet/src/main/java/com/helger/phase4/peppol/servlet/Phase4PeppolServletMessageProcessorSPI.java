/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReadException;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpclient.ISMPServiceMetadataProvider;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IAS4Attachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.IAS4IncomingRequestMetadata;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.phase4.util.Phase4Exception;
import com.helger.sbdh.builder.SBDHReader;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * This is the SPI implementation to handle incoming AS4 requests from
 * phase4-servlet. Users of this package must implement
 * {@link IPhase4PeppolIncomingSBDHandlerSPI} instead.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4PeppolServletMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  public static final class ReadAttachment
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

  public static final ESMPTransportProfile DEFAULT_TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2;
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolServletMessageProcessorSPI.class);

  private ICommonsList <IPhase4PeppolIncomingSBDHandlerSPI> m_aHandlers;
  private ISMPTransportProfile m_aTransportProfile = DEFAULT_TRANSPORT_PROFILE;

  public Phase4PeppolServletMessageProcessorSPI ()
  {
    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IPhase4PeppolIncomingSBDHandlerSPI.class);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPhase4PeppolIncomingSBDHandlerSPI> getAllHandler ()
  {
    return m_aHandlers.getClone ();
  }

  public void setAllHandler (@Nonnull final Iterable <? extends IPhase4PeppolIncomingSBDHandlerSPI> aHandlers)
  {
    ValueEnforcer.notNull (aHandlers, "Handlers");
    m_aHandlers = new CommonsArrayList <> (aHandlers);
  }

  /**
   * @return the transport profile to be handled. Never <code>null</code>. By
   *         default it is "Peppol AS4 v2".
   */
  @Nonnull
  public ISMPTransportProfile getTransportProfile ()
  {
    return m_aTransportProfile;
  }

  /**
   * Set the transport profile to be used. By default it is Peppol AS4 v2.
   *
   * @param aTransportProfile
   *        The transport profile to be used. May not be <code>null</code>.
   */
  public void setTransportProfile (@Nonnull final ISMPTransportProfile aTransportProfile)
  {
    ValueEnforcer.notNull (aTransportProfile, "TransportProfile");
    m_aTransportProfile = aTransportProfile;
  }

  @Nullable
  private EndpointType _getReceiverEndpoint (@Nonnull final String sLogPrefix,
                                             @Nullable final IParticipantIdentifier aRecipientID,
                                             @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                             @Nullable final IProcessIdentifier aProcessID) throws Phase4PeppolServletException
  {
    // Get configured client
    final ISMPServiceMetadataProvider aSMPClient = Phase4PeppolServletConfiguration.getSMPClient ();
    if (aSMPClient == null)
      throw new Phase4PeppolServletException (sLogPrefix + "No SMP client configured!");

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

      // Query the SMP
      return aSMPClient.getEndpoint (aRecipientID, aDocTypeID, aProcessID, m_aTransportProfile);
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

  private static void _checkIfReceiverEndpointURLMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws Phase4PeppolServletException
  {
    // Get our public endpoint address from the configuration
    final String sOwnAPUrl = Phase4PeppolServletConfiguration.getAS4EndpointURL ();
    if (StringHelper.hasNoText (sOwnAPUrl))
      throw new Phase4PeppolServletException (sLogPrefix + "The endpoint URL of this AP is not configured!");

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

  private static void _checkIfEndpointCertificateMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws Phase4PeppolServletException
  {
    final X509Certificate aOurCert = Phase4PeppolServletConfiguration.getAPCertificate ();
    if (aOurCert == null)
      throw new Phase4PeppolServletException (sLogPrefix + "The certificate of this AP is not configured!");

    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    X509Certificate aRecipientCert = null;
    try
    {
      aRecipientCert = CertificateHelper.convertStringToCertficate (sRecipientCertString);
    }
    catch (final CertificateException t)
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

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingRequestMetadata aRequestMetadata,
                                                          @Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aSrcPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Invoking processAS4UserMessage");

    final String sMessageID = aUserMessage.getMessageInfo ().getMessageId ();
    final String sService = aUserMessage.getCollaborationInfo ().getServiceValue ();
    final String sAction = aUserMessage.getCollaborationInfo ().getAction ();
    final String sConversationID = aUserMessage.getCollaborationInfo ().getConversationId ();

    // Debug log
    if (LOGGER.isDebugEnabled ())
    {
      if (aSrcPMode == null)
        LOGGER.debug ("  No Source PMode present");
      else
        LOGGER.debug ("  Source PMode = " + aSrcPMode.getID ());
      LOGGER.debug ("  Message ID = '" + sMessageID + "'");
      LOGGER.debug ("  Service = '" + sService + "'");
      LOGGER.debug ("  Action = '" + sAction + "'");
      LOGGER.debug ("  ConversationId = '" + sConversationID + "'");

      // Log source properties
      LOGGER.debug ("  MessageProperties:");
      for (final Ebms3Property p : aUserMessage.getMessageProperties ().getProperty ())
        LOGGER.debug ("    [" + p.getName () + "] = [" + p.getValue () + "]");

      if (aPayload == null)
        LOGGER.debug ("  No Payload present");
      else
        LOGGER.debug ("  Payload = " + XMLWriter.getNodeAsString (aPayload));
    }

    // Read all attachments
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
        try (final InputStream aSIS = aIncomingAttachment.getSourceStream ())
        {
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
          final Ebms3Error aEbmsError = EEbmsError.EBMS_DECOMPRESSION_FAILURE.getAsEbms3Error (aState.getLocale (),
                                                                                               aState.getMessageID ());
          aProcessingErrorMessages.add (aEbmsError);
          return AS4MessageProcessorResult.createFailure ("Processing errors occurred");
        }

        // Read data as SBDH
        a.m_aSBDH = SBDHReader.standardBusinessDocument ().read (a.m_aPayloadBytes);
        if (a.m_aSBDH == null)
        {
          final Ebms3Error aEbmsError = EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getAsEbms3Error (aState.getLocale (),
                                                                                                aState.getMessageID ());
          aProcessingErrorMessages.add (aEbmsError);
          return AS4MessageProcessorResult.createFailure ("Failed to interprete payload as SBDH");
        }

        aReadAttachments.add (a);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Attachment " +
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

    if (aReadAttachments.size () != 1)
    {
      // In Peppol there must be exactly one payload
      final Ebms3Error aEbmsError = EEbmsError.EBMS_OTHER.getAsEbms3Error (aState.getLocale (), aState.getMessageID ());
      aProcessingErrorMessages.add (aEbmsError);
      return AS4MessageProcessorResult.createFailure ("In Peppol exactly one payload attachment is expected. This request has " +
                                                      aReadAttachments.size () +
                                                      " attachments");
    }

    if (m_aHandlers.isEmpty ())
      LOGGER.warn ("No handler is present - the message is unhandled and discarded");
    else
    {
      final ReadAttachment aReadAttachment = aReadAttachments.getFirst ();
      final String sLogPrefix = "[" + sMessageID + "] ";

      // Start consistency checks?
      if (Phase4PeppolServletConfiguration.isReceiverCheckEnabled ())
      {
        try
        {
          // Extract Peppol values from SBD
          final PeppolSBDHDocument aDD = new PeppolSBDHDocumentReader ().extractData (aReadAttachment.standardBusinessDocument ());

          // Get the endpoint information required from the recipient
          // Check if an endpoint is registered
          final EndpointType aReceiverEndpoint = _getReceiverEndpoint (sLogPrefix,
                                                                       aDD.getReceiverAsIdentifier (),
                                                                       aDD.getDocumentTypeAsIdentifier (),
                                                                       aDD.getProcessAsIdentifier ());

          if (aReceiverEndpoint == null)
          {
            return AS4MessageProcessorResult.createFailure (sLogPrefix +
                                                            "Failed to resolve endpoint for provided receiver/documentType/process - not handling incoming AS4 document");
          }

          // Check if the message is for us
          _checkIfReceiverEndpointURLMatches (sMessageID, aReceiverEndpoint);

          // Get the recipient certificate from the SMP
          _checkIfEndpointCertificateMatches (sMessageID, aReceiverEndpoint);
        }
        catch (final Phase4Exception | PeppolSBDHDocumentReadException ex)
        {
          return AS4MessageProcessorResult.createFailure (sLogPrefix +
                                                          "The contained StandardBusinessDocument could not be read. Technical details: " +
                                                          ex.getMessage ());
        }
      }
      else
      {
        LOGGER.info (sLogPrefix + "Endpoint checks for incoming AS4 messages are disabled");
      }

      for (final IPhase4PeppolIncomingSBDHandlerSPI aHandler : m_aHandlers)
      {
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sLogPrefix + "Invoking Peppol handler " + aHandler);
          aHandler.handleIncomingSBD (aHttpHeaders.getClone (),
                                      aReadAttachment.payloadBytes (),
                                      aReadAttachment.standardBusinessDocument ());
        }
        catch (final Exception ex)
        {
          LOGGER.error (sLogPrefix + "Error invoking Peppol handler " + aHandler, ex);
        }
      }
    }

    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  @UnsupportedOperation
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final IAS4IncomingRequestMetadata aRequestMetadata,
                                                                  @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4MessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.error ("Invoking processAS4SignalMessage is not supported");
    throw new UnsupportedOperationException ();
  }
}
