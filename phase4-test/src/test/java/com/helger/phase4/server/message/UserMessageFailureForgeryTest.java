/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.message;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.messaging.mime.AS4SoapMimeMultipart;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

import jakarta.mail.internet.MimeBodyPart;

/**
 * Tests the basic functionality of sending UserMessages with SOAP Version 1.1.
 * IMPORTANT: If these tests are expected to work there needs to be a local
 * holodeck server running.
 *
 * @author bayerlma
 */
@RunWith (Parameterized.class)
public final class UserMessageFailureForgeryTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSoapVersion;

  public UserMessageFailureForgeryTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSoapVersion = eSOAPVersion;
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
    MockMessages.createEmptyUserMessage (m_eSoapVersion, null, null);
    fail ();
  }

  @Test
  public void testTwoUserMessagesShouldFail () throws Exception
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/TwoUserMessages.xml").getInputStream ());

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithNoPartyIDShouldFail () throws Exception
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/UserMessageNoPartyID.xml").getInputStream ());

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_INVALID_HEADER.getErrorCode ());
  }

  // Tinkering with the signature

  @Test
  public void testUserMessageNoSOAPBodyPayloadNoAttachmentSignedSuccess () throws Exception
  {
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, null, null, s_aResMgr);
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);
  }

  @Test
  public void testPayloadChangedAfterSigningShouldFail () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, aPayload, aAttachments, s_aResMgr);
    final NodeList nList = aDoc.getElementsByTagName (m_eSoapVersion.getNamespacePrefix () + ":Body");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element eElement = (Element) nNode;
      eElement.setAttribute ("INVALID", "INVALID");
    }
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_FAILED_DECRYPTION.getErrorCode ());
  }

  @Test
  public void testWrongAttachmentIDShouldFail () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_XML_GZ))
                                                                                         .mimeType (CMimeType.APPLICATION_GZIP)
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSoapVersion, null, aAttachments);
    final Document aDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                         aMsg.getAsSoapDocument (),
                                                         m_eSoapVersion,
                                                         aMsg.getMessagingID (),
                                                         aAttachments,
                                                         s_aResMgr,
                                                         false,
                                                         AS4SigningParams.createDefault ());

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartInfo");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element aElement = (Element) nNode;
      if (aElement.hasAttribute ("href"))
        aElement.setAttribute ("href", MessageHelperMethods.PREFIX_CID + "invalid" + i);
    }
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg),
                     false,
                     EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  // Encryption
  // Cannot be tested easily, since the error gets only thrown if the SPI tries
  // to read the stream of the attachment
  // @Ignore
  @Test
  public void testUserMessageEncryptedMimeAttachmentForged () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSoapVersion,
                                                                       MockMessages.createUserMessageNotSigned (m_eSoapVersion,
                                                                                                                null,
                                                                                                                aAttachments)
                                                                                   .getAsSoapDocument (),
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       true,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);

    final AS4SoapMimeMultipart aMultipart = (AS4SoapMimeMultipart) aMimeMsg.getContent ();
    // Since we want to change the attachment
    final MimeBodyPart aMimeBodyPart = (MimeBodyPart) aMultipart.getBodyPart (1);
    aMimeBodyPart.setContent ("Crappy text".getBytes (StandardCharsets.ISO_8859_1),
                              CMimeType.APPLICATION_OCTET_STREAM.getAsString ());

    aMimeMsg.saveChanges ();
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), false, EEbmsError.EBMS_FAILED_DECRYPTION.getErrorCode ());
  }

  @Test
  public void testUserMessageWithPayloadInfoOnly () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    // Delete the added Payload in the soap body to confirm right behaviour when
    // the payload is missing
    final NodeList nList = aDoc.getElementsByTagName (m_eSoapVersion.getNamespacePrefix () + ":Body");
    for (int i = 0; i < nList.getLength (); i++)
    {
      final Node nNode = nList.item (i);
      final Element aElement = (Element) nNode;
      XMLHelper.removeAllChildElements (aElement);
    }

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithBodyPayloadOnlyNoInfo () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    // Delete the added Payload in the soap body to confirm right behaviour when
    // the payload is missing
    Element aNext = XMLHelper.getFirstChildElementOfName (aDoc, "Envelope");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, "Header");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, "Messaging");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, AS4TestConstants.USERMESSAGE_ASSERTCHECK);
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.EBMS_NS, "PayloadInfo");

    aNext.getParentNode ().removeChild (aNext);

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithAttachmentPartInfoOnly () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_XML_GZ))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                            MockMessages.createUserMessageNotSigned (m_eSoapVersion,
                                                                                                                     null,
                                                                                                                     aAttachments)
                                                                                        .getAsSoapDocument (),
                                                                            aAttachments);

    final AS4SoapMimeMultipart aMultipart = (AS4SoapMimeMultipart) aMimeMsg.getContent ();

    // Since we want to remove the attachment
    aMultipart.removeBodyPart (1);

    aMimeMsg.saveChanges ();
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg),
                     false,
                     EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Test
  public void testUserMessageWithOnlyAttachmentsNoPartInfo () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();

    final Document aSoapDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, null, aAttachments)
                                          .getAsSoapDocument ();

    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_XML_GZ))
                                                                                         .mimeType (CMimeType.APPLICATION_GZIP)
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aSoapDoc, aAttachments);
    aMimeMsg.saveChanges ();
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg),
                     false,
                     EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Test
  public void testUserMessageWithMoreAttachmentsThenPartInfo () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_XML_GZ))
                                                                                         .mimeType (CMimeType.APPLICATION_GZIP)
                                                                                         .build (),
                                                                    s_aResMgr));

    final Document aSoapDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, null, aAttachments)
                                          .getAsSoapDocument ();

    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG2_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aSoapDoc, aAttachments);
    aMimeMsg.saveChanges ();
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg),
                     false,
                     EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Test
  public void testUserMessageWrongSigningAlgorithm () throws Exception
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/WrongSigningAlgorithm.xml").getInputStream ());

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_FAILED_AUTHENTICATION.getErrorCode ());
  }

  @Test
  public void testUserMessageWrongSigningDigestAlgorithm () throws Exception
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    final Document aDoc = builder.parse (new ClassPathResource ("testfiles/WrongSigningDigestAlgorithm.xml").getInputStream ());

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_FAILED_AUTHENTICATION.getErrorCode ());
  }

  @Test
  public void testForceFailureFromSPI () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.testUserMessageNotSignedNotPModeConform (m_eSoapVersion, aPayload, null);

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_OTHER.getErrorCode ());
  }
}
