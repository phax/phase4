package com.helger.as4lib.attachment;

import java.io.Serializable;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.Attachment;

import com.helger.commons.id.IHasID;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * Base interface for an attachment.
 *
 * @author Philip Helger
 */
public interface IAS4Attachment extends IHasID <String>, Serializable
{
  /**
   * @return The content transfer encoding to be used. Required for MIME
   *         multipart handling only.
   */
  @Nonnull
  EContentTransferEncoding getContentTransferEncoding ();

  /**
   * @return The charset to be used for text-based MIME types. By default no
   *         MIME type is present.
   */
  @Nullable
  Charset getCharset ();

  /**
   * @return <code>true</code> if a charset is defined, <code>false</code>
   *         otherwise.
   */
  default boolean hasCharset ()
  {
    return getCharset () != null;
  }

  /**
   * @return The MIME type to be used for this attachment.
   */
  @Nonnull
  IMimeType getMimeType ();

  /**
   * @return The MIME type to be used for this attachment.
   */
  @Nonnull
  default IMimeType getMimeTypeWithCharset ()
  {
    final Charset aCharset = getCharset ();
    return aCharset == null ? getMimeType ()
                            : new MimeType (getMimeType ()).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                          aCharset.name ());
  }

  /**
   * AS4 spec, 3.1 Compression<br>
   * When compression, signature and encryption are required, any attached
   * payload(s) MUST be compressed prior to being signed and/or encrypted.
   *
   * @return The compression mode to be used. May be <code>null</code> to
   *         indicate no compression.
   */
  @Nullable
  EAS4CompressionMode getCompressionMode ();

  /**
   * @return <code>true</code> if this attachment should be compressed,
   *         <code>false</code> otherwise.
   */
  default boolean hasCompressionMode ()
  {
    return getCompressionMode () != null;
  }

  /**
   * @param aMimeMultipart
   *        The multipart message to add to. May not be <code>null</code>.
   */
  void addToMimeMultipart (@Nonnull MimeMultipart aMimeMultipart, boolean bEncrypt) throws MessagingException;

  /**
   * @return This attachment as a WSS4J attachment. May not be
   *         <code>null</code>.
   */
  @Nonnull
  Attachment getAsWSS4JAttachment ();

  void setEncryptedAttachment (final Attachment aEncryptedAttachment);
}
