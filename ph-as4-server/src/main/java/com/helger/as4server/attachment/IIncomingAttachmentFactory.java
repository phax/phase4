package com.helger.as4server.attachment;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.ICommonsList;

/**
 * Factory interface for {@link IIncomingAttachment} objects.
 * 
 * @author Philip Helger
 */
public interface IIncomingAttachmentFactory extends Serializable
{
  @Nonnull
  IIncomingAttachment createAttachment (@Nonnull MimeBodyPart aBodyPart) throws IOException, MessagingException;

  @Nonnull
  @Nonempty
  ICommonsList <File> getAllTempFiles ();
}
