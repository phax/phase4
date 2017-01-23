/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.attachment;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.as4.util.AS4ResourceManager;

/**
 * Default implementation of {@link IIncomingAttachmentFactory}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class DefaultIncomingAttachmentFactory implements IIncomingAttachmentFactory
{
  @Nonnull
  public WSS4JAttachment createAttachment (@Nonnull final AS4ResourceManager aResMgr,
                                           @Nonnull final MimeBodyPart aBodyPart) throws IOException, MessagingException
  {
    return WSS4JAttachment.createIncomingFileAttachment (aBodyPart, aResMgr);
  }
}
