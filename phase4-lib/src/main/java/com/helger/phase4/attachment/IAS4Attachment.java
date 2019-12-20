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
package com.helger.phase4.attachment;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  String getId ();

  String getMimeType ();

  /**
   * @return The MIME type of the uncompressed attachment. May be
   *         <code>null</code>.
   */
  @Nullable
  String getUncompressedMimeType ();

  /**
   * Get the source stream of the attachment using the default resource helper.
   *
   * @return A non-<code>null</code> InputStream on the source.
   */
  @Nonnull
  InputStream getSourceStream ();

  /**
   * Get the source stream of the attachment using the provided resource helper.
   * This can be helpful, if the source helper is already out of scope.
   *
   * @param aResourceHelper
   *        The resource helper to use. May not be <code>null</code>.
   * @return A non-<code>null</code> InputStream on the source.
   */
  @Nonnull
  InputStream getSourceStream (@Nonnull AS4ResourceHelper aResourceHelper);

  @Nullable
  IHasInputStream getInputStreamProvider ();

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

  default boolean hasCompressionMode ()
  {
    return getCompressionMode () != null;
  }

  @Nonnull
  default Charset getCharset ()
  {
    return getCharsetOrDefault (StandardCharsets.ISO_8859_1);
  }

  @Nullable
  Charset getCharsetOrDefault (@Nullable final Charset aDefault);

  boolean hasCharset ();
}
