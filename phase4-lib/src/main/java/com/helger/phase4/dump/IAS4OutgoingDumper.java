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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Interface for dumping outgoing requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4OutgoingDumper
{
  /**
   * Called for new requests. It's the responsibility of the caller to close the
   * created output stream.
   *
   * @param sMessageID
   *        The message ID of the outgoing message. Neither <code>null</code>
   *        nor empty.
   * @param aCustomHeaders
   *        Custom headers to be added to the HTTP entity. May be
   *        <code>null</code>.
   * @param nTry
   *        The index of the try. The first try has always index 0, the first
   *        retry has index 1, the second retry has index 2 etc. Always &ge; 0.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         written to the target stream is also written to that output stream.
   * @throws IOException
   *         in case of an error
   */
  @Nullable
  OutputStream onBeginRequest (@Nonnull @Nonempty String sMessageID,
                               @Nullable HttpHeaderMap aCustomHeaders,
                               @Nonnegative int nTry) throws IOException;

  /**
   * Called after the request is finished. Can e.g. be used to cleanup resources
   * belonging to the message. This method may not throw an exception.
   *
   * @param sMessageID
   *        The message ID for which something happens
   */
  default void onEndRequest (@Nonnull @Nonempty final String sMessageID)
  {}
}
