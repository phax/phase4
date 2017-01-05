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

import java.io.Serializable;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeMultipart;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.commons.id.IHasID;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * Base interface for an attachment to be used for sending messages.
 *
 * @author Philip Helger
 */
public interface IAS4OutgoingAttachment extends IHasID <String>, Serializable
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
  void addToMimeMultipart (@Nonnull MimeMultipart aMimeMultipart) throws Exception;

  /**
   * @return This attachment as a WSS4J attachment. May not be
   *         <code>null</code>.
   */
  @Nonnull
  WSS4JAttachment getAsWSS4JAttachment ();
}
