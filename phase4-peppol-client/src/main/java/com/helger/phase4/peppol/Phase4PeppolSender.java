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
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.ESuccess;
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
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
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
 * {@link #sendAS4Message(HttpClientFactory, IPMode, IDocumentTypeIdentifier, IProcessIdentifier, IParticipantIdentifier, IParticipantIdentifier, String, String, Element, IMimeType, boolean, SMPClientReadOnly, Consumer, Consumer)}
 * as the main method to trigger the sending, with all potential customization.
 *
 * @author Philip Helger
 */
public final class Phase4PeppolSender
{
  public static final PeppolIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  public static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  public static final IPModeResolver PMODE_RESOLVER = DefaultPModeResolver.DEFAULT_PMODE_RESOLVER;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolSender.class);

  private Phase4PeppolSender ()
  {}

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
          // Read response as XML
          final Document aSoapDoc = DOMReader.readXMLDOM (aResponseEntity.getResponse ());
          if (aSoapDoc == null || aSoapDoc.getDocumentElement () == null)
            throw new IllegalStateException ("Failed to parse response as XML");

          final ESOAPVersion eSOAPVersion = ESOAPVersion.getFromNamespaceURIOrNull (aSoapDoc.getDocumentElement ()
                                                                                            .getNamespaceURI ());
          if (eSOAPVersion == null)
            throw new IllegalStateException ("Failed to determine SOAP version");

          {
            // Find SOAP header
            final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSoapDoc.getDocumentElement (),
                                                                           eSOAPVersion.getNamespaceURI (),
                                                                           eSOAPVersion.getHeaderElementName ());
            if (aHeaderNode == null)
              throw new IllegalStateException ("SOAP document is missing a Header element");

            // Iterate all SOAP header elements
            for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
            {
              final QName aQName = XMLHelper.getQName (aHeaderChild);
              if (aQName.equals (SOAPHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING))
              {
                final AS4MessageState aState = new AS4MessageState (eSOAPVersion,
                                                                    aClient.getAS4ResourceHelper (),
                                                                    Locale.US);
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
                aSignalMsgConsumer.accept (aSignalMessage);
                break;
              }
            }
          }
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
                                     UUID.randomUUID ().toString (),
                                     PDTFactory.getCurrentLocalDateTime ());
    aData.setBusinessMessage (aBusinessMsg);
    final StandardBusinessDocument aSBD = new PeppolSBDHDocumentWriter ().createStandardBusinessDocument (aData);
    return aSBD;
  }

  @Nonnull
  public static ESuccess sendAS4Message (@Nonnull final HttpClientFactory aHttpClientFactory,
                                         @Nonnull final IPMode aSrcPMode,
                                         @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                         @Nonnull final IProcessIdentifier aProcID,
                                         @Nonnull final IParticipantIdentifier aSenderID,
                                         @Nonnull final IParticipantIdentifier aReceiverID,
                                         @Nonnull @Nonempty final String sSenderPartyID,
                                         @Nonnull final String sConversationID,
                                         @Nonnull final Element aPayloadElement,
                                         @Nonnull final IMimeType aPayloadMimeType,
                                         final boolean bCompressPayload,
                                         @Nonnull final SMPClientReadOnly aSMPClient,
                                         @Nullable final Consumer <AS4ClientSentMessage <byte []>> aResponseConsumer,
                                         @Nullable final Consumer <Ebms3SignalMessage> aSignalMsgConsumer)
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    ValueEnforcer.notNull (aSrcPMode, "SrcPMode");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcID, "ProcID");
    ValueEnforcer.notNull (aSenderID, "SenderID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");
    ValueEnforcer.notEmpty (sSenderPartyID, "SenderPartyID");
    ValueEnforcer.notNull (sConversationID, "ConversationID");
    ValueEnforcer.notNull (aPayloadElement, "PayloadElement");
    ValueEnforcer.notNull (aPayloadMimeType, "PayloadMimeType");
    ValueEnforcer.notNull (aSMPClient, "SMPClient");

    // Create deliver
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
      final EndpointType aEndpoint = aSMPClient.getEndpoint (aReceiverID,
                                                             aDocTypeID,
                                                             aProcID,
                                                             ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2);
      if (aEndpoint == null)
        throw new IllegalStateException ("Failed to resolve SMP endpoint");

      final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
      aUserMsg.setHttpClientFactory (aHttpClientFactory);

      // Otherwise Oxalis dies
      aUserMsg.setQuoteHttpHeaders (false);
      aUserMsg.setSOAPVersion (ESOAPVersion.SOAP_12);
      aUserMsg.setAS4CryptoFactory (AS4CryptoFactory.DEFAULT_INSTANCE);
      aUserMsg.signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_256);
      aUserMsg.signingParams ().setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
      aUserMsg.cryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);
      aUserMsg.setPMode (aSrcPMode, true);

      // Certificate from SMP lookup
      final X509Certificate aReceiverCert = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Received the following AP certificate from the SMP: " + aReceiverCert);
      aUserMsg.cryptParams ().setCertificate (aReceiverCert);

      // Explicit parameters have precedence over PMode
      aUserMsg.setAgreementRefValue (PeppolPMode.DEFAULT_AGREEMENT_ID);
      aUserMsg.setServiceType (aProcID.getScheme ());
      aUserMsg.setServiceValue (aProcID.getValue ());
      aUserMsg.setAction (aDocTypeID.getURIEncoded ());
      aUserMsg.setConversationID (sConversationID);

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

        final StandardBusinessDocument aSBD = createSBDH (aDocTypeID, aProcID, aSenderID, aReceiverID, aPayloadElement);
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
      LOGGER.error ("Error sending out AS4 message", ex);
      return ESuccess.FAILURE;
    }
  }
}
