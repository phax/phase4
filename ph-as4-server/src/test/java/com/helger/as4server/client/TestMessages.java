package com.helger.as4server.client;

import java.io.File;
import java.util.Locale;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.mime.SoapMimeMultipart;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.SerializerXML;
import com.helger.as4server.message.CreateErrorMessage;
import com.helger.as4server.message.CreateReceiptMessage;
import com.helger.as4server.message.CreateSignedMessage;
import com.helger.as4server.message.CreateUserMessage;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.http.CHTTPHeader;
import com.helger.mail.cte.EContentTransferEncoding;

public class TestMessages
{
  // TODO testMessage for developing delete if not needed anymore
  public static Document testUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                          @Nullable final Node aPayload,
                                          @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final CreateSignedMessage aClient = new CreateSignedMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Document aSignedDoc = aClient.createSignedMessage (aUserMessage.createUserMessage (aUserMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com"),
                                                                                             aUserMessage.createEbms3PayloadInfo (aPayload,
                                                                                                                                  aAttachments),
                                                                                             aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                                                        "MyServiceTypes",
                                                                                                                                        "QuoteToCollect",
                                                                                                                                        "4321",
                                                                                                                                        "pm-esens-generic-resp",
                                                                                                                                        "http://agreements.holodeckb2b.org/examples/agreement0"),
                                                                                             aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                                                                                "APP_1000000101",
                                                                                                                                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                                                                                "APP_1000000101"),
                                                                                             aUserMessage.createEbms3MessageProperties (aEbms3Properties),
                                                                                             eSOAPVersion)
                                                                         .setMustUnderstand (false)
                                                                         .getAsSOAPDocument (aPayload),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             false);
    return aSignedDoc;
  }

  public static Document testErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException
  {
    final CreateErrorMessage aErrorMessage = new CreateErrorMessage ();
    final CreateSignedMessage aClient = new CreateSignedMessage ();
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList<> (EEbmsError.EBMS_INVALID_HEADER.getAsEbms3Error (Locale.US));
    final Document aSignedDoc = aClient.createSignedMessage (aErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                               aErrorMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com"),
                                                                                               aEbms3ErrorList)
                                                                          .setMustUnderstand (false)
                                                                          .getAsSOAPDocument (),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             false);
    return aSignedDoc;
  }

  public static Document testReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                             @Nullable final Node aPayload,
                                             @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException,
                                                                                                               DOMException
  {
    final Document aUserMessage = testUserMessage (eSOAPVersion, aPayload, aAttachments);

    final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
    final CreateSignedMessage aClient = new CreateSignedMessage ();
    final Document aDoc = aReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                aReceiptMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com",
                                                                                                        null),
                                                                aUserMessage)
                                         .setMustUnderstand (false)
                                         .getAsSOAPDocument ();

    final Document aSignedDoc = aClient.createSignedMessage (aDoc, eSOAPVersion, aAttachments, false);
    return aSignedDoc;
  }

  /**
   * JUST FOR TESTING
   *
   * @param eSOAPVersion
   *        SOAP version
   * @return MIME message
   * @throws MessagingException
   *         if MIME problems occur
   */
  public static MimeMessage testMIMEMessage (@Nonnull final ESOAPVersion eSOAPVersion) throws MessagingException
  {
    final MimeMultipart aMimeMultipart = new SoapMimeMultipart (eSOAPVersion);

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      final byte [] aEBMSMsg = StreamHelper.getAllBytes (new ClassPathResource ("TestMimeMessage12.xml"));
      aMessagePart.setContent (aEBMSMsg, eSOAPVersion.getMimeType (CCharset.CHARSET_UTF_8_OBJ).getAsString ());
      aMessagePart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding.BINARY.getID ());
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    {
      // File Payload
      final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
      final File aAttachment = new File ("data/test.xml.gz");
      final DataSource fds = new FileDataSource (aAttachment);
      aMimeBodyPart.setDataHandler (new DataHandler (fds));
      aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TYPE, CMimeType.APPLICATION_GZIP.getAsString ());
      aMimeBodyPart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding.BINARY.getID ());
      aMimeMultipart.addBodyPart (aMimeBodyPart);
    }

    final MimeMessage message = new MimeMessage ((Session) null);
    message.setContent (aMimeMultipart);
    message.saveChanges ();

    return message;
  }

  public static MimeMessage testMIMEMessageGenerated (final Document aSoapEnvelope,
                                                      @Nonnull final ESOAPVersion eSOAPVersion) throws Exception
  {
    final MimeMultipart aMimeMultipart = new SoapMimeMultipart (eSOAPVersion);

    {
      // Message Itself
      final MimeBodyPart aMessagePart = new MimeBodyPart ();
      final String aDoc = SerializerXML.serializeXML (aSoapEnvelope);
      aMessagePart.setContent (aDoc, eSOAPVersion.getMimeType (CCharset.CHARSET_UTF_8_OBJ).getAsString ());
      aMessagePart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding.BINARY.getID ());
      aMimeMultipart.addBodyPart (aMessagePart);
    }

    {
      // File Payload
      final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
      final File aAttachment = new File ("data/test.xml.gz");
      final DataSource fds = new FileDataSource (aAttachment);
      aMimeBodyPart.setDataHandler (new DataHandler (fds));
      aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TYPE, CMimeType.APPLICATION_GZIP.getAsString ());
      aMimeBodyPart.setHeader ("Content-Transfer-Encoding", EContentTransferEncoding.BINARY.getID ());
      aMimeBodyPart.setHeader ("Content-ID", "test-xml");
      aMimeMultipart.addBodyPart (aMimeBodyPart);
    }

    final MimeMessage message = new MimeMessage ((Session) null);
    message.setContent (aMimeMultipart);
    message.saveChanges ();

    message.writeTo (System.err);

    return message;
  }

  public static Document testUserMessageSoapNotSigned (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       @Nullable final Node aPayload,
                                                       @Nullable final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      "pm-esens-generic-resp",
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              "APP_1000000101",
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              "APP_1000000101");
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSOAPVersion)
                                            .setMustUnderstand (false);
    return aDoc.getAsSOAPDocument (aPayload);
  }
}
