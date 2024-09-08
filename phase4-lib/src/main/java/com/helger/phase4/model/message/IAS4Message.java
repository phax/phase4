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
package com.helger.phase4.model.message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.model.ESoapVersion;

/**
 * Base interface for an AS4 message.
 *
 * @author Philip Helger
 */
public interface IAS4Message
{
  /**
   * @return The SOAP version to use. May not be <code>null</code>.
   * @since v0.9.8
   */
  @Nonnull
  ESoapVersion getSoapVersion ();

  /**
   * @return The type of the underlying message. Never <code>null</code>.
   */
  @Nonnull
  EAS4MessageType getMessageType ();

  /**
   * @return The ID of the "Messaging" element for referencing in signing.
   *         Should not be <code>null</code>. This is NOT the AS4 Message ID.
   */
  @Nonnull
  @Nonempty
  String getMessagingID ();

  /**
   * Set the "mustUnderstand" value depending on the used SOAP version.
   *
   * @param bMustUnderstand
   *        <code>true</code> for must understand, <code>false</code> otherwise.
   * @return this for chaining
   */
  @Nonnull
  IAS4Message setMustUnderstand (boolean bMustUnderstand);

  /**
   * Create a SOAP document from this message without a SOAP body payload.
   *
   * @return The created DOM document
   * @since v0.9.8
   * @see #getAsSoapDocument(Node)
   */
  @Nonnull
  default Document getAsSoapDocument ()
  {
    return getAsSoapDocument ((Node) null);
  }

  /**
   * Create a SOAP document from this message with the specified optional SOAP
   * body payload. Attachments are not handled by this method.
   *
   * @param aSoapBodyPayload
   *        The payload to be added into the SOAP body. May be
   *        <code>null</code>.
   * @return The created DOM document.
   * @since v0.9.8
   * @see #getAsSoapDocument()
   */
  @Nonnull
  Document getAsSoapDocument (@Nullable Node aSoapBodyPayload);
}
