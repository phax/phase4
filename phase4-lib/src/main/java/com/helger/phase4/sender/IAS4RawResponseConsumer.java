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
package com.helger.phase4.sender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.util.Phase4Exception;

/**
 * Specialized interface for the raw HTTP response consumer.
 *
 * @author Philip Helger
 * @since 0.9.14
 */
@FunctionalInterface
public interface IAS4RawResponseConsumer
{
  /**
   * Handling a HTTP response
   *
   * @param aResponseMsg
   *        The response message in relation to the source message
   * @throws Phase4Exception
   *         In case of error.
   */
  void handleResponse (@Nonnull AS4ClientSentMessage <byte []> aResponseMsg) throws Phase4Exception;

  /**
   * Chain this instance with another instance of the same type. This handler is
   * called first.
   *
   * @param rhs
   *        The handler to chain with. May be <code>null</code>.
   * @return A non-<code>null</code> response consumer.
   * @since 0.13.0
   */
  @Nonnull
  default IAS4RawResponseConsumer and (@Nullable final IAS4RawResponseConsumer rhs)
  {
    return and (this, rhs);
  }

  /**
   * Chain two instances of the same type to a single instance. This first
   * handler is called first, the second handler is called afterwards. If any of
   * the two parameters is <code>null</code> no chaining happens.
   *
   * @param lhs
   *        The first handler to invoke. May be <code>null</code>.
   * @param rhs
   *        The second handler to invoke. May be <code>null</code>.
   * @return <code>null</code> if both parameters are <code>null</code>.
   * @since 0.13.0
   */
  @Nullable
  static IAS4RawResponseConsumer and (@Nullable final IAS4RawResponseConsumer lhs,
                                      @Nullable final IAS4RawResponseConsumer rhs)
  {
    if (lhs == null)
      return rhs;

    if (rhs == null)
      return lhs;

    return x -> {
      lhs.handleResponse (x);
      rhs.handleResponse (x);
    };
  }
}
