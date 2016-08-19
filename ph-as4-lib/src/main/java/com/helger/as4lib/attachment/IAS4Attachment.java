package com.helger.as4lib.attachment;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.Attachment;

import com.helger.commons.id.IHasID;

/**
 * Base interface for an attachment.
 *
 * @author Philip Helger
 */
public interface IAS4Attachment extends IHasID <String>
{
  /**
   * @param aMimeMultipart
   *        The multipart message to add to. May not be <code>null</code>.
   */
  void addToMimeMultipart (@Nonnull MimeMultipart aMimeMultipart) throws MessagingException;

  /**
   * @return This attachment as a WSS4J attachment. May not be
   *         <code>null</code>.
   */
  @Nonnull
  Attachment getAsWSS4JAttachment ();
}
