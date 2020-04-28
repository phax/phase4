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
package com.helger.phase4.client;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.phase4.util.Phase4Exception;

/**
 * Specialized interface for the raw HTTP response consumer.
 *
 * @author Philip Helger
 * @since 0.9.14
 */
@FunctionalInterface
public interface IAS4RawResponseConsumer extends Serializable
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
}
