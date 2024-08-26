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
package com.helger.phase4.incoming.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * Specific callback interface to inform interested entities about the end of
 * processing of an incoming message.<br>
 * Note: this interface is NOT called for outgoing messages, as for sending it
 * is clean and deterministic when it is done.
 *
 * @author Philip Helger
 * @since 2.5.0
 */
@IsSPIInterface
public interface IAS4IncomingMessageProcessingStatusSPI
{
  /**
   * This method is called before the incoming message is started to be
   * processed. It is called before dumping is started.
   *
   * @param aMessageMetadata
   *        The message metadata of the incoming message for aligning it.
   */
  void onMessageProcessingStarted (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata);

  /**
   * This method is called after the incoming message is completely processed.
   * It is called after dumping is finalized.
   *
   * @param aMessageMetadata
   *        The message metadata of the incoming message for aligning it.
   * @param aCaughtException
   *        In case message processing failed an exception was thrown, it is
   *        contained in here. You may use it to identify errors in processing.
   */
  void onMessageProcessingEnded (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nullable Exception aCaughtException);
}
