/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;

/**
 * This is the interface that must be implemented to handle incoming SBD documents.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IPhase4PeppolIncomingSBDHandlerSPI
{
  /**
   * Handle the provided incoming StandardBusinessDocument
   *
   * @param aMessageMetadata
   *        Message metadata. Includes data when and from whom it was received. Never
   *        <code>null</code>. Since v0.9.8.
   * @param aHeaders
   *        The (HTTP) headers of the incoming request. Never <code>null</code>.
   * @param aUserMessage
   *        The received EBMS user message. Never <code>null</code>. Since v0.9.8.
   * @param aSBDBytes
   *        The raw SBD bytes. These are the bytes as received via AS4, just decrypted and
   *        decompressed. Never <code>null</code>.
   * @param aSBD
   *        The incoming parsed Standard Business Document as JAXB data model. This is the
   *        pre-parsed SBD bytes. Use {@link com.helger.sbdh.SBDMarshaller} to serialize the
   *        document. Never <code>null</code>
   * @param aPeppolSBD
   *        The pre-parsed Peppol Standard Business Document. Never <code>null</code>. Since v0.9.8.
   * @param aState
   *        The message state. Can e.g. be used to retrieve information about the certificate found
   *        in the message. Never <code>null</code>. Since v0.9.8
   * @param aProcessingErrorMessages
   *        List for error messages that occur during processing. Never <code>null</code>. Since
   *        v2.6.0.
   * @throws Exception
   *         In case it cannot be processed.
   */
  void handleIncomingSBD (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                          @Nonnull HttpHeaderMap aHeaders,
                          @Nonnull Ebms3UserMessage aUserMessage,
                          @Nonnull byte [] aSBDBytes,
                          @Nonnull StandardBusinessDocument aSBD,
                          @Nonnull PeppolSBDHData aPeppolSBD,
                          @Nonnull IAS4IncomingMessageState aState,
                          @Nonnull ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception;

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
  default void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                          @Nonnull final IAS4IncomingMessageState aIncomingState,
                                          @Nonnull @Nonempty final String sResponseMessageID,
                                          @Nullable final byte [] aResponseBytes,
                                          final boolean bResponsePayloadIsAvailable)
  {}
}
