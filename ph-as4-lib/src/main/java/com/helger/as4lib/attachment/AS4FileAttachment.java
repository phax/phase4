/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.attachment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.Nonnull;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.http.CHTTPHeader;

/**
 * File based attachment.
 *
 * @author Philip Helger
 */
public class AS4FileAttachment extends AbstractAS4Attachment
{
  private final File m_aFile;

  public AS4FileAttachment (@Nonnull final File aFile, @Nonnull final IMimeType aMimeType)
  {
    super (aMimeType, (EAS4CompressionMode) null);
    ValueEnforcer.notNull (aFile, "File");
    m_aFile = aFile;
  }

  public AS4FileAttachment (@Nonnull final File aFile,
                            @Nonnull final IMimeType aMimeType,
                            @Nonnull final EAS4CompressionMode aEAS4CompressionMode)
  {
    super (aMimeType, aEAS4CompressionMode);
    ValueEnforcer.notNull (aFile, "File");
    m_aFile = aFile;
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
      // TODO file suffix in enum
      final File aCompressedFile = File.createTempFile (m_aFile.getName () + "Compressed", ".gz");

      getCompressionMode ().getCompressStream (new FileOutputStream (aCompressedFile))
                           .write (Files.readAllBytes (m_aFile.toPath ()));
      aMimeBodyPart.setDataHandler (new DataHandler (new FileDataSource (aCompressedFile)));
    }
    else
      aMimeBodyPart.setDataHandler (new DataHandler (new FileDataSource (m_aFile)));
    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());
    aMimeMultipart.addBodyPart (aMimeBodyPart);
  }

  @Nonnull
  public Attachment getAsWSS4JAttachment ()
  {
    final ICommonsMap <String, String> aHeaders = new CommonsHashMap<> ();
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION,
                  "attachment; filename=\"" + FilenameHelper.getWithoutPath (m_aFile) + "\"");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + getID () + ">");
    aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());

    final Attachment aAttachment = new Attachment ();
    aAttachment.setMimeType (getMimeType ().getAsString ());
    aAttachment.addHeaders (aHeaders);
    aAttachment.setId (getID ());
    aAttachment.setSourceStream (FileHelper.getInputStream (m_aFile));
    return aAttachment;
  }
}
