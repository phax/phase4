package com.helger.as4lib.message;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.SerializerXML;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class UserMessageSoapBodyPayloadTests extends BaseUserMessageSetUp
{
  public UserMessageSoapBodyPayloadTests (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    super (eSOAPVersion);
  }

  @Test
  public void testSendUnsignedMessageSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, null);

    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    final Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedMimeSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testSignedUserMessage (m_eSOAPVersion,
                                                                                                                              aPayload,
                                                                                                                              null),
                                                                                          null,
                                                                                          null);
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aMsg, aPost);
    _sendMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadEncryptSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, aAttachments);
    aDoc = new EncryptionCreator ().encryptSoapBodyPayload (m_eSOAPVersion, aDoc, false);

    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedEncryptedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    aDoc = new EncryptionCreator ().encryptSoapBodyPayload (m_eSOAPVersion, aDoc, false);

    _sendMessage (new StringEntity (SerializerXML.serializeXML (aDoc)), true, null);
  }
}
