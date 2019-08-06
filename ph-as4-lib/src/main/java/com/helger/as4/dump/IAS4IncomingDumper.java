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
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

/**
 * Interface for dumping incoming requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4IncomingDumper extends Serializable
{
  /**
   * Called for new requests.
   *
   * @param aHttpServletRequest
   *        The current servlet request.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         read from the source stream is written to that output stream.
   * @throws IOException
   *         in case of an error
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull HttpServletRequest aHttpServletRequest) throws IOException;
}
