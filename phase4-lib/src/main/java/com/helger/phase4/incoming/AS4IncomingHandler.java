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
package com.helger.phase4.incoming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.xml.namespace.QName;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3PartInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.soap.AS4SingleSoapHeader;
import com.helger.phase4.incoming.soap.ISoapHeaderElementProcessor;
import com.helger.phase4.incoming.soap.SoapHeaderElementProcessorRegistry;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessingStatusSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.AS4Helper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.IAS4ProfileValidator;
import com.helger.phase4.profile.IAS4ProfileValidator.EAS4ProfileValidationMode;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;
import com.helger.phase4.util.Phase4Exception;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.sax.WrappedCollectingSAXErrorHandler;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.read.DOMReaderSettings;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

/**
 * Utility methods for incoming AS4 messages.
 *
 * @author Philip Helger
 * @since v0.9.7
 */
public final class AS4IncomingHandler
{
  /**
   * Callback interface for handling the parsing result.
   *
   * @author Philip Helger
   */
  public interface IAS4ParsedMessageCallback
  {
    /**
     * Callback method
     *
     * @param aHttpHeaders
     *        Incoming HTTP headers. Never <code>null</code> but maybe empty.
     * @param aSoapDocument
     *        Parsed SOAP document. Never <code>null</code>.
     * @param eSoapVersion
     *        SOAP version in use. Never <code>null</code>.
     * @param aIncomingAttachments
     *        Incoming attachments. Never <code>null</code> but maybe empty.
     * @throws WSSecurityException
     *         In case of WSS4J errors
     * @throws MessagingException
     *         In case of MIME errors
     * @throws Phase4Exception
     *         In case of a processing error (since 0.9.11)
     */
    void handle (@Nonnull HttpHeaderMap aHttpHeaders,
                 @Nonnull Document aSoapDocument,
                 @Nonnull ESoapVersion eSoapVersion,
                 @Nonnull ICommonsList <WSS4JAttachment> aIncomingAttachments) throws WSSecurityException,
                                                                               MessagingException,
                                                                               Phase4Exception;
  }

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4IncomingHandler.class);

  private AS4IncomingHandler ()
  {}

  public static void parseAS4Message (@Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                      @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                      @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                      @Nonnull @WillClose final InputStream aPayloadIS,
                                      @Nonnull final HttpHeaderMap aHttpHeaders,
                                      @Nonnull final IAS4ParsedMessageCallback aCallback,
                                      @Nullable final IAS4IncomingDumper aIncomingDumper) throws Phase4Exception,
                                                                                          IOException,
                                                                                          MessagingException,
                                                                                          WSSecurityException
  {
    ValueEnforcer.notNull (aIAF, "IncomingAttachmentFactory");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aIncomingMessageMetadata, "IncomingMessageMetadata");
    ValueEnforcer.notNull (aPayloadIS, "PayloadIS");
    ValueEnforcer.notNull (aHttpHeaders, "aHttpHeaders");
    ValueEnforcer.notNull (aCallback, "Callback");

    LOGGER.info ("phase4 --- parsemessage:start");

    // Determine content type
    final String sContentType = aHttpHeaders.getFirstHeaderValue (CHttpHeader.CONTENT_TYPE);
    if (StringHelper.hasNoText (sContentType))
      throw new Phase4Exception ("Content-Type header is missing").setRetryFeasible (false);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Received Content-Type string: '" + sContentType + "'");
    final IMimeType aContentType = MimeTypeParser.safeParseMimeType (sContentType);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Received Content-Type object: " + aContentType);
    if (aContentType == null)
      throw new Phase4Exception ("Failed to parse Content-Type '" + sContentType + "'").setRetryFeasible (false);
    final IMimeType aPlainContentType = aContentType.getCopyWithoutParameters ();

    // Fallback to global dumper if none is provided
    final IAS4IncomingDumper aRealIncomingDumper = aIncomingDumper != null ? aIncomingDumper : AS4DumpManager
                                                                                                             .getIncomingDumper ();

    Document aSoapDocument = null;
    ESoapVersion eSoapVersion = null;
    final ICommonsList <WSS4JAttachment> aIncomingAttachments = new CommonsArrayList <> ();
    final Wrapper <OutputStream> aDumpOSHolder = new Wrapper <> ();
    Exception aCaughtException = null;

    // Load all SPIs
    final ICommonsList <IAS4IncomingMessageProcessingStatusSPI> aStatusSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4IncomingMessageProcessingStatusSPI.class);
    for (final IAS4IncomingMessageProcessingStatusSPI aStatusSPI : aStatusSPIs)
      try
      {
        aStatusSPI.onMessageProcessingStarted (aIncomingMessageMetadata);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("IAS4IncomingMessageProcessingStatusSPI.onMessageProcessingStarted failed. SPI=" +
                      aStatusSPI +
                      "; MessageMetadata=" +
                      aIncomingMessageMetadata,
                      ex);
      }

    try
    {
      ErrorList aXSDErrorList = new ErrorList ();

      if (aPlainContentType.equals (AS4RequestHandler.MT_MULTIPART_RELATED))
      {
        // MIME message
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Received MIME message");

        final String sBoundary = aContentType.getParameterValueWithName ("boundary");
        if (StringHelper.hasNoText (sBoundary))
          throw new Phase4Exception ("Content-Type '" + sContentType + "' misses 'boundary' parameter")
                                                                                                       .setRetryFeasible (false);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("MIME Boundary: '" + sBoundary + "'");

        // Ensure the stream gets closed correctly
        // This methods opens the stream for the incoming dump
        // Note: This closes the incoming dump stream, when InputStream is
        // closed
        try (final InputStream aRequestIS = AS4DumpManager.getIncomingDumpAwareInputStream (aRealIncomingDumper,
                                                                                            aPayloadIS,
                                                                                            aIncomingMessageMetadata,
                                                                                            aHttpHeaders,
                                                                                            aDumpOSHolder))
        {
          // PARSING MIME Message via MultipartStream
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
              LOGGER.debug ("Found MIME part #" + nIndex);

            try (final MultipartItemInputStream aBodyPartIS = aMulti.createInputStream ())
            {
              // Read headers AND content
              final MimeBodyPart aBodyPart = new MimeBodyPart (aBodyPartIS);

              if (nIndex == 0)
              {
                // First MIME part -> SOAP document
                if (LOGGER.isDebugEnabled ())
                  LOGGER.debug ("Parsing first MIME part as SOAP document");

                // Read SOAP document
                aSoapDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream (),
                                                      new DOMReaderSettings ().setErrorHandler (new WrappedCollectingSAXErrorHandler (aXSDErrorList)));

                IMimeType aPlainPartMT = MimeTypeParser.safeParseMimeType (aBodyPart.getContentType ());
                if (aPlainPartMT != null)
                  aPlainPartMT = aPlainPartMT.getCopyWithoutParameters ();

                // Determine SOAP version from MIME part content type
                eSoapVersion = ESoapVersion.getFromMimeTypeOrNull (aPlainPartMT);
                if (eSoapVersion != null && LOGGER.isDebugEnabled ())
                  LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from Content-Type");

                if (eSoapVersion == null && aSoapDocument != null)
                {
                  // Determine SOAP version from the read document
                  eSoapVersion = ESoapVersion.getFromNamespaceURIOrNull (XMLHelper.getNamespaceURI (aSoapDocument));
                  if (eSoapVersion != null && LOGGER.isDebugEnabled ())
                    LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from XML root element namespace URI");
                }
              }
              else
              {
                // MIME Attachment (index is gt 0)
                if (LOGGER.isDebugEnabled ())
                  LOGGER.debug ("Parsing MIME part #" + nIndex + " as attachment");

                final WSS4JAttachment aAttachment = aIAF.createAttachment (aBodyPart, aResHelper);
                aIncomingAttachments.add (aAttachment);
              }
            }
            nIndex++;
          }
        }
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Read MIME message with " + aIncomingAttachments.size () + " attachment(s)");
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Received plain message");

        // Expect plain SOAP - read whole request to DOM
        // This methods opens the stream for the incoming dump
        // Note: this may require a huge amount of memory for large requests
        // Note: This closes the incoming dump stream, when InputStream is
        // closed
        aSoapDocument = DOMReader.readXMLDOM (AS4DumpManager.getIncomingDumpAwareInputStream (aRealIncomingDumper,
                                                                                              aPayloadIS,
                                                                                              aIncomingMessageMetadata,
                                                                                              aHttpHeaders,
                                                                                              aDumpOSHolder),
                                              new DOMReaderSettings ().setErrorHandler (new WrappedCollectingSAXErrorHandler (aXSDErrorList)));

        if (LOGGER.isDebugEnabled ())
        {
          if (aSoapDocument != null)
            LOGGER.debug ("Successfully parsed payload as XML");
          else
            LOGGER.debug ("Failed to parse payload as XML");
        }

        if (aSoapDocument != null)
        {
          // Determine SOAP version from the read document
          final String sNamespaceURI = XMLHelper.getNamespaceURI (aSoapDocument);
          eSoapVersion = ESoapVersion.getFromNamespaceURIOrNull (sNamespaceURI);
          if (eSoapVersion != null)
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Determined SOAP version " +
                            eSoapVersion +
                            " from XML root element namespace URI '" +
                            sNamespaceURI +
                            "'");
          }
          else
            LOGGER.warn ("Failed to determine SOAP version from XML root element namespace URI '" +
                         sNamespaceURI +
                         "'");
        }

        if (eSoapVersion == null)
        {
          // Determine SOAP version from content type
          eSoapVersion = ESoapVersion.getFromMimeTypeOrNull (aPlainContentType);
          if (eSoapVersion != null)
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Determined SOAP version " +
                            eSoapVersion +
                            " from Content-Type '" +
                            aPlainContentType.getAsString () +
                            "'");
          }
          else
            LOGGER.warn ("Failed to determine SOAP version from Content-Type '" +
                         aPlainContentType.getAsString () +
                         "'");
        }
      }

      if (aSoapDocument == null || aXSDErrorList.containsAtLeastOneError ())
      {
        // We don't have a SOAP document
        StringBuilder aErrorMessage = new StringBuilder ();
        aErrorMessage.append (eSoapVersion == null ? "Failed to parse incoming message!"
                                                   : "Failed to parse incoming SOAP " +
                                                     eSoapVersion.getVersion () +
                                                     " document!");
        if (aXSDErrorList.isNotEmpty ())
        {
          aErrorMessage.append (" Technical details:");
          for (IError aError : aXSDErrorList)
            aErrorMessage.append ('\n').append (aError.getAsStringLocaleIndepdent ());
        }
        throw new Phase4Exception (aErrorMessage.toString ()).setRetryFeasible (false);
      }

      if (eSoapVersion == null)
      {
        // We're missing a SOAP version
        throw new Phase4Exception ("Failed to determine SOAP version of XML document!").setRetryFeasible (false);
      }

      // Main processing
      aCallback.handle (aHttpHeaders, aSoapDocument, eSoapVersion, aIncomingAttachments);
    }
    catch (final Phase4Exception | IOException | MessagingException | WSSecurityException ex)
    {
      // Remember for callback
      aCaughtException = ex;
      throw ex;
    }
    finally
    {
      // Here, the incoming dump is finally written, closed and usable
      if (aRealIncomingDumper != null && aDumpOSHolder.isSet ())
        try
        {
          aRealIncomingDumper.onEndRequest (aIncomingMessageMetadata, aCaughtException);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("IncomingDumper.onEndRequest failed. Dumper=" +
                        aRealIncomingDumper +
                        "; MessageMetadata=" +
                        aIncomingMessageMetadata,
                        ex);
        }

      // Inform interested parties about the end of processing
      for (final IAS4IncomingMessageProcessingStatusSPI aStatusSPI : aStatusSPIs)
        try
        {
          aStatusSPI.onMessageProcessingEnded (aIncomingMessageMetadata, aCaughtException);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("IAS4IncomingMessageProcessingStatusSPI.onMessageProcessingEnded failed. SPI=" +
                        aStatusSPI +
                        "; MessageMetadata=" +
                        aIncomingMessageMetadata,
                        ex);
        }

      LOGGER.info ("phase4 --- parsemessage:end");
    }
  }

  private static void _processSoapHeaderElements (@Nonnull final SoapHeaderElementProcessorRegistry aRegistry,
                                                  @Nonnull final Document aSoapDocument,
                                                  @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                  @Nonnull final AS4IncomingMessageState aIncomingState,
                                                  @Nonnull final ICommonsList <Ebms3Error> aEbmsErrorMessagesTarget) throws Phase4Exception
  {
    final ESoapVersion eSoapVersion = aIncomingState.getSoapVersion ();
    final ICommonsList <AS4SingleSoapHeader> aHeadersInMessage = new CommonsArrayList <> ();
    {
      // Find SOAP header
      final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSoapDocument.getDocumentElement (),
                                                                     eSoapVersion.getNamespaceURI (),
                                                                     eSoapVersion.getHeaderElementName ());
      if (aHeaderNode == null)
        throw new Phase4Exception ("SOAP document is missing a Header element {" +
                                   eSoapVersion.getNamespaceURI () +
                                   "}" +
                                   eSoapVersion.getHeaderElementName ()).setRetryFeasible (false);

      // Extract all header elements including their "mustUnderstand" value
      for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
      {
        final QName aQName = XMLHelper.getQName (aHeaderChild);
        final String sMustUnderstand = aHeaderChild.getAttributeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
        final boolean bIsMustUnderstand = eSoapVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
        aHeadersInMessage.add (new AS4SingleSoapHeader (aHeaderChild, aQName, bIsMustUnderstand));
      }
    }

    final ICommonsOrderedMap <QName, ISoapHeaderElementProcessor> aAllRegisteredProcessors = aRegistry.getAllElementProcessors ();
    if (aAllRegisteredProcessors.isEmpty ())
      LOGGER.error ("No SOAP Header element processor is registered");

    // handle all headers in the order of the registered handlers!
    for (final Map.Entry <QName, ISoapHeaderElementProcessor> aEntry : aAllRegisteredProcessors.entrySet ())
    {
      final QName aQName = aEntry.getKey ();

      // Check if this message contains a header for the current handler
      final AS4SingleSoapHeader aHeader = aHeadersInMessage.findFirst (x -> aQName.equals (x.getQName ()));
      if (aHeader == null)
      {
        // no header element for current processor
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Message contains no SOAP header element with QName " + aQName.toString ());
        continue;
      }

      final ISoapHeaderElementProcessor aProcessor = aEntry.getValue ();
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

      // Error list for this processor
      final ICommonsList <Ebms3Error> aProcessingErrorMessagesTarget = new CommonsArrayList <> ();

      try
      {
        // Process element
        if (aProcessor.processHeaderElement (aSoapDocument,
                                             aHeader.getNode (),
                                             aIncomingAttachments,
                                             aIncomingState,
                                             aProcessingErrorMessagesTarget).isSuccess ())
        {
          // Mark header as processed (for mustUnderstand check)
          aHeader.setProcessed (true);
        }
        else
        {
          // upon failure, the element stays unprocessed and sends back a signal
          // message with the errors
          LOGGER.error ("Failed to process SOAP header element " +
                        aQName.toString () +
                        " with processor " +
                        aProcessor +
                        "; error details: " +
                        aProcessingErrorMessagesTarget);

          // Remember all errors from this processor
          aEbmsErrorMessagesTarget.addAll (aProcessingErrorMessagesTarget);

          // Stop processing of other headers
          break;
        }
      }
      catch (final Exception ex)
      {
        // upon failure, the element stays unprocessed and sends back a signal
        // message with the errors
        final String sDetails = "Error processing SOAP header element " +
                                aQName.toString () +
                                " with processor " +
                                aProcessor;
        LOGGER.error (sDetails, ex);
        aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_OTHER.errorBuilder (aIncomingState.getLocale ())
                                                           .refToMessageInError (aIncomingState.getMessageID ())
                                                           .errorDetail (sDetails, ex)
                                                           .build ());
        // Stop processing of other headers
        break;
      }
    }

    // If an error message is present, send it back gracefully
    if (aEbmsErrorMessagesTarget.isEmpty ())
    {
      // Now check if all must understand headers were processed
      // Are all must-understand headers processed?
      for (final AS4SingleSoapHeader aHeader : aHeadersInMessage)
        if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
          throw new Phase4Exception ("Required SOAP header element " +
                                     aHeader.getQName ().toString () +
                                     " could not be handled").setRetryFeasible (false);
    }
  }

  private static void _decompressAttachments (@Nonnull final ICommonsList <WSS4JAttachment> aIncomingDecryptedAttachments,
                                              @Nonnull final Ebms3UserMessage aUserMessage,
                                              @Nonnull final IAS4IncomingMessageState aIncomingState)
  {
    // For all incoming attachments
    for (final WSS4JAttachment aIncomingAttachment : aIncomingDecryptedAttachments.getClone ())
    {
      final EAS4CompressionMode eCompressionMode = aIncomingState.getAttachmentCompressionMode (aIncomingAttachment.getId ());
      if (eCompressionMode != null)
      {
        final IHasInputStream aOldISP = aIncomingAttachment.getInputStreamProvider ();
        aIncomingAttachment.setSourceStreamProvider (new HasInputStream ( () -> {
          try
          {
            final InputStream aSrcIS = aOldISP.getInputStream ();
            if (aSrcIS == null)
              throw new IllegalStateException ("Failed to create InputStream from " + aOldISP);

            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Decompressing attachment with ID '" +
                            aIncomingAttachment.getId () +
                            "' using " +
                            eCompressionMode);
            return eCompressionMode.getDecompressStream (aSrcIS);
          }
          catch (final IOException ex)
          {
            // This is e.g. invoked, if the GZIP decompression failed because of
            // invalid payload
            throw new AS4DecompressException (ex);
          }
        }, aOldISP.isReadMultiple ()));

        // Remember the compression mode
        aIncomingAttachment.setCompressionMode (eCompressionMode);

        final String sAttachmentContentID = StringHelper.trimStart (aIncomingAttachment.getId (), "attachment=");
        // x.getHref() != null needed since, if a message contains a payload and
        // an attachment, it would throw a NullPointerException since a payload
        // does not have anything written in its partinfo therefore also now
        // href
        final Ebms3PartInfo aPartInfo = CollectionHelper.findFirst (aUserMessage.getPayloadInfo ().getPartInfo (),
                                                                    x -> x.getHref () != null &&
                                                                         (x.getHref ().equals (sAttachmentContentID) ||
                                                                          x.getHref ()
                                                                           .equals (MessageHelperMethods.PREFIX_CID +
                                                                                    sAttachmentContentID)));
        if (aPartInfo != null && aPartInfo.getPartProperties () != null)
        {
          // Find "MimeType" property
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

  @Nonnull
  public static IAS4IncomingMessageState processEbmsMessage (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                             @Nonnull final Locale aLocale,
                                                             @Nonnull final SoapHeaderElementProcessorRegistry aRegistry,
                                                             @Nonnull final HttpHeaderMap aHttpHeaders,
                                                             @Nonnull final Document aSoapDocument,
                                                             @Nonnull final ESoapVersion eSoapVersion,
                                                             @Nonnull final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                             @Nonnull final IAS4IncomingProfileSelector aAS4ProfileSelector,
                                                             @Nonnull final ICommonsList <Ebms3Error> aEbmsErrorMessagesTarget,
                                                             @Nonnull final IAS4IncomingMessageMetadata aMessageMetadata) throws Phase4Exception
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aLocale, "Locale");
    ValueEnforcer.notNull (aHttpHeaders, "HttpHeaders");
    ValueEnforcer.notNull (aSoapDocument, "SoapDocument");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aIncomingAttachments, "IncomingAttachments");
    ValueEnforcer.notNull (aAS4ProfileSelector, "AS4ProfileSelector");
    ValueEnforcer.notNull (aEbmsErrorMessagesTarget, "EbmsErrorMessagesTarget");
    ValueEnforcer.notNull (aMessageMetadata, "MessageMetadata");

    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug ("Received the following SOAP " + eSoapVersion.getVersion () + " document:");
      LOGGER.debug (AS4XMLHelper.serializeXML (aSoapDocument));
      if (aIncomingAttachments.isEmpty ())
      {
        LOGGER.debug ("Without any incoming attachments");
      }
      else
      {
        LOGGER.debug ("Including the following " + aIncomingAttachments.size () + " attachments:");
        LOGGER.debug (aIncomingAttachments.toString ());
      }
    }

    // This is where all data from the SOAP headers is stored to
    final AS4IncomingMessageState aIncomingState = new AS4IncomingMessageState (eSoapVersion, aResHelper, aLocale);

    // Handle all headers - modifies the state
    _processSoapHeaderElements (aRegistry,
                                aSoapDocument,
                                aIncomingAttachments,
                                aIncomingState,
                                aEbmsErrorMessagesTarget);

    // Here we know, if the message was signed and/or decrypted

    // Remember if header processing was successful or not
    final boolean bSoapHeaderElementProcessingSuccess = aEbmsErrorMessagesTarget.isEmpty ();
    aIncomingState.setSoapHeaderElementProcessingSuccessful (bSoapHeaderElementProcessingSuccess);
    if (bSoapHeaderElementProcessingSuccess)
    {
      // Every message can only contain 1 User message or 1 pull message
      // aUserMessage can be null on incoming Pull-Message!
      final Ebms3UserMessage aEbmsUserMessage = aIncomingState.getEbmsUserMessage ();
      final Ebms3SignalMessage aEbmsSignalMessage = aIncomingState.getEbmsSignalMessage ();
      final Ebms3Error aEbmsError = aIncomingState.getEbmsError ();
      final Ebms3PullRequest aEbmsPullRequest = aIncomingState.getEbmsPullRequest ();
      final Ebms3Receipt aEbmsReceipt = aIncomingState.getEbmsReceipt ();

      // Check payload consistency
      final int nCountData = (aEbmsUserMessage != null ? 1 : 0) +
                             (aEbmsPullRequest != null ? 1 : 0) +
                             (aEbmsReceipt != null ? 1 : 0) +
                             (aEbmsError != null ? 1 : 0);
      if (nCountData != 1)
      {
        final String sDetails = "Expected a UserMessage(" +
                                (aEbmsUserMessage != null ? 1 : 0) +
                                "), a PullRequest(" +
                                (aEbmsPullRequest != null ? 1 : 0) +
                                "), a Receipt(" +
                                (aEbmsReceipt != null ? 1 : 0) +
                                ") or an Error(" +
                                (aEbmsError != null ? 1 : 0) +
                                ")";
        LOGGER.error (sDetails);

        // send EBMS:0001 error back
        aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_VALUE_NOT_RECOGNIZED.errorBuilder (aLocale)
                                                                          .refToMessageInError (aIncomingState.getMessageID ())
                                                                          .errorDetail (sDetails)
                                                                          .build ());
      }

      // Determine AS4 profile ID (since 0.13.0)
      final String sProfileID = aAS4ProfileSelector.getAS4ProfileID (aIncomingState);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Determined AS4 profile ID '" + sProfileID + "' for current message");

      final IPMode aPMode = aIncomingState.getPMode ();
      final PModeLeg aEffectiveLeg = aIncomingState.getEffectivePModeLeg ();

      final IAS4Profile aProfile;
      final IAS4ProfileValidator aValidator;
      // Only do profile checks if a profile is set
      if (StringHelper.hasText (sProfileID))
      {
        // Resolve profile ID
        aProfile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sProfileID);
        if (aProfile == null)
          throw new IllegalStateException ("The configured AS4 profile '" + sProfileID + "' does not exist.");

        aIncomingState.setAS4Profile (aProfile);

        // Profile Checks gets set when started with Server
        aValidator = aProfile.getValidator ();
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4 state contains no AS4 profile ID - therefore no consistency checks are performed");

        aProfile = null;
        aValidator = null;
      }

      if (aEbmsUserMessage != null)
      {
        // User message requires PMode
        if (aPMode == null)
          throw new Phase4Exception ("No AS4 P-Mode configuration found for UserMessage!");

        // Only check leg if the message is a usermessage
        if (aEffectiveLeg == null)
          throw new Phase4Exception ("No AS4 P-Mode leg could be determined!");

        // Only do profile checks if a profile is set
        // Profile Checks gets set when started with Server
        if (aValidator != null)
        {
          if (aAS4ProfileSelector.validateAgainstProfile ())
          {
            final ErrorList aErrorList = new ErrorList ();
            aValidator.validatePMode (aPMode, aErrorList, EAS4ProfileValidationMode.USER_MESSAGE);
            aValidator.validateUserMessage (aEbmsUserMessage, aErrorList);
            aValidator.validateInitiatorIdentity (aEbmsUserMessage,
                                                  aIncomingState.getSigningCertificate (),
                                                  aMessageMetadata,
                                                  aErrorList);

            if (aErrorList.containsAtLeastOneError ())
            {
              LOGGER.error ("Error validating incoming AS4 UserMessage with the profile '" +
                            aProfile.getDisplayName () +
                            "'");

              for (final IError aError : aErrorList)
              {
                final String sDetails = aError.getAsString (aLocale);
                if (aError.isError ())
                {
                  LOGGER.error (sDetails);
                  aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                        .refToMessageInError (aIncomingState.getMessageID ())
                                                                                        .errorDetail (sDetails,
                                                                                                      aError.getLinkedException ())
                                                                                        .build ());
                }
                else
                  LOGGER.warn (sDetails);
              }

              // Was previously a thrown exception - that's why we break heer
              return aIncomingState;
            }
          }
          else
          {
            LOGGER.warn ("The AS4 profile '" +
                         sProfileID +
                         "' has a validation configured, but the usage was disabled using the IAS4IncomingProfileSelector");
          }
        }

        // Ensure the decrypted attachments are used
        final ICommonsList <WSS4JAttachment> aDecryptedAttachments = aIncomingState.hasDecryptedAttachments () ? aIncomingState.getDecryptedAttachments ()
                                                                                                               : aIncomingState.getOriginalAttachments ();

        // Decompress attachments (if compressed)
        // Result is directly in the decrypted attachments list!
        _decompressAttachments (aDecryptedAttachments, aEbmsUserMessage, aIncomingState);
      }
      else
      {
        // Signal message

        // Pull-request also requires PMode
        if (aEbmsPullRequest != null)
          if (aPMode == null)
            throw new Phase4Exception ("No AS4 P-Mode configuration found for PullRequest!");

        if (aValidator != null)
        {
          if (aAS4ProfileSelector.validateAgainstProfile ())
          {
            final ErrorList aErrorList = new ErrorList ();
            if (aPMode != null)
              aValidator.validatePMode (aPMode, aErrorList, EAS4ProfileValidationMode.SIGNAL_MESSAGE);
            aValidator.validateSignalMessage (aEbmsSignalMessage, aErrorList);

            if (aErrorList.containsAtLeastOneError ())
            {
              LOGGER.error ("Error validating incoming AS4 SignalMessage with the profile '" +
                            aProfile.getDisplayName () +
                            "'");

              for (final IError aError : aErrorList)
              {
                final String sDetails = aError.getAsString (aLocale);
                if (aError.isError ())
                {
                  LOGGER.error (sDetails);
                  aEbmsErrorMessagesTarget.add (EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.errorBuilder (aLocale)
                                                                                        .refToMessageInError (aIncomingState.getMessageID ())
                                                                                        .errorDetail (sDetails)
                                                                                        .build ());
                }
                else
                  LOGGER.warn (sDetails);
              }

              // Was previously a thrown exception - that's why we break heer
              return aIncomingState;
            }
          }
          else
          {
            LOGGER.warn ("The AS4 profile '" +
                         sProfileID +
                         "' has a validation configured, but the usage was disabled using the AS4ProfileSelector");
          }
        }
      }

      final boolean bUseDecryptedSoap = aIncomingState.hasDecryptedSoapDocument ();
      final Document aRealSoapDoc = bUseDecryptedSoap ? aIncomingState.getDecryptedSoapDocument () : aSoapDocument;
      assert aRealSoapDoc != null;

      // Find SOAP body (mandatory according to SOAP XSD)
      final Node aBodyNode = XMLHelper.getFirstChildElementOfName (aRealSoapDoc.getDocumentElement (),
                                                                   eSoapVersion.getNamespaceURI (),
                                                                   eSoapVersion.getBodyElementName ());
      if (aBodyNode == null)
        throw new Phase4Exception ((bUseDecryptedSoap ? "Decrypted" : "Original") +
                                   " SOAP document is missing a Body element").setRetryFeasible (false);

      aIncomingState.setSoapBodyPayloadNode (aBodyNode.getFirstChild ());

      final boolean bIsPingMessage = AS4Helper.isPingMessage (aPMode);
      aIncomingState.setPingMessage (bIsPingMessage);
    }

    return aIncomingState;
  }

  @Nullable
  private static IAS4IncomingMessageState _parseMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                         @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                         @Nonnull final IAS4PModeResolver aPModeResolver,
                                                         @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                         @Nonnull final IAS4IncomingProfileSelector aAS4ProfileSelector,
                                                         @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                         @Nullable final IPMode aSendingPMode,
                                                         @Nonnull final Locale aLocale,
                                                         @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                         @Nonnull final HttpResponse aHttpResponse,
                                                         @Nonnull final byte [] aMessagePayload,
                                                         @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                         @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                         @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration) throws Phase4Exception
  {
    // This wrapper will take the result
    final Wrapper <IAS4IncomingMessageState> aRetWrapper = new Wrapper <> ();

    // Handler for the parsed message
    final IAS4ParsedMessageCallback aCallback = (aHttpHeaders, aSoapDocument, eSoapVersion, aIncomingAttachments) -> {
      final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList <> ();

      // Use the sending PMode as fallback, because from the incoming
      // receipt/error it is impossible to detect a PMode
      final SoapHeaderElementProcessorRegistry aRegistry = SoapHeaderElementProcessorRegistry.createDefault (aPModeResolver,
                                                                                                             aCryptoFactorySign,
                                                                                                             aCryptoFactoryCrypt,
                                                                                                             aSendingPMode,
                                                                                                             aIncomingSecurityConfiguration,
                                                                                                             aIncomingReceiverConfiguration);

      // Parse AS4, verify signature etc
      final IAS4IncomingMessageState aIncomingState = processEbmsMessage (aResHelper,
                                                                          aLocale,
                                                                          aRegistry,
                                                                          aHttpHeaders,
                                                                          aSoapDocument,
                                                                          eSoapVersion,
                                                                          aIncomingAttachments,
                                                                          aAS4ProfileSelector,
                                                                          aErrorMessages,
                                                                          aIncomingMessageMetadata);

      if (aIncomingState.isSoapHeaderElementProcessingSuccessful ())
      {
        // Remember the parsed signal message
        aRetWrapper.set (aIncomingState);
      }
      else
      {
        throw new Phase4Exception ("Error processing AS4 message", aIncomingState.getSoapWSS4JException ())
                                                                                                           .setRetryFeasible (false);
      }
    };

    // Create header map from response headers
    final HttpHeaderMap aHttpHeaders = new HttpHeaderMap ();
    for (final Header aHeader : aHttpResponse.getHeaders ())
      aHttpHeaders.addHeader (aHeader.getName (), aHeader.getValue ());

    try (final NonBlockingByteArrayInputStream aPayloadIS = new NonBlockingByteArrayInputStream (aMessagePayload))
    {
      // Parse incoming message
      parseAS4Message (aIAF,
                       aResHelper,
                       aIncomingMessageMetadata,
                       aPayloadIS,
                       aHttpHeaders,
                       aCallback,
                       aIncomingDumper);
    }
    catch (final Phase4Exception ex)
    {
      throw ex;
    }
    catch (final Exception ex)
    {
      throw new Phase4Exception ("Error parsing AS4 message", ex);
    }

    // This one contains the result
    return aRetWrapper.get ();
  }

  // Parse an AS4 SignalMessage
  @Nullable
  public static Ebms3SignalMessage parseSignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                       @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                       @Nonnull final IAS4PModeResolver aPModeResolver,
                                                       @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                       @Nonnull final IAS4IncomingProfileSelector aAS4ProfileSelector,
                                                       @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                       @Nullable final IPMode aSendingPMode,
                                                       @Nonnull final Locale aLocale,
                                                       @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                       @Nonnull final HttpResponse aHttpResponse,
                                                       @Nonnull final byte [] aMessagePayload,
                                                       @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                       @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                       @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                       @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer) throws Phase4Exception
  {
    final IAS4IncomingMessageState aIncomingState = _parseMessage (aCryptoFactorySign,
                                                                   aCryptoFactoryCrypt,
                                                                   aPModeResolver,
                                                                   aIAF,
                                                                   aAS4ProfileSelector,
                                                                   aResHelper,
                                                                   aSendingPMode,
                                                                   aLocale,
                                                                   aIncomingMessageMetadata,
                                                                   aHttpResponse,
                                                                   aMessagePayload,
                                                                   aIncomingDumper,
                                                                   aIncomingSecurityConfiguration,
                                                                   aIncomingReceiverConfiguration);
    if (aIncomingState == null)
    {
      // Error message was already logged
      return null;
    }

    final Ebms3SignalMessage ret = aIncomingState.getEbmsSignalMessage ();
    if (ret == null)
    {
      if (aIncomingState.getEbmsUserMessage () != null)
        LOGGER.warn ("A Message state is present, but it contains a UserMessage instead of a SignalMessage.");
      else
        LOGGER.warn ("A Message state is present, but it contains neither a UserMessage nor a SignalMessage.");
    }
    else
    {
      // Invoke consumer here, because we have the state
      if (aSignalMsgConsumer != null)
        aSignalMsgConsumer.handleSignalMessage (ret, aIncomingMessageMetadata, aIncomingState);
    }
    return ret;
  }

  // Parse an AS4 UserMessage
  @Nullable
  public static Ebms3UserMessage parseUserMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                   @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                   @Nonnull final IAS4PModeResolver aPModeResolver,
                                                   @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                   @Nonnull final IAS4IncomingProfileSelector aAS4ProfileSelector,
                                                   @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                   @Nullable final IPMode aSendingPMode,
                                                   @Nonnull final Locale aLocale,
                                                   @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                   @Nonnull final HttpResponse aHttpResponse,
                                                   @Nonnull final byte [] aMessagePayload,
                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                   @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                   @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                   @Nullable final IAS4UserMessageConsumer aUserMsgConsumer) throws Phase4Exception
  {
    final IAS4IncomingMessageState aIncomingState = _parseMessage (aCryptoFactorySign,
                                                                   aCryptoFactoryCrypt,
                                                                   aPModeResolver,
                                                                   aIAF,
                                                                   aAS4ProfileSelector,
                                                                   aResHelper,
                                                                   aSendingPMode,
                                                                   aLocale,
                                                                   aIncomingMessageMetadata,
                                                                   aHttpResponse,
                                                                   aMessagePayload,
                                                                   aIncomingDumper,
                                                                   aIncomingSecurityConfiguration,
                                                                   aIncomingReceiverConfiguration);
    if (aIncomingState == null)
    {
      // Error message was already logged
      return null;
    }

    final Ebms3UserMessage ret = aIncomingState.getEbmsUserMessage ();
    if (ret == null)
    {
      if (aIncomingState.getEbmsSignalMessage () != null)
        LOGGER.warn ("A Message state is present, but it contains a SignalMessage instead of a UserMessage.");
      else
        LOGGER.warn ("A Message state is present, but it contains neither a SignalMessage nor a UserMessage.");
    }
    else
    {
      // Invoke consumer here, because we have the state
      if (aUserMsgConsumer != null)
        aUserMsgConsumer.handleUserMessage (ret, aIncomingMessageMetadata, aIncomingState);
    }
    return ret;
  }

  // Parse as an AS4 UserMessage or an AS4 SignalMessage
  @Nonnull
  public static ESuccess parseUserOrSignalMessage (@Nonnull final IAS4CryptoFactory aCryptoFactorySign,
                                                   @Nonnull final IAS4CryptoFactory aCryptoFactoryCrypt,
                                                   @Nonnull final IAS4PModeResolver aPModeResolver,
                                                   @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                                   @Nonnull final IAS4IncomingProfileSelector aAS4ProfileSelector,
                                                   @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                   @Nullable final IPMode aSendingPMode,
                                                   @Nonnull final Locale aLocale,
                                                   @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                                   @Nonnull final HttpResponse aHttpResponse,
                                                   @Nonnull final byte [] aMessagePayload,
                                                   @Nullable final IAS4IncomingDumper aIncomingDumper,
                                                   @Nonnull final IAS4IncomingSecurityConfiguration aIncomingSecurityConfiguration,
                                                   @Nonnull final IAS4IncomingReceiverConfiguration aIncomingReceiverConfiguration,
                                                   @Nullable final IAS4UserMessageConsumer aUserMsgConsumer,
                                                   @Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer) throws Phase4Exception
  {
    final IAS4IncomingMessageState aIncomingState = _parseMessage (aCryptoFactorySign,
                                                                   aCryptoFactoryCrypt,
                                                                   aPModeResolver,
                                                                   aIAF,
                                                                   aAS4ProfileSelector,
                                                                   aResHelper,
                                                                   aSendingPMode,
                                                                   aLocale,
                                                                   aIncomingMessageMetadata,
                                                                   aHttpResponse,
                                                                   aMessagePayload,
                                                                   aIncomingDumper,
                                                                   aIncomingSecurityConfiguration,
                                                                   aIncomingReceiverConfiguration);
    if (aIncomingState == null)
    {
      // Error message was already logged
      return ESuccess.FAILURE;
    }

    final Ebms3UserMessage aUserMsg = aIncomingState.getEbmsUserMessage ();
    if (aUserMsg != null)
    {
      // Invoke consumer here, because we have the state
      if (aUserMsgConsumer != null)
        aUserMsgConsumer.handleUserMessage (aUserMsg, aIncomingMessageMetadata, aIncomingState);
    }
    else
    {
      final Ebms3SignalMessage aSignalMsg = aIncomingState.getEbmsSignalMessage ();
      if (aSignalMsg != null)
      {
        // Invoke consumer here, because we have the state
        if (aSignalMsgConsumer != null)
          aSignalMsgConsumer.handleSignalMessage (aSignalMsg, aIncomingMessageMetadata, aIncomingState);
      }
      else
        LOGGER.warn ("A Message state is present, but it contains neither a SignalMessage nor a UserMessage.");
    }
    return ESuccess.SUCCESS;
  }
}
