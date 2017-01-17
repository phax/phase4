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
package com.helger.as4lib.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.ToStringGenerator;
import com.helger.http.CHTTPHeader;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.datasource.InputStreamDataSource;

/**
 * Special WSS4J attachment with an InputStream provider instead of a fixed
 * InputStream
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class WSS4JAttachment extends Attachment
{
  private final AS4ResourceManager m_aResMgr;
  private IHasInputStream m_aISP;
  private EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;
  private EAS4CompressionMode m_eCM;
  private Charset m_aCharset;
  private String m_sUncompressedMimeType;

  public WSS4JAttachment (@Nonnull final AS4ResourceManager aResMgr, @Nullable final String sMimeType)
  {
    m_aResMgr = ValueEnforcer.notNull (aResMgr, "ResMgr");
    overwriteMimeType (sMimeType);
  }

  @Override
  @Deprecated
  public void setMimeType (@Nullable final String sMimeType)
  {
    throw new UnsupportedOperationException ();
  }

  public final void overwriteMimeType (@Nullable final String sMimeType)
  {
    super.setMimeType (sMimeType);
    m_sUncompressedMimeType = sMimeType;
  }

  /**
   * @return The MIME type of the uncompressed attachment.
   */
  @Nullable
  public String getUncompressedMimeType ()
  {
    return m_sUncompressedMimeType;
  }

  @Override
  public InputStream getSourceStream ()
  {
    final InputStream ret = m_aISP.getInputStream ();
    if (ret == null)
      throw new IllegalStateException ("Failed to get InputStream from " + m_aISP);
    m_aResMgr.addCloseable (ret);
    return ret;
  }

  @Override
  @Deprecated
  public void setSourceStream (final InputStream sourceStream)
  {
    throw new UnsupportedOperationException ("Use setSourceStreamProvider instead");
  }

  public void setSourceStreamProvider (@Nonnull final IHasInputStream aISP)
  {
    ValueEnforcer.notNull (aISP, "InputStreamProvider");
    m_aISP = aISP;
  }

  /**
   * @return The content transfer encoding to be used. Required for MIME
   *         multipart handling only.
   */
  @Nonnull
  public final EContentTransferEncoding getContentTransferEncoding ()
  {
    return m_eCTE;
  }

  @Nonnull
  public final WSS4JAttachment setContentTransferEncoding (@Nonnull final EContentTransferEncoding eCTE)
  {
    m_eCTE = ValueEnforcer.notNull (eCTE, "CTE");
    return this;
  }

  @Nullable
  public final EAS4CompressionMode getCompressionMode ()
  {
    return m_eCM;
  }

  public final boolean hasCompressionMode ()
  {
    return m_eCM != null;
  }

  @Nonnull
  public final WSS4JAttachment setCompressionMode (@Nonnull final EAS4CompressionMode eCM)
  {
    ValueEnforcer.notNull (eCM, "CompressionMode");
    m_eCM = eCM;
    if (eCM != null)
    {
      // Main MIME type is now the compression type MIME type
      super.setMimeType (eCM.getMimeType ().getAsString ());
    }
    else
    {
      // Main MIME type is the uncompressed one (which may be null)
      super.setMimeType (m_sUncompressedMimeType);
    }
    return this;
  }

  @Nonnull
  public final Charset getCharset ()
  {
    return m_aCharset;
  }

  public final boolean hasCharset ()
  {
    return m_aCharset != null;
  }

  @Nonnull
  public final WSS4JAttachment setCharset (@Nonnull final Charset aCharset)
  {
    m_aCharset = ValueEnforcer.notNull (aCharset, "Charset");
    return this;
  }

  public void addToMimeMultipart (@Nonnull final MimeMultipart aMimeMultipart) throws Exception
  {
    ValueEnforcer.notNull (aMimeMultipart, "MimeMultipart");

    final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();

    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, getId ());
    // !IMPORTANT! DO NOT CHANGE the order of the adding a DH and then the last
    // headers
    // On some tests the datahandler did reset content-type and transfer
    // encoding, so this is now the correct order
    aMimeBodyPart.setDataHandler (new DataHandler (new InputStreamDataSource (getSourceStream (),
                                                                              getId ()).getEncodingAware (getContentTransferEncoding ())));

    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ());
    aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TRANSFER_ENCODING, getContentTransferEncoding ().getID ());

    aMimeMultipart.addBodyPart (aMimeBodyPart);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", getId ())
                                       .append ("MimeType", getMimeType ())
                                       .append ("Headers", getHeaders ())
                                       .append ("ResourceManager", m_aResMgr)
                                       .append ("ISP", m_aISP)
                                       .append ("CTE", m_eCTE)
                                       .append ("CM", m_eCM)
                                       .append ("Charset", m_aCharset)
                                       .toString ();
  }

  /**
   * Constructor. Performs compression internally.
   *
   * @param aFile
   *        Source, uncompressed, unencrypted file.
   * @param aMimeType
   *        Original mime type of the file.
   * @param eCompressionMode
   *        Optional compression mode to use. May be <code>null</code>.
   * @param aResMgr
   *        The resource manager to use. May not be <code>null</code>.
   * @throws IOException
   *         In case something goes wrong during compression
   */
  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final File aFile,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nonnull final AS4ResourceManager aResMgr) throws IOException
  {
    ValueEnforcer.notNull (aFile, "File");
    ValueEnforcer.notNull (aMimeType, "MimeType");

    final WSS4JAttachment ret = new WSS4JAttachment (aResMgr, aMimeType.getAsString ());
    ret.setId (CAS4.LIB_NAME + "-" + UUID.randomUUID ().toString ());

    // Set after ID and MimeType!
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION,
                   "attachment; filename=\"" + FilenameHelper.getWithoutPath (aFile) + "\"");
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + ret.getId () + ">");
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, ret.getMimeType ());

    // If the attachment has an compressionMode do it directly, so that
    // encryption later on works on the compressed content
    File aRealFile;
    if (eCompressionMode != null)
    {
      ret.setCompressionMode (eCompressionMode);

      // Create temporary file with compressed content
      aRealFile = aResMgr.createTempFile ();
      try (
          final OutputStream aOS = eCompressionMode.getCompressStream (StreamHelper.getBuffered (FileHelper.getOutputStream (aRealFile))))
      {
        StreamHelper.copyInputStreamToOutputStream (StreamHelper.getBuffered (FileHelper.getInputStream (aFile)), aOS);
      }
    }
    else
    {
      // No compression - use file as-is
      aRealFile = aFile;
    }
    ret.setSourceStreamProvider ( () -> StreamHelper.getBuffered (FileHelper.getInputStream (aRealFile)));
    return ret;
  }
}
