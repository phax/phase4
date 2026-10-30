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
package com.helger.phase4.incoming;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;

/**
 * A callback interface that allows to modify an outgoing Receipt or Error signal message, after the
 * SOAP document was created but before it is signed. This is the only place where additional SOAP
 * header elements can be added to a response signal message in a way that they can also be covered
 * by the signature.<br>
 * This interface is deliberately generic - it carries no knowledge of any specific AS4 profile or
 * messaging topology.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@FunctionalInterface
public interface IAS4ResponseSignalCustomizer
{
  /**
   * Invoked after the Receipt or Error SOAP document was created and <b>before</b> it is signed.
   *
   * @param aIncomingState
   *        The processing state of the message that is being answered. Never <code>null</code>.
   * @param eResponseType
   *        The type of the response message. Either {@link EAS4MessageType#RECEIPT} or
   *        {@link EAS4MessageType#ERROR_MESSAGE}. Never <code>null</code>.
   * @param eResponseSoapVersion
   *        The SOAP version used for the response message. Never <code>null</code>.
   * @param aUnsignedResponseDoc
   *        The mutable response document. Never <code>null</code>. Modifications performed here are
   *        part of the document that is signed afterwards.
   * @param aResponseSigningParams
   *        The signing parameters that will be used, or <code>null</code> if the response is not
   *        signed at all. This is a per-response clone, so it may safely be modified - e.g. by
   *        calling
   *        {@link AS4SigningParams#setWSSecSignatureCustomizer(com.helger.phase4.crypto.IWSSecSignatureCustomizer)}
   *        to have additional parts covered by the signature.
   */
  void customizeResponseSignal (@NonNull IAS4IncomingMessageState aIncomingState,
                                @NonNull EAS4MessageType eResponseType,
                                @NonNull ESoapVersion eResponseSoapVersion,
                                @NonNull Document aUnsignedResponseDoc,
                                @Nullable AS4SigningParams aResponseSigningParams);
}
