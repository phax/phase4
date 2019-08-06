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
package com.helger.as4.dump;

import java.io.IOException;
import java.io.OutputStream;

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
   * Called for new requests.
   *
   * @param sMessageID
   *        The message ID of the outgoing message. Neither <code>null</code>
   *        nor empty.
   * @param Custom
   *        headers to be added to the HTTP entity. May be <code>null</code>.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         written to the target stream is also written to that output stream.
   * @throws IOException
   *         in case of an error
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull @Nonempty String sMessageID,
                             @Nullable HttpHeaderMap aCustomHeaders) throws IOException;
}
