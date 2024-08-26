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
package com.helger.phase4.attachment;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.helger.phase4.util.AS4ResourceHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

/**
 * Factory interface for {@link WSS4JAttachment} objects for handling incoming
 * attachments.
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IAS4IncomingAttachmentFactory
{
  /**
   * Create an attachment if the source message is a MIME message
   *
   * @param aBodyPart
   *        The attachment body part
   * @param aResHelper
   *        The resource manager to use. May not be <code>null</code>.
   * @return The internal attachment representation. Never <code>null</code>.
   * @throws IOException
   *         In case of IO error
   * @throws MessagingException
   *         In case MIME part reading fails.
   */
  @Nonnull
  WSS4JAttachment createAttachment (@Nonnull MimeBodyPart aBodyPart, @Nonnull AS4ResourceHelper aResHelper)
                                                                                                            throws IOException,
                                                                                                            MessagingException;

  /**
   * The default instance of {@link IAS4IncomingAttachmentFactory} that uses
   * {@link WSS4JAttachment#createIncomingFileAttachment(MimeBodyPart, AS4ResourceHelper)}
   */
  @Nonnull
  IAS4IncomingAttachmentFactory DEFAULT_INSTANCE = WSS4JAttachment::createIncomingFileAttachment;
}
