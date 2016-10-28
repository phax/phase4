/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.receive.soap;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.Attachment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;

/**
 * Base interface for SOAP header processors that are invoked for incoming
 * messages.
 *
 * @author Philip Helger
 */
public interface ISOAPHeaderElementProcessor
{
  /**
   * Process the passed header element.
   *
   * @param aSOAPDoc
   *        The complete SOAP document (logically no MIME parts are contained).
   *        Never <code>null</code>.
   * @param aHeaderElement
   *        The DOM node with the header element. Never <code>null</code>.
   * @param aAttachments
   *        Existing extracted attachments. Never <code>null</code> but maybe
   *        empty.
   * @param aState
   *        The current processing state. Never <code>null</code>.
   * @param aErrorList
   *        The error list to be filled in case there are processing errors.
   *        Never <code>null</code>. The list is always empty initially.
   * @param aLocale
   *        The locale to be used. May not be <code>null</code>.
   * @return Never <code>null</code>. If {@link ESuccess#FAILURE} than the
   *         header is treated as "not handled".
   */
  @Nonnull
  ESuccess processHeaderElement (@Nonnull Document aSOAPDoc,
                                 @Nonnull Element aHeaderElement,
                                 @Nonnull ICommonsList <Attachment> aAttachments,
                                 @Nonnull AS4MessageState aState,
                                 @Nonnull ErrorList aErrorList,
                                 @Nonnull Locale aLocale);
}
