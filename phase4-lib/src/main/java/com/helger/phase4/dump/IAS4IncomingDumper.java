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
package com.helger.phase4.dump;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * Interface for dumping incoming requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4IncomingDumper
{
  /**
   * Called for new incoming AS4 requests. It's the responsibility of the caller
   * to close the created output stream.
   *
   * @param aIncomingMessageMetadata
   *        Message metadata. Never <code>null</code>. Since v0.9.8.
   * @param aHttpHeaderMap
   *        The HTTP headers of the request. Never <code>null</code>.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         read from the source stream is written to that output stream. The
   *         OutputStream must be closed by the caller.
   * @throws IOException
   *         in case of an error
   * @since v0.9.6 the parameter changed from HttpServletRequest to
   *        {@link HttpHeaderMap}
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                             @Nonnull HttpHeaderMap aHttpHeaderMap) throws IOException;

  /**
   * Called after the AS4 request is handled internally. Can e.g. be used to
   * cleanup resources belonging to the message. This method may not throw an
   * exception. Since 1.3.0 this method is only called, if
   * {@link #onNewRequest(IAS4IncomingMessageMetadata, HttpHeaderMap)} returned
   * non-<code>null</code>.
   *
   * @param aIncomingMessageMetadata
   *        Message metadata. Never <code>null</code>.
   * @param aCaughtException
   *        An eventually caught exception.
   * @since v0.9.9
   */
  void onEndRequest (@Nonnull IAS4IncomingMessageMetadata aIncomingMessageMetadata, @Nullable Exception aCaughtException);
}
