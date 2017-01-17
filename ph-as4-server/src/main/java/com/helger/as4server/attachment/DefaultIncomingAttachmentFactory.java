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
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.as4lib.attachment.incoming.AS4IncomingFileAttachment;
import com.helger.as4lib.attachment.incoming.IAS4IncomingAttachment;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;

/**
 * Default implementation of {@link IIncomingAttachmentFactory}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class DefaultIncomingAttachmentFactory implements IIncomingAttachmentFactory
{
  @Nonnull
  public IAS4IncomingAttachment createAttachment (@Nonnull final AS4ResourceManager aResMgr,
                                                  @Nonnull final MimeBodyPart aBodyPart) throws IOException,
                                                                                         MessagingException
  {
    // Write to temp file
    final File aTempFile = aResMgr.createTempFile ();
    try (final OutputStream aOS = StreamHelper.getBuffered (FileHelper.getOutputStream (aTempFile)))
    {
      aBodyPart.getDataHandler ().writeTo (aOS);
    }
    final AS4IncomingFileAttachment ret = new AS4IncomingFileAttachment (aTempFile);

    // Convert all headers to attributes
    final Enumeration <?> aEnum = aBodyPart.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header aHeader = (Header) aEnum.nextElement ();
      ret.setAttribute (aHeader.getName (), aHeader.getValue ());
    }

    return ret;
  }

  @Nonnull
  public IAS4IncomingAttachment createAttachment (@Nonnull final AS4ResourceManager aResMgr,
                                                  @Nonnull final InputStream aIS,
                                                  @Nonnull final Map <String, String> aHeaders) throws IOException
  {
    // Always write to temp file because we don't know how big the content is
    final File aTempFile = aResMgr.createTempFile ();
    try (final OutputStream aOS = StreamHelper.getBuffered (FileHelper.getOutputStream (aTempFile)))
    {
      StreamHelper.copyInputStreamToOutputStream (aIS, aOS);
    }

    final AS4IncomingFileAttachment ret = new AS4IncomingFileAttachment (aTempFile);
    ret.setAttributes (aHeaders);
    return ret;
  }
}
