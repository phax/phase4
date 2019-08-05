/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.client;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;

import com.helger.as4.messaging.domain.AbstractAS4Message;

/**
 * Callback interface for AS4 client message creation.
 *
 * @author Philip Helger
 */
public interface IAS4ClientBuildMessageCallback extends Serializable
{
  /**
   * Called for the created domain object. That usually also contains the
   * underlying Ebms 3 data model. This method is called for all types.
   *
   * @param aMsg
   *        The created message
   */
  default void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
  {}

  /**
   * Called for the unsigned and unencrypted SOAP document. This method is
   * called for all types.
   *
   * @param aDoc
   *        The created SOAP document
   */
  default void onSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * Called for the signed SOAP document. Not all client messages support
   * signing.
   *
   * @param aDoc
   *        The signed SOAP document
   */
  default void onSignedSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and no attachments are
   * present. Only called for user messages.
   *
   * @param aDoc
   *        The encrypted SOAP document
   */
  default void onEncryptedSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and at least one
   * attachments is present. Only called for user messages.
   *
   * @param aMimeMsg
   *        The encrypted MIME message
   */
  default void onEncryptedMimeMessage (@Nonnull final MimeMessage aMimeMsg)
  {}
}
