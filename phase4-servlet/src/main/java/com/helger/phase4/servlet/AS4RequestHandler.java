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
package com.helger.phase4.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.http.HttpEntity;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.functional.IConsumer;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.WrappedInputStream;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.http.AcceptMimeTypeHandler;
import com.helger.httpclient.response.ResponseHandlerXml;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.BasicHttpPoster;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartInfo;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.http.AS4HttpDebug;
import com.helger.phase4.http.HttpMimeMessageEntity;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.domain.AS4ErrorMessage;
import com.helger.phase4.messaging.domain.AS4ReceiptMessage;
import com.helger.phase4.messaging.domain.AS4UserMessage;
import com.helger.phase4.messaging.domain.EAS4MessageType;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.MimeMessageCreator;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.AS4Helper;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.MEPHelper;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.servlet.mgr.AS4ServletMessageProcessorManager;
import com.helger.phase4.servlet.soap.AS4SingleSOAPHeader;
import com.helger.phase4.servlet.soap.ISOAPHeaderElementProcessor;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Process incoming AS4 transmissions. This class is instantiated per request.
 * The method
 * {@link #handleRequest(IRequestWebScopeWithoutResponse, AS4UnifiedResponse)}
 * is the entry point for the complex processing.
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
public class AS4RequestHandler implements AutoCloseable
{
  private static interface IAS4ResponseFactory
  {
    void applyToResponse (@Nonnull IAS4ResponseAbstraction aHttpResponse);

    @Nonnull
    HttpEntity getHttpEntity (@Nonnull IMimeType aMimType);
  }

  private static final class AS4ResponseFactoryXML implements IAS4ResponseFactory
  {
    private final Document m_aDoc;
    private final IMimeType m_aMimeType;

    public AS4ResponseFactoryXML (@Nonnull final Document aDoc, @Nonnull final IMimeType aMimeType)
    {
      ValueEnforcer.notNull (aDoc, "Doc");
      ValueEnforcer.notNull (aMimeType, "MimeType");
      m_aDoc = aDoc;
      m_aMimeType = aMimeType;
    }

    public void applyToResponse (@Nonnull final IAS4ResponseAbstraction aHttpResponse)
    {
      final String sXML = AS4XMLHelper.serializeXML (m_aDoc);
      final Charset aCharset = AS4XMLHelper.XWS.getCharset ();
      aHttpResponse.setContent (sXML.getBytes (aCharset), aCharset);
      aHttpResponse.setMimeType (m_aMimeType);
    }

    @Nonnull
    public HttpEntity getHttpEntity (@Nonnull final IMimeType aMimType)
    {
      return new HttpXMLEntity (m_aDoc, m_aMimeType);
    }
  }

  private static final class AS4ResponseFactoryMIME implements IAS4ResponseFactory
  {
    private final AS4MimeMessage m_aMimeMsg;
    private final HttpHeaderMap m_aHeaders;

    public AS4ResponseFactoryMIME (@Nonnull final AS4MimeMessage aMimeMsg) throws MessagingException
    {
      ValueEnforcer.notNull (aMimeMsg, "MimeMsg");
      m_aMimeMsg = aMimeMsg;
      m_aHeaders = MessageHelperMethods.getAndRemoveAllHeaders (m_aMimeMsg);
    }

    public void applyToResponse (@Nonnull final IAS4ResponseAbstraction aHttpResponse)
    {
      aHttpResponse.addCustomResponseHeaders (m_aHeaders);
      aHttpResponse.setContent (HasInputStream.multiple ( () -> {
        try
        {
          return m_aMimeMsg.getInputStream ();
        }
        catch (final IOException | MessagingException ex)
        {
          throw new IllegalStateException ("Failed to get MIME input stream", ex);
        }
      }));
      aHttpResponse.setMimeType (MT_MULTIPART_RELATED);
    }

    @Nonnull
    public HttpMimeMessageEntity getHttpEntity (@Nonnull final IMimeType aMimType)
    {
      return new HttpMimeMessageEntity (m_aMimeMsg);
    }
  }

  private static final class SPIInvocationResult implements ISuccessIndicator
  {
    private boolean m_bSuccess = false;
    private Ebms3UserMessage m_aPullReturnUserMsg;
    private String m_sAsyncResponseURL;

    public boolean isSuccess ()
    {
      return m_bSuccess;
    }

    void setSuccess (final boolean bSuccess)
    {
      m_bSuccess = bSuccess;
    }

    void setPullReturnUserMsg (@Nonnull final Ebms3UserMessage aPullReturnUserMsg)
    {
      m_aPullReturnUserMsg = aPullReturnUserMsg;
    }

    @Nullable
    public Ebms3UserMessage getPullReturnUserMsg ()
    {
      return m_aPullReturnUserMsg;
    }

    public boolean hasPullReturnUserMsg ()
    {
      return m_aPullReturnUserMsg != null;
    }

    void setAsyncResponseURL (@Nonnull final String sAsyncResponseURL)
    {
      m_sAsyncResponseURL = sAsyncResponseURL;
    }

    @Nullable
    public String getAsyncResponseURL ()
    {
      return m_sAsyncResponseURL;
    }

    public boolean hasAsyncResponseURL ()
    {
      return StringHelper.hasText (m_sAsyncResponseURL);
    }
  }

  public static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4RequestHandler.class);

  private final AS4ResourceHelper m_aResHelper;
  private final IAS4CryptoFactory m_aCryptoFactory;
  private final IIncomingAttachmentFactory m_aIAF;
  private Locale m_aLocale = CGlobal.DEFAULT_LOCALE;

  /** By default get all message processors from the global SPI registry */
  private ISupplier <ICommonsList <IAS4ServletMessageProcessorSPI>> m_aProcessorSupplier = AS4ServletMessageProcessorManager::getAllProcessors;
  private IConsumer <ICommonsList <Ebms3Error>> m_aErrorConsumer;

  public AS4RequestHandler (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                            @Nonnull final IIncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aIAF, "IAF");
    // Create dynamically here, to avoid leaving too many streams open
    m_aResHelper = new AS4ResourceHelper ();
    m_aCryptoFactory = aCryptoFactory;
    m_aIAF = aIAF;
  }

  public void close ()
  {
    m_aResHelper.close ();
  }

  @Nonnull
  public final Locale getLocale ()
  {
    return m_aLocale;
  }

  @Nonnull
  public final AS4RequestHandler setLocale (@Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aLocale, "Locale");
    m_aLocale = aLocale;
    return this;
  }

  /**
   * @return The supplier used to get all SPIs. By default this is
   *         {@link AS4ServletMessageProcessorManager#getAllProcessors()}.
   */
  @Nonnull
  public final ISupplier <ICommonsList <IAS4ServletMessageProcessorSPI>> getProcessorSupplier ()
  {
    return m_aProcessorSupplier;
  }

  /**
   * Set a different processor supplier
   *
   * @param aProcessorSupplier
   *        The processor supplier to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final AS4RequestHandler setProcessorSupplier (@Nonnull final ISupplier <ICommonsList <IAS4ServletMessageProcessorSPI>> aProcessorSupplier)
  {
    ValueEnforcer.notNull (aProcessorSupplier, "ProcessorSupplier");
    m_aProcessorSupplier = aProcessorSupplier;
    return this;
  }

  /**
   * @return An optional error consumer. <code>null</code> by default.
   * @since 0.9.7
   */
  @Nullable
  public final IConsumer <ICommonsList <Ebms3Error>> getErrorConsumer ()
  {
    return m_aErrorConsumer;
  }

  /**
   * Set an optional error consumer that is invoked with all errors determined
   * during message processing. The consumed list MUST NOT be modified.
   *
   * @param aErrorConsumer
   *        The consumer to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 0.9.7
   */
  @Nonnull
  public final AS4RequestHandler setErrorConsumer (@Nullable final IConsumer <ICommonsList <Ebms3Error>> aErrorConsumer)
  {
    m_aErrorConsumer = aErrorConsumer;
    return this;
  }

  private static void _decompressAttachments (@Nonnull final Ebms3UserMessage aUserMessage,
                                              @Nonnull final IAS4MessageState aState,
                                              @Nonnull final ICommonsList <WSS4JAttachment> aIncomingDecryptedAttachments)
  {
    for (final WSS4JAttachment aIncomingAttachment : aIncomingDecryptedAttachments.getClone ())
    {
      final EAS4CompressionMode eCompressionMode = aState.getAttachmentCompressionMode (aIncomingAttachment.getId ());
      if (eCompressionMode != null)
      {
        final IHasInputStream aOldISP = aIncomingAttachment.getInputStreamProvider ();
        aIncomingAttachment.setSourceStreamProvider (new HasInputStream ( () -> {
          try
          {
            final InputStream aSrcIS = aOldISP.getInputStream ();
            if (aSrcIS == null)
              throw new IllegalStateException ("Failed to create InputStream from " + aOldISP);
            return eCompressionMode.getDecompressStream (aSrcIS);
          }
          catch (final IOException ex)
          {
            // This is e.g. invoked, if the GZIP decompression failed because of
            // invalid payload
            throw new AS4DecompressException (ex);
          }
        }, aOldISP.isReadMultiple ()));

        final String sAttachmentContentID = StringHelper.trimStart (aIncomingAttachment.getId (), "attachment=");
        // x.getHref() != null needed since, if a message contains a payload and
        // an attachment, it would throw a NullPointerException since a payload
        // does not have anything written in its partinfo therefore also now
        // href
        final Ebms3PartInfo aPartInfo = CollectionHelper.findFirst (aUserMessage.getPayloadInfo ().getPartInfo (),
                                                                    x -> x.getHref () != null &&
                                                                         x.getHref ().contains (sAttachmentContentID));
        if (aPartInfo != null && aPartInfo.getPartProperties () != null)
        {
          // Find MimeType property
          final Ebms3Property aProperty = CollectionHelper.findFirst (aPartInfo.getPartProperties ().getProperty (),
                                                                      x -> x.getName ()
                                                                            .equalsIgnoreCase (MessageHelperMethods.PART_PROPERTY_MIME_TYPE));
          if (aProperty != null)
          {
            final String sMimeType = aProperty.getValue ();
            if (MimeTypeParser.safeParseMimeType (sMimeType) == null)
              LOGGER.warn ("Value '" +
                           sMimeType +
                           "' of property '" +
                           MessageHelperMethods.PART_PROPERTY_MIME_TYPE +
                           "' is not a valid MIME type");
            aIncomingAttachment.overwriteMimeType (sMimeType);
          }
        }
      }
    }
  }

  private static void _processSoapHeaderElements (@Nonnull final Document aSoapDocument,
                                                  @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                  @Nonnull final AS4MessageState aState,
                                                  @Nonnull final ICommonsList <Ebms3Error> aErrorMessages) throws AS4BadRequestException
  {
    final ESOAPVersion eSoapVersion = aState.getSOAPVersion ();
    final ICommonsList <AS4SingleSOAPHeader> aHeaders = new CommonsArrayList <> ();
    {
      // Find SOAP header
      final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSoapDocument.getDocumentElement (),
                                                                     eSoapVersion.getNamespaceURI (),
                                                                     eSoapVersion.getHeaderElementName ());
      if (aHeaderNode == null)
        throw new AS4BadRequestException ("SOAP document is missing a Header element");

      // Extract all header elements including their mustUnderstand value
      for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
      {
        final QName aQName = XMLHelper.getQName (aHeaderChild);
        final String sMustUnderstand = aHeaderChild.getAttributeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
        final boolean bIsMustUnderstand = eSoapVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
        aHeaders.add (new AS4SingleSOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
      }
    }

    // handle all headers in the order of the registered handlers!
    for (final Map.Entry <QName, ISOAPHeaderElementProcessor> aEntry : SOAPHeaderElementProcessorRegistry.getInstance ()
                                                                                                         .getAllElementProcessors ()
                                                                                                         .entrySet ())
    {
      final QName aQName = aEntry.getKey ();

      // Check if this message contains a header for the current handler
      final AS4SingleSOAPHeader aHeader = aHeaders.findFirst (x -> aQName.equals (x.getQName ()));
      if (aHeader == null)
      {
        // no header element for current processor
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Message contains no SOAP header element with QName " + aQName.toString ());
        continue;
      }

      final ISOAPHeaderElementProcessor aProcessor = aEntry.getValue ();
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

      // Process element
      final ErrorList aErrorList = new ErrorList ();
      if (aProcessor.processHeaderElement (aSoapDocument, aHeader.getNode (), aIncomingAttachments, aState, aErrorList)
                    .isSuccess ())
      {
        // Mark header as processed (for mustUnderstand check)
        aHeader.setProcessed (true);
      }
      else
      {
        // upon failure, the element stays unprocessed and sends back a signal
        // message with the errors
        LOGGER.warn ("Failed to process SOAP header element " +
                     aQName.toString () +
                     " with processor " +
                     aProcessor +
                     "; error details: " +
                     aErrorList);

        final String sRefToMessageID = aState.getRefToMessageID ();
        final Locale aLocale = aState.getLocale ();
        for (final IError aError : aErrorList)
        {
          final EEbmsError ePredefinedError = EEbmsError.getFromErrorCodeOrNull (aError.getErrorID ());
          if (ePredefinedError != null)
            aErrorMessages.add (ePredefinedError.getAsEbms3Error (aLocale, sRefToMessageID));
          else
          {
            final Ebms3Error aEbms3Error = new Ebms3Error ();
            aEbms3Error.setErrorDetail (aError.getErrorText (aLocale));
            aEbms3Error.setErrorCode (aError.getErrorID ());
            aEbms3Error.setSeverity (aError.getErrorLevel ().getID ());
            aEbms3Error.setOrigin (aError.getErrorFieldName ());
            aEbms3Error.setRefToMessageInError (sRefToMessageID);
            aErrorMessages.add (aEbms3Error);
          }
        }

        // Stop processing of other headers
        break;
      }
    }

    // If an error message is present, send it back gracefully
    if (aErrorMessages.isEmpty ())
    {
      // Now check if all must understand headers were processed
      // Are all must-understand headers processed?
      for (final AS4SingleSOAPHeader aHeader : aHeaders)
        if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
          throw new AS4BadRequestException ("Error processing required SOAP header element " +
                                            aHeader.getQName ().toString ());
    }
  }

  /**
   * Invoke custom SPI message processors
   *
   * @param aHttpHeaders
   *        The received HTTP headers. Never <code>null</code>.
   * @param aUserMessage
   *        Current user message. Either this OR signal message must be
   *        non-<code>null</code>.
   * @param aSignalMessage
   *        The signal message to use. Either this OR user message must be
   *        non-<code>null</code>.
   * @param aPayloadNode
   *        Optional SOAP body payload (only if direct SOAP msg, not for MIME).
   *        May be <code>null</code>.
   * @param aDecryptedAttachments
   *        Original attachments from source message. May be <code>null</code>.
   * @param aPMode
   *        PMode to be used - may be <code>null</code> for Receipt messages.
   * @param aState
   *        The current state. Never <code>null</<code></code>.
   * @param aErrorMessages
   *        The list of error messages to be filled if something goes wrong.
   *        Never <code>null</code>.
   * @param aResponseAttachments
   *        The list of attachments to be added to the response. Never
   *        <code>null</code>.
   * @param aSPIResult
   *        The result object to be filled. May not be <code>null</code>.
   */
  private void _invokeSPIs (@Nonnull final HttpHeaderMap aHttpHeaders,
                            @Nullable final Ebms3UserMessage aUserMessage,
                            @Nullable final Ebms3SignalMessage aSignalMessage,
                            @Nullable final Node aPayloadNode,
                            @Nullable final ICommonsList <WSS4JAttachment> aDecryptedAttachments,
                            @Nullable final IPMode aPMode,
                            @Nonnull final IAS4MessageState aState,
                            @Nonnull final ICommonsList <Ebms3Error> aErrorMessages,
                            @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                            @Nonnull final SPIInvocationResult aSPIResult)
  {
    ValueEnforcer.isTrue (aUserMessage != null || aSignalMessage != null, "User OR Signal Message must be present");
    ValueEnforcer.isFalse (aUserMessage != null && aSignalMessage != null,
                           "Only one of User OR Signal Message may be present");

    final boolean bIsUserMessage = aUserMessage != null;
    final String sMessageID = bIsUserMessage ? aUserMessage.getMessageInfo ().getMessageId ()
                                             : aSignalMessage.getMessageInfo ().getMessageId ();

    // Get all processors
    final ICommonsList <IAS4ServletMessageProcessorSPI> aAllProcessors = m_aProcessorSupplier.get ();
    for (final IAS4ServletMessageProcessorSPI aProcessor : aAllProcessors)
      if (aProcessor != null)
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Invoking AS4 message processor " + aProcessor);

          // Main processing
          final AS4MessageProcessorResult aResult;
          final ICommonsList <Ebms3Error> aProcessingErrorMessages = new CommonsArrayList <> ();
          if (bIsUserMessage)
          {
            aResult = aProcessor.processAS4UserMessage (aHttpHeaders,
                                                        aUserMessage,
                                                        aPMode,
                                                        aPayloadNode,
                                                        aDecryptedAttachments,
                                                        aState,
                                                        aProcessingErrorMessages);
          }
          else
          {
            aResult = aProcessor.processAS4SignalMessage (aHttpHeaders,
                                                          aSignalMessage,
                                                          aPMode,
                                                          aState,
                                                          aProcessingErrorMessages);
          }

          // Result returned?
          if (aResult == null)
            throw new IllegalStateException ("No result object present from AS4 SPI processor " + aProcessor);

          if (aProcessingErrorMessages.isNotEmpty ())
          {
            if (!aResult.isFailure ())
              LOGGER.warn ("Processing errors are present, but success was returned");

            aErrorMessages.addAll (aProcessingErrorMessages);
            // Stop processing
            return;
          }

          if (aResult.isFailure ())
          {
            final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                     aProcessor +
                                     " on '" +
                                     sMessageID +
                                     "' returned a failure: " +
                                     aResult.getErrorMessage ();
            LOGGER.warn (sErrorMsg);
            aErrorMessages.add (EEbmsError.EBMS_OTHER.getAsEbms3Error (m_aLocale, sMessageID, sErrorMsg));
            // Stop processing
            return;
          }

          // SPI invocation was okay
          {
            final String sAsyncResultURL = aResult.getAsyncResponseURL ();
            if (StringHelper.hasText (sAsyncResultURL))
            {
              // URL present
              if (aSPIResult.hasAsyncResponseURL ())
              {
                // A second processor returned a response URL - not allowed
                final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                         aProcessor +
                                         " on '" +
                                         sMessageID +
                                         "' failed: the previous processor already returned an async response URL; it is not possible to handle two URLs. Please check your SPI implementations.";
                LOGGER.error (sErrorMsg);
                aErrorMessages.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsEbms3Error (m_aLocale,
                                                                                        sMessageID,
                                                                                        sErrorMsg));
                // Stop processing
                return;
              }
              aSPIResult.setAsyncResponseURL (sAsyncResultURL);
            }
          }

          if (bIsUserMessage)
          {
            // User message specific processing result handling

            // empty
          }
          else
          {
            // Signal message specific processing result handling
            if (aSignalMessage.getReceipt () == null)
            {
              final Ebms3UserMessage aPullReturnUserMsg = ((AS4SignalMessageProcessorResult) aResult).getPullReturnUserMessage ();
              if (aSPIResult.hasPullReturnUserMsg ())
              {
                // A second processor has committed a response to the
                // pullrequest
                // Which is not allowed since only one response can be sent back
                // to the pullrequest initiator
                if (aPullReturnUserMsg != null)
                {
                  final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                           aProcessor +
                                           " on '" +
                                           sMessageID +
                                           "' failed: the previous processor already returned a usermessage; it is not possible to return two usermessage. Please check your SPI implementations.";
                  LOGGER.warn (sErrorMsg);
                  aErrorMessages.add (EEbmsError.EBMS_VALUE_INCONSISTENT.getAsEbms3Error (m_aLocale,
                                                                                          sMessageID,
                                                                                          sErrorMsg));
                  // Stop processing
                  return;
                }
              }
              else
              {
                // Initial return user msg
                if (aPullReturnUserMsg == null)
                {
                  // No message contained in the MPC
                  final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                           aProcessor +
                                           " on '" +
                                           sMessageID +
                                           "' returned a failure: no UserMessage contained in the MPC";
                  LOGGER.warn (sErrorMsg);
                  aErrorMessages.add (EEbmsError.EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL.getAsEbms3Error (m_aLocale,
                                                                                                       sMessageID,
                                                                                                       sErrorMsg));
                  // Stop processing
                  return;
                }

                // We have something :)
                aSPIResult.setPullReturnUserMsg (aPullReturnUserMsg);
              }
            }
          }

          // Add response attachments, payloads
          aResult.addAllAttachmentsTo (aResponseAttachments);

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Successfully invoked AS4 message processor " + aProcessor);
        }
        catch (final AS4DecompressException ex)
        {
          // Hack for invalid GZip content from WSS4JAttachment.getSourceStream
          aErrorMessages.add (EEbmsError.EBMS_DECOMPRESSION_FAILURE.getAsEbms3Error (m_aLocale, sMessageID));
          return;
        }
        catch (final RuntimeException ex)
        {
          // Re-throw
          throw ex;
        }
        catch (final Exception ex)
        {
          throw new IllegalStateException ("Error processing incoming AS4 message with processor " + aProcessor, ex);
        }

    // Remember success
    aSPIResult.setSuccess (true);
  }

  /**
   * Checks the mandatory properties OriginalSender and FinalRecipient if those
   * two are set.
   *
   * @param aPropertyList
   *        the property list that should be checked for the two specific ones
   * @throws AS4BadRequestException
   *         on error
   */
  private static void _checkPropertiesOrignalSenderAndFinalRecipient (@Nonnull final List <? extends Ebms3Property> aPropertyList) throws AS4BadRequestException
  {
    String sOriginalSenderC1 = null;
    String sFinalRecipientC4 = null;

    for (final Ebms3Property sProperty : aPropertyList)
    {
      if (sProperty.getName ().equals (CAS4.ORIGINAL_SENDER))
        sOriginalSenderC1 = sProperty.getValue ();
      else
        if (sProperty.getName ().equals (CAS4.FINAL_RECIPIENT))
          sFinalRecipientC4 = sProperty.getValue ();
    }

    if (StringHelper.hasNoText (sOriginalSenderC1))
      throw new AS4BadRequestException (CAS4.ORIGINAL_SENDER + " property is empty or not existant but mandatory");
    if (StringHelper.hasNoText (sFinalRecipientC4))
      throw new AS4BadRequestException (CAS4.FINAL_RECIPIENT + " property is empty or not existant but mandatory");
  }

  /**
   * Takes an UserMessage and switches properties to reverse the direction. So
   * previously it was C1 => C4, now its C4 => C1 Also adds attachments if there
   * are some that should be added.
   *
   * @param eSoapVersion
   *        of the message
   * @param aUserMessage
   *        the message that should be reversed
   * @param aResponseAttachments
   *        attachment that should be added
   * @return the reversed usermessage in document form
   */
  @Nonnull
  private static AS4UserMessage _createReversedUserMessage (@Nonnull final ESOAPVersion eSoapVersion,
                                                            @Nonnull final Ebms3UserMessage aUserMessage,
                                                            @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments)
  {
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (MessageHelperMethods.createRandomMessageID (),
                                                                                            aUserMessage.getMessageInfo ()
                                                                                                        .getMessageId ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (false,
                                                                                            aResponseAttachments);

    // Invert from and to role from original user message
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3ReversePartyInfo (aUserMessage.getPartyInfo ());

    // Should be exactly the same as incoming message
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.getCollaborationInfo ();

    // Need to switch C1 and C4 around from the original usermessage
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    {
      Ebms3Property aFinalRecipient = null;
      Ebms3Property aOriginalSender = null;
      for (final Ebms3Property aProp : aUserMessage.getMessageProperties ().getProperty ())
      {
        if (aProp.getName ().equals (CAS4.FINAL_RECIPIENT))
        {
          aOriginalSender = aProp;
        }
        else
          if (aProp.getName ().equals (CAS4.ORIGINAL_SENDER))
          {
            aFinalRecipient = aProp;
          }
      }

      if (aOriginalSender == null)
        throw new IllegalStateException ("Failed to determine new OriginalSender");
      if (aFinalRecipient == null)
        throw new IllegalStateException ("Failed to determine new FinalRecipient");

      aFinalRecipient.setName (CAS4.ORIGINAL_SENDER);
      aOriginalSender.setName (CAS4.FINAL_RECIPIENT);

      aEbms3MessageProperties.addProperty (aFinalRecipient);
      aEbms3MessageProperties.addProperty (aOriginalSender);
    }

    final AS4UserMessage aResponseUserMessage = AS4UserMessage.create (aEbms3MessageInfo,
                                                                       aEbms3PayloadInfo,
                                                                       aEbms3CollaborationInfo,
                                                                       aEbms3PartyInfo,
                                                                       aEbms3MessageProperties,
                                                                       eSoapVersion);
    return aResponseUserMessage;
  }

  /**
   * Checks if in the given PMode isReportAsResponse is set.
   *
   * @param aLeg
   *        The PMode leg to check. May be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendErrorAsResponse (@Nullable final PModeLeg aLeg)
  {
    if (aLeg != null)
      if (aLeg.hasErrorHandling ())
        if (aLeg.getErrorHandling ().isReportAsResponseDefined ())
        {
          // Note: this is enabled in Default PMode
          return aLeg.getErrorHandling ().isReportAsResponse ();
        }
    // Default behavior
    return true;
  }

  /**
   * Checks if a ReceiptReplyPattern is set to Response or not.
   *
   * @param aPLeg
   *        to PMode leg to use. May not be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>TRUE</code>.
   */
  private static boolean _isSendReceiptAsResponse (@Nonnull final PModeLeg aLeg)
  {
    if (aLeg != null)
      if (aLeg.hasSecurity ())
      {
        // Note: this is enabled in Default PMode
        return EPModeSendReceiptReplyPattern.RESPONSE.equals (aLeg.getSecurity ().getSendReceiptReplyPattern ());
      }
    // Default behaviour if the value is not set or no security is existing
    return true;
  }

  /**
   * If the PModeLegSecurity has set a Sign and Digest Algorithm the message
   * will be signed, else the message will be returned as it is.
   *
   * @param aResponseAttachments
   *        attachment that are added
   * @param aSigningParams
   *        Signing parameters
   * @param aDocToBeSigned
   *        the message that should be signed
   * @param eSoapVersion
   *        SOAPVersion that is used
   * @param sMessagingID
   *        The messaging ID to be used for signing
   * @return returns the signed response or just the input document if no
   *         X509SignatureAlgorithm and no X509SignatureHashFunction was set.
   * @throws WSSecurityException
   *         if something in the signing process goes wrong from WSS4j
   */
  @Nonnull
  private Document _signResponseIfNeeded (@Nullable final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                          @Nonnull final AS4SigningParams aSigningParams,
                                          @Nonnull final Document aDocToBeSigned,
                                          @Nonnull final ESOAPVersion eSoapVersion,
                                          @Nonnull @Nonempty final String sMessagingID) throws WSSecurityException
  {
    final Document ret;
    if (aSigningParams.isSigningEnabled ())
    {
      // Sign
      final boolean bMustUnderstand = true;
      ret = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                           aDocToBeSigned,
                                           eSoapVersion,
                                           sMessagingID,
                                           aResponseAttachments,
                                           m_aResHelper,
                                           bMustUnderstand,
                                           aSigningParams.getClone ());
    }
    else
    {
      // No signing
      ret = aDocToBeSigned;
    }
    return ret;
  }

  /**
   * Checks if in the given PMode the isSendReceiptNonRepudiation is set or not.
   *
   * @param aLeg
   *        The PMode leg to check. May not be <code>null</code>.
   * @return Returns the value if set, else DEFAULT <code>false</code>.
   */
  private static boolean _isSendNonRepudiationInformation (@Nonnull final PModeLeg aLeg)
  {
    if (aLeg.hasSecurity ())
      if (aLeg.getSecurity ().isSendReceiptNonRepudiationDefined ())
        return aLeg.getSecurity ().isSendReceiptNonRepudiation ();
    // Default behavior
    return false;
  }

  /**
   * @param aSoapDocument
   *        document which should be used as source for the receipt to convert
   *        it to non-repudiation information. Can be <code>null</code>.
   * @param eSoapVersion
   *        SOAPVersion which should be used
   * @param aEffectiveLeg
   *        the leg that is used to determined, how the receipt should be build
   * @param aUserMessage
   *        used if no non-repudiation information is needed, prints the
   *        usermessage in receipt. Can be <code>null</code>.
   * @param aResponseAttachments
   *        that should be sent back if needed. Can be <code>null</code>.
   * @throws WSSecurityException
   */
  private IAS4ResponseFactory _createResponseReceiptMessage (@Nullable final Document aSoapDocument,
                                                             @Nonnull final ESOAPVersion eSoapVersion,
                                                             @Nonnull final PModeLeg aEffectiveLeg,
                                                             @Nullable final Ebms3UserMessage aUserMessage,
                                                             @Nullable final ICommonsList <WSS4JAttachment> aResponseAttachments) throws WSSecurityException
  {
    final AS4ReceiptMessage aReceiptMessage = AS4ReceiptMessage.create (eSoapVersion,
                                                                        MessageHelperMethods.createRandomMessageID (),
                                                                        aUserMessage,
                                                                        aSoapDocument,
                                                                        _isSendNonRepudiationInformation (aEffectiveLeg))
                                                               .setMustUnderstand (true);

    // We've got our response
    final Document aResponseDoc = aReceiptMessage.getAsSOAPDocument ();
    final AS4SigningParams aSigningParams = new AS4SigningParams ().setFromPMode (aEffectiveLeg.getSecurity ());
    final Document aSignedDoc = _signResponseIfNeeded (aResponseAttachments,
                                                       aSigningParams,
                                                       aResponseDoc,
                                                       aEffectiveLeg.getProtocol ().getSOAPVersion (),
                                                       aReceiptMessage.getMessagingID ());
    return new AS4ResponseFactoryXML (aSignedDoc, eSoapVersion.getMimeType ());
  }

  /**
   * Returns the MimeMessage with encrypted attachment or without depending on
   * what is configured in the PMode within Leg2.
   *
   * @param aResponseDoc
   *        the document that contains the user message
   * @param aResponseAttachments
   *        The Attachments that should be encrypted
   * @param aLeg
   *        Leg to get necessary information, EncryptionAlgorithm, SOAPVersion
   * @param sEncryptToAlias
   *        The alias into the keystore that should be used for encryption
   * @return a MimeMessage to be sent
   * @throws MessagingException
   * @throws WSSecurityException
   */
  @Nonnull
  private AS4MimeMessage _createMimeMessageForResponse (@Nonnull final Document aResponseDoc,
                                                        @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                        @Nonnull final ESOAPVersion eSoapVersion,
                                                        @Nonnull final AS4CryptParams aCryptParms) throws WSSecurityException,
                                                                                                   MessagingException
  {
    final AS4MimeMessage aMimeMsg;
    if (aCryptParms.isCryptEnabled (LOGGER::warn))
    {
      final boolean bMustUnderstand = true;
      aMimeMsg = AS4Encryptor.encryptMimeMessage (eSoapVersion,
                                                  aResponseDoc,
                                                  aResponseAttachments,
                                                  m_aCryptoFactory,
                                                  bMustUnderstand,
                                                  m_aResHelper,
                                                  aCryptParms);
    }
    else
    {
      aMimeMsg = MimeMessageCreator.generateMimeMessage (eSoapVersion, aResponseDoc, aResponseAttachments);
    }
    if (aMimeMsg == null)
      throw new IllegalStateException ("Failed to create MimeMessage!");
    return aMimeMsg;
  }

  /**
   * With this method it is possible to send a usermessage back, the method will
   * check if signing is needed and if the message needs to be a mime message.
   *
   * @param aResponseAttachments
   *        attachments if any that should be added
   * @param aLeg
   *        the leg that should be used, to determine what if any security
   *        should be used
   * @param aSrcDoc
   *        the message that should be sent
   * @param sMessagingID
   *        ID of the "Messaging" element
   * @param aSigningParams
   *        Signing parameters
   * @param aCryptParams
   *        Encryption parameters
   * @throws WSSecurityException
   *         on error
   * @throws MessagingException
   *         on error
   */
  @Nonnull
  private IAS4ResponseFactory _createResponseUserMessage (@Nonnull final ESOAPVersion eSoapVersion,
                                                          @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                          @Nonnull final Document aSrcDoc,
                                                          @Nonnull @Nonempty final String sMessagingID,
                                                          @Nonnull final AS4SigningParams aSigningParams,
                                                          @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException,
                                                                                                      MessagingException
  {
    final Document aSignedDoc = _signResponseIfNeeded (aResponseAttachments,
                                                       aSigningParams,
                                                       aSrcDoc,
                                                       eSoapVersion,
                                                       sMessagingID);

    final IAS4ResponseFactory ret;
    if (aResponseAttachments.isEmpty ())
    {
      // FIXME encryption of SOAP body is missing here
      ret = new AS4ResponseFactoryXML (aSignedDoc, eSoapVersion.getMimeType ());
    }
    else
    {
      // Create (maybe encrypted) MIME message
      final AS4MimeMessage aMimeMsg = _createMimeMessageForResponse (aSignedDoc,
                                                                     aResponseAttachments,
                                                                     eSoapVersion,
                                                                     aCryptParams);
      ret = new AS4ResponseFactoryMIME (aMimeMsg);
    }
    return ret;
  }

  @Nullable
  private IAS4ResponseFactory _handleSoapMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                  @Nonnull final Document aSoapDocument,
                                                  @Nonnull final ESOAPVersion eSoapVersion,
                                                  @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments) throws WSSecurityException,
                                                                                                                      MessagingException
  {
    ValueEnforcer.notNull (aHttpHeaders, "HttpHeaders");
    ValueEnforcer.notNull (aSoapDocument, "SoapDocument");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aIncomingAttachments, "IncomingAttachments");

    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("Received the following SOAP " + eSoapVersion.getVersion () + " document:");
      LOGGER.debug (AS4XMLHelper.serializeXML (aSoapDocument));
      LOGGER.debug ("Including the following " + aIncomingAttachments.size () + " attachments:");
      LOGGER.debug (aIncomingAttachments.toString ());
    }

    // Collect all runtime errors
    final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

    // All further operations should only operate on the interface
    IAS4MessageState aState;
    {
      // This is where all data from the SOAP headers is stored to
      final AS4MessageState aStateImpl = new AS4MessageState (eSoapVersion, m_aResHelper, m_aLocale);

      // Handle all headers - the only place where the AS4MessageState values
      // are written
      _processSoapHeaderElements (aSoapDocument, aIncomingAttachments, aStateImpl, aErrorMessages);

      aState = aStateImpl;
    }

    final IPMode aPMode = aState.getPMode ();
    final PModeLeg aEffectiveLeg = aState.getEffectivePModeLeg ();
    final Ebms3UserMessage aEbmsUserMessage;
    final Ebms3SignalMessage aEbmsSignalMessage;
    final Ebms3Error aEbmsError;
    final Node aPayloadNode;
    final ICommonsList <WSS4JAttachment> aDecryptedAttachments;
    // Storing for two-way response messages
    final ICommonsList <WSS4JAttachment> aResponseAttachments = new CommonsArrayList <> ();
    boolean bCanInvokeSPIs = false;
    String sProfileID = null;

    if (aErrorMessages.isEmpty ())
    {
      // Every message can only contain 1 User message or 1 pull message
      // aUserMessage can be null on incoming Pull-Message!
      aEbmsUserMessage = aState.getMessaging ().hasUserMessageEntries () ? aState.getMessaging ()
                                                                                 .getUserMessageAtIndex (0)
                                                                         : null;
      aEbmsSignalMessage = aState.getMessaging ().hasSignalMessageEntries ()
                                                                             ? aState.getMessaging ()
                                                                                     .getSignalMessageAtIndex (0)
                                                                             : null;
      aEbmsError = aEbmsSignalMessage != null &&
                   aEbmsSignalMessage.hasErrorEntries () ? aEbmsSignalMessage.getErrorAtIndex (0) : null;

      final Ebms3PullRequest aEbmsPullRequest = aEbmsSignalMessage != null ? aEbmsSignalMessage.getPullRequest ()
                                                                           : null;
      final Ebms3Receipt aEbmsReceipt = aEbmsSignalMessage != null ? aEbmsSignalMessage.getReceipt () : null;

      final int nCountData = (aEbmsUserMessage != null ? 1 : 0) +
                             (aEbmsPullRequest != null ? 1 : 0) +
                             (aEbmsReceipt != null ? 1 : 0) +
                             (aEbmsError != null ? 1 : 0);
      // Errors do not count
      if (nCountData != 1)
      {
        // send EBMS:0001 error back
        if (true)
          aErrorMessages.add (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED.getAsEbms3Error (m_aLocale,
                                                                                    aState.getRefToMessageID ()));
        else
          throw new AS4BadRequestException ("Exactly one UserMessage or one PullRequest or one Receipt or on Error must be present!");
      }

      // XXX debugging
      if (LOGGER.isDebugEnabled () && aEbmsReceipt != null)
      {
        LOGGER.debug ("RECEIPT INCOMING");
      }

      // Ensure the decrypted attachments are used
      aDecryptedAttachments = aState.hasDecryptedAttachments () ? aState.getDecryptedAttachments ()
                                                                : aState.getOriginalAttachments ();

      final String sMessageID;
      if (aEbmsUserMessage != null)
      {
        // User message requires PMode
        if (aPMode == null)
          throw new AS4BadRequestException ("No AS4 P-Mode configuration found for user-message!");

        // Only check leg if the message is a usermessage
        if (aEffectiveLeg == null)
          throw new AS4BadRequestException ("No AS4 P-Mode leg could be determined!");

        // The profile ID from the configuration file is optional
        sProfileID = AS4ServerConfiguration.getAS4ProfileID ();

        // Only do profile checks if a profile is set
        if (StringHelper.hasText (sProfileID))
        {
          final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sProfileID);
          if (aProfile == null)
            throw new IllegalStateException ("The configured AS4 profile " + sProfileID + " does not exist.");

          // Profile Checks gets set when started with Server
          final IAS4ProfileValidator aValidator = aProfile.getValidator ();
          if (aValidator != null)
          {
            final ErrorList aErrorList = new ErrorList ();
            aValidator.validatePMode (aPMode, aErrorList);
            aValidator.validateUserMessage (aEbmsUserMessage, aErrorList);
            if (aErrorList.isNotEmpty ())
            {
              throw new AS4BadRequestException ("Error validating incoming AS4 message with the profile " +
                                                aProfile.getDisplayName () +
                                                "\n Following errors are present: " +
                                                aErrorList.getAllErrors ().getAllTexts (m_aLocale));
            }
          }
        }
        sMessageID = aEbmsUserMessage.getMessageInfo ().getMessageId ();
        // Decompress attachments (if compressed)
        // Result is directly in the decrypted attachments list!
        _decompressAttachments (aEbmsUserMessage, aState, aDecryptedAttachments);
      }
      else
      {
        // Signal message

        // Pull-request also requires PMode
        if (aEbmsPullRequest != null && aPMode == null)
          throw new AS4BadRequestException ("No AS4 P-Mode configuration found for pull-request!");

        sMessageID = aEbmsSignalMessage.getMessageInfo ().getMessageId ();
      }

      final boolean bUseDecryptedSOAP = aState.hasDecryptedSOAPDocument ();
      final Document aRealSOAPDoc = bUseDecryptedSOAP ? aState.getDecryptedSOAPDocument () : aSoapDocument;
      assert aRealSOAPDoc != null;

      // Find SOAP body
      final Node aBodyNode = XMLHelper.getFirstChildElementOfName (aRealSOAPDoc.getDocumentElement (),
                                                                   eSoapVersion.getNamespaceURI (),
                                                                   eSoapVersion.getBodyElementName ());
      if (aBodyNode == null)
        throw new AS4BadRequestException ((bUseDecryptedSOAP ? "Decrypted" : "Original") +
                                          " SOAP document is missing a Body element");
      aPayloadNode = aBodyNode.getFirstChild ();

      if (aEbmsUserMessage != null)
      {
        // Check if originalSender and finalRecipient are present
        // Since these two properties are mandatory
        if (aEbmsUserMessage.getMessageProperties () == null)
          throw new AS4BadRequestException ("No Message Properties present but originalSender and finalRecipient have to be present");

        final List <Ebms3Property> aProps = aEbmsUserMessage.getMessageProperties ().getProperty ();
        if (aProps.isEmpty ())
          throw new AS4BadRequestException ("Message Property element present but no properties");

        _checkPropertiesOrignalSenderAndFinalRecipient (aProps);
      }

      final boolean bIsDuplicate = MetaAS4Manager.getIncomingDuplicateMgr ()
                                                 .registerAndCheck (sMessageID,
                                                                    sProfileID,
                                                                    aPMode == null ? null : aPMode.getID ())
                                                 .isBreak ();
      if (bIsDuplicate)
      {
        LOGGER.info ("Not invoking SPIs, because message was already handled!");
        aErrorMessages.add (EEbmsError.EBMS_OTHER.getAsEbms3Error (m_aLocale,
                                                                   sMessageID,
                                                                   "Another message with the same ID was already received!"));
      }
      else
      {
        if (!AS4Helper.isPingMessage (aPMode))
        {
          // Invoke SPIs if
          // * Valid PMode
          // * Exactly one UserMessage or SignalMessage
          // * No ping/test message
          // * No Duplicate message ID
          // * No errors so far (sign, encrypt, ...)
          bCanInvokeSPIs = true;
        }
      }
    }
    else
    {
      aEbmsUserMessage = null;
      aEbmsSignalMessage = null;
      aEbmsError = null;
      aPayloadNode = null;
      aDecryptedAttachments = null;
    }

    final SPIInvocationResult aSPIResult = new SPIInvocationResult ();
    if (bCanInvokeSPIs)
    {
      // PMode may be null for receipts
      if (aPMode == null ||
          aPMode.getMEPBinding ().isSynchronous () ||
          aPMode.getMEPBinding ().isAsynchronousInitiator () ||
          aState.getEffectivePModeLegNumber () != 1)
      {
        // Call synchronous

        // Might add to aErrorMessages
        // Might add to aResponseAttachments
        // Might add to m_aPullReturnUserMsg
        _invokeSPIs (aHttpHeaders,
                     aEbmsUserMessage,
                     aEbmsSignalMessage,
                     aPayloadNode,
                     aDecryptedAttachments,
                     aPMode,
                     aState,
                     aErrorMessages,
                     aResponseAttachments,
                     aSPIResult);
        if (aSPIResult.isFailure ())
          LOGGER.warn ("Error invoking synchronous SPIs");
        else
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Successfully invoked synchronous SPIs");
      }
      else
      {
        // Call asynchronous
        // Only leg1 can be async!

        final Ebms3UserMessage aFinalUserMessage = aEbmsUserMessage;
        final Ebms3SignalMessage aFinalSignalMessage = aEbmsSignalMessage;
        final Node aFinalPayloadNode = aPayloadNode;
        final ICommonsList <WSS4JAttachment> aFinalDecryptedAttachments = aDecryptedAttachments;

        AS4WorkerPool.getInstance ().run ( () -> {
          // Start async
          final ICommonsList <Ebms3Error> aLocalErrorMessages = new CommonsArrayList <> ();
          final ICommonsList <WSS4JAttachment> aLocalResponseAttachments = new CommonsArrayList <> ();

          final SPIInvocationResult aAsyncSPIResult = new SPIInvocationResult ();
          _invokeSPIs (aHttpHeaders,
                       aFinalUserMessage,
                       aFinalSignalMessage,
                       aFinalPayloadNode,
                       aFinalDecryptedAttachments,
                       aPMode,
                       aState,
                       aLocalErrorMessages,
                       aLocalResponseAttachments,
                       aAsyncSPIResult);

          final IAS4ResponseFactory aAsyncResponseFactory;
          if (aAsyncSPIResult.isSuccess ())
          {
            // SPI processing succeeded
            assert aLocalErrorMessages.isEmpty ();

            // The response user message has no explicit payload.
            // All data of the response user message is in the local attachments
            final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSoapVersion,
                                                                                aFinalUserMessage,
                                                                                aLocalResponseAttachments);

            // Send UserMessage or receipt
            final AS4SigningParams aSigningParams = new AS4SigningParams ().setFromPMode (aEffectiveLeg.getSecurity ());
            final String sEncryptionAlias = aFinalUserMessage.getPartyInfo ()
                                                             .getTo ()
                                                             .getPartyIdAtIndex (0)
                                                             .getValue ();
            final AS4CryptParams aCryptParams = new AS4CryptParams ().setFromPMode (aEffectiveLeg.getSecurity ())
                                                                     .setAlias (sEncryptionAlias);
            aAsyncResponseFactory = _createResponseUserMessage (aEffectiveLeg.getProtocol ().getSOAPVersion (),
                                                                aResponseAttachments,
                                                                aResponseUserMsg.getAsSOAPDocument (),
                                                                aResponseUserMsg.getMessagingID (),
                                                                aSigningParams,
                                                                aCryptParams);
          }
          else
          {
            // SPI processing failed
            // Send ErrorMessage
            // Undefined - see https://github.com/phax/ph-as4/issues/4
            final AS4ErrorMessage aResponseErrorMsg = AS4ErrorMessage.create (eSoapVersion,
                                                                              aState.getRefToMessageID (),
                                                                              aLocalErrorMessages);
            aAsyncResponseFactory = new AS4ResponseFactoryXML (aResponseErrorMsg.getAsSOAPDocument (),
                                                               eSoapVersion.getMimeType ());
          }

          // where to send it back (must be determined by SPI!)
          final String sAsyncResponseURL = aAsyncSPIResult.getAsyncResponseURL ();
          if (StringHelper.hasNoText (sAsyncResponseURL))
            throw new IllegalStateException ("No asynchronous response URL present - please check your SPI implementation");

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Responding asynchronous to: " + sAsyncResponseURL);

          // invoke client with new document
          final BasicHttpPoster aSender = new BasicHttpPoster ();
          final Document aAsyncResponse = aSender.sendGenericMessage (sAsyncResponseURL,
                                                                      aAsyncResponseFactory.getHttpEntity (eSoapVersion.getMimeType ()),
                                                                      null,
                                                                      new ResponseHandlerXml ());
          AS4HttpDebug.debug ( () -> "SEND-RESPONSE [async sent] received: " +
                                     XMLWriter.getNodeAsString (aAsyncResponse,
                                                                AS4HttpDebug.getDebugXMLWriterSettings ()));
        });
      }
    }

    // Try building error message
    if (aEbmsError == null)
    {
      // Not an incoming Ebms Error Message
      if (aErrorMessages.isNotEmpty ())
      {
        // Call optional consumer
        if (m_aErrorConsumer != null)
          m_aErrorConsumer.accept (aErrorMessages);

        // Generate ErrorMessage if errors in the process are present and the
        // pmode wants an error response
        // When aLeg == null, the response is true
        if (_isSendErrorAsResponse (aEffectiveLeg))
        {
          final AS4ErrorMessage aErrorMsg = AS4ErrorMessage.create (eSoapVersion,
                                                                    aState.getRefToMessageID (),
                                                                    aErrorMessages);
          return new AS4ResponseFactoryXML (aErrorMsg.getAsSOAPDocument (), eSoapVersion.getMimeType ());
        }
        LOGGER.warn ("Not sending back the error, because sending error response is prohibited in PMode");
      }
      else
      {
        // Do not respond to receipt (except with error message - see above)
        if (aEbmsSignalMessage == null || aEbmsSignalMessage.getReceipt () == null)
        {
          // So now the incoming message is a user message or a pull request
          if (aPMode.getMEP ().isOneWay () || aPMode.getMEPBinding ().isAsynchronous ())
          {
            // If no Error is present check if pmode declared if they want a
            // response and if this response should contain non-repudiation
            // information if applicable
            // Only get in here if pull is part of the EMEPBinding, if it is two
            // way, we need to check if the current application is currently in
            // the pull phase
            if (aPMode.getMEPBinding ().equals (EMEPBinding.PULL) ||
                (aPMode.getMEPBinding ().equals (EMEPBinding.PULL_PUSH) && aSPIResult.hasPullReturnUserMsg ()) ||
                (aPMode.getMEPBinding ().equals (EMEPBinding.PUSH_PULL) && aSPIResult.hasPullReturnUserMsg ()))
            {
              return new AS4ResponseFactoryXML (new AS4UserMessage (eSoapVersion,
                                                                    aSPIResult.getPullReturnUserMsg ()).getAsSOAPDocument (),
                                                eSoapVersion.getMimeType ());
            }

            if (aEbmsUserMessage != null)
            {
              // No errors occurred
              final boolean bSendReceiptAsResponse = _isSendReceiptAsResponse (aEffectiveLeg);

              if (bSendReceiptAsResponse)
              {
                return _createResponseReceiptMessage (aSoapDocument,
                                                      eSoapVersion,
                                                      aEffectiveLeg,
                                                      aEbmsUserMessage,
                                                      aResponseAttachments);
              }
              // else TODO
              LOGGER.info ("Not sending back the receipt response, because sending receipt response is prohibited in PMode");
            }
          }
          else
          {
            // synchronous TWO - WAY (= "SYNC")
            final PModeLeg aLeg2 = aPMode.getLeg2 ();
            if (aLeg2 == null)
              throw new AS4BadRequestException ("PMode has no leg2!");

            if (MEPHelper.isValidResponseTypeLeg2 (aPMode.getMEP (),
                                                   aPMode.getMEPBinding (),
                                                   EAS4MessageType.USER_MESSAGE))
            {
              final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSoapVersion,
                                                                                  aEbmsUserMessage,
                                                                                  aResponseAttachments);

              final AS4SigningParams aSigningParams = new AS4SigningParams ().setFromPMode (aLeg2.getSecurity ());
              final String sEncryptionAlias = aEbmsUserMessage.getPartyInfo ()
                                                              .getTo ()
                                                              .getPartyIdAtIndex (0)
                                                              .getValue ();
              final AS4CryptParams aCryptParams = new AS4CryptParams ().setFromPMode (aLeg2.getSecurity ())
                                                                       .setAlias (sEncryptionAlias);
              return _createResponseUserMessage (aLeg2.getProtocol ().getSOAPVersion (),
                                                 aResponseAttachments,
                                                 aResponseUserMsg.getAsSOAPDocument (),
                                                 aResponseUserMsg.getMessagingID (),
                                                 aSigningParams,
                                                 aCryptParams);
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * @param aHttpHeaders
   *        the HTTP headers of the current request. Never <code>null</code>.
   * @param aRequestInputStream
   *        The InputStream to read the request payload from. Will not be closed
   *        internally. Never <code>null</code>.
   * @param aIncomingDumper
   *        The incoming AS4 dumper. May be <code>null</code>. If
   *        <code>null</code> the global one from {@link AS4DumpManager} is
   *        used.
   * @return the InputStream to be used
   * @throws IOException
   */
  @Nonnull
  private static InputStream _getRequestIS (@Nonnull final HttpHeaderMap aHttpHeaders,
                                            @Nonnull @WillNotClose final InputStream aRequestInputStream,
                                            @Nullable final IAS4IncomingDumper aIncomingDumper) throws IOException
  {
    final IAS4IncomingDumper aDumper = aIncomingDumper != null ? aIncomingDumper : AS4DumpManager.getIncomingDumper ();
    if (aDumper == null)
    {
      // No wrapping needed
      return aRequestInputStream;
    }

    // Dump worthy?
    final OutputStream aOS = aDumper.onNewRequest (aHttpHeaders);
    if (aOS == null)
    {
      // No wrapping needed
      return aRequestInputStream;
    }

    // Read and write at once
    return new WrappedInputStream (aRequestInputStream)
    {
      @Override
      public int read () throws IOException
      {
        final int ret = super.read ();
        if (ret != -1)
        {
          aOS.write (ret & 0xff);
        }
        return ret;
      }

      @Override
      public int read (final byte [] b, final int nOffset, final int nLength) throws IOException
      {
        final int ret = super.read (b, nOffset, nLength);
        if (ret != -1)
        {
          aOS.write (b, nOffset, ret);
        }
        return ret;
      }

      @Override
      public void close () throws IOException
      {
        // Flush and close output stream as well
        StreamHelper.flush (aOS);
        StreamHelper.close (aOS);
        super.close ();
      }
    };
  }

  public void handleRequest (@Nonnull @WillClose final InputStream aServletRequestIS,
                             @Nonnull final HttpHeaderMap aHttpHeaders,
                             @Nonnull final IAS4ResponseAbstraction aHttpResponse) throws AS4BadRequestException,
                                                                                   IOException,
                                                                                   MessagingException,
                                                                                   WSSecurityException
  {
    // Determine content type
    final String sContentType = aHttpHeaders.getFirstHeaderValue (CHttpHeader.CONTENT_TYPE);
    if (StringHelper.hasNoText (sContentType))
      throw new AS4BadRequestException ("Content-Type header is missing");

    final IMimeType aContentType = AcceptMimeTypeHandler.safeParseMimeType (sContentType);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Received Content-Type: " + aContentType);
    if (aContentType == null)
      throw new AS4BadRequestException ("Failed to parse Content-Type '" + sContentType + "'");

    Document aSoapDocument = null;
    ESOAPVersion eSoapVersion = null;
    final ICommonsList <WSS4JAttachment> aIncomingAttachments = new CommonsArrayList <> ();

    final IMimeType aPlainContentType = aContentType.getCopyWithoutParameters ();
    if (aPlainContentType.equals (MT_MULTIPART_RELATED))
    {
      // MIME message
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Received MIME message");

      final String sBoundary = aContentType.getParameterValueWithName ("boundary");
      if (StringHelper.hasNoText (sBoundary))
        throw new AS4BadRequestException ("Content-Type '" + sContentType + "' misses 'boundary' parameter");

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("MIME Boundary = " + sBoundary);

      // Ensure the stream gets closed correctly
      try (final InputStream aRequestIS = _getRequestIS (aHttpHeaders, aServletRequestIS, null))
      {
        // PARSING MIME Message via MultiPartStream
        final MultipartStream aMulti = new MultipartStream (aRequestIS,
                                                            sBoundary.getBytes (StandardCharsets.ISO_8859_1),
                                                            (MultipartProgressNotifier) null);

        int nIndex = 0;
        while (true)
        {
          final boolean bHasNextPart = nIndex == 0 ? aMulti.skipPreamble () : aMulti.readBoundary ();
          if (!bHasNextPart)
            break;

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found MIME part " + nIndex);

          try (final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ())
          {
            // Read headers AND content
            final MimeBodyPart aBodyPart = new MimeBodyPart (aItemIS2);

            if (nIndex == 0)
            {
              // First MIME part -> SOAP document
              final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (aBodyPart.getContentType ())
                                                           .getCopyWithoutParameters ();

              // Determine SOAP version from MIME part content type
              eSoapVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                    x -> aPlainPartMT.equals (x.getMimeType ()));
              if (eSoapVersion == null)
              {
                LOGGER.warn ("Failed to determine SOAP version from Content-Type '" +
                             aPlainPartMT.getAsString () +
                             "'");
                // There is another try down below by reading the payload XML
              }

              // Read SOAP document
              aSoapDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());
            }
            else
            {
              // MIME Attachment (index is gt 0)
              final WSS4JAttachment aAttachment = m_aIAF.createAttachment (aBodyPart, m_aResHelper);
              aIncomingAttachments.add (aAttachment);
            }
          }
          nIndex++;
        }
      }
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Received plain message with Content-Type " + aContentType.getAsString ());

      // Expect plain SOAP - read whole request to DOM
      // Note: this may require a huge amount of memory for large requests
      aSoapDocument = DOMReader.readXMLDOM (_getRequestIS (aHttpHeaders, aServletRequestIS, null));

      if (aSoapDocument != null)
      {
        // Determine SOAP version from the read document
        eSoapVersion = ESOAPVersion.getFromNamespaceURIOrNull (aSoapDocument.getDocumentElement ().getNamespaceURI ());
      }

      if (eSoapVersion == null)
      {
        // Determine SOAP version from content type
        eSoapVersion = ESOAPVersion.getFromMimeTypeOrNull (aPlainContentType);
      }
    }

    if (aSoapDocument == null)
    {
      // We don't have a SOAP document
      throw new AS4BadRequestException (eSoapVersion == null ? "Failed to parse incoming message!"
                                                             : "Failed to parse incoming SOAP " +
                                                               eSoapVersion.getVersion () +
                                                               " document!");
    }

    if (eSoapVersion == null)
    {
      // Determine SOAP version from namespace URI of read document as the
      // last fallback
      final String sNamespaceURI = XMLHelper.getNamespaceURI (aSoapDocument);
      eSoapVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> x.getNamespaceURI ().equals (sNamespaceURI));
      if (eSoapVersion == null)
        throw new AS4BadRequestException ("Failed to determine SOAP version from XML document!");
    }

    // SOAP document and SOAP version are determined
    final IAS4ResponseFactory aResponder = _handleSoapMessage (aHttpHeaders,
                                                               aSoapDocument,
                                                               eSoapVersion,
                                                               aIncomingAttachments);
    if (aResponder != null)
    {
      // Response present -> send back
      aResponder.applyToResponse (aHttpResponse);
    }
    else
    {
      // Success, HTTP No Content
      aHttpResponse.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }
    AS4HttpDebug.debug ( () -> "RECEIVE-END with " + (aResponder != null ? "EBMS message" : "no content"));
  }

  /**
   * This is the main handling routine when called from the Servlet API
   *
   * @param aRequestScope
   *        HTTP request. Never <code>null</code>.
   * @param aHttpResponse
   *        HTTP response. Never <code>null</code>.
   * @throws AS4BadRequestException
   *         in case the request is missing certain prerequisites
   * @throws IOException
   *         In case of IO errors
   * @throws MessagingException
   *         MIME related errors
   * @throws WSSecurityException
   *         In case of WSS4J errors
   * @see #handleRequest(InputStream, HttpHeaderMap, IAS4ResponseAbstraction)
   *      for a more generic API
   */
  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final AS4UnifiedResponse aHttpResponse) throws AS4BadRequestException,
                                                                              IOException,
                                                                              MessagingException,
                                                                              WSSecurityException
  {
    AS4HttpDebug.debug ( () -> "RECEIVE-START at " + aRequestScope.getFullContextAndServletPath ());

    final ServletInputStream aServletRequestIS = aRequestScope.getRequest ().getInputStream ();
    final HttpHeaderMap aHttpHeaders = aRequestScope.headers ().getClone ();
    final IAS4ResponseAbstraction aResponse = IAS4ResponseAbstraction.wrap (aHttpResponse);

    handleRequest (aServletRequestIS, aHttpHeaders, aResponse);
  }
}
