/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging;

import java.time.LocalDateTime;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * This interface lets you access optional metadata for a single incoming
 * message.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public interface IAS4IncomingMessageMetadata
{
  /**
   * @return A unique ID created just for this message metadata. It can be used
   *         to reference to this message internally. Usually this is a UUID.
   *         Never <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getIncomingUniqueID ();

  /**
   * @return The date and time when the request was received. Never
   *         <code>null</code>.
   */
  @Nonnull
  LocalDateTime getIncomingDT ();

  /**
   * @return The message mode. May be <code>null</code>.
   */
  @Nonnull
  EAS4IncomingMessageMode getMode ();

  /**
   * Returns the Internet Protocol (IP) address of the client or last proxy that
   * sent the request.
   *
   * @return a <code>String</code> containing the IP address of the client that
   *         sent the request
   */
  @Nullable
  String getRemoteAddr ();

  default boolean hasRemoteAddr ()
  {
    return StringHelper.hasText (getRemoteAddr ());
  }

  /**
   * Returns the fully qualified name of the client or the last proxy that sent
   * the request. If the engine cannot or chooses not to resolve the hostname
   * (to improve performance), this method returns the dotted-string form of the
   * IP address.
   *
   * @return a <code>String</code> containing the fully qualified name of the
   *         client
   */
  @Nullable
  String getRemoteHost ();

  default boolean hasRemoteHost ()
  {
    return StringHelper.hasText (getRemoteHost ());
  }

  /**
   * Returns the Internet Protocol (IP) source port of the client or last proxy
   * that sent the request.
   *
   * @return an integer specifying the port number or a negative value if not
   *         set
   */
  @CheckForSigned
  int getRemotePort ();

  default boolean hasRemotePort ()
  {
    return getRemotePort () > 0;
  }
}
