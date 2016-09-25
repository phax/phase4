/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
import java.io.InputStream;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * File base incoming attachment.
 *
 * @author Philip Helger
 */
public class IncomingFileAttachment extends AbstractIncomingAttachment
{
  private final File m_aFile;

  public IncomingFileAttachment (@Nonnull final File aFile)
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
    return FileHelper.getInputStream (m_aFile);
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("File", m_aFile).toString ();
  }
}
