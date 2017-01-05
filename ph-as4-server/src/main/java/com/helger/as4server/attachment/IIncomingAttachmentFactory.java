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
package com.helger.as4server.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.as4lib.attachment.incoming.IAS4IncomingAttachment;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;

/**
 * Factory interface for {@link IAS4IncomingAttachment} objects.
 *
 * @author Philip Helger
 */
public interface IIncomingAttachmentFactory extends Serializable
{
  @Nonnull
  IAS4IncomingAttachment createAttachment (@Nonnull MimeBodyPart aBodyPart) throws IOException, MessagingException;

  @Nonnull
  IAS4IncomingAttachment createAttachment (@Nonnull InputStream aIS) throws IOException;

  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <File> getAndRemoveAllTempFiles ();
}
