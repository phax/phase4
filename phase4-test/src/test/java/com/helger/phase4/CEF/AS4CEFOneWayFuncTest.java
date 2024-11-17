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
package com.helger.phase4.CEF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.server.MockPModeGenerator;
import com.helger.phase4.server.message.MockMessages;
import com.helger.phase4.util.AS4XMLHelper;

public final class AS4CEFOneWayFuncTest extends AbstractCEFTestSetUp
{
  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * message to the RMSH with parameter MESSAGEPROPERTIES containing: A property
   * with attributes "name" and "type" present. A property with only attribute
   * "name" present.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA03 () throws Exception
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (m_aPayload != null, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (m_aESENSOneWayPMode.getID (),
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                 AS4TestConstants.CEF_INITIATOR_ID,
                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                 AS4TestConstants.CEF_RESPONDER_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);
    final String sTrackerIdentifier = "trackingidentifier";
    aEbms3MessageProperties.addProperty (MessageHelperMethods.createEbms3Property (sTrackerIdentifier, "tracker"));

    // Can not do a Property without Value (type) since type does not exist
    // final Ebms3Property aPropOnlyName = new Ebms3Property ();
    // aPropOnlyName.setName ("OnlyName");
    // aEbms3MessageProperties.addProperty (aPropOnlyName);

    final AS4UserMessage aMsg = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       null,
                                                       m_eSoapVersion).setMustUnderstand (true);

    final Document aSignedDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                               aMsg.getAsSoapDocument (m_aPayload),
                                                               m_eSoapVersion,
                                                               aMsg.getMessagingID (),
                                                               null,
                                                               s_aResMgr,
                                                               false,
                                                               AS4SigningParams.createDefault ());

    final NodeList aNL = aSignedDoc.getElementsByTagName ("eb:MessageProperties");
    assertEquals (aNL.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent (),
                  sTrackerIdentifier);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aSignedDoc, m_eSoapVersion.getMimeType ()),
                                               true,
                                               null);

    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 message (User Message
   * with payload) to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends a non-repudiation receipt to the SMSH.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA04 () throws Exception
  {
    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Prerequisite:<br>
   * AS4_TA04 <br>
   * <br>
   * Predicate: <br>
   * The SMSH sends a success notification to the producer.
   */
  @Test
  public void testAS4_TA05 ()
  {
    // Is specified in the SPI, so everyone can configure that how it is
    // preferred
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a Message with metadata
   * information and XML payload to the SMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with a gzip compressed payload.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA06 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeType (CMimeType.APPLICATION_XML)
                                                                                         .compression (EAS4CompressionMode.GZIP)
                                                                                         .build (), s_aResMgr));
    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                          createTestSignedUserMessage (m_eSoapVersion,
                                                                                                       m_aPayload,
                                                                                                       aAttachments,
                                                                                                       s_aResMgr),
                                                                          aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a Message with metadata
   * information and payload to the SMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message generated by the SMSH, a property element with name
   * "CompressionType" and value set to "application/gzip" is present.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA07 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeType (CMimeType.APPLICATION_XML)
                                                                                         .compression (EAS4CompressionMode.GZIP)
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final NodeList aNL = aDoc.getElementsByTagName ("eb:PartProperties");
    assertNotNull (aNL);
    assertEquals (1, aNL.getLength ());
    assertEquals ("CompressionType",
                  aNL.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent ());
    assertEquals ("application/gzip", aNL.item (0).getLastChild ().getTextContent ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with payload (ex: xml document) and metadata information including a
   * property element with name "MimeType" and value ("application/xml").<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "MimeType" present and
   * set to the value specified by the producer ("application/xml").
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA08 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final NodeList aNL = aDoc.getElementsByTagName ("eb:PartProperties");
    assertNotNull (aNL);
    assertEquals (1, aNL.getLength ());
    assertEquals ("MimeType", aNL.item (0).getFirstChild ().getAttributes ().getNamedItem ("name").getTextContent ());
    assertEquals ("application/xml", aNL.item (0).getFirstChild ().getTextContent ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). The SMSH is simulated to send an AS4
   * message without property "MimeType" present to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends a synchronous ebMS error response.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA09 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final NodeList aNL = aDoc.getElementsByTagName ("eb:PartProperties");
    aNL.item (0).removeChild (aNL.item (0).getFirstChild ());

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-16) payload and metadata information including payload
   * character set info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-16".
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA10 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                                           .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                                           .mimeTypeXML ()
                                                                                                           .compressionGZIP ()
                                                                                                           .build (),
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_16);
    aAttachments.add (aAttachment);

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    NodeList aNL = aDoc.getElementsByTagName ("eb:PartProperties");
    aNL = aNL.item (0).getChildNodes ();

    boolean bHasCharset = false;
    for (int i = 0; i < aNL.getLength (); i++)
      if (aNL.item (i).getAttributes ().getNamedItem ("name").getTextContent ().equals ("CharacterSet"))
        if (aNL.item (i).getTextContent ().equals ("UTF-16"))
        {
          bHasCharset = true;
          break;
        }
    assertTrue (bHasCharset);
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-8) payload and metadata information including payload
   * character set info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-8".
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA11 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                                           .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                                           .mimeTypeXML ()
                                                                                                           .compressionGZIP ()
                                                                                                           .build (),
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_8);
    aAttachments.add (aAttachment);

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    NodeList aNL = aDoc.getElementsByTagName ("eb:PartProperties");
    aNL = aNL.item (0).getChildNodes ();

    boolean bHasCharset = false;

    for (int i = 0; i < aNL.getLength (); i++)
    {
      if (aNL.item (i).getAttributes ().getNamedItem ("name").getTextContent ().equals ("CharacterSet"))
      {
        if (aNL.item (i).getTextContent ().equals ("UTF-8"))
          bHasCharset = true;
      }
    }

    assertTrue (bHasCharset);
  }

  /**
   * Note: Not testable (might become valid after the requirements EBXMLMSG-87
   * and EBXMLMSG-88 are validated). <= thats what is standing in the
   * document<br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 message
   * with property element "CharacterSet" set to value not conform to section
   * 4.3.3 of [XML10] (example: "!utf*"). The SMSH sends the AS4 message to the
   * RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a synchronous ebMS error message.
   */
  @Test
  public void testAS4_TA12 ()
  {
    // empty so far
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * Message with compressed but damaged payloads. The SMSH sends the AS4 User
   * Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response with error code "Code =
   * EBMS:0303, Short Description = DecompressionFailure, Severity = Failure,
   * Category = Communication".
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA13 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                                           .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                                           .mimeTypeXML ()
                                                                                                           .compressionGZIP ()
                                                                                                           .build (),
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_8);
    aAttachments.add (aAttachment);

    final AS4UserMessage aMsg = createTestUserMessageSoapNotSigned (m_aPayload, aAttachments);
    // Damaged payload: txt file
    aAttachments.get (0).setSourceStreamProvider (new ClassPathResource ("attachment/CompressedPayload.txt"));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                              aMsg.getAsSoapDocument (m_aPayload),
                                                                              aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg),
                     false,
                     EEbmsError.EBMS_DECOMPRESSION_FAILURE.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * AS4_TA13. <br>
   * The User Message is bound to a PMode with parameter
   * PMode[1].ErrorHandling.Report.ProcessErrorNotifyConsumer: set to true. <br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response (with error code "Code =
   * EBMS:0303, Short Description = DecompressionFailure, Severity = Failure,
   * Category = Communication") and the RMSH sends an error notification to the
   * consumer of the message.
   */
  @Test
  public void testAS4_TA14 ()
  {
    // SAME as TA13 and SPI has to send message to the consumer
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User Message with a
   * compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH delivers the message with decompressed payload to the consumer.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA15 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
    // How to check message if it is decompressed hmm?
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User Message with a
   * several compressed payloads (XML and non XML) to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH delivers the message with decompressed payloads to the consumer.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA16 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA13.<br>
   * The SMSH is simulated to send an AS4 User message with a compressed then
   * signed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA17 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA13<br>
   * Simulated SMSH sends a signed AS4 User Message with a signed then
   * compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH receives a WS-Security SOAP Fault.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA18 () throws Exception
  {
    // signed then compressed
    // Should return an error because the uncompressed attachment was signed and
    // not the compressed one
    ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (), s_aResMgr));

    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, aAttachments, s_aResMgr);

    aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion, aDoc, aAttachments);
    sendMimeMessage (HttpMimeMessageEntity.create (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA14.<br>
   * The SMSH is simulated to send a compressed then encrypted AS4 message to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA19 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (), s_aResMgr));

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSoapVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSoapVersion,
                                                                       aMsg.getAsSoapDocument (),
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       false,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA14.<br>
   * Simulated SMSH sends a signed AS4 User Message with an encrypted first,
   * then compressed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH receives a WS-Security SOAP Fault.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA20 () throws Exception
  {
    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSoapVersion, m_aPayload, null);
    final Document aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory,
                                                               m_eSoapVersion,
                                                               aMsg.getAsSoapDocument (m_aPayload),
                                                               true,
                                                               m_aCryptParams);

    final NodeList aNL = aDoc.getElementsByTagName ("S12:Body");

    final NonBlockingByteArrayOutputStream outputStream = new NonBlockingByteArrayOutputStream ();
    final Source xmlSource = new DOMSource (aNL.item (0));
    final Result outputTarget = new StreamResult (outputStream);
    TransformerFactory.newInstance ().newTransformer ().transform (xmlSource, outputTarget);

    final byte [] aSrc = outputStream.toByteArray ();

    // Compression
    final NonBlockingByteArrayOutputStream aCompressedOS = new NonBlockingByteArrayOutputStream ();
    try (final InputStream aIS = new NonBlockingByteArrayInputStream (aSrc);
         final OutputStream aOS = EAS4CompressionMode.GZIP.getCompressStream (aCompressedOS))
    {
      StreamHelper.copyInputStreamToOutputStream (aIS, aOS);
    }
    aNL.item (0).setTextContent (AS4XMLHelper.serializeXML (aDoc));

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_FAILED_DECRYPTION.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). The SMSH sends an AS4 message with a
   * compressed then encrypted and signed payload to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA21 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (), s_aResMgr));

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSoapVersion, null, aAttachments);
    final Document aDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                         aMsg.getAsSoapDocument (null),
                                                         m_eSoapVersion,
                                                         aMsg.getMessagingID (),
                                                         aAttachments,
                                                         s_aResMgr,
                                                         false,
                                                         AS4SigningParams.createDefault ());

    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSoapVersion,
                                                                       aDoc,
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       false,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Note: This test assertion is only valid in case TLS is handled by the AS4
   * message handler. AS4_TA22 - AS4_TA26 all TLS tests
   */
  @Test
  public void testAS4_TA22 ()
  {
    // empty
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH sends an AS4 User message to the
   * RMSH. <br>
   * <br>
   * Predicate: <br>
   * In the message sender|receiver elements reference the MSHs and not the
   * (producer,consumer).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA27 () throws Exception
  {
    final Document aDoc = createTestUserMessageSoapNotSigned (m_aPayload, null).getAsSoapDocument (m_aPayload);

    NodeList aNL = aDoc.getElementsByTagName ("eb:PartyId");
    final String sPartyID = aNL.item (0).getTextContent ();

    aNL = aDoc.getElementsByTagName ("eb:Property");
    final String sOriginalSender = aNL.item (0).getTextContent ();

    assertFalse (sPartyID.equals (sOriginalSender));
  }

  /**
   * Note: Only when using SBDH<br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits two payloads, first
   * being an SBDH document, second being an actual payload (non-XML payload).
   * SMSH sends an AS4 User Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * Message has two additional MIME parts. The first mime part is the SBDH
   * document and the second is the actual payload
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA28 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (), s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (), s_aResMgr));

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                          MockMessages.createUserMessageNotSigned (m_eSoapVersion,
                                                                                                                   null,
                                                                                                                   aAttachments)
                                                                                      .getAsSoapDocument (),
                                                                          aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 User
   * message to the RMSH with parameter PARTPROPERTIES containing: A property
   * with attributes "name" and "type" present. A property with only attribute
   * "name" present. <br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX.
   */
  @Test
  public void testAS4_TA29 ()
  {
    // Same like AS4_TA03 just in Part Properties instead of Message Properties
  }
}
