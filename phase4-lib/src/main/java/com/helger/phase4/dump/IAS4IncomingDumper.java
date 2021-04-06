/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.dump;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;

/**
 * Interface for dumping incoming requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4IncomingDumper
{
  /**
   * Called for new requests. It's the responsibility of the caller to close the
   * created output stream.
   *
   * @param aMessageMetadata
   *        Message metadata. Never <code>null</code>. Since v0.9.8.
   * @param aHttpHeaderMap
   *        The HTTP headers of the request. Never <code>null</code>.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         read from the source stream is written to that output stream.
   * @throws IOException
   *         in case of an error
   * @since v0.9.6 the parameter changed from HttpServletRequest to
   *        {@link HttpHeaderMap}
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                             @Nonnull HttpHeaderMap aHttpHeaderMap) throws IOException;

  /**
   * Called after the request is finished. Can e.g. be used to cleanup resources
   * belonging to the message. This method may not throw an exception. This
   * method is called, also if
   * {@link #onNewRequest(IAS4IncomingMessageMetadata, HttpHeaderMap)} returned
   * <code>null</code>.
   *
   * @param aMessageMetadata
   *        Message metadata. Never <code>null</code>.
   * @since v0.9.9
   */
  default void onEndRequest (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata)
  {}
}
