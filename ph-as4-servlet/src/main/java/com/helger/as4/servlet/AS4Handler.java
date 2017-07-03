/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.servlet;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.http.HttpEntity;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.attachment.WSS4JAttachment.IHasAttachmentSourceStream;
import com.helger.as4.client.BasicAS4Sender;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.AS4ErrorMessage;
import com.helger.as4.messaging.domain.AS4ReceiptMessage;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateErrorMessage;
import com.helger.as4.messaging.domain.CreateReceiptMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.EAS4MessageType;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.encrypt.EncryptionCreator;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.MEPHelper;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.profile.IAS4Profile;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.servlet.mgr.AS4ServletMessageProcessorManager;
import com.helger.as4.servlet.soap.AS4SingleSOAPHeader;
import com.helger.as4.servlet.soap.ISOAPHeaderElementProcessor;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartInfo;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.ebms3header.Ebms3PullRequest;
import com.helger.as4lib.ebms3header.Ebms3Receipt;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.http.HTTPStringHelper;
import com.helger.httpclient.response.ResponseHandlerXml;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Process incoming AS4 transmissions.
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
public final class AS4Handler implements Closeable
{
  private static interface IAS4ResponseFactory
  {
    void applyToResponse (@Nonnull ESOAPVersion eSOAPVersion, @Nonnull AS4Response aHttpResponse);

    @Nonnull
    HttpEntity getHttpEntity ();
  }

  private static final class AS4ResponseFactoryXML implements IAS4ResponseFactory
  {
    private final Document m_aDoc;

    public AS4ResponseFactoryXML (@Nonnull final Document aDoc)
    {
      m_aDoc = aDoc;
    }

    public void applyToResponse (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final AS4Response aHttpResponse)
    {
      final String sXML = AS4XMLHelper.serializeXML (m_aDoc);
      aHttpResponse.setContentAndCharset (sXML, AS4XMLHelper.XWS.getCharsetObj ())
                   .setMimeType (eSOAPVersion.getMimeType ());
    }

    @Nonnull
    public HttpEntity getHttpEntity ()
    {
      return new HttpXMLEntity (m_aDoc);
    }
  }

  private static final class AS4ResponseFactoryMIME implements IAS4ResponseFactory
  {
    private final MimeMessage m_aMimeMsg;
    private final ICommonsOrderedMap <String, String> m_aHeaders = new CommonsLinkedHashMap <> ();

    public AS4ResponseFactoryMIME (@Nonnull final MimeMessage aMimeMsg) throws MessagingException
    {
      m_aMimeMsg = aMimeMsg;
      // Move all mime headers to the HTTP request
      final Enumeration <?> aEnum = m_aMimeMsg.getAllHeaders ();
      while (aEnum.hasMoreElements ())
      {
        final Header h = (Header) aEnum.nextElement ();
        // Make a single-line HTTP header value!
        m_aHeaders.put (h.getName (), HTTPStringHelper.getUnifiedHTTPHeaderValue (h.getValue ()));

        // Remove from MIME message!
        m_aMimeMsg.removeHeader (h.getName ());
      }
    }

    public void applyToResponse (@Nonnull final ESOAPVersion eSOAPVersion, @Nonnull final AS4Response aHttpResponse)
    {
      for (final Map.Entry <String, String> aEntry : m_aHeaders.entrySet ())
        aHttpResponse.addCustomResponseHeader (aEntry.getKey (), aEntry.getValue ());

      aHttpResponse.setContent ( () -> {
        try
        {
          return m_aMimeMsg.getInputStream ();
        }
        catch (final IOException | MessagingException ex)
        {
          throw new IllegalStateException ("Failed to get MIME input stream", ex);
        }
      });
      aHttpResponse.setMimeType (MT_MULTIPART_RELATED);
    }

    @Nonnull
    public HttpMimeMessageEntity getHttpEntity ()
    {
      return new HttpMimeMessageEntity (m_aMimeMsg);
    }
  }

  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Handler.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  private final AS4ResourceManager m_aResMgr = new AS4ResourceManager ();
  private Locale m_aLocale = CGlobal.DEFAULT_LOCALE;

  public AS4Handler ()
  {}

  public void close ()
  {
    m_aResMgr.close ();
  }

  @Nonnull
  public Locale getLocale ()
  {
    return m_aLocale;
  }

  @Nonnull
  public AS4Handler setLocale (@Nonnull final Locale aLocale)
  {
    ValueEnforcer.notNull (aLocale, "Locale");
    m_aLocale = aLocale;
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
        final IHasAttachmentSourceStream aOldISP = aIncomingAttachment.getInputStreamProvider ();
        aIncomingAttachment.setSourceStreamProvider ( () -> eCompressionMode.getDecompressStream (aOldISP.getInputStream ()));

        final String sAttachmentContentID = StringHelper.trimStart (aIncomingAttachment.getId (), "attachment=");
        // x.getHref() != null needed since, if a message contains a payload and
        // an attachment, it would throw a NullPointerException since a payload
        // does not have anything written in its partinfo therefor also now href
        final Ebms3PartInfo aPart = CollectionHelper.findFirst (aUserMessage.getPayloadInfo ().getPartInfo (),
                                                                x -> x.getHref () != null &&
                                                                     x.getHref ().contains (sAttachmentContentID));
        if (aPart != null)
        {
          final Ebms3Property aProperty = CollectionHelper.findFirst (aPart.getPartProperties ().getProperty (),
                                                                      x -> x.getName ()
                                                                            .equals (CreateUserMessage.PART_PROPERTY_MIME_TYPE));
          if (aProperty != null)
          {
            aIncomingAttachment.overwriteMimeType (aProperty.getValue ());
          }
        }
      }
    }
  }

  private void _processSOAPHeaderElements (@Nonnull final Document aSOAPDocument,
                                           @Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                           @Nonnull final AS4MessageState aState,
                                           @Nonnull final ICommonsList <Ebms3Error> aErrorMessages) throws BadRequestException
  {
    final ICommonsList <AS4SingleSOAPHeader> aHeaders = new CommonsArrayList <> ();
    {
      // Find SOAP header
      final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                                     eSOAPVersion.getNamespaceURI (),
                                                                     eSOAPVersion.getHeaderElementName ());
      if (aHeaderNode == null)
        throw new BadRequestException ("SOAP document is missing a Header element");

      // Extract all header elements including their mustUnderstand value
      for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
      {
        final QName aQName = XMLHelper.getQName (aHeaderChild);
        final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
        final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
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
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Message contains no SOAP header element with QName " + aQName.toString ());
        continue;
      }

      final ISOAPHeaderElementProcessor aProcessor = aEntry.getValue ();
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

      // Process element
      final ErrorList aErrorList = new ErrorList ();
      if (aProcessor.processHeaderElement (aSOAPDocument,
                                           aHeader.getNode (),
                                           aIncomingAttachments,
                                           aState,
                                           aErrorList,
                                           m_aLocale)
                    .isSuccess ())
      {
        // Mark header as processed (for mustUnderstand check)
        aHeader.setProcessed (true);
      }
      else
      {
        // upon failure, the element stays unprocessed and sends back a signal
        // message with the errors
        s_aLogger.warn ("Failed to process SOAP header element " +
                        aQName.toString () +
                        " with processor " +
                        aProcessor +
                        "; error details: " +
                        aErrorList);

        for (final IError aError : aErrorList)
        {
          Ebms3MessageInfo aMsgInfo = null;
          if (aState.getMessaging () != null)
            if (aState.getMessaging ().hasUserMessageEntries ())
              aMsgInfo = aState.getMessaging ().getUserMessageAtIndex (0).getMessageInfo ();
            else
              if (aState.getMessaging ().hasSignalMessageEntries ())
                aMsgInfo = aState.getMessaging ().getSignalMessageAtIndex (0).getMessageInfo ();
          final String sRefToMessageID = aMsgInfo != null ? aMsgInfo.getMessageId () : "";

          final EEbmsError ePredefinedError = EEbmsError.getFromErrorCodeOrNull (aError.getErrorID ());
          if (ePredefinedError != null)
            aErrorMessages.add (ePredefinedError.getAsEbms3Error (m_aLocale, sRefToMessageID));
          else
          {
            final Ebms3Error aEbms3Error = new Ebms3Error ();
            aEbms3Error.setErrorDetail (aError.getErrorText (m_aLocale));
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
          throw new BadRequestException ("Error processing required SOAP header element " +
                                         aHeader.getQName ().toString ());
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

  /**
   * Invoke custom SPI message processors
   *
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
   * @param aErrorMessages
   *        The list of error messages to be filled if something goes wrong.
   *        Never <code>null</code>.
   * @param aResponseAttachments
   *        The list of attachments to be added to the response. Never
   *        <code>null</code>.
   * @param aPMode
   *        PMode to be used - may be <code>null</code> for Receipt messages.
   * @param aSPIResult
   *        The result object to be filled. May not be <code>null</code>.
   */
  private void _invokeSPIs (@Nullable final Ebms3UserMessage aUserMessage,
                            @Nullable final Ebms3SignalMessage aSignalMessage,
                            @Nullable final Node aPayloadNode,
                            @Nullable final ICommonsList <WSS4JAttachment> aDecryptedAttachments,
                            @Nonnull final ICommonsList <Ebms3Error> aErrorMessages,
                            @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                            @Nullable final IPMode aPMode,
                            @Nonnull final SPIInvocationResult aSPIResult,
                            @Nullable final X509Certificate aCert)
  {
    ValueEnforcer.isTrue (aUserMessage != null || aSignalMessage != null, "User OR Signal Message must be present");
    ValueEnforcer.isFalse (aUserMessage != null &&
                           aSignalMessage != null,
                           "Only one of User OR Signal Message may be present");

    final boolean bIsUserMessage = aUserMessage != null;

    final String sMessageID = bIsUserMessage ? aUserMessage.getMessageInfo ().getMessageId ()
                                             : aSignalMessage.getMessageInfo ().getMessageId ();

    // Invoke all SPIs
    for (final IAS4ServletMessageProcessorSPI aProcessor : AS4ServletMessageProcessorManager.getAllProcessors ())
      try
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Invoking AS4 message processor " + aProcessor);

        // Main processing
        AS4MessageProcessorResult aResult;
        if (bIsUserMessage)
          aResult = aProcessor.processAS4UserMessage (aUserMessage, aPMode, aPayloadNode, aDecryptedAttachments, aCert);
        else
          aResult = aProcessor.processAS4SignalMessage (aSignalMessage, aPMode, aCert);

        // Result returned?
        if (aResult == null)
          throw new IllegalStateException ("No result object present from AS4 SPI processor " + aProcessor);

        if (aResult.isFailure ())
        {
          final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                   aProcessor +
                                   " on '" +
                                   sMessageID +
                                   "' returned a failure: " +
                                   aResult.getErrorMessage ();
          s_aLogger.warn (sErrorMsg);
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
              s_aLogger.warn (sErrorMsg);
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
              // A second processor has commited a response to the pullrequest
              // Which is not allowed since only one response can be sent back
              // to the pullrequest initiator
              if (aPullReturnUserMsg != null)
              {
                final String sErrorMsg = "Invoked AS4 message processor SPI " +
                                         aProcessor +
                                         " on '" +
                                         sMessageID +
                                         "' failed: the previous processor already returned a usermessage; it is not possible to return two usermessage. Please check your SPI implementations.";
                s_aLogger.warn (sErrorMsg);
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
                s_aLogger.warn (sErrorMsg);
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

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Successfully invoked AS4 message processor " + aProcessor);
      }
      catch (final Throwable t)
      {
        // Hack for invalid GZip content from WSS4JAttachment.getSourceStream
        if (t.getCause () instanceof ZipException)
        {
          aErrorMessages.add (EEbmsError.EBMS_DECOMPRESSION_FAILURE.getAsEbms3Error (m_aLocale, sMessageID));
          return;
        }

        // Re-throw
        if (t instanceof RuntimeException)
          throw (RuntimeException) t;
        throw new IllegalStateException ("Error processing incoming AS4 message with processor " + aProcessor, t);
      }

    // Remember success
    aSPIResult.setSuccess (true);
  }

  @Nullable
  private IAS4ResponseFactory _handleSOAPMessage (@Nonnull final Document aSOAPDocument,
                                                  @Nonnull final ESOAPVersion eSOAPVersion,
                                                  @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments) throws WSSecurityException,
                                                                                                                      MessagingException
  {
    ValueEnforcer.notNull (aSOAPDocument, "SOAPDocument");
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aIncomingAttachments, "IncomingAttachments");

    if (s_aLogger.isDebugEnabled ())
    {
      s_aLogger.debug ("Received the following SOAP " + eSOAPVersion.getVersion () + " document:");
      s_aLogger.debug (AS4XMLHelper.serializeXML (aSOAPDocument));
      s_aLogger.debug ("Including the following attachments:");
      s_aLogger.debug (aIncomingAttachments.toString ());
    }

    // Collect all runtime errors
    final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

    // All further operations should only operate on the interface
    IAS4MessageState aState;
    {
      // This is where all data from the SOAP headers is stored to
      final AS4MessageState aStateImpl = new AS4MessageState (eSOAPVersion, m_aResMgr);

      // Handle all headers - the only place where the AS4MessageState values
      _processSOAPHeaderElements (aSOAPDocument, eSOAPVersion, aIncomingAttachments, aStateImpl, aErrorMessages);

      aState = aStateImpl;
    }

    final IPMode aPMode = aState.getPMode ();
    final PModeLeg aEffectiveLeg = aState.getEffectivePModeLeg ();
    final boolean bIsEffectiveLeg1 = aState.getEffectivePModeLegNumber () == 1;
    Ebms3UserMessage aEbmsUserMessage = null;
    Ebms3SignalMessage aEbmsSignalMessage = null;
    Ebms3Error aEbmsError = null;
    Node aPayloadNode = null;
    ICommonsList <WSS4JAttachment> aDecryptedAttachments = null;
    // Storing for two-way response messages
    final ICommonsList <WSS4JAttachment> aResponseAttachments = new CommonsArrayList <> ();
    boolean bCanInvokeSPIs = false;
    String sMessageID = null;
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
      aEbmsError = aEbmsSignalMessage != null && !aEbmsSignalMessage.getError ().isEmpty ()
                                                                                            ? aEbmsSignalMessage.getErrorAtIndex (0)
                                                                                            : null;

      final Ebms3PullRequest aEbmsPullRequest = aEbmsSignalMessage != null ? aEbmsSignalMessage.getPullRequest ()
                                                                           : null;
      final Ebms3Receipt aEbmsReceipt = aEbmsSignalMessage != null ? aEbmsSignalMessage.getReceipt () : null;

      final int nCountData = (aEbmsUserMessage != null ? 1 : 0) +
                             (aEbmsPullRequest != null ? 1 : 0) +
                             (aEbmsReceipt != null ? 1 : 0) +
                             (aEbmsError != null ? 1 : 0);
      // Errors do not count
      if (nCountData != 1)
        throw new BadRequestException ("Exactly one UserMessage or one PullRequest or one Receipt or on Error must be present!");

      // XXX debugging
      if (aEbmsReceipt != null)
      {
        s_aLogger.info ("RECEIPT INCOMING");
      }

      // Ensure the decrypted attachments are used
      aDecryptedAttachments = aState.hasDecryptedAttachments () ? aState.getDecryptedAttachments ()
                                                                : aState.getOriginalAttachments ();

      if (aEbmsUserMessage != null)
      {
        // User message requires PMode
        if (aPMode == null)
          throw new BadRequestException ("No AS4 P-Mode configuration found for user-message!");

        // Only check leg if the message is a usermessage
        if (aEffectiveLeg == null)
          throw new BadRequestException ("No AS4 P-Mode leg could be determined!");

        // Only do profile checks if a profile is set
        sProfileID = AS4ServerConfiguration.getAS4ProfileID ();
        if (StringHelper.hasText (sProfileID))
        {
          final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sProfileID);
          if (aProfile == null)
            throw new IllegalStateException ("The configured AS4 profile " + sProfileID + " does not exist.");

          // Profile Checks gets set when started with Server
          final ErrorList aErrorList = new ErrorList ();
          aProfile.getValidator ().validatePMode (aPMode, aErrorList);
          aProfile.getValidator ().validateUserMessage (aEbmsUserMessage, aErrorList);
          if (aErrorList.isNotEmpty ())
          {
            throw new BadRequestException ("Error validating incoming AS4 message with the profile " +
                                           aProfile.getDisplayName () +
                                           "\n Following errors are present: " +
                                           aErrorList.getAllErrors ().getAllTexts (m_aLocale));
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
          throw new BadRequestException ("No AS4 P-Mode configuration found for pull-request!");

        sMessageID = aEbmsSignalMessage.getMessageInfo ().getMessageId ();
      }

      final boolean bUseDecryptedSOAP = aState.hasDecryptedSOAPDocument ();
      final Document aRealSOAPDoc = bUseDecryptedSOAP ? aState.getDecryptedSOAPDocument () : aSOAPDocument;
      assert aRealSOAPDoc != null;

      // Find SOAP body
      final Node aBodyNode = XMLHelper.getFirstChildElementOfName (aRealSOAPDoc.getDocumentElement (),
                                                                   eSOAPVersion.getNamespaceURI (),
                                                                   eSOAPVersion.getBodyElementName ());
      if (aBodyNode == null)
        throw new BadRequestException ((bUseDecryptedSOAP ? "Decrypted" : "Original") +
                                       " SOAP document is missing a Body element");
      aPayloadNode = aBodyNode.getFirstChild ();

      if (aEbmsUserMessage != null)
      {
        // Check if originalSender and finalRecipient are present
        // Since these two properties are mandatory
        if (aEbmsUserMessage.getMessageProperties () == null)
          throw new BadRequestException ("No Message Properties present but OriginalSender and finalRecipient have to be present");

        final List <Ebms3Property> aProps = aEbmsUserMessage.getMessageProperties ().getProperty ();
        if (aProps.isEmpty ())
          throw new BadRequestException ("Message Property element present but no properties");

        _checkPropertiesOrignalSenderAndFinalRecipient (aProps);
      }

      final boolean bIsDuplicate = MetaAS4Manager.getIncomingDuplicateMgr ()
                                                 .registerAndCheck (sMessageID,
                                                                    sProfileID,
                                                                    aPMode == null ? null : aPMode.getID ())
                                                 .isBreak ();
      if (bIsDuplicate)
      {
        s_aLogger.info ("Not invoking SPIs, because message was already handled!");
        aErrorMessages.add (EEbmsError.EBMS_OTHER.getAsEbms3Error (m_aLocale,
                                                                   sMessageID,
                                                                   "Another message with the same ID was already received!"));
      }
      else
      {
        if (_isNotPingMessage (aPMode))
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

    final SPIInvocationResult aSPIResult = new SPIInvocationResult ();
    if (bCanInvokeSPIs)
    {
      // PMode may be null for receipts
      if (aPMode == null ||
          aPMode.getMEPBinding ().isSynchronous () ||
          aPMode.getMEPBinding ().isAsynchronousInitiator () ||
          !bIsEffectiveLeg1)
      {
        // Call synchronous

        // Might add to aErrorMessages
        // Might add to aResponseAttachments
        // Might add to m_aPullReturnUserMsg
        _invokeSPIs (aEbmsUserMessage,
                     aEbmsSignalMessage,
                     aPayloadNode,
                     aDecryptedAttachments,
                     aErrorMessages,
                     aResponseAttachments,
                     aPMode,
                     aSPIResult,
                     aState.getUsedCertificate ());
        if (aSPIResult.isFailure ())
          s_aLogger.warn ("Error invoking synchronous SPIs");
        else
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Successfully invoked synchronous SPIs");
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
          final ICommonsList <Ebms3Error> aLocalErrorMessages = new CommonsArrayList <> ();
          final ICommonsList <WSS4JAttachment> aLocalResponseAttachments = new CommonsArrayList <> ();
          IAS4ResponseFactory aAsyncResponseFactory;

          final SPIInvocationResult aAsyncSPIResult = new SPIInvocationResult ();
          _invokeSPIs (aFinalUserMessage,
                       aFinalSignalMessage,
                       aFinalPayloadNode,
                       aFinalDecryptedAttachments,
                       aLocalErrorMessages,
                       aLocalResponseAttachments,
                       aPMode,
                       aAsyncSPIResult,
                       aState.getUsedCertificate ());
          if (aAsyncSPIResult.isSuccess ())
          {
            // SPI processing succeeded
            assert aLocalErrorMessages.isEmpty ();

            // The response user message has no explicit payload.
            // All data of the response user message is in the local attachments
            final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSOAPVersion,
                                                                                aFinalUserMessage,
                                                                                aLocalResponseAttachments);

            // Send UserMessage or receipt
            aAsyncResponseFactory = _createResponseUserMessage (aResponseAttachments,
                                                                aEffectiveLeg,
                                                                aResponseUserMsg.getAsSOAPDocument ());

          }
          else
          {
            // SPI processing failed
            // Send ErrorMessage
            // Undefined - see https://github.com/phax/ph-as4/issues/4
            final AS4ErrorMessage aResponseErrorMsg = CreateErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                                             aLocalErrorMessages);
            aAsyncResponseFactory = new AS4ResponseFactoryXML (aResponseErrorMsg.getAsSOAPDocument ());
          }

          // where to send it back (must be determined by SPI!)
          final String sAsyncResponseURL = aAsyncSPIResult.getAsyncResponseURL ();
          if (StringHelper.hasNoText (sAsyncResponseURL))
            throw new IllegalStateException ("No asynchronous response URL present!");

          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Responding asynchronous to: " + sAsyncResponseURL);

          // invoke client with new document
          final BasicAS4Sender aSender = new BasicAS4Sender ();
          final Document aAsyncResponse = aSender.sendGenericMessage (sAsyncResponseURL,
                                                                      aAsyncResponseFactory.getHttpEntity (),
                                                                      new ResponseHandlerXml ());
          AS4HttpDebug.debug ( () -> "SEND-RESPONSE [async sent] received: " +
                                     XMLWriter.getNodeAsString (aAsyncResponse,
                                                                new XMLWriterSettings ().setIndent (EXMLSerializeIndent.NONE)));
        });
      }
    }

    // Try building error message
    if (aEbmsError == null)
    {
      // Not an incoming Ebms Error Message
      if (aErrorMessages.isNotEmpty ())
      {
        // Generate ErrorMessage if errors in the process are present and the
        // pmode wants an error response
        // When aLeg == null, the response is true
        if (_isSendErrorAsResponse (aEffectiveLeg))
        {
          final AS4ErrorMessage aErrorMsg = CreateErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                   MessageHelperMethods.createEbms3MessageInfo (),
                                                                                   aErrorMessages);
          return new AS4ResponseFactoryXML (aErrorMsg.getAsSOAPDocument ());
        }
        s_aLogger.warn ("Not sending back the error, because sending error response is prohibited in PMode");
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
                aPMode.getMEPBinding ().equals (EMEPBinding.PULL_PUSH) && aSPIResult.hasPullReturnUserMsg () ||
                aPMode.getMEPBinding ().equals (EMEPBinding.PUSH_PULL) && aSPIResult.hasPullReturnUserMsg ())
            {
              return new AS4ResponseFactoryXML (new AS4UserMessage (eSOAPVersion,
                                                                    aSPIResult.getPullReturnUserMsg ()).getAsSOAPDocument ());
            }

            if (aEbmsUserMessage != null)
            {
              // No errors occurred
              final boolean bSendReceiptAsResponse = _isSendReceiptAsResponse (aEffectiveLeg);

              if (bSendReceiptAsResponse)
              {
                return _createReceiptMessage (aSOAPDocument,
                                              eSOAPVersion,
                                              aEffectiveLeg,
                                              aEbmsUserMessage,
                                              aResponseAttachments);
              }
              // else TODO
              s_aLogger.info ("Not sending back the receipt response, because sending receipt response is prohibited in PMode");
            }
          }
          else
          {
            // synchronous TWO - WAY (= "SYNC")
            final PModeLeg aLeg2 = aPMode.getLeg2 ();
            if (aLeg2 == null)
              throw new BadRequestException ("PMode has no leg2!");

            if (MEPHelper.isValidResponseTypeLeg2 (aPMode.getMEP (),
                                                   aPMode.getMEPBinding (),
                                                   EAS4MessageType.USER_MESSAGE))
            {
              final AS4UserMessage aResponseUserMsg = _createReversedUserMessage (eSOAPVersion,
                                                                                  aEbmsUserMessage,
                                                                                  aResponseAttachments);

              return _createResponseUserMessage (aResponseAttachments, aLeg2, aResponseUserMsg.getAsSOAPDocument ());
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * @param aSOAPDocument
   * @param eSOAPVersion
   * @param aEffectiveLeg
   * @param aUserMessage
   * @param aResponseAttachments
   * @throws WSSecurityException
   */
  private IAS4ResponseFactory _createReceiptMessage (final Document aSOAPDocument,
                                                     final ESOAPVersion eSOAPVersion,
                                                     final PModeLeg aEffectiveLeg,
                                                     final Ebms3UserMessage aUserMessage,
                                                     final ICommonsList <WSS4JAttachment> aResponseAttachments) throws WSSecurityException
  {
    final AS4ReceiptMessage aReceiptMessage = CreateReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                                         aUserMessage,
                                                                                         aSOAPDocument,
                                                                                         _isSendNonRepudiationInformation (aEffectiveLeg))
                                                                  .setMustUnderstand (true);

    // We've got our response
    Document aResponseDoc = aReceiptMessage.getAsSOAPDocument ();
    aResponseDoc = _signResponseIfNeeded (aResponseAttachments,
                                          aEffectiveLeg.getSecurity (),
                                          aResponseDoc,
                                          aEffectiveLeg.getProtocol ().getSOAPVersion ());
    return new AS4ResponseFactoryXML (aResponseDoc);
  }

  /**
   * Takes an UserMessage and switches properties to reverse the direction. So
   * previously it was C1 => C4, now its C4 => C1 Also adds attachments if there
   * are some that should be added.
   *
   * @param eSOAPVersion
   *        of the message
   * @param aUserMessage
   *        the message that should be reversed
   * @param aResponseAttachments
   *        attachment that should be added
   * @return the reversed usermessage in document form
   */
  @Nonnull
  private static AS4UserMessage _createReversedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                            @Nonnull final Ebms3UserMessage aUserMessage,
                                                            @Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments)
  {
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (MessageHelperMethods.createRandomMessageID (),
                                                                                            aUserMessage.getMessageInfo ()
                                                                                                        .getMessageId ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (null, aResponseAttachments);

    // Invert from and to role from original user message
    final Ebms3PartyInfo aEbms3PartyInfo = CreateUserMessage.createEbms3ReversePartyInfo (aUserMessage.getPartyInfo ());

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

    final AS4UserMessage aResponseUserMessage = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                                     aEbms3PayloadInfo,
                                                                                     aEbms3CollaborationInfo,
                                                                                     aEbms3PartyInfo,
                                                                                     aEbms3MessageProperties,
                                                                                     eSOAPVersion);
    return aResponseUserMessage;
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
   * @param aDoc
   *        the message that should be sent
   * @throws WSSecurityException
   * @throws MessagingException
   */
  @Nonnull
  private IAS4ResponseFactory _createResponseUserMessage (@Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                          @Nonnull final PModeLeg aLeg,
                                                          @Nonnull final Document aDoc) throws WSSecurityException,
                                                                                        MessagingException
  {
    Document aResponseDoc;
    if (aLeg.getSecurity () != null)
    {
      aResponseDoc = _signResponseIfNeeded (aResponseAttachments,
                                            aLeg.getSecurity (),
                                            aDoc,
                                            aLeg.getProtocol ().getSOAPVersion ());
    }
    else
    {
      // No sign
      aResponseDoc = aDoc;
    }

    if (aResponseAttachments.isEmpty ())
      return new AS4ResponseFactoryXML (aResponseDoc);

    final MimeMessage aMimeMsg = _generateMimeMessageForResponse (aResponseAttachments, aLeg, aResponseDoc);
    return new AS4ResponseFactoryMIME (aMimeMsg);
  }

  /**
   * If the PModeLegSecurity has set a Sign and Digest Algorithm the message
   * will be signed, else the message will be returned as it is.
   *
   * @param m_aResMgr
   * @param aResponseAttachments
   * @param aSecurity
   * @param aDocToBeSigned
   * @param eSOAPVersion
   * @return
   * @throws WSSecurityException
   */
  private Document _signResponseIfNeeded (@Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                          @Nonnull final PModeLegSecurity aSecurity,
                                          @Nonnull final Document aDocToBeSigned,
                                          @Nonnull final ESOAPVersion eSOAPVersion) throws WSSecurityException
  {
    if (aSecurity.getX509SignatureAlgorithm () != null && aSecurity.getX509SignatureHashFunction () != null)
    {
      final SignedMessageCreator aCreator = new SignedMessageCreator ();
      final boolean bMustUnderstand = true;
      return aCreator.createSignedMessage (aDocToBeSigned,
                                           eSOAPVersion,
                                           aResponseAttachments,
                                           m_aResMgr,
                                           bMustUnderstand,
                                           aSecurity.getX509SignatureAlgorithm (),
                                           aSecurity.getX509SignatureHashFunction ());
    }
    return aDocToBeSigned;
  }

  /**
   * Returns the MimeMessage with encrypted attachment or without depending on
   * what is configured in the PMode within Leg2.
   *
   * @param aResponseAttachments
   *        The Attachments that should be encrypted
   * @param aLeg
   *        Leg2 to get necessary information, EncryptionAlgorithm, SOAPVersion
   * @param aResponseDoc
   *        the document that contains the user message
   * @return a MimeMessage to be sent
   * @throws MessagingException
   * @throws WSSecurityException
   */
  @Nonnull
  private MimeMessage _generateMimeMessageForResponse (@Nonnull final ICommonsList <WSS4JAttachment> aResponseAttachments,
                                                       @Nonnull final PModeLeg aLeg,
                                                       @Nonnull final Document aResponseDoc) throws WSSecurityException,
                                                                                             MessagingException
  {
    MimeMessage aMimeMsg = null;
    if (aLeg.getSecurity () != null && aLeg.getSecurity ().getX509EncryptionAlgorithm () != null)
    {
      final EncryptionCreator aEncryptCreator = new EncryptionCreator ();
      aMimeMsg = aEncryptCreator.encryptMimeMessage (aLeg.getProtocol ().getSOAPVersion (),
                                                     aResponseDoc,
                                                     true,
                                                     aResponseAttachments,
                                                     m_aResMgr,
                                                     aLeg.getSecurity ().getX509EncryptionAlgorithm ());

    }
    else
    {
      aMimeMsg = new MimeMessageCreator (aLeg.getProtocol ()
                                             .getSOAPVersion ()).generateMimeMessage (aResponseDoc,
                                                                                      aResponseAttachments);
    }
    if (aMimeMsg == null)
      throw new IllegalStateException ("No MimeMessage created!");
    return aMimeMsg;
  }

  /**
   * EBMS core specification 4.2 details these default values. In eSENS they get
   * used to implement a ping service, we took this over even outside of eSENS.
   * If you use these default values you can try to "ping" the server, the
   * method just checks if the pmode got these exact values set. If true, no SPI
   * processing is done.
   *
   * @param aPMode
   *        to check
   * @return true if the default values to ping are not used else false
   */
  private static boolean _isNotPingMessage (@Nonnull final IPMode aPMode)
  {
    if (aPMode != null)
    {
      // Leg 2 wouldn't make sense... Only leg 1 can be pinged
      final PModeLegBusinessInformation aBInfo = aPMode.getLeg1 ().getBusinessInfo ();

      if (aBInfo != null &&
          CAS4.DEFAULT_ACTION_URL.equals (aBInfo.getAction ()) &&
          CAS4.DEFAULT_SERVICE_URL.equals (aBInfo.getService ()))
      {
        return false;
      }
    }
    return true;
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
    if (aLeg.getSecurity () != null)
      if (aLeg.getSecurity ().isSendReceiptNonRepudiationDefined ())
        return aLeg.getSecurity ().isSendReceiptNonRepudiation ();
    // Default behavior
    return false;
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
      if (aLeg.getErrorHandling () != null)
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
      if (aLeg.getSecurity () != null)
      {
        // Note: this is enabled in Default PMode
        return EPModeSendReceiptReplyPattern.RESPONSE.equals (aLeg.getSecurity ().getSendReceiptReplyPattern ());
      }
    // Default behaviour if the value is not set or no security is existing
    return true;
  }

  /**
   * Checks the mandatory properties OriginalSender and FinalRecipient if those
   * two are set.
   *
   * @param aPropertyList
   *        the property list that should be checked for the two specific ones
   * @throws BadRequestException
   *         on error
   */
  private static void _checkPropertiesOrignalSenderAndFinalRecipient (@Nonnull final List <Ebms3Property> aPropertyList) throws BadRequestException
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
      throw new BadRequestException (CAS4.ORIGINAL_SENDER + " property is empty or not existant but mandatory");
    if (StringHelper.hasNoText (sFinalRecipientC4))
      throw new BadRequestException (CAS4.FINAL_RECIPIENT + " property is empty or not existant but mandatory");
  }

  @Nonnull
  private static InputStream _getRequestIS (@Nonnull final HttpServletRequest aHttpServletRequest) throws IOException
  {
    final InputStream aIS = aHttpServletRequest.getInputStream ();
    return aIS;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final AS4Response aHttpResponse) throws BadRequestException,
                                                                       IOException,
                                                                       MessagingException,
                                                                       SAXException,
                                                                       WSSecurityException
  {
    AS4HttpDebug.debug ( () -> "RECEIVE-START at " + aRequestScope.getFullContextAndServletPath ());

    final HttpServletRequest aHttpServletRequest = aRequestScope.getRequest ();

    // Determine content type
    final String sContentType = aHttpServletRequest.getContentType ();
    if (StringHelper.hasNoText (sContentType))
      throw new BadRequestException ("Content-Type header is missing");

    final MimeType aContentType = MimeTypeParser.parseMimeType (sContentType);
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Received Content-Type: " + aContentType);
    if (aContentType == null)
      throw new BadRequestException ("Failed to parse Content-Type '" + sContentType + "'");

    Document aSOAPDocument = null;
    ESOAPVersion eSOAPVersion = null;
    final ICommonsList <WSS4JAttachment> aIncomingAttachments = new CommonsArrayList <> ();

    final IMimeType aPlainContentType = aContentType.getCopyWithoutParameters ();
    if (aPlainContentType.equals (MT_MULTIPART_RELATED))
    {
      // MIME message
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Received MIME message");

      final String sBoundary = aContentType.getParameterValueWithName ("boundary");
      if (StringHelper.hasNoText (sBoundary))
      {
        throw new BadRequestException ("Content-Type '" + sContentType + "' misses boundary parameter");
      }

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("MIME Boundary = " + sBoundary);

      // PARSING MIME Message via MultiPartStream
      final MultipartStream aMulti = new MultipartStream (_getRequestIS (aHttpServletRequest),
                                                          sBoundary.getBytes (StandardCharsets.ISO_8859_1),
                                                          (MultipartProgressNotifier) null);
      final IIncomingAttachmentFactory aIAF = AS4ServerSettings.getIncomingAttachmentFactory ();

      int nIndex = 0;
      while (true)
      {
        final boolean bHasNextPart = nIndex == 0 ? aMulti.skipPreamble () : aMulti.readBoundary ();
        if (!bHasNextPart)
          break;

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Found MIME part " + nIndex);
        final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ();

        final MimeBodyPart aBodyPart = new MimeBodyPart (aItemIS2);
        if (nIndex == 0)
        {
          // First MIME part -> SOAP document
          final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (aBodyPart.getContentType ())
                                                       .getCopyWithoutParameters ();

          // Determine SOAP version from MIME part content type
          eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainPartMT.equals (x.getMimeType ()));

          // Read SOAP document
          aSOAPDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());
        }
        else
        {
          // MIME Attachment (index is gt 0)
          final WSS4JAttachment aAttachment = aIAF.createAttachment (aBodyPart, m_aResMgr);
          aIncomingAttachments.add (aAttachment);
        }
        nIndex++;
      }
    }
    else
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Received plain message with Content-Type " + aContentType.getAsString ());

      // Expect plain SOAP - read whole request to DOM
      // Note: this may require a huge amount of memory for large requests
      aSOAPDocument = DOMReader.readXMLDOM (_getRequestIS (aHttpServletRequest));

      // Determine SOAP version from content type
      eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainContentType.equals (x.getMimeType ()));
    }

    if (aSOAPDocument == null)
    {
      // We don't have a SOAP document
      throw new BadRequestException (eSOAPVersion == null ? "Failed to parse incoming message!"
                                                          : "Failed to parse incoming SOAP " +
                                                            eSOAPVersion.getVersion () +
                                                            " document!");
    }

    if (eSOAPVersion == null)
    {
      // Determine SOAP version from namespace URI of read document as the
      // last fallback
      final String sNamespaceURI = XMLHelper.getNamespaceURI (aSOAPDocument);
      eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> x.getNamespaceURI ().equals (sNamespaceURI));
      if (eSOAPVersion == null)
        throw new BadRequestException ("Failed to determine SOAP version from XML document!");
    }

    // SOAP document and SOAP version are determined
    final IAS4ResponseFactory aResponder = _handleSOAPMessage (aSOAPDocument, eSOAPVersion, aIncomingAttachments);
    if (aResponder != null)
    {
      // Response present -> send back
      aResponder.applyToResponse (eSOAPVersion, aHttpResponse);
    }
    else
    {
      // Success, HTTP No Content
      aHttpResponse.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }
    AS4HttpDebug.debug ( () -> "RECEIVE-END with " + (aResponder != null ? "EBMS message" : "no content"));
  }
}
