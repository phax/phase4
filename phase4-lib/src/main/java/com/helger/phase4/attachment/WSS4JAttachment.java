/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.wss4j.common.ext.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.io.stream.NonBlockingBufferedOutputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.datasource.InputStreamProviderDataSource;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

/**
 * Special WSS4J attachment with an InputStream provider instead of a fixed
 * InputStream<br>
 * Note: cannot be serializable because base class is not serializable and
 * because we're dealing with {@link InputStream}s.
 *
 * @author bayerlma
 * @author Philip Helger
 */
@NotThreadSafe
public class WSS4JAttachment extends Attachment implements IAS4Attachment
{
  public static final String CONTENT_DESCRIPTION_ATTACHMENT = "Attachment";
  public static final String CONTENT_ID_PREFIX = "<attachment=";
  public static final String CONTENT_ID_SUFFIX = ">";

  private static final Logger LOGGER = LoggerFactory.getLogger (WSS4JAttachment.class);

  private final AS4ResourceHelper m_aResHelper;
  private IHasInputStream m_aISP;
  private EContentTransferEncoding m_eCTE = EContentTransferEncoding.BINARY;
  private EAS4CompressionMode m_eCompressionMode;
  private Charset m_aCharset;
  private String m_sUncompressedMimeType;
  private final ICommonsOrderedMap <String, String> m_aCustomPartProps = new CommonsLinkedHashMap <> ();

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
    setId (MessageHelperMethods.createRandomContentID ());
  }

  /**
   * @deprecated Do not use this. If you need to use this, use
   *             {@link #overwriteMimeType(String)}
   */
  @Override
  @Deprecated (forRemoval = false)
  @UnsupportedOperation
  public final void setMimeType (@Nullable final String sMimeType)
  {
    throw new UnsupportedOperationException ();
  }

  public final void overwriteMimeType (@Nullable final String sMimeType)
  {
    super.setMimeType (sMimeType);
    m_sUncompressedMimeType = sMimeType;
    addHeader (CHttpHeader.CONTENT_TYPE, sMimeType);
  }

  @Override
  public final void addHeader (final String sName, final String sValue)
  {
    // This overrides an existing header and should therefore be called
    // "setHeader", as in other places the headers is a Map<String,List<String>>
    super.addHeader (sName, sValue);
  }

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
  @Deprecated (forRemoval = false)
  @UnsupportedOperation
  public void setSourceStream (final InputStream sourceStream)
  {
    throw new UnsupportedOperationException ("Use setSourceStreamProvider instead");
  }

  @Nullable
  public IHasInputStream getInputStreamProvider ()
  {
    return m_aISP;
  }

  public void setSourceStreamProvider (@Nonnull final IHasInputStream aISP)
  {
    ValueEnforcer.notNull (aISP, "InputStreamProvider");
    m_aISP = aISP;
  }

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
    return m_eCompressionMode;
  }

  @Nonnull
  public final WSS4JAttachment setCompressionMode (@Nonnull final EAS4CompressionMode eCompressionMode)
  {
    ValueEnforcer.notNull (eCompressionMode, "CompressionMode");
    m_eCompressionMode = eCompressionMode;

    // Main MIME type is now the compression type MIME type
    super.setMimeType (eCompressionMode.getMimeType ().getAsString ());

    return this;
  }

  @Nullable
  public final Charset getCharsetOrDefault (@Nullable final Charset aDefault)
  {
    return m_aCharset != null ? m_aCharset : aDefault;
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

    // Add custom headers before the special ones
    for (final Map.Entry <String, String> aEntry : getHeaders ().entrySet ())
    {
      final String sName = aEntry.getKey ();
      if (!sName.equals (CHttpHeader.CONTENT_ID) &&
          !sName.equals (CHttpHeader.CONTENT_TRANSFER_ENCODING) &&
          !sName.equals (CHttpHeader.CONTENT_TYPE))
        aMimeBodyPart.setHeader (sName, aEntry.getValue ());
    }

    {
      // According to
      // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
      // chapter 5.2 the CID must be enclosed in angle brackets
      // That is based on RFRC 2045 stating
      // "Content-ID" ":" msg-id
      // and RFC 822 stating
      // msg-id = "<" addr-spec ">" ; Unique message id
      // addr-spec = local-part "@" domain ; global address
      // etc.
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

  @Nonnull
  @ReturnsMutableObject
  public ICommonsOrderedMap <String, String> customPartProperties ()
  {
    return m_aCustomPartProps;
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
                                       .append ("CM", m_eCompressionMode)
                                       .append ("Charset", m_aCharset)
                                       .getToString ();
  }

  private static void _addOutgoingHeaders (@Nonnull final WSS4JAttachment aAttachment, @Nullable final String sFilename)
  {
    // Ensure an ID is present
    if (StringHelper.hasNoText (aAttachment.getId ()))
      aAttachment.setUniqueID ();

    // Set after ID and MimeType!
    aAttachment.addHeader (CHttpHeader.CONTENT_DESCRIPTION, CONTENT_DESCRIPTION_ATTACHMENT);
    if (StringHelper.hasText (sFilename))
    {
      if (sFilename.indexOf ('"') >= 0)
        LOGGER.warn ("The filename '" +
                     sFilename +
                     "' contains a double quote which will most likely break the Content-Disposition");
      aAttachment.addHeader (CHttpHeader.CONTENT_DISPOSITION, "attachment; filename=\"" + sFilename + "\"");
    }
    aAttachment.addHeader (CHttpHeader.CONTENT_ID, CONTENT_ID_PREFIX + aAttachment.getId () + CONTENT_ID_SUFFIX);
    aAttachment.addHeader (CHttpHeader.CONTENT_TYPE, aAttachment.getMimeType ());
  }

  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final AS4OutgoingAttachment aAttachment,
                                                              @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws IOException
  {
    if (aAttachment.hasDataBytes ())
    {
      // Byte array
      final WSS4JAttachment ret = createOutgoingFileAttachment (aAttachment.getDataBytes ().bytes (),
                                                                aAttachment.getContentID (),
                                                                aAttachment.getFilename (),
                                                                aAttachment.getMimeType (),
                                                                aAttachment.getCompressionMode (),
                                                                aAttachment.getCharset (),
                                                                aResHelper);
      ret.customPartProperties ().addAll (aAttachment.customProperties ());
      return ret;
    }

    if (aAttachment.hasDataFile ())
    {
      // File based
      final WSS4JAttachment ret = createOutgoingFileAttachment (aAttachment.getDataFile (),
                                                                aAttachment.getContentID (),
                                                                aAttachment.getFilename (),
                                                                aAttachment.getMimeType (),
                                                                aAttachment.getCompressionMode (),
                                                                aAttachment.getCharset (),
                                                                aResHelper);
      ret.customPartProperties ().addAll (aAttachment.customProperties ());
      return ret;
    }

    // Must be one of the 2 variants
    throw new IllegalStateException ("Unsupported outgoing attachment data provider: " + aAttachment);
  }

  /**
   * Quasi constructor. Performs compression internally if necessary.
   *
   * @param aSrcFile
   *        Source, uncompressed, unencrypted file.
   * @param sContentID
   *        Content-ID of the attachment. If <code>null</code> a random ID is
   *        created.
   * @param sFilename
   *        Filename of the attachment. May be <code>null</code> in which case
   *        no <code>Content-Disposition</code> header is created.
   * @param aMimeType
   *        Original mime type of the file.
   * @param eCompressionMode
   *        Optional compression mode to use. May be <code>null</code>.
   * @param aCharset
   *        The character set to use. May be <code>null</code> (since 0.14.0)
   * @param aResHelper
   *        The resource manager to use. May not be <code>null</code>.
   * @return The newly created attachment instance. Never <code>null</code>.
   * @throws IOException
   *         In case something goes wrong during compression
   */
  @SuppressWarnings ("resource")
  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final File aSrcFile,
                                                              @Nullable final String sContentID,
                                                              @Nullable final String sFilename,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nullable final Charset aCharset,
                                                              @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws IOException
  {
    ValueEnforcer.notNull (aSrcFile, "File");
    ValueEnforcer.notNull (aMimeType, "MimeType");

    final WSS4JAttachment ret = new WSS4JAttachment (aResHelper, aMimeType.getAsString ());
    ret.setId (sContentID);
    ret.setCharset (aCharset);
    _addOutgoingHeaders (ret, sFilename);

    // If the attachment has an compressionMode do it directly, so that
    // encryption later on works on the compressed content
    final File aRealFile;
    if (eCompressionMode != null)
    {
      ret.setCompressionMode (eCompressionMode);

      // Create temporary file with compressed content to avoid that the
      // original is compressed more than once
      aRealFile = aResHelper.createTempFile ();
      try (final NonBlockingBufferedOutputStream aFOS = FileHelper.getBufferedOutputStream (aRealFile))
      {
        if (aFOS != null)
          try (final OutputStream aOS = eCompressionMode.getCompressStream (aFOS))
          {
            StreamHelper.copyInputStreamToOutputStream (FileHelper.getBufferedInputStream (aSrcFile), aOS);
          }
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
   * Quasi constructor. Performs compression internally.
   *
   * @param aSrcData
   *        Source in-memory data, uncompressed, unencrypted.
   * @param sContentID
   *        Optional content ID or <code>null</code> to create a random one.
   *        Filename of the attachment. May be <code>null</code> in which case
   *        no <code>Content-Disposition</code> header is created.
   * @param sFilename
   *        Optional filename to use in the "Content-Disposition" headers. May
   *        be <code>null</code>.
   * @param aMimeType
   *        Original mime type of the file. May not be <code>null</code>.
   * @param eCompressionMode
   *        Optional compression mode to use. May be <code>null</code>.
   * @param aCharset
   *        The character set to use. May be <code>null</code> (since 0.14.0)
   * @param aResHelper
   *        The resource manager to use. May not be <code>null</code>.
   * @return The newly created attachment instance. Never <code>null</code>.
   * @throws IOException
   *         In case something goes wrong during compression
   */
  @SuppressWarnings ("resource")
  @Nonnull
  public static WSS4JAttachment createOutgoingFileAttachment (@Nonnull final byte [] aSrcData,
                                                              @Nullable final String sContentID,
                                                              @Nullable final String sFilename,
                                                              @Nonnull final IMimeType aMimeType,
                                                              @Nullable final EAS4CompressionMode eCompressionMode,
                                                              @Nullable final Charset aCharset,
                                                              @Nonnull final AS4ResourceHelper aResHelper) throws IOException
  {
    ValueEnforcer.notNull (aSrcData, "Data");
    ValueEnforcer.notNull (aMimeType, "MimeType");
    ValueEnforcer.notNull (aResHelper, "ResHelper");

    final WSS4JAttachment ret = new WSS4JAttachment (aResHelper, aMimeType.getAsString ());
    ret.setId (sContentID);
    ret.setCharset (aCharset);
    _addOutgoingHeaders (ret, sFilename);

    // If the attachment has an compressionMode do it directly, so that
    // encryption later on works on the compressed content
    if (eCompressionMode != null)
    {
      ret.setCompressionMode (eCompressionMode);

      // Create temporary file with compressed content
      final File aRealFile = aResHelper.createTempFile ();
      try (final NonBlockingBufferedOutputStream aFOS = FileHelper.getBufferedOutputStream (aRealFile))
      {
        if (aFOS != null)
          try (final OutputStream aOS = eCompressionMode.getCompressStream (aFOS))
          {
            aOS.write (aSrcData);
          }
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

  /**
   * Check if an incoming attachment can be kept in memory, or if a temporary
   * file is needed.
   *
   * @param nBytes
   *        File size.
   * @return <code>true</code> if the size is &le; than 64 Kilobytes
   */
  public static boolean canBeKeptInMemory (final long nBytes)
  {
    return nBytes <= 64 * CGlobal.BYTES_PER_KILOBYTE;
  }

  @SuppressWarnings ("resource")
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

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Creating incoming WSS4J attachment with " + aBodyPart.getSize () + " bytes");

    if (canBeKeptInMemory (aBodyPart.getSize ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Keeping WSS4J attachment in-memory");

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
            throw new UncheckedIOException ("Failed to get InputStream from DataSource", ex);
          }
        }));
      }
      else
      {
        // Can only be read once
        LOGGER.warn ("Having a DataHandler that can be read only once: " + aDH);

        ret.setSourceStreamProvider (HasInputStream.once ( () -> {
          try
          {
            return aDH.getInputStream ();
          }
          catch (final IOException ex)
          {
            throw new UncheckedIOException ("Failed to get InputStream from DataHandler", ex);
          }
        }));
      }
    }
    else
    {
      // Write to temp file
      final File aTempFile = aResHelper.createTempFile ();

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Storing WSS4J attachment to temporary file '" + aTempFile.getAbsolutePath () + "'");

      try (final OutputStream aOS = FileHelper.getBufferedOutputStream (aTempFile))
      {
        aBodyPart.getDataHandler ().writeTo (aOS);
      }
      ret.setSourceStreamProvider (HasInputStream.multiple ( () -> FileHelper.getBufferedInputStream (aTempFile)));
    }

    // Read all MIME part headers
    final Enumeration <Header> aEnum = aBodyPart.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header aHeader = aEnum.nextElement ();
      ret.addHeader (aHeader.getName (), aHeader.getValue ());
    }

    // These headers are mandatory and overwrite headers from the MIME body part
    ret.addHeader (CHttpHeader.CONTENT_DESCRIPTION, CONTENT_DESCRIPTION_ATTACHMENT);
    ret.addHeader (CHttpHeader.CONTENT_ID, CONTENT_ID_PREFIX + ret.getId () + CONTENT_ID_SUFFIX);
    ret.addHeader (CHttpHeader.CONTENT_TYPE, ret.getMimeType ());

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished handling of incoming WSS4J attachment");

    return ret;
  }
}
