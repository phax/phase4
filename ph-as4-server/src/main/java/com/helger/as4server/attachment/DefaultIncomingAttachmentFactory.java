package com.helger.as4server.attachment;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;

/**
 * Default implementation of {@link IIncomingAttachmentFactory}.
 * 
 * @author Philip Helger
 */
public class DefaultIncomingAttachmentFactory implements IIncomingAttachmentFactory
{
  private final ICommonsList <File> m_aTempFiles = new CommonsArrayList<> ();

  @Nonnull
  public IIncomingAttachment createAttachment (@Nonnull final MimeBodyPart aBodyPart) throws IOException,
                                                                                      MessagingException
  {
    final int nSize = aBodyPart.getSize ();
    AbstractIncomingAttachment ret;
    if (nSize >= 0 && nSize <= 30 * CGlobal.BYTES_PER_KILOBYTE)
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
      m_aTempFiles.add (aTempFile);
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
  @ReturnsMutableCopy
  public ICommonsList <File> getAllTempFiles ()
  {
    return m_aTempFiles.getClone ();
  }
}
