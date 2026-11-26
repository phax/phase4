/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;

/**
 * An incoming document (e.g. a payload attachment) that can be read as often as needed, without
 * being held in memory as a whole. Every call to {@link #getInputStream()} reads the document
 * again from the start - e.g. decompressing it again - so reading it costs CPU but no heap.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public interface IAS4IncomingDocument
{
  /**
   * Open a new stream over the document. These are the bytes as received via AS4, just decrypted
   * and decompressed. The caller is responsible for closing the stream.
   *
   * @return A new input stream on every call. Never <code>null</code>.
   * @throws IOException
   *         If the stream cannot be opened, e.g. because the decompression fails
   */
  @NonNull
  InputStream getInputStream () throws IOException;

  /**
   * Read the complete document into memory. This is a convenience method for small documents only -
   * for large documents prefer {@link #getInputStream()}, as the memory needed by this method
   * depends on the document size.
   *
   * @return The document bytes. Never <code>null</code>.
   * @throws IOException
   *         If the document cannot be read
   */
  default byte @NonNull [] getAllBytes () throws IOException
  {
    try (final InputStream aIS = getInputStream ())
    {
      return aIS.readAllBytes ();
    }
  }
}
