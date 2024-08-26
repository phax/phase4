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
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hc.core5.http.NoHttpResponseException;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.ScopedAS4Configuration;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.server.MockPModeGenerator;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import jakarta.mail.Multipart;

public final class AS4eSENSCEFOneWayFuncTest extends AbstractCEFTestSetUp
{
  // For Lambdas
  static final Logger LOGGER = LoggerFactory.getLogger (AS4eSENSCEFOneWayFuncTest.class);

  public AS4eSENSCEFOneWayFuncTest ()
  {}

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 User message to the
   * RMSH. <br>
   * <br>
   * Predicate:<br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX (for more details on http response codes please refer to
   * https://issues.oasis-open.org/browse/EBXMLMSG-57?jql=project%20%3D%20EBXMLMSG).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA01 () throws Exception
  {
    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                        createTestSignedUserMessage (m_eSoapVersion,
                                                                                                     m_aPayload,
                                                                                                     null,
                                                                                                     s_aResMgr),
                                                                        null);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH and RMSH exchange several AS4 User
   * Messages.<br>
   * <br>
   * Predicate: <br>
   * Each exchanged AS4 message contains single ORIGIN and DESTINATION elements.
   * <br>
   * Goal: "Both UserMessage/PartyInfo/From and UserMessage/PartyInfo/To must
   * not include more than one PartyId element"
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA03 () throws Exception
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
    aEbms3PartyInfo.getTo ().addPartyId (MessageHelperMethods.createEbms3PartyId ("Second ID"));

    // Check if we added a second party id
    assertTrue (aEbms3PartyInfo.getTo ().getPartyId ().size () == 2);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setPartyInfo (aEbms3PartyInfo);
    aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aUserMessage.setMessageProperties (aEbms3MessageProperties);
    aUserMessage.setPayloadInfo (aEbms3PayloadInfo);
    aUserMessage.setMessageInfo (aEbms3MessageInfo);

    final Document aDoc = new AS4UserMessage (ESoapVersion.AS4_DEFAULT, aUserMessage).getAsSoapDocument (m_aPayload);
    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP) SMSH is simulated to produce
   * uncompressed payloads. The SMSH sends the AS4 message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt and delivers the message to the
   * consumer. <br>
   * <br>
   * k Goal: Do not throw an error if a uncompressed payload gets sent, also
   * check if compressed payloads are acceptable aswell.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA04 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

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
   * e-SENS profile. (One-Way/Push MEP)Producer submits a message with metadata
   * information and an XML payload to the SMSH. SMSH generates an AS4 message
   * to send to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message created by the SMSH, the compressed payload is carried
   * in a separate MIME part and the soap body is empty. <br>
   * <br>
   * Goal:"Due to the mandatory use of the AS4 compression feature in this
   * profile (see section 2.2.3.3), XML payloads MAY be converted to binary
   * data, which is carried in separate MIME parts and not in the SOAP Body.
   * Compliant AS4 message always have an empty SOAP Body. "
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA05 () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

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
   * e-SENS profile. (One-Way/Push MEP) Producer submits a message to the SMSH
   * with metadata information, an XML payload (leading business document) and
   * other payloads (XML and non XML). SMSH generates an AS4 message to send to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 message created by the SMSH, the compressed payloads are carried
   * in separate MIME parts and the soap body is empty.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA06 () throws Exception
  {
    // same stuff as TA05 only one step further
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                        createTestSignedUserMessage (m_eSoapVersion,
                                                                                                     m_aPayload,
                                                                                                     aAttachments,
                                                                                                     s_aResMgr),
                                                                        aAttachments);

    final Multipart aMultipart = (Multipart) aMsg.getContent ();
    // 3 attachments + 1 Main/Bodypart
    assertTrue (aMultipart.getCount () == 4);
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA06<br>
   * SMSH sends an AS4 message to the RMSH. <br>
   * <br>
   * Predicate: <br>
   * The RMSH successfully processes the AS4 message and sends a non-repudiation
   * receipt to the SMSH.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA07 () throws Exception
  {
    // same stuff as TA05 only one step further
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

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
   * NOTE: Not testable => CEF document<br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile. (One-Way/Push MEP) SMSH is simulated to send an AS4 message
   * to the RMSH with non XML payloads and without a leading business document
   * payload. The SMSH sends the AS4 User Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous error response.
   */
  @Test
  public void testEsens_TA08 ()
  {
    // empty
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). SMSH is simulated to send an AS4 user
   * message with a payload hyperlink reference.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a synchronous ebMS error message.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA09 () throws Exception
  {
    // Would throw an error in our implementation since the user would have said
    // there is a payload (With the hyperlink reference) but nothing is
    // attached.
    final DocumentBuilderFactory aDbfac = DocumentBuilderFactory.newInstance ();
    final DocumentBuilder aDocBuilder = aDbfac.newDocumentBuilder ();
    final Document aDoc = aDocBuilder.parse (ClassPathResource.getAsFile ("attachment/HyperlinkPayload.xml"));

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      EEbmsError.EBMS_EXTERNAL_PAYLOAD_ERROR.getErrorCode ());
  }

  @Nonnull
  private static HttpProxyServer _startProxyServerInterceptingFirstCall (@Nonnegative final int nProxyPort)
  {
    // Using LittleProxy
    // https://github.com/adamfisk/LittleProxy
    final int nResponsesToIntercept = 1;
    final HttpProxyServer aProxyServer = DefaultHttpProxyServer.bootstrap ()
                                                               .withPort (nProxyPort)
                                                               .withFiltersSource (new HttpFiltersSourceAdapter ()
                                                               {
                                                                 private int m_nFilterCount = 0;

                                                                 @Override
                                                                 public HttpFilters filterRequest (final HttpRequest originalRequest,
                                                                                                   final ChannelHandlerContext ctx)
                                                                 {
                                                                   return new HttpFiltersAdapter (originalRequest)
                                                                   {
                                                                     @Override
                                                                     public HttpResponse clientToProxyRequest (final HttpObject httpObject)
                                                                     {
                                                                       return null;
                                                                     }

                                                                     @Override
                                                                     public HttpObject serverToProxyResponse (final HttpObject httpObject)
                                                                     {
                                                                       final int nIndex = m_nFilterCount++;
                                                                       if (nIndex < nResponsesToIntercept)
                                                                       {
                                                                         LOGGER.error ("Proxy purposely intercepted call " +
                                                                                       nIndex);
                                                                         return null;
                                                                       }

                                                                       LOGGER.info ("Proxy purposely passes on call " +
                                                                                    nIndex);
                                                                       return httpObject;
                                                                     }
                                                                   };
                                                                 }
                                                               })
                                                               .start ();
    return aProxyServer;
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Simulate the RMSH to not send receipts
   * (can be done by intercepting the receipts). SMSH tries to send an AS4 User
   * Message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The SMSH retries to send the AS4 User Message (at least once).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA10 () throws Exception
  {
    final int nProxyPort = 8001;
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (SETTINGS_SERVER_PROXY_ENABLED, true);
    aSettings.putIn (SETTINGS_SERVER_PROXY_ADDRESS, "localhost");
    aSettings.putIn (SETTINGS_SERVER_PROXY_PORT, nProxyPort);
    try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.createTestConfig (aSettings))
    {
      final HttpProxyServer aProxyServer = _startProxyServerInterceptingFirstCall (nProxyPort);
      try
      {
        // send message
        final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                            createTestSignedUserMessage (m_eSoapVersion,
                                                                                                         m_aPayload,
                                                                                                         null,
                                                                                                         s_aResMgr),
                                                                            null);
        sendMimeMessage (HttpMimeMessageEntity.create (aMsg), false, EEbmsError.EBMS_OTHER.getErrorCode ());
      }
      finally
      {
        aProxyServer.stop ();
      }
    }
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Simulate the RMSH to not send receipts.
   * SMSH tries to send an AS4 User Message to the RMSH. Before a TIME_OUT is
   * reached network connection is restored (RMSH is able to send a
   * non-repudiation receipt).<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back an AS4 non-repudiation receipt to the SMSH and delivers
   * only one message to the consumer and the SMSH stops resending the original
   * AS4 User Message.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA11 () throws Exception
  {
    final int nProxyPort = 8001;
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (SETTINGS_SERVER_PROXY_ENABLED, true);
    aSettings.putIn (SETTINGS_SERVER_PROXY_ADDRESS, "localhost");
    aSettings.putIn (SETTINGS_SERVER_PROXY_PORT, nProxyPort);

    try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.createTestConfig (aSettings))
    {
      // Simulating a timeout with Thread.sleep but before it entirely triggers
      // let the program continue as if the Connection is back up again
      final HttpProxyServer aProxyServer = DefaultHttpProxyServer.bootstrap ()
                                                                 .withPort (nProxyPort)
                                                                 .withFiltersSource (new HttpFiltersSourceAdapter ()
                                                                 {
                                                                   @Override
                                                                   public HttpFilters filterRequest (final HttpRequest originalRequest,
                                                                                                     final ChannelHandlerContext ctx)
                                                                   {
                                                                     return new HttpFiltersAdapter (originalRequest)
                                                                     {
                                                                       @Override
                                                                       public HttpResponse clientToProxyRequest (final HttpObject httpObject)
                                                                       {
                                                                         ThreadHelper.sleep (500);
                                                                         return null;
                                                                       }

                                                                       @Override
                                                                       public HttpObject serverToProxyResponse (final HttpObject httpObject)
                                                                       {
                                                                         LOGGER.error ("Forcing a timeout from retryhandler ");
                                                                         return httpObject;
                                                                       }
                                                                     };
                                                                   }
                                                                 })
                                                                 .start ();
      try
      {
        // send message
        final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                            createTestSignedUserMessage (m_eSoapVersion,
                                                                                                         m_aPayload,
                                                                                                         null,
                                                                                                         s_aResMgr),
                                                                            null);
        sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
      }
      finally
      {
        aProxyServer.stop ();
      }
    }
  }

  /**
   * NOTE: Not testable <= is the hint in the CEF pdf (just wanted to add it for
   * completion) <br>
   * <br>
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile.<br>
   * <br>
   * Predicate: <br>
   * PMode parameter " PMode[1].ErrorHandling.Report.SenderErrors" is not set.
   */
  @Test
  public void testEsens_TA12 ()
  {
    // Empty
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message with metadata
   * information and a payload to the SMSH. SMSH sends an AS4 signed message to
   * the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 Message generated by the SMSH: - Signature Hash function
   * parameter is set to http://www.w3.org/2001/04/xmlenc#sha256 - Signature
   * Algorithm parameter is set to
   * http://www.w3.org/2001/04/xmldsig-more#rsa-sha256 - Signature Certificate
   * used is the certificate of the SMSH.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA13 () throws Exception
  {
    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);

    NodeList aNL = aDoc.getElementsByTagName ("ds:SignatureMethod");
    String sAlgorithmToCheck = aNL.item (0).getAttributes ().getNamedItem ("Algorithm").getTextContent ();

    // Checking Signature Algorithm
    assertEquals (sAlgorithmToCheck, ECryptoAlgorithmSign.RSA_SHA_256.getAlgorithmURI ());

    aNL = aDoc.getElementsByTagName ("ds:DigestMethod");
    sAlgorithmToCheck = aNL.item (0).getAttributes ().getNamedItem ("Algorithm").getTextContent ();

    // Checking Digest Algorithm
    assertEquals (sAlgorithmToCheck, ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getAlgorithmURI ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile (One-Way/Push MEP). Producer submits a message with metadata
   * information and a payload to the SMSH. SMSH sends an AS4 encrypted message
   * to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * In the AS4 Message generated by the SMSH: - Encryption Algorithm is set to
   * http://www.w3.org/2009/xmlenc11#aes128-gcm - Encryption Certificate used is
   * the certificate of the RMSH.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA14 () throws Exception
  {
    Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);
    aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory, m_eSoapVersion, aDoc, true, m_aCryptParams);

    final NodeList aNL = aDoc.getElementsByTagName ("xenc:EncryptionMethod");
    // Needs to be the second item in the message, since first would be
    // <xenc:EncryptionMethod
    // Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
    // for the key encryption => real encryption algorithm occurs in the soap
    // body
    final String sAlgorithmToCheck = aNL.item (1).getAttributes ().getNamedItem ("Algorithm").getTextContent ();

    // Checking Signature Algorithm
    assertEquals (sAlgorithmToCheck, ECryptoAlgorithmCrypt.AES_128_GCM.getAlgorithmURI ());
  }

  /**
   * Prerequisite:<br>
   * Producer submits a business document with the information “Sender” and
   * “destination” to the SMSH. SMSH and RMSH are configured to exchange AS4
   * messages according to the e-SENS profile (One-Way/Push MEP). SMSH sends an
   * AS4 User message to the RMSH.<br>
   * <br>
   * Predicate: <br>
   * The message received by the RMSH contains 2 property elements in the
   * MessageProperties node with attributes name and type. One has name =
   * "OriginalSender" and value producerID and the other has name =
   * "finalRecipient" and value consumerID (producerID and consumerID are
   * provided by the original message submitted by the producer).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA15 () throws Exception
  {
    final Document aDoc = createTestSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);

    final NodeList aNL = aDoc.getElementsByTagName ("eb:MessageProperties");
    assertEquals ("originalSender",
                  aNL.item (0).getFirstChild ().getAttributes ().getNamedItem ("name").getTextContent ());
    assertEquals ("finalRecipient",
                  aNL.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent ());
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA10. TIME_OUT for resending the messages is reached.<br>
   * <br>
   * Predicate: <br>
   * The SMSH reports an error to the message producer.
   *
   * @throws Exception
   *         In case of error
   */
  @Test (expected = NoHttpResponseException.class)
  public void testEsens_TA17 () throws Exception
  {
    final int nProxyPort = 8001;
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (SETTINGS_SERVER_PROXY_ENABLED, true);
    aSettings.putIn (SETTINGS_SERVER_PROXY_ADDRESS, "localhost");
    aSettings.putIn (SETTINGS_SERVER_PROXY_PORT, nProxyPort);

    try (final ScopedAS4Configuration aSC = ScopedAS4Configuration.createTestConfig (aSettings))
    {
      // Forcing a Timeout from the retry handler
      final HttpProxyServer aProxyServer = DefaultHttpProxyServer.bootstrap ()
                                                                 .withPort (nProxyPort)
                                                                 .withFiltersSource (new HttpFiltersSourceAdapter ()
                                                                 {
                                                                   @Override
                                                                   public HttpFilters filterRequest (final HttpRequest originalRequest,
                                                                                                     final ChannelHandlerContext ctx)
                                                                   {
                                                                     return new HttpFiltersAdapter (originalRequest)
                                                                     {
                                                                       @Override
                                                                       public HttpResponse clientToProxyRequest (final HttpObject httpObject)
                                                                       {
                                                                         return null;
                                                                       }

                                                                       @Override
                                                                       public HttpObject serverToProxyResponse (final HttpObject httpObject)
                                                                       {
                                                                         LOGGER.warn ("Forcing a timeout from retryhandler for testing purposes.");
                                                                         return null;
                                                                       }
                                                                     };
                                                                   }
                                                                 })
                                                                 .start ();
      try
      {
        // send message
        final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                            createTestSignedUserMessage (m_eSoapVersion,
                                                                                                         m_aPayload,
                                                                                                         null,
                                                                                                         s_aResMgr),
                                                                            null);
        sendMimeMessage (HttpMimeMessageEntity.create (aMsg), false, EEbmsError.EBMS_OTHER.getErrorCode ());
      }
      finally
      {
        aProxyServer.stop ();
      }
    }
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. Producer submits a "ping" message with
   * metadata information to the SMSH (to "ping" consumer).<br>
   * <br>
   * Predicate: <br>
   * The SMSH generates an AS4 message with values (and sends it to the RMSH):
   * UserMessage/CollaborationInfo/Service set to
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service
   * UserMessage/CollaborationInfo/Action set to
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test
   */
  @Test
  public void testEsens_TA18 ()
  {
    // See com.helger.as4.server.servlet.PModePingTest;
  }

  /**
   * Prerequisite:<br>
   * eSENS_TA18. The consumer is reachable.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a non-repudiation receipt within a HTTP response with
   * status code 2XX and the consumer doesn't receive any message.
   */
  @Test
  public void testEsens_TA19 ()
  {
    // See com.helger.as4.server.servlet.PModePingTest;
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: One-Way/Push MEP. SMSH sends an AS4 User Message including
   * a TRACKINGIDENTIFIER property set by the producer.<br>
   * <br>
   * Predicate: <br>
   * The RMSH returns a non-repudiation receipt within a HTTP response with
   * status code 2XX and the received AS4 message contains the
   * TRACKINGIDENTIFIER property.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testEsens_TA20 () throws Exception
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

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       null,
                                                       m_eSoapVersion)
                                              .setMustUnderstand (true);

    final Document aSignedDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                               aDoc.getAsSoapDocument (m_aPayload),
                                                               m_eSoapVersion,
                                                               aDoc.getMessagingID (),
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
}
