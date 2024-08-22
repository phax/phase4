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
package com.helger.phase4.servlet.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Node;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.v3.ChangePhase4V3;

/**
 * Implement this SPI interface to handle incoming messages appropriate.
 *
 * @author Philip Helger
 */
@IsSPIInterface
@ChangePhase4V3 ("Rename to 'IAS4IncomingMessageProcessorSPI'")
public interface IAS4ServletMessageProcessorSPI
{
  /**
   * Process incoming AS4 user message
   *
   * @param aMessageMetadata
   *        Message metadata. Never <code>null</code>. Since v0.9.8.
   * @param aHttpHeaders
   *        The original HTTP headers. Never <code>null</code>.
   * @param aUserMessage
   *        The received user message. May not be <code>null</code>.
   * @param aPMode
   *        The source PMode used to parse the message.
   * @param aPayload
   *        Extracted, decrypted and verified payload node (e.g. SBDH). May be
   *        <code>null</code>. May also be <code>null</code> if a MIME message
   *        comes in - in that case the SOAP body MUST be empty and the main
   *        payload can be found in aIncomingAttachments[0].
   * @param aIncomingAttachments
   *        Extracted, decrypted and verified attachments. May be
   *        <code>null</code> or empty if no attachments are present.
   * @param aState
   *        The current message state. Can be used to determine all other things
   *        potentially necessary for processing the incoming message. Never
   *        <code>null</code>.
   * @param aProcessingErrorMessages
   *        List for error messages that occur during processing. Never
   *        <code>null</code>.
   * @return A non-<code>null</code> result object. If a failure is returned,
   *         the message of the failure object itself is returned as an
   *         EBMS_OTHER error.
   */
  @Nonnull
  AS4MessageProcessorResult processAS4UserMessage (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                                                   @Nonnull HttpHeaderMap aHttpHeaders,
                                                   @Nonnull Ebms3UserMessage aUserMessage,
                                                   @Nonnull IPMode aPMode,
                                                   @Nullable Node aPayload,
                                                   @Nullable ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                   @Nonnull IAS4MessageState aState,
                                                   @Nonnull ICommonsList <Ebms3Error> aProcessingErrorMessages);

  /**
   * Process incoming AS4 signal message - pull-request and receipt.<br>
   * Attachment and Payload are not needed since they are allowed, but should
   * not be added to a SignalMessage Because the will be ignored in the MSH -
   * Processing.
   *
   * @param aMessageMetadata
   *        Request metadata. Never <code>null</code>. Since v0.9.8.
   * @param aHttpHeaders
   *        The original HTTP headers. Never <code>null</code>.
   * @param aSignalMessage
   *        The received signal message. May not be <code>null</code>.
   * @param aPMode
   *        PMode - only needed for pull-request. May be <code>null</code>.
   * @param aState
   *        The current message state. Can be used to determine all other things
   *        potentially necessary for processing the incoming message. Never
   *        <code>null</code>.
   * @param aProcessingErrorMessages
   *        List for error messages that occur during processing. Never
   *        <code>null</code>.
   * @return A non-<code>null</code> result object. If a failure is returned,
   *         the message of the failure object itself is returned as an
   *         EBMS_OTHER error.
   */
  @Nonnull
  AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                                                           @Nonnull HttpHeaderMap aHttpHeaders,
                                                           @Nonnull Ebms3SignalMessage aSignalMessage,
                                                           @Nullable IPMode aPMode,
                                                           @Nonnull IAS4MessageState aState,
                                                           @Nonnull ICommonsList <Ebms3Error> aProcessingErrorMessages);

  /**
   * Optional callback to process a response message
   *
   * @param aMessageMetadata
   *        Incoming message metadata. Never <code>null</code>.
   * @param aState
   *        The current message state. Can be used to determine all other things
   *        potentially necessary for processing the response message. Never
   *        <code>null</code>.
   * @param sResponseMessageID
   *        The AS4 message ID of the response. Neither <code>null</code> nor
   *        empty. Since v1.2.0.
   * @param aResponseBytes
   *        The response bytes to be written. May be <code>null</code> for
   *        several reasons.
   * @param bResponsePayloadIsAvailable
   *        This indicates if a response payload is available at all. If this is
   *        <code>false</code> than the response bytes are <code>null</code>.
   *        Special case: if this is <code>true</code> and response bytes is
   *        <code>null</code> than most likely the response entity is not
   *        repeatable and cannot be handled more than once - that's why it is
   *        <code>null</code> here in this callback, but non-<code>null</code>
   *        in the originally returned message.
   * @since v0.9.8
   */
  @ChangePhase4V3 ("Remove default")
  default void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                          @Nonnull final IAS4MessageState aState,
                                          @Nonnull @Nonempty final String sResponseMessageID,
                                          @Nullable final byte [] aResponseBytes,
                                          final boolean bResponsePayloadIsAvailable)
  {
    // Do nothing for backwards compatibility
  }
}
