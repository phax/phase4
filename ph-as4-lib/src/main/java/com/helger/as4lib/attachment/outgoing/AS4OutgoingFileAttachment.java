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
package com.helger.as4lib.attachment.outgoing;

import java.io.File;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.http.CHTTPHeader;

/**
 * File based attachment.
 *
 * @author Philip Helger
 */
public class AS4OutgoingFileAttachment extends AbstractAS4OutgoingAttachment
{
  private final File m_aFile;
  private final AS4ResourceManager m_aResMgr;

  public AS4OutgoingFileAttachment (@Nonnull final File aFile,
                                    @Nonnull final IMimeType aMimeType,
                                    @Nonnull final AS4ResourceManager aTempFileHdl)
  {
    this (aFile, aMimeType, (EAS4CompressionMode) null, aTempFileHdl);
  }

  public AS4OutgoingFileAttachment (@Nonnull final File aFile,
                                    @Nonnull final IMimeType aMimeType,
                                    @Nullable final EAS4CompressionMode aEAS4CompressionMode,
                                    @Nonnull final AS4ResourceManager aResMgr)
  {
    super (aMimeType, aEAS4CompressionMode);
    m_aFile = ValueEnforcer.notNull (aFile, "File");
    m_aResMgr = ValueEnforcer.notNull (aResMgr, "ResMgr");
  }

  public void addToMimeMultipart (@Nonnull final MimeMultipart aMimeMultipart) throws Exception
  {
    ValueEnforcer.notNull (aMimeMultipart, "MimeMultipart");

    final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();

    aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TRANSFER_ENCODING, getContentTransferEncoding ().getID ());
    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, getID ());
    // If the attachment has an compressionMode assigned the compressed file
    // will be set as DataSource
    if (hasCompressionMode ())
    {
      // Create temporary file with compressed content
      final File aCompressedFile = m_aResMgr.createTempFile ();
      try (
          final OutputStream aOS = getCompressionMode ().getCompressStream (StreamHelper.getBuffered (FileHelper.getOutputStream (aCompressedFile))))
      {
        StreamHelper.copyInputStreamToOutputStream (StreamHelper.getBuffered (FileHelper.getInputStream (m_aFile)),
                                                    aOS);
      }
      aMimeBodyPart.setDataHandler (new DataHandler (new FileDataSource (aCompressedFile)));
    }
    else
    {
      // Use source file
      aMimeBodyPart.setDataHandler (new DataHandler (new FileDataSource (m_aFile)));
    }
    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());
    aMimeMultipart.addBodyPart (aMimeBodyPart);
  }

  @Nonnull
  public WSS4JAttachment getAsWSS4JAttachment ()
  {
    final ICommonsMap <String, String> aHeaders = new CommonsHashMap <> ();
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION,
                  "attachment; filename=\"" + FilenameHelper.getWithoutPath (m_aFile) + "\"");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + getID () + ">");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());

    final WSS4JAttachment aAttachment = new WSS4JAttachment (m_aResMgr);
    aAttachment.setMimeType (getMimeType ().getAsString ());
    aAttachment.addHeaders (aHeaders);
    aAttachment.setId (getID ());
    aAttachment.setSourceStreamProvider ( () -> StreamHelper.getBuffered (FileHelper.getInputStream (m_aFile)));
    return aAttachment;
  }
}
