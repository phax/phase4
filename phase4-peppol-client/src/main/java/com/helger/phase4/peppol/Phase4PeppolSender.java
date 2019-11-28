/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.bdve.executorset.VESID;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.callback.exception.IExceptionCallback;
import com.helger.commons.callback.exception.LoggingExceptionCallback;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.sbdh.CPeppolSBDH;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.write.PeppolSBDHDocumentWriter;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.peppol.utils.EPeppolCertificateCheckResult;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.profile.peppol.PeppolPMode;
import com.helger.phase4.servlet.AS4MessageState;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.sbdh.builder.SBDHWriter;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * This class contains all the specifics to send AS4 messages to PEPPOL. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with
 * all potential customization.
 *
 * @author Philip Helger
 */
public final class Phase4PeppolSender
{
  public static final PeppolIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  public static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  public static final IPModeResolver PMODE_RESOLVER = DefaultPModeResolver.DEFAULT_PMODE_RESOLVER;
  public static final String DEFAULT_SBDH_DOCUMENT_IDENTIFICATION_UBL_VERSION_ID = CPeppolSBDH.TYPE_VERSION_21;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolSender.class);

  private Phase4PeppolSender ()
  {}

  @Nullable
  public static Ebms3SignalMessage parseSignalMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                       @Nonnull final byte [] aBytes) throws Phase4PeppolException
  {
    // Read response as XML
    final Document aSoapDoc = DOMReader.readXMLDOM (aBytes);
    if (aSoapDoc == null || aSoapDoc.getDocumentElement () == null)
      throw new Phase4PeppolException ("Failed to parse as XML");

    // Check if it is SOAP
    final ESOAPVersion eSOAPVersion = ESOAPVersion.getFromNamespaceURIOrNull (aSoapDoc.getDocumentElement ()
                                                                                      .getNamespaceURI ());
    if (eSOAPVersion == null)
      throw new Phase4PeppolException ("Failed to determine SOAP version");

    // Find SOAP header
    final Node aSOAPHeaderNode = XMLHelper.getFirstChildElementOfName (aSoapDoc.getDocumentElement (),
                                                                       eSOAPVersion.getNamespaceURI (),
                                                                       eSOAPVersion.getHeaderElementName ());
    if (aSOAPHeaderNode == null)
      throw new Phase4PeppolException ("SOAP document is missing a Header element");

    // Iterate all SOAP header elements
    for (final Element aHeaderChild : new ChildElementIterator (aSOAPHeaderNode))
    {
      final QName aQName = XMLHelper.getQName (aHeaderChild);
      if (aQName.equals (SOAPHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING))
      {
        final AS4MessageState aState = new AS4MessageState (eSOAPVersion, aResHelper, Locale.US);
        final ErrorList aErrorList = new ErrorList ();
        new SOAPHeaderElementProcessorExtractEbms3Messaging (PMODE_RESOLVER).processHeaderElement (aSoapDoc,
                                                                                                   aHeaderChild,
                                                                                                   new CommonsArrayList <> (),
                                                                                                   aState,
                                                                                                   aErrorList);
        // Check if a signal message is contained
        final Ebms3SignalMessage aSignalMessage = CollectionHelper.getAtIndex (aState.getMessaging ()
                                                                                     .getSignalMessage (),
                                                                               0);
        return aSignalMessage;
      }
    }
    return null;
  }

  private static void _sendHttp (@Nonnull final AS4ClientUserMessage aClient,
                                 @Nonnull final String sURL,
                                 @Nullable final IAS4ClientBuildMessageCallback aCallback,
                                 @Nullable final IPhase4PeppolResponseConsumer aResponseConsumer,
                                 @Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer) throws Exception
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Sending AS4 message to '" + sURL + "' with max. " + aClient.getMaxRetries () + " retries");

    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("  ServiceType = '" + aClient.getServiceType () + "'");
      LOGGER.debug ("  Service = '" + aClient.getServiceValue () + "'");
      LOGGER.debug ("  Action = '" + aClient.getAction () + "'");
      LOGGER.debug ("  ConversationId = '" + aClient.getConversationID () + "'");
      LOGGER.debug ("  MessageProperties:");
      for (final Ebms3Property p : aClient.ebms3Properties ())
        LOGGER.debug ("    [" + p.getName () + "] = [" + p.getValue () + "]");
      LOGGER.debug ("  Attachments (" + aClient.attachments ().size () + "):");
      for (final WSS4JAttachment a : aClient.attachments ())
      {
        LOGGER.debug ("    [" +
                      a.getId () +
                      "] with [" +
                      a.getMimeType () +
                      "] and [" +
                      a.getCharsetOrDefault (null) +
                      "] and [" +
                      a.getCompressionMode () +
                      "] and [" +
                      a.getContentTransferEncoding () +
                      "]");
      }
    }

    final AS4ClientSentMessage <byte []> aResponseEntity = aClient.sendMessageWithRetries (sURL,
                                                                                           new ResponseHandlerByteArray (),
                                                                                           aCallback);
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Successfully transmitted AS4 document with message ID '" +
                   aResponseEntity.getMessageID () +
                   "' to '" +
                   sURL +
                   "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aResponseEntity);

    if (aSignalMsgConsumer != null)
    {
      // Try interpret result as SignalMessage
      if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
      {
        // Read response as EBMS3 Signal Message
        final Ebms3SignalMessage aSignalMessage = parseSignalMessage (aClient.getAS4ResourceHelper (),
                                                                      aResponseEntity.getResponse ());
        if (aSignalMessage != null)
          aSignalMsgConsumer.handleSignalMessage (aSignalMessage);
      }
      else
        LOGGER.info ("AS4 ResponseEntity is empty");
    }
  }

  @Nonnull
  public static StandardBusinessDocument createSBDH (@Nonnull final IParticipantIdentifier aSenderID,
                                                     @Nonnull final IParticipantIdentifier aReceiverID,
                                                     @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                                     @Nonnull final IProcessIdentifier aProcID,
                                                     @Nullable final String sInstanceIdentifier,
                                                     @Nullable final String sUBLVersion,
                                                     @Nonnull final Element aPayloadElement)
  {
    final PeppolSBDHDocument aData = new PeppolSBDHDocument (IF);
    aData.setSender (aSenderID.getScheme (), aSenderID.getValue ());
    aData.setReceiver (aReceiverID.getScheme (), aReceiverID.getValue ());
    aData.setDocumentType (aDocTypeID.getScheme (), aDocTypeID.getValue ());
    aData.setProcess (aProcID.getScheme (), aProcID.getValue ());
    aData.setDocumentIdentification (aPayloadElement.getNamespaceURI (),
                                     StringHelper.hasText (sUBLVersion) ? sUBLVersion
                                                                        : DEFAULT_SBDH_DOCUMENT_IDENTIFICATION_UBL_VERSION_ID,
                                     aPayloadElement.getLocalName (),
                                     StringHelper.hasText (sInstanceIdentifier) ? sInstanceIdentifier
                                                                                : UUID.randomUUID ().toString (),
                                     PDTFactory.getCurrentLocalDateTime ());
    aData.setBusinessMessage (aPayloadElement);
    final StandardBusinessDocument aSBD = new PeppolSBDHDocumentWriter ().createStandardBusinessDocument (aData);
    return aSBD;
  }

  /**
   * Send an AS4 message. It is highly recommend to use the {@link Builder}
   * class, because it is very likely, that this API is NOT stable.
   *
   * @param aHttpClientFactory
   *        The HTTP client factory to be used. May not be <code>null</code>.
   * @param aCryptoFactory
   *        The crypto factory to be used. May not be <code>null</code>.
   * @param aSrcPMode
   *        The source PMode to be used. May not be <code>null</code>.
   * @param aDocTypeID
   *        The Peppol Document type ID to be used. May not be <code>null</code>
   * @param aProcID
   *        The Peppol process ID to be used. May not be <code>null</code>.
   * @param aSenderID
   *        The Peppol sending participant ID to be used. May not be
   *        <code>null</code>.
   * @param aReceiverID
   *        The Peppol receiving participant ID to send to. May not be
   *        <code>null</code>.
   * @param sSenderPartyID
   *        The sending party ID (the CN part of the senders certificate
   *        subject). May not be <code>null</code>.
   * @param sConversationID
   *        The AS4 conversation to be used. If none is provided, a random UUID
   *        is used. May be <code>null</code>.
   * @param sSBDHInstanceIdentifier
   *        The optional SBDH instance identifier. If none is provided, a random
   *        UUID is used. May be <code>null</code>.
   * @param sSBDHUBLVersionID
   *        The UBL version ID for the SBDH document identification. If none is
   *        provided, the default <code>2.1</code> will be used. May be
   *        <code>null</code>.
   * @param aPayloadElement
   *        The Peppol XML payload to be send. May not be <code>null</code>.
   * @param aPayloadMimeType
   *        The MIME type of the payload. Usually "application/xml". May not be
   *        <code>null</code>.
   * @param bCompressPayload
   *        <code>true</code> to use AS4 compression on the payload,
   *        <code>false</code> to not compress it.
   * @param aSMPClient
   *        The SMP client to be used. Needs to be passed in to handle proxy
   *        settings etc. May not be <code>null</code>.
   * @param aCertificateConsumer
   *        An optional consumer that is invoked with the received SMP
   *        certificate to be used for the transmission. The certification check
   *        result must be considered when used. May be <code>null</code>.
   * @param aVESID
   *        The Validation Execution Set ID to be used for client side
   *        XML/Schematron validation. If this parameter is <code>null</code>,
   *        no validation is performed. This parameter is only effective in
   *        combination with aValidationResultHandler.
   * @param aValidationResultHandler
   *        The result handler to be used for XML/Schematron validation. If this
   *        parameter is <code>null</code>, no validation is performed. This
   *        parameter is only effective in combination with aVESID.
   * @param aResponseConsumer
   *        An optional consumer for the AS4 message that was sent. May be
   *        <code>null</code>.
   * @param aSignalMsgConsumer
   *        An optional consumer that will contain the parsed Ebms3 response
   *        signal message. May be <code>null</code>.
   * @param aExceptionCallback
   *        The generic exception handler for all caught exceptions. May not be
   *        <code>null</code>.
   * @throws Phase4PeppolException
   *         if something goes wrong
   */
  private static void _sendAS4Message (@Nonnull final HttpClientFactory aHttpClientFactory,
                                       @Nonnull final AS4CryptoFactory aCryptoFactory,
                                       @Nonnull final IPMode aSrcPMode,
                                       @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                       @Nonnull final IProcessIdentifier aProcID,
                                       @Nonnull final IParticipantIdentifier aSenderID,
                                       @Nonnull final IParticipantIdentifier aReceiverID,
                                       @Nonnull @Nonempty final String sSenderPartyID,
                                       @Nullable final String sConversationID,
                                       @Nullable final String sSBDHInstanceIdentifier,
                                       @Nullable final String sSBDHUBLVersionID,
                                       @Nonnull final Element aPayloadElement,
                                       @Nonnull final IMimeType aPayloadMimeType,
                                       final boolean bCompressPayload,
                                       @Nonnull final SMPClientReadOnly aSMPClient,
                                       @Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer,
                                       @Nullable final VESID aVESID,
                                       @Nullable final IPhase4PeppolValidatonResultHandler aValidationResultHandler,
                                       @Nullable final IPhase4PeppolResponseConsumer aResponseConsumer,
                                       @Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer) throws Phase4PeppolException
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aSrcPMode, "SrcPMode");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcID, "ProcID");
    ValueEnforcer.notNull (aSenderID, "SenderID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");
    ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
    ValueEnforcer.notNull (aPayloadElement, "PayloadElement");
    ValueEnforcer.notNull (aPayloadElement.getNamespaceURI (), "PayloadElement.NamespaceURI");
    ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
    ValueEnforcer.notNull (aSMPClient, "SMPClient");

    // Client side validation
    if (aVESID != null)
    {
      if (aValidationResultHandler != null)
        Phase4PeppolValidation.validateOutgoingBusinessDocument (aPayloadElement, aVESID, aValidationResultHandler);
      else
        LOGGER.warn ("A VES ID is present but no ValidationResultHandler - therefore no validation is performed");
    }
    else
      if (aValidationResultHandler != null)
        LOGGER.warn ("A ValidationResultHandler is present but no VESID - therefore no validation is performed");

    // Temporary file manager
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

      // Perform SMP lookup
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Start performing SMP lookup (" +
                      aReceiverID.getURIEncoded () +
                      ", " +
                      aDocTypeID.getURIEncoded () +
                      ", " +
                      aProcID.getURIEncoded () +
                      ")");

      // Perform SMP lookup
      final EndpointType aEndpoint;
      try
      {
        aEndpoint = aSMPClient.getEndpoint (aReceiverID,
                                            aDocTypeID,
                                            aProcID,
                                            ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2);
        if (aEndpoint == null)
          throw new Phase4PeppolException ("Failed to resolve SMP endpoint");
      }
      catch (final SMPClientException ex)
      {
        throw new Phase4PeppolException ("Failed to resolve SMP endpoint", ex);
      }

      // Certificate from SMP lookup
      final X509Certificate aReceiverCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Received the following AP certificate from the SMP: " + aReceiverCert);

        final EPeppolCertificateCheckResult eCertCheckResult = PeppolCertificateChecker.checkPeppolAPCertificate (aReceiverCert,
                                                                                                                  aNow,
                                                                                                                  ETriState.UNDEFINED,
                                                                                                                  ETriState.UNDEFINED);

        // Interested in the certificate?
        if (aCertificateConsumer != null)
          aCertificateConsumer.onCertificateCheckResult (aReceiverCert, aNow, eCertCheckResult);

        if (eCertCheckResult.isInvalid ())
        {
          throw new Phase4PeppolException ("The received AP certificate from the SMP is not valid (at " +
                                           aNow +
                                           ") and cannot be used for sending. Aborting. Reason: " +
                                           eCertCheckResult.getReason ());
        }
      }

      // URL from SMP lookup
      final String sDestURL = SMPClientReadOnly.getEndpointAddress (aEndpoint);
      if (StringHelper.hasNoText (sDestURL))
        throw new Phase4PeppolException ("Failed to determine the destination URL from the SMP endpoint: " + aEndpoint);

      // Start building AS4 User Message
      final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
      aUserMsg.setHttpClientFactory (aHttpClientFactory);

      // Otherwise Oxalis dies
      aUserMsg.setQuoteHttpHeaders (false);
      aUserMsg.setSOAPVersion (ESOAPVersion.SOAP_12);
      // Set the keystore/truststore parameters
      aUserMsg.setAS4CryptoFactory (aCryptoFactory);
      aUserMsg.setPMode (aSrcPMode, true);

      aUserMsg.cryptParams ().setCertificate (aReceiverCert);

      // Explicit parameters have precedence over PMode
      aUserMsg.setAgreementRefValue (PeppolPMode.DEFAULT_AGREEMENT_ID);
      // The eb3:AgreementRef element also includes an optional attribute pmode
      // which can be used to include the PMode.ID. This attribute MUST NOT be
      // used as Access Points may use just one generic P-Mode for receiving
      // messages.
      aUserMsg.setPModeIDFactory (x -> null);
      aUserMsg.setServiceType (aProcID.getScheme ());
      aUserMsg.setServiceValue (aProcID.getValue ());
      aUserMsg.setAction (aDocTypeID.getURIEncoded ());
      aUserMsg.setConversationID (StringHelper.hasText (sConversationID) ? sConversationID
                                                                         : UUID.randomUUID ().toString ());

      // Backend or gateway?
      aUserMsg.setFromPartyIDType (PeppolPMode.DEFAULT_PARTY_TYPE_ID);
      aUserMsg.setFromPartyID (sSenderPartyID);
      aUserMsg.setToPartyIDType (PeppolPMode.DEFAULT_PARTY_TYPE_ID);
      aUserMsg.setToPartyID (PeppolCertificateHelper.getSubjectCN (aReceiverCert));

      aUserMsg.ebms3Properties ()
              .add (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER, aSenderID.getURIEncoded ()));
      aUserMsg.ebms3Properties ()
              .add (MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT, aReceiverID.getURIEncoded ()));

      // No payload - only one attachment
      aUserMsg.setPayload (null);

      // Create SBDH and add as attachment
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Start creating SBDH for AS4 message");

        final StandardBusinessDocument aSBD = createSBDH (aSenderID,
                                                          aReceiverID,
                                                          aDocTypeID,
                                                          aProcID,
                                                          sSBDHInstanceIdentifier,
                                                          sSBDHUBLVersionID,
                                                          aPayloadElement);
        final byte [] aSBDBytes = SBDHWriter.standardBusinessDocument ().getAsBytes (aSBD);
        aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aSBDBytes,
                                                                              null,
                                                                              "document.xml",
                                                                              aPayloadMimeType,
                                                                              bCompressPayload ? EAS4CompressionMode.GZIP
                                                                                               : null,
                                                                              aResHelper));
      }

      // Main sending
      _sendHttp (aUserMsg, sDestURL, null, aResponseConsumer, aSignalMsgConsumer);
    }
    catch (final Phase4PeppolException ex)
    {
      // Re-throw
      throw ex;
    }
    catch (final Exception ex)
    {
      // wrap
      throw new Phase4PeppolException ("Wrapped Phase4PeppolExceptioN", ex);
    }
  }

  /**
   * @return Create a new Builder for AS4 messages. Never <code>null</code>.
   * @since 0.9.4
   */
  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * The builder class for sending AS4 messages using Peppol specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Philip Helger
   * @since 0.9.4
   */
  public static class Builder
  {
    private HttpClientFactory m_aHttpClientFactory;
    private AS4CryptoFactory m_aCryptoFactory;
    private IPMode m_aPMode;
    private IDocumentTypeIdentifier m_aDocTypeID;
    private IProcessIdentifier m_aProcessID;
    private IParticipantIdentifier m_aSenderID;
    private IParticipantIdentifier m_aReceiverID;
    private String m_sSenderPartyID;
    private String m_sConversationID;
    private String m_sSBDHInstanceIdentifier;
    private String m_sSBDHUBLVersion;
    private Element m_aPayloadElement;
    private IMimeType m_aPayloadMimeType;
    private boolean m_bCompressPayload;
    private SMPClientReadOnly m_aSMPClient;
    private IPhase4PeppolCertificateCheckResultHandler m_aCertificateConsumer;
    private VESID m_aVESID;
    private IPhase4PeppolValidatonResultHandler m_aValidationResultHandler;
    private IPhase4PeppolResponseConsumer m_aResponseConsumer;
    private IPhase4PeppolSignalMessageConsumer m_aSignalMsgConsumer;

    /**
     * Create a new builder, with the following fields already set:<br>
     * {@link #setHttpClientFactory(HttpClientFactory)}<br>
     * {@link #setPMode(IPMode)}<br>
     * {@link #setPayloadMimeType(IMimeType)}<br>
     * {@link #setCompressPayload(boolean)}<br>
     * {@link #setExceptionCallback(IExceptionCallback)}<br>
     */
    public Builder ()
    {
      // Set default values
      try
      {
        setHttpClientFactory (new Phase4PeppolHttpClientFactory ());
        setPMode (Phase4PeppolSender.PMODE_RESOLVER.getPModeOfID (null, "s", "a", "i", "r", null));
        setPayloadMimeType (CMimeType.APPLICATION_XML);
        setCompressPayload (true);
        setExceptionCallback (new LoggingExceptionCallback ());
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the HTTP client factory to be used. By default an instance of
     * {@link Phase4PeppolHttpClientFactory} is used and there is no need to
     * invoke this method.
     *
     * @param aHttpClientFactory
     *        The new HTTP client factory to be used. May not be
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setHttpClientFactory (@Nonnull final HttpClientFactory aHttpClientFactory)
    {
      ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
      m_aHttpClientFactory = aHttpClientFactory;
      return this;
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
    public Builder setCryptoFactory (@Nonnull final AS4CryptoFactory aCryptoFactory)
    {
      ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
      m_aCryptoFactory = aCryptoFactory;
      return this;
    }

    /**
     * Set the PMode to be used. By default a generic PMode for Peppol purposes
     * is used so there is no need to invoke this method.
     *
     * @param aPMode
     *        The PMode to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setPMode (@Nonnull final IPMode aPMode)
    {
      ValueEnforcer.notNull (aPMode, "PMode");
      m_aPMode = aPMode;
      return this;
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
    public Builder setDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
    {
      ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
      m_aDocTypeID = aDocTypeID;
      return this;
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
    public Builder setProcessID (@Nonnull final IProcessIdentifier aProcessID)
    {
      ValueEnforcer.notNull (aProcessID, "ProcessID");
      m_aProcessID = aProcessID;
      return this;
    }

    /**
     * Set the sender participant ID of the message. The participant ID must be
     * provided prior to sending.
     *
     * @param aSenderID
     *        The sender participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setSenderParticipantID (@Nonnull final IParticipantIdentifier aSenderID)
    {
      ValueEnforcer.notNull (aSenderID, "SenderID");
      m_aSenderID = aSenderID;
      return this;
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must
     * be provided prior to sending.
     *
     * @param aReceiverID
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setReceiverParticipantID (@Nonnull final IParticipantIdentifier aReceiverID)
    {
      ValueEnforcer.notNull (aReceiverID, "ReceiverID");
      m_aReceiverID = aReceiverID;
      return this;
    }

    /**
     * Set the "sender party ID" which is the CN part of the PEPPOL AP
     * certificate. An example value is e.g. "POP000123" but it MUST match the
     * certificate you are using. This must be provided prior to sending.
     *
     * @param sSenderPartyID
     *        The sender party ID. May neither be <code>null</code> nor empty.
     * @return this for chaining
     */
    @Nonnull
    public Builder setSenderPartyID (@Nonnull @Nonempty final String sSenderPartyID)
    {
      ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
      m_sSenderPartyID = sSenderPartyID;
      return this;
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
    public Builder setConversationID (@Nullable final String sConversationID)
    {
      m_sConversationID = sConversationID;
      return this;
    }

    /**
     * Set the SBDH instance identifier. If none is provided, a random ID is
     * used. Usually this must NOT be set.
     *
     * @param sSBDHInstanceIdentifier
     *        The SBDH instance identifier to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setSBDHInstanceIdentifier (@Nullable final String sSBDHInstanceIdentifier)
    {
      m_sSBDHInstanceIdentifier = sSBDHInstanceIdentifier;
      return this;
    }

    /**
     * Set the SBDH document identification UBL version. If none is provided,
     * the constant "2.1" is used.
     *
     * @param sSBDHUBLVersion
     *        The SBDH document identification UBL version to be used. May be
     *        <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setSBDHUBLVersion (@Nullable final String sSBDHUBLVersion)
    {
      m_sSBDHUBLVersion = sSBDHUBLVersion;
      return this;
    }

    /**
     * Set the payload element to be used, if it is available as a parsed DOM
     * element.
     *
     * @param aPayload
     *        The payload element to be used. They payload element MUST have a
     *        namespace URI. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setPayload (@Nonnull final Element aPayload)
    {
      ValueEnforcer.notNull (aPayload, "Payload");
      ValueEnforcer.notNull (aPayload.getNamespaceURI (), "Payload.NamespaceURI");
      m_aPayloadElement = aPayload;
      return this;
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
    public Builder setPayloadMimeType (@Nonnull final IMimeType aPayloadMimeType)
    {
      ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
      m_aPayloadMimeType = aPayloadMimeType;
      return this;
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
    public Builder setCompressPayload (final boolean bCompressPayload)
    {
      m_bCompressPayload = bCompressPayload;
      return this;
    }

    /**
     * Set the SMP client to be used. This is the point where e.g. the
     * differentiation between SMK and SML can be done. This must be set prior
     * to sending.
     *
     * @param aSMPClient
     *        The SMP client to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setSMPClient (@Nonnull final SMPClientReadOnly aSMPClient)
    {
      ValueEnforcer.notNull (aSMPClient, "SMPClient");
      m_aSMPClient = aSMPClient;
      return this;
    }

    /**
     * Set an optional Consumer for the retrieved certificate, independent of
     * its usability.
     *
     * @param aCertificateConsumer
     *        The consumer to be used. The first parameter is the certificate
     *        itself and the second parameter is the internal check result. May
     *        be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setCertificateConsumer (@Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer)
    {
      m_aCertificateConsumer = aCertificateConsumer;
      return this;
    }

    /**
     * Set the client side validation to be used. If this method is not invoked,
     * than it's the responsibility of the caller to validate the document prior
     * to sending it. This method uses a default "do nothing validation result
     * handler".
     *
     * @param aVESID
     *        The Validation Execution Set ID as in
     *        <code>PeppolValidation390.VID_OPENPEPPOL_INVOICE_V3</code>. May be
     *        <code>null</code>.
     * @return this for chaining
     * @see #setValidationConfiguration(VESID,
     *      IPhase4PeppolValidatonResultHandler)
     */
    @Nonnull
    public Builder setValidationConfiguration (@Nullable final VESID aVESID)
    {
      final IPhase4PeppolValidatonResultHandler aHdl = aVESID == null ? null
                                                                      : new IPhase4PeppolValidatonResultHandler ()
                                                                      {};
      return setValidationConfiguration (aVESID, aHdl);
    }

    /**
     * Set the client side validation to be used. If this method is not invoked,
     * than it's the responsibility of the caller to validate the document prior
     * to sending it. If the validation should happen internally, both the VESID
     * AND the result handler must be set.
     *
     * @param aVESID
     *        The Validation Execution Set ID as in
     *        <code>PeppolValidation390.VID_OPENPEPPOL_INVOICE_V3</code>. May be
     *        <code>null</code>.
     * @param aValidationResultHandler
     *        The validation result handler for positive and negative response
     *        handling. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setValidationConfiguration (@Nullable final VESID aVESID,
                                               @Nullable final IPhase4PeppolValidatonResultHandler aValidationResultHandler)
    {
      m_aVESID = aVESID;
      m_aValidationResultHandler = aValidationResultHandler;
      return this;
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
    public Builder setResponseConsumer (@Nullable final IPhase4PeppolResponseConsumer aResponseConsumer)
    {
      m_aResponseConsumer = aResponseConsumer;
      return this;
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
    public Builder setSignalMsgConsumer (@Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer)
    {
      m_aSignalMsgConsumer = aSignalMsgConsumer;
      return this;
    }

    /**
     * Set the exception callback to be used in case something goes wrong. By
     * default a logging exception handler is installed, so this method must not
     * be called explicitly.
     *
     * @param aExceptionCallback
     *        The exception callback to be used. May not be <code>null</code>.
     * @return this for chaining
     * @deprecated in 0.9.5 - has no effect anymore
     */
    @Deprecated
    @Nonnull
    public Builder setExceptionCallback (@Nonnull final IExceptionCallback <? super Exception> aExceptionCallback)
    {
      return this;
    }

    public boolean isEveryRequiredFieldSet ()
    {
      if (m_aHttpClientFactory == null)
        return false;
      if (m_aPMode == null)
        return false;
      if (m_aDocTypeID == null)
        return false;
      if (m_aProcessID == null)
        return false;
      if (m_aSenderID == null)
        return false;
      if (m_aReceiverID == null)
        return false;
      if (StringHelper.hasNoText (m_sSenderPartyID))
        return false;
      // m_sConversationID is optional
      // m_sSBDHInstanceIdentifier is optional
      if (m_aPayloadElement == null)
        return false;
      if (m_aPayloadMimeType == null)
        return false;
      // m_bCompressPayload cannot be null
      if (m_aSMPClient == null)
        return false;
      // m_aOnInvalidCertificateConsumer may be null
      // m_aVESID may be null
      // m_aValidationResultHandler may be null
      // m_aResponseConsumer may be null
      // m_aSignalMsgConsumer may be null

      // All valid
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
     * @throws Phase4PeppolException
     *         In case of any error
     * @see #isEveryRequiredFieldSet()
     */
    @Nonnull
    public ESuccess sendMessage () throws Phase4PeppolException
    {
      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }
      _sendAS4Message (m_aHttpClientFactory,
                       m_aCryptoFactory,
                       m_aPMode,
                       m_aDocTypeID,
                       m_aProcessID,
                       m_aSenderID,
                       m_aReceiverID,
                       m_sSenderPartyID,
                       m_sConversationID,
                       m_sSBDHInstanceIdentifier,
                       m_sSBDHUBLVersion,
                       m_aPayloadElement,
                       m_aPayloadMimeType,
                       m_bCompressPayload,
                       m_aSMPClient,
                       m_aCertificateConsumer,
                       m_aVESID,
                       m_aValidationResultHandler,
                       m_aResponseConsumer,
                       m_aSignalMsgConsumer);
      return ESuccess.SUCCESS;
    }
  }
}
