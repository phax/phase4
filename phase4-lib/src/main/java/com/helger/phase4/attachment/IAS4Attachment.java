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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Read-only interface for an attachment.
 *
 * @author Philip Helger
 */
public interface IAS4Attachment
{
  /**
   * @return The attachment Content ID. Must NOT contain any "cid" prefix.
   */
  String getId ();

  /**
   * @return The MIME type in a string representation. If the attachment was
   *         compressed and the <code>MimeType</code> part property was set it
   *         can be queried via {@link #getUncompressedMimeType()}
   * @see #getUncompressedMimeType()
   */
  String getMimeType ();

  /**
   * @return The MIME type of the uncompressed attachment. May be
   *         <code>null</code>. This is only set, if compression was active.
   *         Otherwise use {@link #getMimeType()}.
   * @see #getMimeType()
   */
  @Nullable
  String getUncompressedMimeType ();

  /**
   * Get the source stream of the attachment using the default resource helper.
   * The streams retrieved by this method are automatically closed when the
   * respective resource helper goes out of scope.
   *
   * @return A non-<code>null</code> InputStream on the source.
   */
  @Nonnull
  InputStream getSourceStream ();

  /**
   * Get the source stream of the attachment using the provided resource helper,
   * to automatically close the stream at the end of the message. This can be
   * helpful, if the source helper is already out of scope.
   *
   * @param aResourceHelper
   *        The resource helper to use. May not be <code>null</code>.
   * @return A non-<code>null</code> InputStream on the source.
   */
  @Nonnull
  InputStream getSourceStream (@Nonnull AS4ResourceHelper aResourceHelper);

  /**
   * This is primarily an internal method. If you use it, you need to make sure
   * to close the streams yourself if you open them.
   *
   * @return The internal input stream provider. May be <code>null</code>.
   */
  @Nullable
  IHasInputStream getInputStreamProvider ();

  /**
   * @return <code>true</code> if the input stream backing this attachment can
   *         be read multiple times, <code>false</code> if not.
   */
  default boolean isRepeatable ()
  {
    final IHasInputStream aISP = getInputStreamProvider ();
    return aISP != null && aISP.isReadMultiple ();
  }

  /**
   * @return The content transfer encoding to be used. Required for MIME
   *         multipart handling only. May not be <code>null</code>.
   */
  @Nonnull
  EContentTransferEncoding getContentTransferEncoding ();

  /**
   * @return The compression mode to use or <code>null</code> if the attachment
   *         is not compressed.
   */
  @Nullable
  EAS4CompressionMode getCompressionMode ();

  /**
   * @return <code>true</code> if a compression mode is set, <code>false</code>
   *         if not.
   */
  default boolean hasCompressionMode ()
  {
    return getCompressionMode () != null;
  }

  /**
   * @return The defined character set, falling back to ISO-8859-1 if none is
   *         defined.
   */
  @Nonnull
  default Charset getCharset ()
  {
    return getCharsetOrDefault (StandardCharsets.ISO_8859_1);
  }

  /**
   * Get the specified character set or the provided default value.
   *
   * @param aDefault
   *        The default value to be returned, if no character set is provided.
   *        May be <code>null</code>.
   * @return Only <code>null</code> if no character set is defined and the
   *         provided default value is <code>null</code>.
   */
  @Nullable
  Charset getCharsetOrDefault (@Nullable Charset aDefault);

  /**
   * @return <code>true</code> if a character set is defined, <code>false</code>
   *         if not.
   */
  boolean hasCharset ();

  /**
   * @return A non-<code>null</code> but maybe empty map of custom
   *         PartInfo/PartProperties for the UserMessage.
   * @since 0.12.0
   */
  @Nonnull
  @ReturnsMutableObject
  ICommonsOrderedMap <String, String> customPartProperties ();
}
