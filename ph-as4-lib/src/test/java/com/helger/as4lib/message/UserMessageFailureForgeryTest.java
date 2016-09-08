package com.helger.as4lib.message;

import static org.junit.Assert.fail;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.as4lib.attachment.AS4FileAttachment;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.mime.SoapMimeMultipart;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Tests the basic functionality of sending UserMessages with SOAP Version 1.1.
 * IMPORTANT: If these tests are expected to work there needs to be a local
 * holodeck server running.
 *
 * @author bayerlma
 */
@RunWith (Parameterized.class)
public class UserMessageFailureForgeryTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageFailureForgeryTest (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  // Empty Messages

  @Test
  public void testEmptyMessage () throws Exception
  {
    // the third parameter has to be empty String, since there is no EBMS
    // exception coming back
    sendPlainMessage (new StringEntity (""), false, "");
  }

  @Test (expected = IllegalStateException.class)
  public void testEmptyUserMessage ()
  {
    TestMessages.emptyUserMessage (m_eSOAPVersion, null, null);
    fail ();
  }

  // Tinkering with the signature

  @Test
  public void testUserMessageNoSOAPBodyPayloadNoAttachmentSignedSuccess () throws Exception
  {
    final Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, null, null);
    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testPayloadChangedAfterSigningShouldFail () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    final Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    final NodeList nList = aDoc.getElementsByTagName (m_eSOAPVersion.getNamespacePrefix () + ":Body");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element eElement = (Element) nNode;
      eElement.setAttribute ("INVALID", "INVALID");
    }
    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_FAILED_AUTHENTICATION.getErrorCode ());
  }

  @Test
  public void testWrongAttachmentIDShouldFail () throws Exception
  {

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();

    final Document aDoc = aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                  null,
                                                                                                  aAttachments),
                                                       m_eSOAPVersion,
                                                       aAttachments,
                                                       false);

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartInfo");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element eElement = (Element) nNode;
      if (eElement.hasAttribute ("href"))
        eElement.setAttribute ("href", "cid:invalid");
    }
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aDoc, aAttachments, null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  // False pmode settings

  @Test
  public void testFalsePartyIDToTriggerPModeError () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSignedNotPModeConform (m_eSOAPVersion, aPayload, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  // Encrpytion

  @Test
  public void testUserMessageEncrypteMimeAttachmentForged () throws Exception
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

    final SoapMimeMultipart aMultipart = (SoapMimeMultipart) aMsg.getContent ();
    // Since we want to change the attachment
    final MimeBodyPart aMimeBodyPart = (MimeBodyPart) aMultipart.getBodyPart (1);
    aMimeBodyPart.attachFile (ClassPathResource.getAsFile ("attachment/test-img.jpg"));

    aMsg.saveChanges ();

    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_FAILED_DECRYPTION.getErrorCode ());
  }
}
