/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.CEF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.mail.internet.MimeMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.encrypt.EncryptionCreator;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.MockMessages;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;

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
  public void AS4_TA03 () throws Exception
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (m_aPayload, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (m_aESENSOneWayPMode.getID (),
                                                                                 DEFAULT_AGREEMENT,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
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

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       m_eSOAPVersion)
                                              .setMustUnderstand (true);

    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          aDoc.getAsSOAPDocument (m_aPayload),
                                                                          m_eSOAPVersion,
                                                                          null,
                                                                          new AS4ResourceManager (),
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final NodeList nList = aSignedDoc.getElementsByTagName ("eb:MessageProperties");
    assertEquals (nList.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent (),
                  sTrackerIdentifier);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aSignedDoc, m_eSOAPVersion), true, null);

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
  public void AS4_TA04 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, new AS4ResourceManager ());

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSOAPVersion), true, null);

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
  @Ignore
  @Test
  public void AS4_TA05 ()
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
  public void AS4_TA06 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));
    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion,
                                                                     testSignedUserMessage (m_eSOAPVersion,
                                                                                            m_aPayload,
                                                                                            aAttachments,
                                                                                            new AS4ResourceManager ()),
                                                                     aAttachments);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
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
  public void AS4_TA07 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartProperties");
    assertEquals (nList.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent (),
                  "CompressionType");
    assertEquals (nList.item (0).getLastChild ().getTextContent (), "application/gzip");
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
  public void AS4_TA08 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartProperties");
    assertEquals (nList.item (0).getFirstChild ().getAttributes ().getNamedItem ("name").getTextContent (), "MimeType");
    assertEquals (nList.item (0).getFirstChild ().getTextContent (), "application/xml");
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
  public void AS4_TA09 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final NodeList nList = aDoc.getElementsByTagName ("eb:PartProperties");
    nList.item (0).removeChild (nList.item (0).getFirstChild ());

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-16) payload and metadata information including payload
   * characterset info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-16".
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void AS4_TA10 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                                      CMimeType.APPLICATION_XML,
                                                                                      EAS4CompressionMode.GZIP,
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_16);
    aAttachments.add (aAttachment);

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    NodeList nList = aDoc.getElementsByTagName ("eb:PartProperties");
    nList = nList.item (0).getChildNodes ();

    boolean bHasCharset = false;

    for (int i = 0; i < nList.getLength (); i++)
    {
      if (nList.item (i).getAttributes ().getNamedItem ("name").getTextContent ().equals ("CharacterSet"))
      {
        if (nList.item (i).getTextContent ().equals ("UTF-16"))
          bHasCharset = true;
      }
    }

    assertTrue (bHasCharset);
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message to the SMSH
   * with xml (UTF-8) payload and metadata information including payload
   * characterset info.<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with the property "CharacterSet" present
   * and set to the value "UTF-8".
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void AS4_TA11 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                                      CMimeType.APPLICATION_XML,
                                                                                      EAS4CompressionMode.GZIP,
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_8);
    aAttachments.add (aAttachment);

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    NodeList nList = aDoc.getElementsByTagName ("eb:PartProperties");
    nList = nList.item (0).getChildNodes ();

    boolean bHasCharset = false;

    for (int i = 0; i < nList.getLength (); i++)
    {
      if (nList.item (i).getAttributes ().getNamedItem ("name").getTextContent ().equals ("CharacterSet"))
      {
        if (nList.item (i).getTextContent ().equals ("UTF-8"))
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
  public void AS4_TA12 ()
  {
    // XXX
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
  public void AS4_TA13 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final WSS4JAttachment aAttachment = WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                                      CMimeType.APPLICATION_XML,
                                                                                      EAS4CompressionMode.GZIP,
                                                                                      s_aResMgr);
    aAttachment.setCharset (StandardCharsets.UTF_8);
    aAttachments.add (aAttachment);

    final Document aDoc = testUserMessageSoapNotSigned (m_aPayload, aAttachments);
    // Damaged payload: txt file
    aAttachments.get (0).setSourceStreamProvider (new ClassPathResource ("attachment/CompressedPayload.txt"));

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_DECOMPRESSION_FAILURE.getErrorCode ());
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
  public void AS4_TA14 ()
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
  public void AS4_TA15 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
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
  public void AS4_TA16 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                                                                    CMimeType.IMAGE_JPG,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
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
  public void AS4_TA17 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
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
  public void AS4_TA18 () throws Exception
  {
    // signed then compressed
    // Should return an error because the uncompressed attachment was signed and
    // not the compressed one
    ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    s_aResMgr));

    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, aAttachments, new AS4ResourceManager ());

    aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
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
  public void AS4_TA19 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    s_aResMgr));

    final MimeMessage aMsg = new EncryptionCreator (AS4CryptoFactory.DEFAULT_INSTANCE).encryptMimeMessage (m_eSOAPVersion,
                                                                                                           MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                                      null,
                                                                                                                                                      aAttachments),
                                                                                                           false,
                                                                                                           aAttachments,
                                                                                                           s_aResMgr,
                                                                                                           ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);

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
  public void AS4_TA20 () throws Exception
  {
    final Document aDoc = new EncryptionCreator (AS4CryptoFactory.DEFAULT_INSTANCE).encryptSoapBodyPayload (m_eSOAPVersion,
                                                                                                            MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                                       m_aPayload,
                                                                                                                                                       null),
                                                                                                            true,
                                                                                                            ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);

    final NodeList nList = aDoc.getElementsByTagName ("S12:Body");

    final NonBlockingByteArrayOutputStream outputStream = new NonBlockingByteArrayOutputStream ();
    final Source xmlSource = new DOMSource (nList.item (0));
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
    nList.item (0).setTextContent (AS4XMLHelper.serializeXML (aDoc));

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSOAPVersion),
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
  public void AS4_TA21 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    EAS4CompressionMode.GZIP,
                                                                    s_aResMgr));

    final Document aDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                    MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                               null,
                                                                                                               aAttachments),
                                                                    m_eSOAPVersion,
                                                                    aAttachments,
                                                                    s_aResMgr,
                                                                    false,
                                                                    ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                    ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final MimeMessage aMsg = new EncryptionCreator (AS4CryptoFactory.DEFAULT_INSTANCE).encryptMimeMessage (m_eSOAPVersion,
                                                                                                           aDoc,
                                                                                                           false,
                                                                                                           aAttachments,
                                                                                                           s_aResMgr,
                                                                                                           ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Note: This test assertion is only valid in case TLS is handled by the AS4
   * message handler. AS4_TA22 - AS4_TA26 all TLS tests
   */
  @Ignore
  @Test
  public void AS4_TA22 ()
  {

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
  public void AS4_TA27 () throws Exception
  {
    final Document aDoc = testUserMessageSoapNotSigned (m_aPayload, null);

    NodeList nList = aDoc.getElementsByTagName ("eb:PartyId");
    final String sPartyID = nList.item (0).getTextContent ();

    nList = aDoc.getElementsByTagName ("eb:Property");
    final String sOriginalSender = nList.item (0).getTextContent ();

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
  public void AS4_TA28 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));
    final AS4ResourceManager aResMgr1 = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG),
                                                                    CMimeType.IMAGE_JPG,
                                                                    null,
                                                                    aResMgr1));

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion,
                                                                     MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                null,
                                                                                                                aAttachments),
                                                                     aAttachments);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);

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
  public void AS4_TA29 ()
  {
    // Same like AS4_TA03 just in Part Properties instead of Message Properties
  }
}
