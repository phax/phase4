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
