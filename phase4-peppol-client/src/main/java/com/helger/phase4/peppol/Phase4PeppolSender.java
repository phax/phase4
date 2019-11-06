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

import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.callback.exception.IExceptionCallback;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.write.PeppolSBDHDocumentWriter;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
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

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolSender.class);

  /** Sorted list with all issuers we're accepting. Never empty. */
  private static final ICommonsList <X509Certificate> PEPPOL_AP_CA_CERTS = new CommonsArrayList <> ();
  private static final ICommonsList <X500Principal> PEPPOL_AP_CA_ISSUERS = new CommonsArrayList <> ();

  static
  {
    // PKI v3
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2018.CERTIFICATE_PILOT_AP);
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2018.CERTIFICATE_PRODUCTION_AP);
    // PKI v2 after v3 because lower precedence
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2010.CERTIFICATE_PILOT_AP);
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2010.CERTIFICATE_PRODUCTION_AP);

    // all issuers
    PEPPOL_AP_CA_ISSUERS.addAllMapped (PEPPOL_AP_CA_CERTS, X509Certificate::getSubjectX500Principal);
  }

  private Phase4PeppolSender ()
  {}

  /**
   * Check if the provided certificate is a valid Peppol AP certificate.
   *
   * @param aCert
   *        The certificate to be checked. May be <code>null</code>.
   * @param aCheckDT
   *        The check date and time to use. May not be <code>null</code>.
   * @return {@link EPeppolCertificateCheckResult} and never <code>null</code>.
   */
  @Nonnull
  public static EPeppolCertificateCheckResult isValidPeppolAPCertificate (@Nullable final X509Certificate aCert,
                                                                          @Nonnull final LocalDateTime aCheckDT)
  {
    if (aCert == null)
      return EPeppolCertificateCheckResult.NO_CERTIFICATE_PROVIDED;

    // Check date valid
    final Date aCheckDate = PDTFactory.createDate (aCheckDT);
    try
    {
      aCert.checkValidity (aCheckDate);
    }
    catch (final CertificateNotYetValidException ex)
    {
      return EPeppolCertificateCheckResult.NOT_YET_VALID;
    }
    catch (final CertificateExpiredException ex)
    {
      return EPeppolCertificateCheckResult.EXPIRED;
    }

    // Check if issuer is known
    final X500Principal aIssuer = aCert.getIssuerX500Principal ();
    if (!PEPPOL_AP_CA_ISSUERS.contains (aIssuer))
    {
      // Not a PEPPOL AP certificate
      return EPeppolCertificateCheckResult.UNSUPPORTED_ISSUER;
    }

    // check OCSP and CLR
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      // Certificate -> trust anchors; name constraints MUST be null
      final X509CertSelector aSelector = new X509CertSelector ();
      aSelector.setCertificate (aCert);
      final PKIXBuilderParameters aPKIXParams = new PKIXBuilderParameters (new CommonsHashSet <> (PEPPOL_AP_CA_CERTS,
                                                                                                  x -> new TrustAnchor (x,
                                                                                                                        null)),
                                                                           aSelector);

      aPKIXParams.setRevocationEnabled (true);

      // Enable On-Line Certificate Status Protocol (OCSP) support
      final boolean bEnableOCSP = true;
      if (bEnableOCSP)
      {
        Security.setProperty ("ocsp.enable", "true");
      }

      // Check at what date?
      aPKIXParams.setDate (aCheckDate);

      // Specify a list of intermediate certificates ("Collection" is a key in
      // the "SUN" security provider)
      final CertStore aIntermediateCertStore = CertStore.getInstance ("Collection",
                                                                      new CollectionCertStoreParameters (PEPPOL_AP_CA_CERTS));
      aPKIXParams.addCertStore (aIntermediateCertStore);

      // Throws an exception in case of an error
      final CertPathBuilder aCPB = CertPathBuilder.getInstance ("PKIX");
      final PKIXCertPathBuilderResult aBuilderResult = (PKIXCertPathBuilderResult) aCPB.build (aPKIXParams);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("OCSP/CLR builder result = " + aBuilderResult);

      final CertPathValidator aCPV = CertPathValidator.getInstance ("PKIX");
      final PKIXCertPathValidatorResult aValidateResult = (PKIXCertPathValidatorResult) aCPV.validate (aBuilderResult.getCertPath (),
                                                                                                       aPKIXParams);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("OCSP/CLR validation result = " + aValidateResult);
    }
    catch (final GeneralSecurityException ex)
    {
      LOGGER.warn ("Failed to perform OCSP/CLR check", ex);
      return EPeppolCertificateCheckResult.REVOKED;
    }
    finally
    {
      final long nMillis = aSW.stopAndGetMillis ();
      if (nMillis > 100)
        LOGGER.warn ("OCSP/CLR check took " + nMillis + " milliseconds which is too long");
    }
    return EPeppolCertificateCheckResult.VALID;
  }

  @Nullable
  public static Ebms3SignalMessage parseSignalMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                       @Nonnull final byte [] aBytes)
  {
    // Read response as XML
    final Document aSoapDoc = DOMReader.readXMLDOM (aBytes);
    if (aSoapDoc == null || aSoapDoc.getDocumentElement () == null)
      throw new IllegalStateException ("Failed to parse as XML");

    // Check if it is SOAP
    final ESOAPVersion eSOAPVersion = ESOAPVersion.getFromNamespaceURIOrNull (aSoapDoc.getDocumentElement ()
                                                                                      .getNamespaceURI ());
    if (eSOAPVersion == null)
      throw new IllegalStateException ("Failed to determine SOAP version");

    // Find SOAP header
    final Node aSOAPHeaderNode = XMLHelper.getFirstChildElementOfName (aSoapDoc.getDocumentElement (),
                                                                       eSOAPVersion.getNamespaceURI (),
                                                                       eSOAPVersion.getHeaderElementName ());
    if (aSOAPHeaderNode == null)
      throw new IllegalStateException ("SOAP document is missing a Header element");

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

  @Nonnull
  private static ESuccess _sendHttp (@Nonnull final AS4ClientUserMessage aClient,
                                     @Nonnull final String sURL,
                                     @Nullable final IAS4ClientBuildMessageCallback aCallback,
                                     @Nullable final Consumer <AS4ClientSentMessage <byte []>> aResponseConsumer,
                                     @Nullable final Consumer <Ebms3SignalMessage> aSignalMsgConsumer)
  {
    try
    {
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Sending AS4 to '" + sURL + "' with max. " + aClient.getMaxRetries () + " retries");

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
        LOGGER.info ("Successfully transmitted document with message ID '" +
                     aResponseEntity.getMessageID () +
                     "' to '" +
                     sURL +
                     "'");

      if (aResponseConsumer != null)
        aResponseConsumer.accept (aResponseEntity);

      if (aSignalMsgConsumer != null)
      {
        // Try interpret result as SignalMessage
        if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
        {
          // Read response as EBMS3 Signal Message
          final Ebms3SignalMessage aSignalMessage = parseSignalMessage (aClient.getAS4ResourceHelper (),
                                                                        aResponseEntity.getResponse ());
          if (aSignalMessage != null)
            aSignalMsgConsumer.accept (aSignalMessage);
        }
        else
          LOGGER.info ("ResponseEntity is empty");
      }

      return ESuccess.SUCCESS;
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error sending message to '" + sURL + "'", ex);
      return ESuccess.FAILURE;
    }
  }

  @Nonnull
  public static StandardBusinessDocument createSBDH (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                                     @Nonnull final IProcessIdentifier aProcID,
                                                     @Nonnull final IParticipantIdentifier aSenderID,
                                                     @Nonnull final IParticipantIdentifier aReceiverID,
                                                     @Nullable final String sInstanceIdentifier,
                                                     @Nonnull final Element aBusinessMsg)
  {
    final PeppolSBDHDocument aData = new PeppolSBDHDocument (IF);
    aData.setSender (aSenderID.getScheme (), aSenderID.getValue ());
    aData.setReceiver (aReceiverID.getScheme (), aReceiverID.getValue ());
    aData.setDocumentType (aDocTypeID.getScheme (), aDocTypeID.getValue ());
    aData.setProcess (aProcID.getScheme (), aProcID.getValue ());
    aData.setDocumentIdentification (aBusinessMsg.getNamespaceURI (),
                                     "2.1",
                                     aBusinessMsg.getLocalName (),
                                     StringHelper.hasText (sInstanceIdentifier) ? sInstanceIdentifier
                                                                                : UUID.randomUUID ().toString (),
                                     PDTFactory.getCurrentLocalDateTime ());
    aData.setBusinessMessage (aBusinessMsg);
    final StandardBusinessDocument aSBD = new PeppolSBDHDocumentWriter ().createStandardBusinessDocument (aData);
    return aSBD;
  }

  /**
   * Send an AS4 message
   *
   * @param aHttpClientFactory
   *        The HTTP client factory to be used. May not be <code>null</code>.
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
   * @param aOnInvalidCertificateConsumer
   *        An optional consumer that is only invoked, if the received SMP
   *        certificate cannot be used for the transmission. May be
   *        <code>null</code>.
   * @param aResponseConsumer
   *        An optional consumer for the AS4 message that was sent. May be
   *        <code>null</code>.
   * @param aSignalMsgConsumer
   *        An optional consumer that will contain the parsed Ebms3 response
   *        signal message. May be <code>null</code>.
   * @param aExceptionCallback
   *        The generic exception handler for all caught exceptions. May not be
   *        <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if everything went well,
   *         {@link ESuccess#FAILURE} in an exception was thrown, or sending
   *         failed or the SMP certificate is invalid.
   */
  @Nonnull
  public static ESuccess sendAS4Message (@Nonnull final HttpClientFactory aHttpClientFactory,
                                         @Nonnull final IPMode aSrcPMode,
                                         @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                         @Nonnull final IProcessIdentifier aProcID,
                                         @Nonnull final IParticipantIdentifier aSenderID,
                                         @Nonnull final IParticipantIdentifier aReceiverID,
                                         @Nonnull @Nonempty final String sSenderPartyID,
                                         @Nullable final String sConversationID,
                                         @Nullable final String sSBDHInstanceIdentifier,
                                         @Nonnull final Element aPayloadElement,
                                         @Nonnull final IMimeType aPayloadMimeType,
                                         final boolean bCompressPayload,
                                         @Nonnull final SMPClientReadOnly aSMPClient,
                                         @Nullable final BiConsumer <X509Certificate, EPeppolCertificateCheckResult> aOnInvalidCertificateConsumer,
                                         @Nullable final Consumer <AS4ClientSentMessage <byte []>> aResponseConsumer,
                                         @Nullable final Consumer <Ebms3SignalMessage> aSignalMsgConsumer,
                                         @Nonnull final IExceptionCallback <? super Exception> aExceptionCallback)
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    ValueEnforcer.notNull (aSrcPMode, "SrcPMode");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcID, "ProcID");
    ValueEnforcer.notNull (aSenderID, "SenderID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");
    ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
    ValueEnforcer.notNull (aPayloadElement, "PayloadElement");
    ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
    ValueEnforcer.notNull (aSMPClient, "SMPClient");
    ValueEnforcer.notNull (aExceptionCallback, "ExceptionCallback");

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // Perform SMP lookup
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Start performing SMP lookup (" +
                      aReceiverID.getURIEncoded () +
                      ", " +
                      aDocTypeID.getURIEncoded () +
                      ", " +
                      aProcID.getURIEncoded () +
                      ")");

      final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

      final EndpointType aEndpoint = aSMPClient.getEndpoint (aReceiverID,
                                                             aDocTypeID,
                                                             aProcID,
                                                             ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2);
      if (aEndpoint == null)
        throw new IllegalStateException ("Failed to resolve SMP endpoint");

      // Start building AS4 User Message
      final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
      aUserMsg.setHttpClientFactory (aHttpClientFactory);

      // Otherwise Oxalis dies
      aUserMsg.setQuoteHttpHeaders (false);
      aUserMsg.setSOAPVersion (ESOAPVersion.SOAP_12);
      aUserMsg.setAS4CryptoFactory (AS4CryptoFactory.DEFAULT_INSTANCE);
      aUserMsg.setPMode (aSrcPMode, true);

      // Certificate from SMP lookup
      final X509Certificate aReceiverCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Received the following AP certificate from the SMP: " + aReceiverCert);

      final EPeppolCertificateCheckResult eCertCheckResult = isValidPeppolAPCertificate (aReceiverCert, aNow);
      if (eCertCheckResult.isInvalid ())
      {
        LOGGER.error ("The received SMP certificate is not valid (at " +
                      aNow +
                      ") and cannot be used for sending. Aborting. Reason: " +
                      eCertCheckResult.getReason ());
        if (aOnInvalidCertificateConsumer != null)
          aOnInvalidCertificateConsumer.accept (aReceiverCert, eCertCheckResult);
        return ESuccess.FAILURE;
      }
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
          LOGGER.debug ("Start creating SBDH");

        final StandardBusinessDocument aSBD = createSBDH (aDocTypeID,
                                                          aProcID,
                                                          aSenderID,
                                                          aReceiverID,
                                                          sSBDHInstanceIdentifier,
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

      // URL from SMP lookup
      final String sDestURL = SMPClientReadOnly.getEndpointAddress (aEndpoint);
      if (sDestURL == null)
      {
        LOGGER.error ("Failed to determine the destination URL from the SMP endpoint: " + aEndpoint);
        return ESuccess.FAILURE;
      }

      // Main sending
      return _sendHttp (aUserMsg, sDestURL, null, aResponseConsumer, aSignalMsgConsumer);
    }
    catch (final Exception ex)
    {
      aExceptionCallback.onException (ex);
      return ESuccess.FAILURE;
    }
  }
}
