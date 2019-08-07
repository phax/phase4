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
package com.helger.as4.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.util.AS4ResourceHelper;
import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.datasource.InputStreamProviderDataSource;

/**
 * Special WSS4J attachment with an InputStream provider instead of a fixed
 * InputStream<br>
 * Note: cannot be serializable because base class is not serializable and
 * because we're dealing with {@link InputStream}s.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class WSS4JAttachment extends Attachment
{
  private final AS4ResourceHelper m_aResHelper;
  private IHasInputStream m_aISP;
  private EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;
  private EAS4CompressionMode m_eCM;
  private Charset m_aCharset;
  private String m_sUncompressedMimeType;

  public WSS4JAttachment (@Nonnull @WillNotClose final AS4ResourceHelper aResHelper, @Nullable final String sMimeType)
  {
    m_aResHelper = ValueEnforcer.notNull (aResHelper, "ResHelper");
    overwriteMimeType (sMimeType);
  }

  /**
   * @return The resource helper provided in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4ResourceHelper getResHelper ()
  {
    return m_aResHelper;
  }

  /**
   * Create a random UUID based ID and call {@link #setId(String)}
   */
  public void setUniqueID ()
  {
    setId (MessageHelperMethods.createRandomAttachmentID ());
  }

  @Override
  @Deprecated
  public final void setMimeType (@Nullable final String sMimeType)
  {
    throw new UnsupportedOperationException ();
  }

  public final void overwriteMimeType (@Nullable final String sMimeType)
  {
    super.setMimeType (sMimeType);
    m_sUncompressedMimeType = sMimeType;
    addHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, sMimeType);
  }

  @Override
  public final void addHeader (final String sName, final String sValue)
  {
    super.addHeader (sName, sValue);
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
  @Nonnull
  public InputStream getSourceStream ()
  {
    return getSourceStream (m_aResHelper);
  }

  /**
   * Get the source stream of the attachment using the provided resource helper.
   * This can be helpful, if the source helper is already out of scope.
   *
   * @param aResourceHelper
   *        The resource helper to use. May not be <code>null</code>.
   * @return A non-<code>null</code> InputStream on the source.
   */
  @Nonnull
  public InputStream getSourceStream (@Nonnull final AS4ResourceHelper aResourceHelper)
  {
    ValueEnforcer.notNull (aResourceHelper, "ResourceHelper");

    // This will e.g. throw an UncheckedIOException if compression is enabled,
    // but the transmitted document is not compressed
    final InputStream ret = m_aISP.getInputStream ();
    if (ret == null)
      throw new IllegalStateException ("Got no InputStream from " + m_aISP);
    aResourceHelper.addCloseable (ret);
    return ret;
  }

  /**
   * @deprecated Do not use this, because it can be opened only once. Use
   *             {@link #setSourceStreamProvider(IHasInputStream)} instead.
   */
  @Override
  @Deprecated
  public void setSourceStream (final InputStream sourceStream)
  {
    throw new UnsupportedOperationException ("Use setSourceStreamProvider instead");
  }

  @Nullable
  public IHasInputStream getInputStreamProvider ()
  {
    return m_aISP;
  }

  public boolean isRepeatable ()
  {
    return m_aISP != null && m_aISP.isReadMultiple ();
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
    return m_aCharset == null ? StandardCharsets.ISO_8859_1 : m_aCharset;
  }

  public final boolean hasCharset ()
  {
    return m_aCharset != null;
  }

  @Nonnull
  public final WSS4JAttachment setCharset (@Nullable final Charset aCharset)
  {
    m_aCharset = aCharset;
    return this;
  }

  @Nonnull
  private DataSource _getAsDataSource ()
  {
    final InputStreamProviderDataSource aDS = new InputStreamProviderDataSource (m_aISP, getId (), getMimeType ());
    return aDS.getEncodingAware (getContentTransferEncoding ());
  }

  public void addToMimeMultipart (@Nonnull final MimeMultipart aMimeMultipart) throws MessagingException
  {
    ValueEnforcer.notNull (aMimeMultipart, "MimeMultipart");

    final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();

    {
      // According to
      // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
      // chapter 5.2 the CID must be enclosed in angle brackets
      String sContentID = getId ();
      if (StringHelper.hasText (sContentID))
      {
        if (sContentID.charAt (0) != '<')
          sContentID = '<' + sContentID + '>';
        aMimeBodyPart.setHeader (CHttpHeader.CONTENT_ID, sContentID);
      }
    }

    // !IMPORTANT! DO NOT CHANGE the order of the adding a DH and then the last
    // headers
    // On some tests the datahandler did reset content-type and transfer
    // encoding, so this is now the correct order
    aMimeBodyPart.setDataHandler (new DataHandler (_getAsDataSource ()));

    // After DataHandler!!
    aMimeBodyPart.setHeader (CHttpHeader.CONTENT_TYPE, getMimeType ());
    aMimeBodyPart.setHeader (CHttpHeader.CONTENT_TRANSFER_ENCODING, getContentTransferEncoding ().getID ());

    aMimeMultipart.addBodyPart (aMimeBodyPart);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", getId ())
                                       .append ("MimeType", getMimeType ())
                                       .append ("Headers", getHeaders ())
                                       .append ("ResourceManager", m_aResHelper)
                                       .append ("ISP", m_aISP)
                                       .append ("CTE", m_eCTE)
                                       .append ("CM", m_eCM)
                                       .append ("Charset", m_aCharset)
                                       .getToString ();
  }

  private static void _addOutgoingHeaders (@Nonnull final WSS4JAttachment aAttachment, @Nonnull final String sFilename)
  {
    // Ensure an ID is present
    if (StringHelper.hasNoText (aAttachment.getId ()))
      aAttachment.setUniqueID ();

    // Set after ID and MimeType!
    aAttachment.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    aAttachment.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION,
                           "attachment; filename=\"" + sFilename + "\"");
    aAttachment.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + aAttachment.getId () + '>');
    aAttachment.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, aAttachment.getMimeType ());
  }

  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final File aSrcFile,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nonnull final AS4ResourceHelper aResHelper) throws IOException
  {
    return createOutgoingFileAttachment (aSrcFile, null, aMimeType, eCompressionMode, aResHelper);
  }

  /**
   * Constructor. Performs compression internally.
   *
   * @param aSrcFile
   *        Source, uncompressed, unencrypted file.
   * @param aMimeType
   *        Original mime type of the file.
   * @param eCompressionMode
   *        Optional compression mode to use. May be <code>null</code>.
   * @param aResHelper
   *        The resource manager to use. May not be <code>null</code>.
   * @return The newly created attachment instance. Never <code>null</code>.
   * @throws IOException
   *         In case something goes wrong during compression
   */
  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final File aSrcFile,
                                                              @Nullable final String sContentID,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws IOException
  {
    ValueEnforcer.notNull (aSrcFile, "File");
    ValueEnforcer.notNull (aMimeType, "MimeType");

    final WSS4JAttachment ret = new WSS4JAttachment (aResHelper, aMimeType.getAsString ());
    ret.setId (sContentID);
    _addOutgoingHeaders (ret, FilenameHelper.getWithoutPath (aSrcFile));

    // If the attachment has an compressionMode do it directly, so that
    // encryption later on works on the compressed content
    File aRealFile;
    if (eCompressionMode != null)
    {
      ret.setCompressionMode (eCompressionMode);

      // Create temporary file with compressed content
      aRealFile = aResHelper.createTempFile ();
      try (final OutputStream aOS = eCompressionMode.getCompressStream (FileHelper.getBufferedOutputStream (aRealFile)))
      {
        StreamHelper.copyInputStreamToOutputStream (FileHelper.getBufferedInputStream (aSrcFile), aOS);
      }
    }
    else
    {
      // No compression - use file as-is
      aRealFile = aSrcFile;
    }

    // Set a stream provider that can be read multiple times (opens a new
    // FileInputStream internally)
    ret.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aRealFile)));
    return ret;
  }

  /**
   * Constructor. Performs compression internally.
   *
   * @param aSrcData
   *        Source in-memory data, uncompressed, unencrypted.
   * @param sContentID
   *        Optional content ID or <code>null</code> to create a random one.
   * @param sFilename
   *        Filename of the attachment. May not be <code>null</code>.
   * @param aMimeType
   *        Original mime type of the file. May not be <code>null</code>.
   * @param eCompressionMode
   *        Optional compression mode to use. May be <code>null</code>.
   * @param aResHelper
   *        The resource manager to use. May not be <code>null</code>.
   * @return The newly created attachment instance. Never <code>null</code>.
   * @throws IOException
   *         In case something goes wrong during compression
   */
  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final byte [] aSrcData,
                                                              @Nullable final String sContentID,
                                                              @Nonnull @Nonempty final String sFilename,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nonnull final AS4ResourceHelper aResHelper) throws IOException
  {
    ValueEnforcer.notNull (aSrcData, "Data");
    ValueEnforcer.notEmpty (sFilename, "Filename");
    ValueEnforcer.notNull (aMimeType, "MimeType");

    final WSS4JAttachment ret = new WSS4JAttachment (aResHelper, aMimeType.getAsString ());
    ret.setId (sContentID);
    _addOutgoingHeaders (ret, sFilename);

    // If the attachment has an compressionMode do it directly, so that
    // encryption later on works on the compressed content
    if (eCompressionMode != null)
    {
      ret.setCompressionMode (eCompressionMode);

      // Create temporary file with compressed content
      final File aRealFile = aResHelper.createTempFile ();
      try (final OutputStream aOS = eCompressionMode.getCompressStream (FileHelper.getBufferedOutputStream (aRealFile)))
      {
        aOS.write (aSrcData);
      }
      ret.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aRealFile)));
    }
    else
    {
      // No compression - use data as-is
      ret.setSourceStreamProvider (HasInputStream.multiple ( () -> new NonBlockingByteArrayInputStream (aSrcData)));
    }
    return ret;
  }

  public static boolean canBeKeptInMemory (final long nBytes)
  {
    return nBytes <= 64 * CGlobal.BYTES_PER_KILOBYTE;
  }

  @Nonnull
  public static WSS4JAttachment createIncomingFileAttachment (@Nonnull final MimeBodyPart aBodyPart,
                                                              @Nonnull final AS4ResourceHelper aResHelper) throws MessagingException,
                                                                                                           IOException
  {
    ValueEnforcer.notNull (aBodyPart, "BodyPart");
    ValueEnforcer.notNull (aResHelper, "ResHelper");

    final WSS4JAttachment ret = new WSS4JAttachment (aResHelper, aBodyPart.getContentType ());

    {
      // Reference in Content-ID header is: "<ID>"
      // See
      // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
      // chapter 5.2
      final String sRealContentID = StringHelper.trimStartAndEnd (aBodyPart.getContentID (), '<', '>');
      ret.setId (sRealContentID);
    }

    if (canBeKeptInMemory (aBodyPart.getSize ()))
    {
      // keep some small parts in memory
      final DataHandler aDH = aBodyPart.getDataHandler ();
      final DataSource aDS = aDH.getDataSource ();
      if (aDS != null)
      {
        // DataSource InputStreams can be retrieved over and over again
        ret.setSourceStreamProvider (HasInputStream.multiple ( () -> {
          try
          {
            return aDS.getInputStream ();
          }
          catch (final IOException ex)
          {
            throw new UncheckedIOException (ex);
          }
        }));
      }
      else
      {
        // Can only be read once
        ret.setSourceStreamProvider (HasInputStream.once ( () -> {
          try
          {
            return aDH.getInputStream ();
          }
          catch (final IOException ex)
          {
            throw new UncheckedIOException (ex);
          }
        }));
      }
    }
    else
    {
      // Write to temp file
      final File aTempFile = aResHelper.createTempFile ();
      try (final OutputStream aOS = FileHelper.getBufferedOutputStream (aTempFile))
      {
        aBodyPart.getDataHandler ().writeTo (aOS);
      }
      ret.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aTempFile)));
    }

    // Convert all headers to attributes
    final Enumeration <Header> aEnum = aBodyPart.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header aHeader = aEnum.nextElement ();
      ret.addHeader (aHeader.getName (), aHeader.getValue ());
    }

    // These headers are mandatory and overwrite headers from the MIME body part
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + ret.getId () + '>');
    ret.addHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, ret.getMimeType ());

    return ret;
  }
}
