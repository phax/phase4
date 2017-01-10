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
package com.helger.as4lib.attachment.incoming;

import java.io.File;
import java.io.InputStream;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * File base incoming attachment.
 *
 * @author Philip Helger
 */
public class AS4IncomingFileAttachment extends AbstractAS4IncomingAttachment
{
  private final File m_aFile;

  public AS4IncomingFileAttachment (@Nonnull final File aFile)
  {
    m_aFile = ValueEnforcer.notNull (aFile, "File");
    ValueEnforcer.isTrue (FileHelper.canReadAndWriteFile (aFile), () -> aFile + " must be readable and writable");
  }

  @Nonnull
  public File getFile ()
  {
    return m_aFile;
  }

  @Nonnull
  public InputStream getInputStream ()
  {
    return StreamHelper.getBuffered (FileHelper.getInputStream (m_aFile));
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("File", m_aFile).toString ();
  }
}
