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
package com.helger.as4server.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
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
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final ICommonsList <File> m_aTempFiles = new CommonsArrayList<> ();

  public boolean canKeepInMemory (@Nonnegative final long nSize)
  {
    return nSize >= 0 && nSize <= 30 * CGlobal.BYTES_PER_KILOBYTE;
  }

  @Nonnull
  public IIncomingAttachment createAttachment (@Nonnull final MimeBodyPart aBodyPart) throws IOException,
                                                                                      MessagingException
  {
    final int nSize = aBodyPart.getSize ();
    AbstractIncomingAttachment ret;
    if (canKeepInMemory (nSize))
    {
      // Store in memory
      ret = new IncomingInMemoryAttachment (StreamHelper.getAllBytes (aBodyPart.getInputStream ()));
    }
    else
    {
      // Write to temp file
      final File aTempFile = File.createTempFile ("as4-incoming", "attachment");
      try (OutputStream aOS = FileHelper.getOutputStream (aTempFile))
      {
        aBodyPart.getDataHandler ().writeTo (aOS);
      }
      m_aRWLock.writeLocked ( () -> m_aTempFiles.add (aTempFile));
      ret = new IncomingFileAttachment (aTempFile);
    }

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
  public IIncomingAttachment createAttachment (@Nonnull final InputStream aIS) throws IOException
  {
    // Write to temp file
    final File aTempFile = File.createTempFile ("as4-incoming", "attachment");
    try (OutputStream aOS = FileHelper.getOutputStream (aTempFile))
    {
      StreamHelper.copyInputStreamToOutputStream (aIS, aOS);
    }
    m_aRWLock.writeLocked ( () -> m_aTempFiles.add (aTempFile));
    return new IncomingFileAttachment (aTempFile);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <File> getAndRemoveAllTempFiles ()
  {
    return m_aRWLock.writeLocked ( () -> {
      final ICommonsList <File> ret = m_aTempFiles.getClone ();
      m_aTempFiles.clear ();
      return ret;
    });
  }
}
