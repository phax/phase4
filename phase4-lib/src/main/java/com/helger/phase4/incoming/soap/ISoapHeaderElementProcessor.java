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
package com.helger.phase4.incoming.soap;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.AS4IncomingMessageState;

/**
 * Base interface for SOAP header processors that are invoked for incoming messages.
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface ISoapHeaderElementProcessor
{
  /**
   * Process the passed header element.
   *
   * @param aSoapDoc
   *        The complete SOAP document (logically no MIME parts are contained). Never
   *        <code>null</code>.
   * @param aHeaderElement
   *        The DOM node with the header element. Never <code>null</code>.
   * @param aAttachments
   *        Existing extracted attachments. Never <code>null</code> but maybe empty.
   * @param aIncomingState
   *        The current processing state (mutable implementation version needed). Never
   *        <code>null</code>.
   * @param aProcessingErrorMessagesTarget
   *        The error list to be filled in case there are processing errors. Never
   *        <code>null</code>. The list is always empty initially.
   * @return Never <code>null</code>. If {@link ESuccess#FAILURE} than the header is treated as "not
   *         handled".
   */
  @NonNull
  ESuccess processHeaderElement (@NonNull Document aSoapDoc,
                                 @NonNull Element aHeaderElement,
                                 @NonNull ICommonsList <WSS4JAttachment> aAttachments,
                                 @NonNull AS4IncomingMessageState aIncomingState,
                                 @NonNull AS4ErrorList aProcessingErrorMessagesTarget);
}
