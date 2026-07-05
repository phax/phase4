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
package com.helger.phase4.client;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.model.message.AbstractAS4Message;

/**
 * Callback interface for AS4 client message creation.
 *
 * @author Philip Helger
 */
public interface IAS4ClientBuildMessageCallback
{
  /**
   * Called for the created domain object. That usually also contains the underlying EBMS 3 data
   * model. This method is called for all types.
   *
   * @param aMsg
   *        The created message
   */
  default void onAS4Message (@NonNull final AbstractAS4Message <?> aMsg)
  {}

  /**
   * Called for all attachments of a User Message, after eventual compression was applied, but
   * before signing and encryption took place. This is the way to access the compressed attachment
   * data over which the digital signature digests are calculated - e.g. to preserve it for
   * non-repudiation purposes, because GZIP compression is not deterministic, and the compressed
   * data cannot be re-created from the uncompressed data. Use
   * {@link com.helger.phase4.attachment.IAS4Attachment#getCompressedSourceStreamProvider()} to
   * access the compressed data of compressed attachments. The attachment data is backed by
   * temporary files that are deleted when the sending process is finished, so the data must be
   * consumed inside this method. Only called for User Messages with at least one attachment.
   *
   * @param aAttachments
   *        The outgoing attachments. Never <code>null</code> nor empty. Must not be modified.
   * @since 4.5.4
   */
  default void onBuiltAttachments (@NonNull @Nonempty final ICommonsList <WSS4JAttachment> aAttachments)
  {}

  /**
   * Called for the unsigned and unencrypted SOAP document. This method is called for all types.
   *
   * @param aDoc
   *        The created SOAP document
   */
  default void onSoapDocument (@NonNull final Document aDoc)
  {}

  /**
   * Called for the signed SOAP document. Not all client messages support signing.
   *
   * @param aDoc
   *        The signed SOAP document
   */
  default void onSignedSoapDocument (@NonNull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and no attachments are present. Only called
   * for User Messages.
   *
   * @param aDoc
   *        The encrypted SOAP document
   */
  default void onEncryptedSoapDocument (@NonNull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and at least one attachments is present.
   * Only called for User Messages.
   *
   * @param aMimeMsg
   *        The encrypted MIME message
   */
  default void onEncryptedMimeMessage (@NonNull final AS4MimeMessage aMimeMsg)
  {}
}
