/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.model.soapfault.AS4SoapFault;
import com.helger.phase4.util.Phase4Exception;

/**
 * Callback interface for handling a SOAP Fault that was received as the synchronous response to an
 * outgoing AS4 message.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@FunctionalInterface
public interface IAS4SoapFaultConsumer
{
  /**
   * Handle a received SOAP Fault.
   *
   * @param sSentMessageID
   *        The AS4 Message ID of the sent message the fault was received for. May be
   *        <code>null</code> if it could not be determined.
   * @param aSoapFault
   *        The received SOAP Fault. Never <code>null</code>.
   * @param aClientSentMessage
   *        The client sent message context. May be <code>null</code> if the fault interrupted the
   *        sending process before the context was created.
   * @throws Phase4Exception
   *         in case of error
   */
  void handleSoapFault (@Nullable String sSentMessageID,
                        @NonNull AS4SoapFault aSoapFault,
                        @Nullable AS4ClientSentMessage <byte []> aClientSentMessage) throws Phase4Exception;
}
