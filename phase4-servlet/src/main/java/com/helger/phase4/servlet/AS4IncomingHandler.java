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
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.WrappedInputStream;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.http.AcceptMimeTypeHandler;
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.web.multipart.MultipartProgressNotifier;
import com.helger.web.multipart.MultipartStream;
import com.helger.web.multipart.MultipartStream.MultipartItemInputStream;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Utility methods for incoming AS4 messages.
 *
 * @author Philip Helger
 * @since v0.9.7
 */
public class AS4IncomingHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4IncomingHandler.class);

  private AS4IncomingHandler ()
  {}

  /**
   * Callback interface for handling the parsing result.
   *
   * @author Philip Helger
   */
  public static interface IAS4ParsedMessageCallback
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
     */
    void handle (@Nonnull HttpHeaderMap aHttpHeaders,
                 @Nonnull Document aSoapDocument,
                 @Nonnull ESOAPVersion eSoapVersion,
                 @Nonnull ICommonsList <WSS4JAttachment> aIncomingAttachments) throws WSSecurityException,
                                                                               MessagingException;
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

  public static void parseAS4Message (@Nonnull final IIncomingAttachmentFactory aIAF,
                                      @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                      @Nonnull @WillClose final InputStream aPayloadIS,
                                      @Nonnull final HttpHeaderMap aHttpHeaders,
                                      @Nonnull final IAS4ParsedMessageCallback aCallback,
                                      @Nullable final IAS4IncomingDumper aIncomingDumper) throws AS4BadRequestException,
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
    final IMimeType aPlainContentType = aContentType.getCopyWithoutParameters ();

    Document aSoapDocument = null;
    ESOAPVersion eSoapVersion = null;
    final ICommonsList <WSS4JAttachment> aIncomingAttachments = new CommonsArrayList <> ();
    if (aPlainContentType.equals (AS4RequestHandler.MT_MULTIPART_RELATED))
    {
      // MIME message
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Received MIME message");

      final String sBoundary = aContentType.getParameterValueWithName ("boundary");
      if (StringHelper.hasNoText (sBoundary))
        throw new AS4BadRequestException ("Content-Type '" + sContentType + "' misses 'boundary' parameter");

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("MIME Boundary: '" + sBoundary + "'");

      // Ensure the stream gets closed correctly
      try (final InputStream aRequestIS = _getRequestIS (aHttpHeaders, aPayloadIS, aIncomingDumper))
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

              // Read SOAP document
              aSoapDocument = DOMReader.readXMLDOM (aBodyPart.getInputStream ());

              final IMimeType aPlainPartMT = MimeTypeParser.parseMimeType (aBodyPart.getContentType ())
                                                           .getCopyWithoutParameters ();

              // Determine SOAP version from MIME part content type
              eSoapVersion = ESOAPVersion.getFromMimeTypeOrNull (aPlainPartMT);
              if (eSoapVersion != null && LOGGER.isDebugEnabled ())
                LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from Content-Type");

              if (eSoapVersion == null && aSoapDocument != null)
              {
                // Determine SOAP version from the read document
                eSoapVersion = ESOAPVersion.getFromNamespaceURIOrNull (XMLHelper.getNamespaceURI (aSoapDocument));
                if (eSoapVersion != null && LOGGER.isDebugEnabled ())
                  LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from XML root element namespace URI");
              }
            }
            else
            {
              // MIME Attachment (index is gt 0)
              final WSS4JAttachment aAttachment = aIAF.createAttachment (aBodyPart, aResHelper);
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
        LOGGER.debug ("Received plain message");

      // Expect plain SOAP - read whole request to DOM
      // Note: this may require a huge amount of memory for large requests
      aSoapDocument = DOMReader.readXMLDOM (_getRequestIS (aHttpHeaders, aPayloadIS, aIncomingDumper));

      if (LOGGER.isDebugEnabled ())
        if (aSoapDocument != null)
          LOGGER.debug ("Successfully parsed payload as XML");
        else
          LOGGER.debug ("Failed to parse payload as XML");

      if (aSoapDocument != null)
      {
        // Determine SOAP version from the read document
        eSoapVersion = ESOAPVersion.getFromNamespaceURIOrNull (XMLHelper.getNamespaceURI (aSoapDocument));
        if (eSoapVersion != null && LOGGER.isDebugEnabled ())
          LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from XML root element namespace URI");
      }

      if (eSoapVersion == null)
      {
        // Determine SOAP version from content type
        eSoapVersion = ESOAPVersion.getFromMimeTypeOrNull (aPlainContentType);
        if (eSoapVersion != null && LOGGER.isDebugEnabled ())
          LOGGER.debug ("Determined SOAP version " + eSoapVersion + " from Content-Type");
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
      // We're missing a SOAP version
      throw new AS4BadRequestException ("Failed to determine SOAP version of XML document!");
    }

    aCallback.handle (aHttpHeaders, aSoapDocument, eSoapVersion, aIncomingAttachments);
  }
}
