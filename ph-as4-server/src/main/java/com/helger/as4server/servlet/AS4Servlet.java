/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.ext.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4ErrorMessage;
import com.helger.as4lib.message.AS4ReceiptMessage;
import com.helger.as4lib.message.CreateErrorMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.attachment.IIncomingAttachment;
import com.helger.as4server.mgr.MetaManager;
import com.helger.as4server.receive.AS4MessageState;
import com.helger.as4server.receive.soap.ISOAPHeaderElementProcessor;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.mime.EMimeContentType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.http.EHTTPMethod;
import com.helger.http.EHTTPVersion;
import com.helger.photon.core.servlet.AbstractUnifiedResponseServlet;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.servlet.response.UnifiedResponse;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

public class AS4Servlet extends AbstractUnifiedResponseServlet
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Servlet.class);
  private static final IMimeType MT_MULTIPART_RELATED = EMimeContentType.MULTIPART.buildMimeType ("related");

  public AS4Servlet ()
  {}

  private void _handleSOAPMessage (@Nonnull final Document aSOAPDocument,
                                   @Nonnull final ESOAPVersion eSOAPVersion,
                                   @Nonnull final ICommonsList <IIncomingAttachment> aIncomingAttachments,
                                   @Nonnull final AS4Response aUR) throws Exception
  {
    // TODO remove if or entire statement
    if (true)
      s_aLogger.info (AS4XMLHelper.serializeXML (aSOAPDocument));

    s_aLogger.info ("!!!!" + aIncomingAttachments.toString ());

    // Find SOAP header
    final Node aHeaderNode = XMLHelper.getFirstChildElementOfName (aSOAPDocument.getDocumentElement (),
                                                                   eSOAPVersion.getNamespaceURI (),
                                                                   eSOAPVersion.getHeaderElementName ());
    if (aHeaderNode == null)
    {
      aUR.setBadRequest ("SOAP document is missing a Header element");
      return;
    }

    // Extract all header elements
    final ICommonsList <AS4SOAPHeader> aHeaders = new CommonsArrayList<> ();
    for (final Element aHeaderChild : new ChildElementIterator (aHeaderNode))
    {
      final QName aQName = AS4XMLHelper.getQName (aHeaderChild);
      final String sMustUnderstand = aHeaderChild.getAttributeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
      final boolean bIsMustUnderstand = eSOAPVersion.getMustUnderstandValue (true).equals (sMustUnderstand);
      aHeaders.add (new AS4SOAPHeader (aHeaderChild, aQName, bIsMustUnderstand));
    }

    final AS4MessageState aState = new AS4MessageState (eSOAPVersion);
    final ICommonsList <Attachment> aWSS4JAttachments = new CommonsArrayList<> ();

    // Need to check, since not every message will have attachments
    if (aIncomingAttachments.isNotEmpty ())
      aWSS4JAttachments.addAllMapped (aIncomingAttachments, IIncomingAttachment::getAsWSS4JAttachment);

    // handle all headers in the order of the registered handlers!
    for (final Map.Entry <QName, ISOAPHeaderElementProcessor> aEntry : SOAPHeaderElementProcessorRegistry.getInstance ()
                                                                                                         .getAllElementProcessors ()
                                                                                                         .entrySet ())
    {
      final QName aQName = aEntry.getKey ();
      final AS4SOAPHeader aHeader = aHeaders.findFirst (x -> aQName.equals (x.getQName ()));
      if (aHeader != null)
      {
        final ISOAPHeaderElementProcessor aProcessor = aEntry.getValue ();
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Processing SOAP header element " + aQName.toString () + " with processor " + aProcessor);

        // Process element
        final ErrorList aErrorList = new ErrorList ();
        if (aProcessor.processHeaderElement (aSOAPDocument, aHeader.getNode (), aWSS4JAttachments, aState, aErrorList)
                      .isSuccess ())
          aHeader.setProcessed (true);
        else
        {

          // upon failure, the element stays unprocessed and sends back a signal
          // message with the errors
          s_aLogger.info ("Failed to process SOAP header element " +
                          aQName.toString () +
                          " with processor " +
                          aProcessor +
                          "; error details: " +
                          aErrorList);

          final ICommonsList <Ebms3Error> aErrorMessages = new CommonsArrayList<> ();

          // TODO change Locale to dynamic
          aErrorList.forEach (error -> aErrorMessages.add (EEbmsError.getFromErrorCodeOrNull (error.getErrorID ())
                                                                     .getAsEbms3Error (Locale.US)));

          final CreateErrorMessage aErrorMessage = new CreateErrorMessage ();
          final AS4ErrorMessage aDoc = aErrorMessage.createErrorMessage (eSOAPVersion,
                                                                         aErrorMessage.createEbms3MessageInfo ("AS4-Server"),
                                                                         aErrorMessages);

          aUR.setContentAndCharset (AS4XMLHelper.serializeXML (aDoc.getAsSOAPDocument ()), CCharset.CHARSET_UTF_8_OBJ)
             .setMimeType (eSOAPVersion.getMimeType ());

          return;
        }
      }
      // else: no header element for current processor
    }
    // Now check if all must understand headers were processed
    for (final AS4SOAPHeader aHeader : aHeaders)
      if (aHeader.isMustUnderstand () && !aHeader.isProcessed ())
      {
        aUR.setBadRequest ("Error processing required SOAP header element " + aHeader.getQName ().toString ());
        return;
      }

    final Ebms3Messaging aMessaging = aState.getMessaging ();
    if (aMessaging == null)
    {
      aUR.setBadRequest ("No Ebms3 Messaging header was found");
      return;
    }

    // Every message should only contain 1 UserMessage and n (0..n)
    // SignalMessages
    if (aMessaging.getUserMessageCount () != 1)
    {
      aUR.setBadRequest ("Unexpected number of Ebms3 UserMessages found: " + aMessaging.getUserMessageCount ());
      return;
    }

    // Decompressing the attachments
    for (final IIncomingAttachment aIncomingAttachment : aIncomingAttachments)
    {
      if (aState.getCompressedAttachmentIDs ().contains (aIncomingAttachment.getContentID ()))
      {
        final IIncomingAttachment aDecompressedAttachment = MetaManager.getIncomingAttachmentFactory ()
                                                                       .createAttachment (new GZIPInputStream (aIncomingAttachment.getInputStream ()));
        aIncomingAttachments.remove (aIncomingAttachment);
        aIncomingAttachments.add (aDecompressedAttachment);
      }
    }

    // TODO Do something with the attachments

    final Ebms3UserMessage aEbms3UserMessage = aMessaging.getUserMessageAtIndex (0);
    final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
    final Ebms3MessageInfo aEbms3MessageInfo = aReceiptMessage.createEbms3MessageInfo ("AS4-Server", null);
    final AS4ReceiptMessage aDoc = aReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                         aEbms3MessageInfo,
                                                                         aEbms3UserMessage,
                                                                         aSOAPDocument)
                                                  .setMustUnderstand (true);

    // We've got our response
    final Document testi = aDoc.getAsSOAPDocument ();
    aUR.setContentAndCharset (AS4XMLHelper.serializeXML (testi), CCharset.CHARSET_UTF_8_OBJ)
       .setMimeType (eSOAPVersion.getMimeType ());
  }

  @Override
  @Nonnull
  protected AS4Response createUnifiedResponse (@Nonnull final EHTTPVersion eHTTPVersion,
                                               @Nonnull final EHTTPMethod eHTTPMethod,
                                               @Nonnull final HttpServletRequest aHttpRequest)
  {
    return new AS4Response (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  @Override
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final AS4Response aUR = (AS4Response) aUnifiedResponse;
    final HttpServletRequest aHttpServletRequest = aRequestScope.getRequest ();

    try
    {
      // Determine content type
      final MimeType aMT = MimeTypeParser.parseMimeType (aHttpServletRequest.getContentType ());
      s_aLogger.info ("Content-Type: " + aMT);
      if (aMT == null)
      {
        aUR.setBadRequest ("Failed to parse Content-Type '" + aHttpServletRequest.getContentType () + "'");
        return;
      }

      Document aSOAPDocument = null;
      ESOAPVersion eSOAPVersion = null;
      final ICommonsList <IIncomingAttachment> aIncomingAttachments = new CommonsArrayList<> ();

      final IMimeType aPlainMT = aMT.getCopyWithoutParameters ();
      if (aPlainMT.equals (MT_MULTIPART_RELATED))
      {
        // MIME message
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Received MIME message");

        final String sBoundary = aMT.getParameterValueWithName ("boundary");
        if (StringHelper.hasNoText (sBoundary))
        {
          aUR.setBadRequest ("Content-Type '" + aHttpServletRequest.getContentType () + "' misses boundary parameter");
        }
        else
        {
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("MIME Boundary = " + sBoundary);

          // PARSING MIME Message via MultiPartStream
          final MultipartStream aMulti = new MultipartStream (aHttpServletRequest.getInputStream (),
                                                              sBoundary.getBytes (CCharset.CHARSET_ISO_8859_1_OBJ),
                                                              (MultipartProgressNotifier) null);

          int nIndex = 0;
          while (true)
          {
            final boolean bNextPart = nIndex == 0 ? aMulti.skipPreamble () : aMulti.readBoundary ();
            if (!bNextPart)
              break;
            s_aLogger.info ("Found part " + nIndex);
            final MultipartItemInputStream aItemIS2 = aMulti.createInputStream ();

            final MimeBodyPart aBodyPart = new MimeBodyPart (aItemIS2);
            if (nIndex == 0)
            {
              // SOAP document
              final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (aBodyPart.getContentType ())
                                                           .getCopyWithoutParameters ();

              // Determine SOAP version from MIME part content type
              eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                    x -> aPlainPartMT.equals (x.getMimeType ()));

              // Read SOAP document
              aSOAPDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());
            }
            else
            {
              // MIME Attachment
              final IIncomingAttachment aAttachment = MetaManager.getIncomingAttachmentFactory ()
                                                                 .createAttachment (aBodyPart);
              aIncomingAttachments.add (aAttachment);
            }
            nIndex++;
          }
        }
      }
      else
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Received plain message with Content-Type " + aMT.getAsString ());

        // Expect plain SOAP - read whole request to DOM
        // Note: this may require a huge amount of memory for large requests
        aSOAPDocument = DOMReader.readXMLDOM (aHttpServletRequest.getInputStream ());

        // Determine SOAP version from content type
        eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (), x -> aPlainMT.equals (x.getMimeType ()));
      }

      if (aSOAPDocument == null)
      {
        aUR.setBadRequest ("Failed to parse " + eSOAPVersion + " document!");
      }
      else
      {
        if (eSOAPVersion == null)
        {
          // Determine from namespace URI of read document
          final String sNamespaceURI = XMLHelper.getNamespaceURI (aSOAPDocument);
          eSOAPVersion = ArrayHelper.findFirst (ESOAPVersion.values (),
                                                x -> x.getNamespaceURI ().equals (sNamespaceURI));
        }

        if (eSOAPVersion == null)
        {
          aUR.setBadRequest ("Failed to determine SOAP version from XML document!");
        }
        else
        {
          // SOAP document and SOAP version are determined
          _handleSOAPMessage (aSOAPDocument, eSOAPVersion, aIncomingAttachments, aUR);
        }
      }
    }
    catch (final Throwable t)
    {
      aUR.setResponseError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error processing AS4 request", t);
    }
  }
}
