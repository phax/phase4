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
package com.helger.phase4.client;

import javax.annotation.Nonnull;

import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.util.Phase4Exception;

/**
 * Specialized interface for the EBMS 3 User Message consumer.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@FunctionalInterface
public interface IAS4UserMessageConsumer
{
  /**
   * Handling an EBMS 3 User Message
   *
   * @param aUserMsg
   *        The User Message domain object. Never <code>null</code>.
   * @param aMessageMetadata
   *        The message metadata of the synchronously received message. Never
   *        <code>null</code>. Added in v2.5.0.
   * @param aState
   *        The internal processing state of the signal message. Never
   *        <code>null</code>. Added in v2.5.0.
   * @throws Phase4Exception
   *         in case of error
   */
  void handleUserMessage (@Nonnull Ebms3UserMessage aUserMsg,
                          @Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                          @Nonnull IAS4MessageState aState) throws Phase4Exception;
}
