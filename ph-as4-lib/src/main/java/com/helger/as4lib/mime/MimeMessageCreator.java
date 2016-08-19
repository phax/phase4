package com.helger.as4lib.mime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.SerializerXML;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.mail.cte.EContentTransferEncoding;

public final class MimeMessageCreator
{
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

  private final ESOAPVersion m_eSOAPVersion;

  public MimeMessageCreator (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  @Nonnull
  public MimeMessage generateMimeMessage (@Nonnull final Document aSOAPEnvelope,
                                          @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws Exception
  {
    final SoapMimeMultipart aMimeMultipart = new SoapMimeMultipart (m_eSOAPVersion);

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      final String aDoc = SerializerXML.serializeXML (aSOAPEnvelope);
      aMessagePart.setContent (aDoc, m_eSOAPVersion.getMimeType (CCharset.CHARSET_UTF_8_OBJ).getAsString ());
      aMessagePart.setHeader (CONTENT_TRANSFER_ENCODING, EContentTransferEncoding.BINARY.getID ());
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    // Add all attachments (if any)
    if (aAttachments != null)
      for (final IAS4Attachment aAttachment : aAttachments)
        aAttachment.addToMimeMultipart (aMimeMultipart);

    final MimeMessage message = new MimeMessage ((Session) null);
    message.setContent (aMimeMultipart);
    message.saveChanges ();

    if (false)
      message.writeTo (System.err);

    return message;
  }
}
