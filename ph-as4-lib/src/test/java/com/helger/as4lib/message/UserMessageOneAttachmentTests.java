package com.helger.as4lib.message;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.AS4FileAttachment;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;

@RunWith (Parameterized.class)
public class UserMessageOneAttachmentTests extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageOneAttachmentTests (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageOneAttachmentMimeSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     null,
                                                                                                                                     aAttachments),

                                                                                          aAttachments,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageOneAttachmentSignedMimeSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                                                  null,
                                                                                                                                                                  aAttachments),
                                                                                                                       m_eSOAPVersion,
                                                                                                                       aAttachments,
                                                                                                                       false),
                                                                                          aAttachments,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageOneAttachmentEncryptedMimeSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion,
                                                                          TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments),
                                                                          false,
                                                                          aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageMimeSignedEncryptedSuccess () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/shortXML.xml"),
                                             CMimeType.TEXT_XML));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final Document aDoc = aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                  null,
                                                                                                  aAttachments),
                                                       m_eSOAPVersion,
                                                       aAttachments,
                                                       false);

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion, aDoc, false, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }
}
