package com.helger.mime;

import java.io.File;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Ignore;
import org.junit.Test;

import com.helger.commons.charset.CCharset;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.http.CHTTPHeader;
import com.helger.mail.cte.EContentTransferEncoding;

public class MimeMessageTest
{

  @Test
  public void createTestMimedMessageWithSoap () throws MessagingException, IOException
  {

    final MimeMultipart aMimeMultipart = new MimeMultipart ();

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      final byte [] aEBMSMsg = StreamHelper.getAllBytes (new ClassPathResource ("TestMimeMessage.xml"));
      aMessagePart.setContent (aEBMSMsg,
                               new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                      CCharset.CHARSET_UTF_8)
                                                                       .getAsString ());
      aMessagePart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding._8BIT.getID ());
      aMessagePart.setHeader (CHTTPHeader.CONTENT_LENGTH, Integer.toString (aEBMSMsg.length));
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    {
      // File Payload
      final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
      final File aAttachment = new File ("data/test.xml");
      final DataSource fds = new FileDataSource (aAttachment);
      aMimeBodyPart.setDataHandler (new DataHandler (fds));
      aMimeBodyPart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding.BINARY.getID ());
      aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TYPE, CMimeType.APPLICATION_GZIP.getAsString ());
      aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_LENGTH, Long.toString (aAttachment.length ()));
      aMimeBodyPart.setFileName (fds.getName ());
      aMimeMultipart.addBodyPart (aMimeBodyPart);
    }

    final MimeMessage message = new MimeMessage ((Session) null);
    message.setHeader ("MIME-Version", "1.0");
    message.setContent (aMimeMultipart);
    message.saveChanges ();

    message.writeTo (System.out);
  }

  // application/gzip
  @Test
  @Ignore
  public void createMimeMessageTest () throws MessagingException, IOException
  {
    final MimeMultipart aMimeMultipart = new MimeMultipart ();
    final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
    final DataSource fds = new FileDataSource (new FileSystemResource ("data/test.xml").getAsFile ());
    aMimeBodyPart.setDataHandler (new DataHandler (fds));
    aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TYPE, CMimeType.APPLICATION_GZIP.getAsString ());
    aMimeBodyPart.setFileName (fds.getName ());
    aMimeMultipart.addBodyPart (aMimeBodyPart);

    final MimeMessage message = new MimeMessage ((Session) null);
    message.setHeader ("MIME-Version", "1.0");
    message.setContent (aMimeMultipart);
    message.saveChanges ();

    message.writeTo (System.out);
  }
}
