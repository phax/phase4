/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.servlet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.dbnalliance.xhe.DBNAllianceXHEData;
import com.helger.http.header.HttpHeaderMap;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;

/**
 * This is the interface that must be implemented to handle incoming SBD documents.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IPhase4DBNAllianceIncomingXHEHandlerSPI
{
  /**
   * Handle the provided incoming StandardBusinessDocument
   *
   * @param aMessageMetadata
   *        Message metadata. Includes data when and from whom it was received. Never
   *        <code>null</code>.
   * @param aHeaders
   *        The (HTTP) headers of the incoming request. Never <code>null</code>.
   * @param aUserMessage
   *        The received EBMS user message. Never <code>null</code>.
   * @param aIncomingXHE
   *        The incoming Exchange Header Envelope. It is not held in memory: use
   *        {@link Phase4DBNAllianceIncomingXHE#getInputStream()} to read the bytes as received via
   *        AS4, just decrypted and decompressed - as often as needed, and the payload specific
   *        methods (e.g. {@link Phase4DBNAllianceIncomingXHE#openPayloadBinaryStream(int)}) to read
   *        the content of a single payload. Use {@link Phase4DBNAllianceIncomingXHE#getXHE()} only
   *        if the parsed document is really needed, as its memory depends on the document size. The
   *        data is only available while this method runs. Never <code>null</code>. Since v5.0.0.
   * @param aDBNAllianceXHE
   *        The pre-parsed DBNAlliance Exchange Header Envelope data. It was read in a streaming
   *        way, so the payloads contain no content, only its kind and for XML payloads the name of
   *        the root element. The payload indices match the ones of the payload specific methods of
   *        the incoming XHE. Never <code>null</code>.
   * @param aState
   *        The message state. Can e.g. be used to retrieve information about the certificate found
   *        in the message. Never <code>null</code>.
   * @param aProcessingErrorMessages
   *        List for error messages that occur during processing. Never <code>null</code>.
   * @throws Exception
   *         In case it cannot be processed.
   */
  void handleIncomingXHE (@NonNull IAS4IncomingMessageMetadata aMessageMetadata,
                          @NonNull HttpHeaderMap aHeaders,
                          @NonNull Ebms3UserMessage aUserMessage,
                          @NonNull Phase4DBNAllianceIncomingXHE aIncomingXHE,
                          @NonNull DBNAllianceXHEData aDBNAllianceXHE,
                          @NonNull IAS4IncomingMessageState aState,
                          @NonNull AS4ErrorList aProcessingErrorMessages) throws Exception;

  /**
   * Optional callback to process a response message
   *
   * @param aIncomingMessageMetadata
   *        Incoming message metadata. Never <code>null</code>.
   * @param aIncomingState
   *        The current message state. Can be used to determine all other things potentially
   *        necessary for processing the response message. Never <code>null</code>.
   * @param sResponseMessageID
   *        The AS4 message ID of the response. Neither <code>null</code> nor empty. Since v1.2.0.
   * @param aResponseBytes
   *        The response bytes to be written. May be <code>null</code> for several reasons.
   * @param bResponsePayloadIsAvailable
   *        This indicates if a response payload is available at all. If this is <code>false</code>
   *        than the response bytes are <code>null</code>. Special case: if this is
   *        <code>true</code> and response bytes is <code>null</code> than most likely the response
   *        entity is not repeatable and cannot be handled more than once - that's why it is
   *        <code>null</code> here in this callback, but non-<code>null</code> in the originally
   *        returned message.
   * @since v3.1.0
   */
  void processAS4ResponseMessage (@NonNull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                  @NonNull final IAS4IncomingMessageState aIncomingState,
                                  @NonNull @Nonempty final String sResponseMessageID,
                                  final byte @Nullable [] aResponseBytes,
                                  final boolean bResponsePayloadIsAvailable);
}
