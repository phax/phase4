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
package com.helger.phase4.incoming;

import javax.annotation.Nonnull;

import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.util.Phase4Exception;

/**
 * Specialized interface for the EBMS 3 Signal Message consumer.
 *
 * @author Philip Helger
 * @since 0.9.14
 */
@FunctionalInterface
public interface IAS4SignalMessageConsumer
{
  /**
   * Handling an EBMS 3 Signal Message. Make sure to copy all attachments you
   * are interested in, because by default they are only available based on
   * temporary files during the processing of the inbound request.
   *
   * @param aEbmsSignalMsg
   *        The Signal Message domain object. Never <code>null</code>.
   * @param aIncomingMessageMetadata
   *        The message metadata of the synchronously received message. Never
   *        <code>null</code>. Added in v2.5.0.
   * @param aIncomingState
   *        The internal processing state of the signal message. Never
   *        <code>null</code>. Added in v2.5.0.
   * @throws Phase4Exception
   *         in case of error
   */
  void handleSignalMessage (@Nonnull Ebms3SignalMessage aEbmsSignalMsg,
                            @Nonnull IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                            @Nonnull IAS4IncomingMessageState aIncomingState) throws Phase4Exception;
}
