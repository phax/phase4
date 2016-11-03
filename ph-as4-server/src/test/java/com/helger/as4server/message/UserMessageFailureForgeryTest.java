/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4server.message;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
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
import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
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
import com.helger.xml.XMLHelper;
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

  // TODO NOT WORKING SINCE SOMEHOW THE SECOND USER MESSAGE GETS IGNORED!?!?!
  @Ignore
  @Test
  public void testTwoUserMessageShouldFail () throws Exception
  {

    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/TwoUserMessages.xml").getInputStream ());

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
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
                                                       false,
                                                       ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                       ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartInfo");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element aElement = (Element) nNode;
      if (aElement.hasAttribute ("href"))
        aElement.setAttribute ("href", "cid:invalid");
    }
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aDoc, aAttachments, null);
    // TODO DELETE OUTPUT IF not needed anymore
    final HttpMimeMessageEntity aEntity = new HttpMimeMessageEntity (aMsg);
    System.out.println (EntityUtils.toString (aEntity));
    sendMimeMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
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

  // Encryption

  // @Ignore
  @Test
  public void testUserMessageEncryptedMimeAttachmentForged () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/ShortXML.xml"),
                                             CMimeType.APPLICATION_XML));

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion,
                                                                          TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments),
                                                                          true,
                                                                          aAttachments);

    final SoapMimeMultipart aMultipart = (SoapMimeMultipart) aMsg.getContent ();
    // Since we want to change the attachment
    final MimeBodyPart aMimeBodyPart = (MimeBodyPart) aMultipart.getBodyPart (1);
    if (true)
      aMimeBodyPart.setContent ("Crappy text".getBytes (StandardCharsets.ISO_8859_1),
                                CMimeType.APPLICATION_OCTET_STREAM.getAsString ());

    aMsg.saveChanges ();
    // TODO remove when output not needed anymore
    final HttpMimeMessageEntity aEntity = new HttpMimeMessageEntity (aMsg);
    if (false)
      System.out.println (EntityUtils.toString (aEntity));
    sendMimeMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithPayloadInfoOnly () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, null);

    // Delete the added Payload in the soap body to confirm right behaviour when
    // the payload is missing
    final NodeList nList = aDoc.getElementsByTagName (m_eSOAPVersion.getNamespacePrefix () + ":Body");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element aElement = (Element) nNode;
      XMLHelper.removeAllChildElements (aElement);
    }

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithBodyPayloadOnlyNoInfo () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, null);

    // Delete the added Payload in the soap body to confirm right behaviour when
    // the payload is missing
    Node aNext = XMLHelper.getFirstChildElementOfName (aDoc, "Envelope");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, "Header");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, "Messaging");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, "UserMessage");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, "PayloadInfo");

    aNext.getParentNode ().removeChild (aNext);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithAttachmentInfoOnly () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     null,
                                                                                                                                     aAttachments),

                                                                                          aAttachments,
                                                                                          null);

    final SoapMimeMultipart aMultipart = (SoapMimeMultipart) aMsg.getContent ();

    // Since we want to remove the attachment
    aMultipart.removeBodyPart (1);

    aMsg.saveChanges ();
    // TODO remove when output not needed anymore
    final HttpMimeMessageEntity aEntity = new HttpMimeMessageEntity (aMsg);
    System.out.println (EntityUtils.toString (aEntity));
    sendMimeMessage (aEntity, false, EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Test
  public void testUserMessageWithOnlyAttachmentsNoPartInfo () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();

    final Document aSoapDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);

    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSoapDoc,

                                                                                          aAttachments,
                                                                                          null);
    aMsg.saveChanges ();
    // TODO remove when output not needed anymore
    final HttpMimeMessageEntity aEntity = new HttpMimeMessageEntity (aMsg);
    System.out.println (EntityUtils.toString (aEntity));
    sendMimeMessage (aEntity, false, EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Test
  public void testUserMessageWithMoreAttachmentsThenPartInfo () throws Exception
  {
    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test.xml.gz"),
                                             CMimeType.APPLICATION_GZIP));

    final Document aSoapDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);

    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                             CMimeType.IMAGE_JPG));
    aAttachments.add (new AS4FileAttachment (ClassPathResource.getAsFile ("attachment/test-img2.jpg"),
                                             CMimeType.IMAGE_JPG));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSoapDoc,

                                                                                          aAttachments,
                                                                                          null);
    aMsg.saveChanges ();
    // TODO remove when output not needed anymore
    final HttpMimeMessageEntity aEntity = new HttpMimeMessageEntity (aMsg);
    System.out.println (EntityUtils.toString (aEntity));
    sendMimeMessage (aEntity, false, EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }
}
