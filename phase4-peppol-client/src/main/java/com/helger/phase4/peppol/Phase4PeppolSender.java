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
package com.helger.phase4.peppol;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.bdve.executorset.VESID;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.response.ResponseHandlerHttpEntity;
import com.helger.peppol.sbdh.CPeppolSBDH;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.write.PeppolSBDHDocumentWriter;
import com.helger.peppol.smpclient.ISMPServiceMetadataProvider;
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
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.AS4CryptoFactory;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.messaging.EAS4IncomingMessageMode;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.profile.peppol.PeppolPMode;
import com.helger.phase4.servlet.AS4IncomingHandler;
import com.helger.phase4.servlet.AS4IncomingHandler.IAS4ParsedMessageCallback;
import com.helger.phase4.servlet.AS4IncomingMessageMetadata;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.sbdh.builder.SBDHWriter;
import com.helger.xml.serialize.read.DOMReader;

/**
 * This class contains all the specifics to send AS4 messages to PEPPOL. See
 * <code>sendAS4Message</code> as the main method to trigger the sending, with
 * all potential customization.
 *
 * @author Philip Helger
 */
@Immutable
public final class Phase4PeppolSender
{
  public static final PeppolIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  public static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  public static final String DEFAULT_SBDH_DOCUMENT_IDENTIFICATION_UBL_VERSION_ID = CPeppolSBDH.TYPE_VERSION_21;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolSender.class);

  private Phase4PeppolSender ()
  {}

  @Nullable
  public static Ebms3SignalMessage parseSignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                       @Nonnull final IPModeResolver aPModeResolver,
                                                       @Nonnull final IIncomingAttachmentFactory aIAF,
                                                       @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                       @Nullable final IPMode aSendingPMode,
                                                       @Nonnull final Locale aLocale,
                                                       @Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                       @Nonnull final HttpResponse aHttpResponse,
                                                       @Nonnull final byte [] aResponsePayload,
                                                       @Nullable final IAS4IncomingDumper aIncomingDumper) throws Phase4PeppolException
  {
    // This wrapper will take the result
    final Wrapper <Ebms3SignalMessage> aRetWrapper = new Wrapper <> ();

    // Handler for the parsed message
    final IAS4ParsedMessageCallback aCallback = (aHttpHeaders, aSoapDocument, eSoapVersion, aIncomingAttachments) -> {
      final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

      // Use the sending PMode as fallback, because from the incoming
      // receipt/error it is impossible to detect a PMode
      final SOAPHeaderElementProcessorRegistry aRegistry = SOAPHeaderElementProcessorRegistry.createDefault (aPModeResolver,
                                                                                                             aCryptoFactory,
                                                                                                             aSendingPMode);

      // Parse AS4, verify signature etc
      final IAS4MessageState aState = AS4IncomingHandler.processEbmsMessage (aResHelper,
                                                                             aLocale,
                                                                             aRegistry,
                                                                             aHttpHeaders,
                                                                             aSoapDocument,
                                                                             eSoapVersion,
                                                                             aIncomingAttachments,
                                                                             aErrorMessages);

      // Remember the parsed signal message
      aRetWrapper.set (aState.getEbmsSignalMessage ());
    };

    // Create header map from response headers
    final HttpHeaderMap aHttpHeaders = new HttpHeaderMap ();
    for (final Header aHeader : aHttpResponse.getAllHeaders ())
      aHttpHeaders.addHeader (aHeader.getName (), aHeader.getValue ());

    // Parse incoming message
    try (final NonBlockingByteArrayInputStream aPayloadIS = new NonBlockingByteArrayInputStream (aResponsePayload))
    {
      AS4IncomingHandler.parseAS4Message (aIAF,
                                          aResHelper,
                                          aMessageMetadata,
                                          aPayloadIS,
                                          aHttpHeaders,
                                          aCallback,
                                          aIncomingDumper);
    }
    catch (final Exception ex)
    {
      throw new Phase4PeppolException ("Error parsing signal message", ex);
    }

    // This one contains the result
    return aRetWrapper.get ();
  }

  private static void _sendHttp (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                 @Nonnull final IPModeResolver aPModeResolver,
                                 @Nonnull final IIncomingAttachmentFactory aIAF,
                                 @Nonnull final AS4ClientUserMessage aClientUserMsg,
                                 @Nonnull final Locale aLocale,
                                 @Nonnull final String sURL,
                                 @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                 @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                 @Nullable final IAS4IncomingDumper aIncomingDumper,
                                 @Nullable final IPhase4PeppolResponseConsumer aResponseConsumer,
                                 @Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer) throws Exception
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Sending AS4 message to '" + sURL + "' with max. " + aClientUserMsg.getMaxRetries () + " retries");

    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("  ServiceType = '" + aClientUserMsg.getServiceType () + "'");
      LOGGER.debug ("  Service = '" + aClientUserMsg.getServiceValue () + "'");
      LOGGER.debug ("  Action = '" + aClientUserMsg.getAction () + "'");
      LOGGER.debug ("  ConversationId = '" + aClientUserMsg.getConversationID () + "'");
      LOGGER.debug ("  MessageProperties:");
      for (final Ebms3Property p : aClientUserMsg.ebms3Properties ())
        LOGGER.debug ("    [" + p.getName () + "] = [" + p.getValue () + "]");
      LOGGER.debug ("  Attachments (" + aClientUserMsg.attachments ().size () + "):");
      for (final WSS4JAttachment a : aClientUserMsg.attachments ())
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

    final Wrapper <HttpResponse> aWrappedResponse = new Wrapper <> ();
    final ResponseHandler <byte []> aResponseHdl = aHttpResponse -> {
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;
      aWrappedResponse.set (aHttpResponse);
      return EntityUtils.toByteArray (aEntity);
    };
    final AS4ClientSentMessage <byte []> aResponseEntity = aClientUserMsg.sendMessageWithRetries (sURL,
                                                                                                  aResponseHdl,
                                                                                                  aBuildMessageCallback,
                                                                                                  aOutgoingDumper);
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Successfully transmitted AS4 document with message ID '" +
                   aResponseEntity.getMessageID () +
                   "' to '" +
                   sURL +
                   "'");

    if (aResponseConsumer != null)
      aResponseConsumer.handleResponse (aResponseEntity);

    // Try interpret result as SignalMessage
    if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
    {
      final IAS4IncomingMessageMetadata aMessageMetadata = new AS4IncomingMessageMetadata ().setIncomingDTNow ()
                                                                                            .setMode (EAS4IncomingMessageMode.RESPONSE);

      // Read response as EBMS3 Signal Message
      final Ebms3SignalMessage aSignalMessage = parseSignalMessage (aCryptoFactory,
                                                                    aPModeResolver,
                                                                    aIAF,
                                                                    aClientUserMsg.getAS4ResourceHelper (),
                                                                    aClientUserMsg.getPMode (),
                                                                    aLocale,
                                                                    aMessageMetadata,
                                                                    aWrappedResponse.get (),
                                                                    aResponseEntity.getResponse (),
                                                                    aIncomingDumper);
      if (aSignalMessage != null && aSignalMsgConsumer != null)
        aSignalMsgConsumer.handleSignalMessage (aSignalMessage);
    }
    else
      LOGGER.info ("AS4 ResponseEntity is empty");
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
   * @param aPayloadElement
   *        The payload element to be validated. May not be <code>null</code>.
   * @param aVESID
   *        The VESID to validate against. May be <code>null</code>.
   * @param aValidationResultHandler
   *        The validation result handler to be used. May be <code>null</code>.
   * @throws Phase4PeppolException
   *         If the validation result handler decides to do so....
   */
  private static void _validatePayload (@Nonnull final Element aPayloadElement,
                                        @Nullable final VESID aVESID,
                                        @Nullable final IPhase4PeppolValidatonResultHandler aValidationResultHandler) throws Phase4PeppolException
  {
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
  }

  /**
   * @param aSenderID
   *        Sender participant ID. May not be <code>null</code>.
   * @param aReceiverID
   *        Receiver participant ID. May not be <code>null</code>.
   * @param aDocTypeID
   *        Document type ID. May not be <code>null</code>.
   * @param aProcID
   *        Process ID. May not be <code>null</code>.
   * @param sSBDHInstanceIdentifier
   *        SBDH instance identifier. May be <code>null</code> to create a
   *        random ID.
   * @param sSBDHUBLVersionID
   *        SBDH UBL version ID. May be <code>null</code> to use the default.
   * @param aPayloadElement
   *        Payload element to be wrapped. May not be <code>null</code>.
   * @return The byte array of the XML representation of the created SBDH. Never
   *         <code>null</code>.
   */
  @Nonnull
  private static byte [] _createSBDH (@Nonnull final IParticipantIdentifier aSenderID,
                                      @Nonnull final IParticipantIdentifier aReceiverID,
                                      @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                      @Nonnull final IProcessIdentifier aProcID,
                                      @Nullable final String sSBDHInstanceIdentifier,
                                      @Nullable final String sSBDHUBLVersionID,
                                      @Nonnull final Element aPayloadElement)
  {
    ValueEnforcer.notNull (aPayloadElement, "PayloadElement");
    ValueEnforcer.notNull (aPayloadElement.getNamespaceURI (), "PayloadElement.NamespaceURI");

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
    return aSBDBytes;
  }

  /**
   * Get the receiver certificate from the specified SMP endpoint.
   *
   * @param aReceiverCert
   *        The determined receiver AP certificate to check. Never
   *        <code>null</code>.
   * @param aCertificateConsumer
   *        An optional consumer that is invoked with the received AP
   *        certificate to be used for the transmission. The certification check
   *        result must be considered when used. May be <code>null</code>.
   * @throws Phase4PeppolException
   *         in case of error
   */
  private static void _checkReceiverAPCert (@Nullable final X509Certificate aReceiverCert,
                                            @Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer) throws Phase4PeppolException
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Using the following receiver AP certificate from the SMP: " + aReceiverCert);

    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final EPeppolCertificateCheckResult eCertCheckResult = PeppolCertificateChecker.checkPeppolAPCertificate (aReceiverCert,
                                                                                                              aNow,
                                                                                                              ETriState.UNDEFINED,
                                                                                                              ETriState.UNDEFINED);

    // Interested in the certificate?
    if (aCertificateConsumer != null)
      aCertificateConsumer.onCertificateCheckResult (aReceiverCert, aNow, eCertCheckResult);

    if (eCertCheckResult.isInvalid ())
    {
      throw new Phase4PeppolException ("The configured receiver AP certificate is not valid (at " +
                                       aNow +
                                       ") and cannot be used for sending. Aborting. Reason: " +
                                       eCertCheckResult.getReason ());
    }
  }

  /**
   * Send an AS4 message. It is highly recommend to use the {@link Builder}
   * class, because it is very likely, that this API is NOT stable.
   *
   * @param aHttpClientFactory
   *        The HTTP client factory to be used. May not be <code>null</code>.
   * @param aCryptoFactory
   *        The crypto factory to be used. May not be <code>null</code>.
   * @param aPModeResolver
   *        PMode resolver. May not be <code>null</code>.
   * @param aSrcPMode
   *        The source PMode to be used. May not be <code>null</code>.
   * @param aSenderID
   *        The Peppol sending participant ID to be used. May not be
   *        <code>null</code>.
   * @param aReceiverID
   *        The Peppol receiving participant ID to send to. May not be
   *        <code>null</code>.
   * @param aDocTypeID
   *        The Peppol Document type ID to be used. May not be <code>null</code>
   * @param aProcessID
   *        The Peppol process ID to be used. May not be <code>null</code>.
   * @param sSenderPartyID
   *        The sending party ID (the CN part of the senders certificate
   *        subject). May not be <code>null</code>.
   * @param sMessageID
   *        The AS4 message ID to be used. If none is provided, a random UUID is
   *        used. May be <code>null</code>.
   * @param sConversationID
   *        The AS4 conversation ID to be used. If none is provided, a random
   *        UUID is used. May be <code>null</code>.
   * @param aEndpoint
   *        The determined SMP endpoint. Never <code>null</code>.
   * @param aCertificateConsumer
   *        An optional consumer that is invoked with the received AP
   *        certificate to be used for the transmission. The certification check
   *        result must be considered when used. May be <code>null</code>.
   * @param aPayloadSBDBytes
   *        The Peppol SBDH payload to be send. May not be <code>null</code>.
   * @param aPayloadMimeType
   *        The MIME type of the payload. Usually "application/xml". May not be
   *        <code>null</code>.
   * @param bCompressPayload
   *        <code>true</code> to use AS4 compression on the payload,
   *        <code>false</code> to not compress it.
   * @param aBuildMessageCallback
   *        An internal callback to do something with the build Ebms message.
   *        May be <code>null</code>.
   * @param aOutgoingDumper
   *        An outgoing dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global outgoing dumper is used.
   * @param aIncomingDumper
   *        An incoming dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global incoming dumper is used.
   * @param aResponseConsumer
   *        An optional consumer for the AS4 message that was sent. May be
   *        <code>null</code>.
   * @param aSignalMsgConsumer
   *        An optional consumer that will contain the parsed Ebms3 response
   *        signal message. May be <code>null</code>.
   * @throws Phase4PeppolException
   *         if something goes wrong
   */
  private static void _sendAS4Message (@Nonnull final HttpClientFactory aHttpClientFactory,
                                       @Nonnull final IAS4CryptoFactory aCryptoFactory,
                                       @Nonnull final IPModeResolver aPModeResolver,
                                       @Nonnull final IIncomingAttachmentFactory aIAF,
                                       @Nonnull final IPMode aSrcPMode,
                                       @Nonnull final IParticipantIdentifier aSenderID,
                                       @Nonnull final IParticipantIdentifier aReceiverID,
                                       @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                       @Nonnull final IProcessIdentifier aProcessID,
                                       @Nonnull @Nonempty final String sSenderPartyID,
                                       @Nullable final String sMessageID,
                                       @Nullable final String sConversationID,
                                       @Nonnull final X509Certificate aReceiverCert,
                                       @Nonnull @Nonempty final String sReceiverEndpointURL,
                                       @Nonnull final byte [] aPayloadSBDBytes,
                                       @Nonnull final IMimeType aPayloadMimeType,
                                       final boolean bCompressPayload,
                                       @Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback,
                                       @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                       @Nullable final IAS4IncomingDumper aIncomingDumper,
                                       @Nullable final IPhase4PeppolResponseConsumer aResponseConsumer,
                                       @Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer) throws Phase4PeppolException
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aSrcPMode, "SrcPMode");
    ValueEnforcer.notNull (aSenderID, "SenderID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcessID, "ProcID");
    ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
    ValueEnforcer.notNull (aReceiverCert, "ReceiverCert");
    ValueEnforcer.notEmpty (sReceiverEndpointURL, "ReceiverEndpointURL");
    ValueEnforcer.notNull (aPayloadSBDBytes, "PayloadSBDBytes");
    ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");

    // Temporary file manager
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // Start building AS4 User Message
      final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
      aUserMsg.setHttpClientFactory (aHttpClientFactory);

      // Otherwise Oxalis dies
      aUserMsg.setQuoteHttpHeaders (false);
      aUserMsg.setSOAPVersion (ESOAPVersion.SOAP_12);
      // Set the keystore/truststore parameters
      aUserMsg.setAS4CryptoFactory (aCryptoFactory);
      aUserMsg.setPMode (aSrcPMode, true);

      // Set after PMode
      aUserMsg.cryptParams ().setCertificate (aReceiverCert);

      // Explicit parameters have precedence over PMode
      aUserMsg.setAgreementRefValue (PeppolPMode.DEFAULT_AGREEMENT_ID);
      // The eb3:AgreementRef element also includes an optional attribute pmode
      // which can be used to include the PMode.ID. This attribute MUST NOT be
      // used as Access Points may use just one generic P-Mode for receiving
      // messages.
      aUserMsg.setPModeIDFactory (x -> null);
      aUserMsg.setServiceType (aProcessID.getScheme ());
      aUserMsg.setServiceValue (aProcessID.getValue ());
      aUserMsg.setAction (aDocTypeID.getURIEncoded ());
      if (StringHelper.hasText (sMessageID))
        aUserMsg.setMessageID (sMessageID);
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

      // Add SBDH as attachment only
      aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aPayloadSBDBytes,
                                                                            null,
                                                                            "document.xml",
                                                                            aPayloadMimeType,
                                                                            bCompressPayload ? EAS4CompressionMode.GZIP
                                                                                             : null,
                                                                            aResHelper));

      // Main sending
      _sendHttp (aCryptoFactory,
                 aPModeResolver,
                 aIAF,
                 aUserMsg,
                 Locale.US,
                 sReceiverEndpointURL,
                 aBuildMessageCallback,
                 aOutgoingDumper,
                 aIncomingDumper,
                 aResponseConsumer,
                 aSignalMsgConsumer);
    }
    catch (final Phase4PeppolException ex)
    {
      // Re-throw
      throw ex;
    }
    catch (final Exception ex)
    {
      // wrap
      throw new Phase4PeppolException ("Wrapped Phase4PeppolException", ex);
    }
  }

  /**
   * @return Create a new Builder for AS4 messages if the payload is present and
   *         the SBDH should be created internally. Never <code>null</code>.
   * @since 0.9.4
   */
  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * @return Create a new Builder for AS4 messages if the SBDH payload is
   *         already present. Never <code>null</code>.
   * @since 0.9.6
   */
  @Nonnull
  public static SBDHBuilder sbdhBuilder ()
  {
    return new SBDHBuilder ();
  }

  /**
   * Abstract builder base class with the minimum requirements configuration
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   * @since 0.9.6
   */
  public static abstract class AbstractBaseBuilder <IMPLTYPE extends AbstractBaseBuilder <IMPLTYPE>> implements
                                                   IGenericImplTrait <IMPLTYPE>
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
    protected String m_sSenderPartyID;
    protected String m_sMessageID;
    protected String m_sConversationID;

    protected IMimeType m_aPayloadMimeType;
    protected boolean m_bCompressPayload;

    protected IPhase4PeppolEndpointDetailProvider m_aEndpointDetailProvider;

    protected IAS4ClientBuildMessageCallback m_aBuildMessageCallback;
    protected IAS4OutgoingDumper m_aOutgoingDumper;
    protected IAS4IncomingDumper m_aIncomingDumper;
    protected IPhase4PeppolResponseConsumer m_aResponseConsumer;
    protected IPhase4PeppolSignalMessageConsumer m_aSignalMsgConsumer;

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
        setHttpClientFactory (new Phase4PeppolHttpClientFactory ());
        setCryptoFactory (AS4CryptoFactory.getDefaultInstance ());
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
     * {@link Phase4PeppolHttpClientFactory} is used and there is no need to
     * invoke this method.
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
     * @since 0.9.7
     */
    @Nonnull
    public final IPMode getPMode ()
    {
      return m_aPMode;
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
    public final IMPLTYPE setPMode (@Nonnull final IPMode aPMode)
    {
      ValueEnforcer.notNull (aPMode, "PMode");
      m_aPMode = aPMode;
      return thisAsT ();
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
    public final IMPLTYPE setSenderParticipantID (@Nonnull final IParticipantIdentifier aSenderID)
    {
      ValueEnforcer.notNull (aSenderID, "SenderID");
      m_aSenderID = aSenderID;
      return thisAsT ();
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
     * Set the "sender party ID" which is the CN part of the PEPPOL AP
     * certificate. An example value is e.g. "POP000123" but it MUST match the
     * certificate you are using. This must be provided prior to sending.
     *
     * @param sSenderPartyID
     *        The sender party ID. May neither be <code>null</code> nor empty.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setSenderPartyID (@Nonnull @Nonempty final String sSenderPartyID)
    {
      ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
      m_sSenderPartyID = sSenderPartyID;
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
     * @see #setSMPClient(ISMPServiceMetadataProvider)
     */
    @Nonnull
    public final IMPLTYPE setEndpointDetailProvider (@Nonnull final IPhase4PeppolEndpointDetailProvider aEndpointDetailProvider)
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
     * @see #setEndpointDetailProvider(IPhase4PeppolEndpointDetailProvider)
     */
    @Nonnull
    public final IMPLTYPE setSMPClient (@Nonnull final ISMPServiceMetadataProvider aSMPClient)
    {
      return setEndpointDetailProvider (new Phase4PeppolEndpointDetailProviderSMP (aSMPClient));
    }

    @Nonnull
    public final IMPLTYPE setReceiverEndpointDetails (@Nonnull final X509Certificate aCert,
                                                      @Nonnull @Nonempty final String sDestURL)
    {
      return setEndpointDetailProvider (new Phase4PeppolEndpointDetailProviderConstant (aCert, sDestURL));
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
     * @since 0.9.6
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
     * @since 0.9.6
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
     * @since 0.9.7
     */
    @Nonnull
    public final IMPLTYPE setIncomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
    {
      m_aIncomingDumper = aIncomingDumper;
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
    public final IMPLTYPE setResponseConsumer (@Nullable final IPhase4PeppolResponseConsumer aResponseConsumer)
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
    public final IMPLTYPE setSignalMsgConsumer (@Nullable final IPhase4PeppolSignalMessageConsumer aSignalMsgConsumer)
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
      if (StringHelper.hasNoText (m_sSenderPartyID))
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
     * @throws Phase4PeppolException
     *         In case of any error
     * @see #isEveryRequiredFieldSet()
     */
    @Nonnull
    public abstract ESuccess sendMessage () throws Phase4PeppolException;
  }

  /**
   * The builder class for sending AS4 messages using Peppol specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.<br>
   * This builder class assumes, that only the payload (e.g. the Invoice) is
   * present, and that both validation and SBDH creation happens inside.
   *
   * @author Philip Helger
   * @since 0.9.4
   */
  public static class Builder extends AbstractBaseBuilder <Builder>
  {
    private String m_sSBDHInstanceIdentifier;
    private String m_sSBDHUBLVersion;
    private VESID m_aVESID;
    private IPhase4PeppolValidatonResultHandler m_aValidationResultHandler;
    private Element m_aPayloadElement;
    private byte [] m_aPayloadBytes;
    private IPhase4PeppolCertificateCheckResultHandler m_aCertificateConsumer;
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
     * element. If this method is called, it overwrites any other explicitly set
     * payload.
     *
     * @param aPayloadElement
     *        The payload element to be used. They payload element MUST have a
     *        namespace URI. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder setPayload (@Nonnull final Element aPayloadElement)
    {
      ValueEnforcer.notNull (aPayloadElement, "Payload");
      ValueEnforcer.notNull (aPayloadElement.getNamespaceURI (), "Payload.NamespaceURI");
      m_aPayloadElement = aPayloadElement;
      m_aPayloadBytes = null;
      return this;
    }

    /**
     * Set the payload to be used as a byte array. It will be parsed internally
     * to a DOM element. If this method is called, it overwrites any other
     * explicitly set payload.
     *
     * @param aPayloadBytes
     *        The payload bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     * @since 0.9.6
     */
    @Nonnull
    public Builder setPayload (@Nonnull final byte [] aPayloadBytes)
    {
      ValueEnforcer.notNull (aPayloadBytes, "PayloadBytes");
      m_aPayloadBytes = aPayloadBytes;
      m_aPayloadElement = null;
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
     * Set an optional Consumer for the destination AP address, independent of
     * its usability.
     *
     * @param aAPEndointURLConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     * @since v0.9.8
     */
    @Nonnull
    public Builder setAPEndointURLConsumer (@Nullable final Consumer <String> aAPEndointURLConsumer)
    {
      m_aAPEndointURLConsumer = aAPEndointURLConsumer;
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
                                                                      : new Phase4PeppolValidatonResultHandler ();
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

    @Override
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      // m_sSBDHInstanceIdentifier may be null
      // m_sSBDHUBLVersion may be null
      if (m_aPayloadElement == null && m_aPayloadBytes == null)
        return false;
      // m_aCertificateConsumer may be null
      // m_aVESID may be null
      // m_aValidationResultHandler may be null

      // All valid
      return true;
    }

    @Override
    @Nonnull
    public ESuccess sendMessage () throws Phase4PeppolException
    {
      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // Ensure a DOM element is present
      Element aPayloadElement = null;
      if (m_aPayloadElement != null)
        aPayloadElement = m_aPayloadElement;
      else
        if (m_aPayloadBytes != null)
        {
          // Parse it
          final Document aDoc = DOMReader.readXMLDOM (m_aPayloadBytes);
          if (aDoc == null)
            throw new Phase4PeppolException ("Failed to parse payload bytes to a DOM node");
          aPayloadElement = aDoc.getDocumentElement ();
          if (aPayloadElement == null || aPayloadElement.getNamespaceURI () == null)
            throw new Phase4PeppolException ("The parsed XML document must have a root element that has a namespace URI");
        }
        else
          throw new IllegalStateException ("Unexpected - neither element nor bytes are present");

      // Optional payload validation
      _validatePayload (aPayloadElement, m_aVESID, m_aValidationResultHandler);

      // e.g. SMP lookup
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      _checkReceiverAPCert (aReceiverCert, m_aCertificateConsumer);

      // URL from e.g. SMP lookup
      final String sDestURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sDestURL);

      // Created SBDH
      final byte [] aSBDBytes = _createSBDH (m_aSenderID,
                                             m_aReceiverID,
                                             m_aDocTypeID,
                                             m_aProcessID,
                                             m_sSBDHInstanceIdentifier,
                                             m_sSBDHUBLVersion,
                                             aPayloadElement);

      _sendAS4Message (m_aHttpClientFactory,
                       m_aCryptoFactory,
                       m_aPModeResolver,
                       m_aIAF,
                       m_aPMode,
                       m_aSenderID,
                       m_aReceiverID,
                       m_aDocTypeID,
                       m_aProcessID,
                       m_sSenderPartyID,
                       m_sMessageID,
                       m_sConversationID,
                       aReceiverCert,
                       sDestURL,
                       aSBDBytes,
                       m_aPayloadMimeType,
                       m_bCompressPayload,
                       m_aBuildMessageCallback,
                       m_aOutgoingDumper,
                       m_aIncomingDumper,
                       m_aResponseConsumer,
                       m_aSignalMsgConsumer);
      return ESuccess.SUCCESS;
    }
  }

  /**
   * A builder class for sending AS4 messages using Peppol specifics. Use
   * {@link #sendMessage()} to trigger the main transmission.<br>
   * This builder class assumes, that the SBDH was created outside, therefore no
   * validation can occur.
   *
   * @author Philip Helger
   * @since 0.9.6
   */
  public static class SBDHBuilder extends AbstractBaseBuilder <SBDHBuilder>
  {
    private byte [] m_aPayloadBytes;
    private IPhase4PeppolCertificateCheckResultHandler m_aCertificateConsumer;
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
    public SBDHBuilder ()
    {}

    /**
     * Set the payload to be used as a byte array.
     *
     * @param aSBDHBytes
     *        The SBDH bytes to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public SBDHBuilder setPayload (@Nonnull final byte [] aSBDHBytes)
    {
      ValueEnforcer.notNull (aSBDHBytes, "SBDHBytes");
      m_aPayloadBytes = aSBDHBytes;
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
    public SBDHBuilder setCertificateConsumer (@Nullable final IPhase4PeppolCertificateCheckResultHandler aCertificateConsumer)
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
     * @since v0.9.8
     */
    @Nonnull
    public SBDHBuilder setAPEndointURLConsumer (@Nullable final Consumer <String> aAPEndointURLConsumer)
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
      // m_aCertificateConsumer may be null

      // All valid
      return true;
    }

    @Override
    @Nonnull
    public ESuccess sendMessage () throws Phase4PeppolException
    {
      if (!isEveryRequiredFieldSet ())
      {
        LOGGER.error ("At least one mandatory field is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // e.g. SMP lookup
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      _checkReceiverAPCert (aReceiverCert, m_aCertificateConsumer);

      // URL from e.g. SMP lookup
      final String sDestURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sDestURL);

      _sendAS4Message (m_aHttpClientFactory,
                       m_aCryptoFactory,
                       m_aPModeResolver,
                       m_aIAF,
                       m_aPMode,
                       m_aSenderID,
                       m_aReceiverID,
                       m_aDocTypeID,
                       m_aProcessID,
                       m_sSenderPartyID,
                       m_sMessageID,
                       m_sConversationID,
                       aReceiverCert,
                       sDestURL,
                       m_aPayloadBytes,
                       m_aPayloadMimeType,
                       m_bCompressPayload,
                       m_aBuildMessageCallback,
                       m_aOutgoingDumper,
                       m_aIncomingDumper,
                       m_aResponseConsumer,
                       m_aSignalMsgConsumer);
      return ESuccess.SUCCESS;
    }
  }
}
