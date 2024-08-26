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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;

import jakarta.mail.MessagingException;

/**
 * Run with many attachments. <br>
 * Note: these tests will fail when testing against Holodeck with
 * ESens-Connector enabled because it can only take exactly one payload!
 *
 * @author bayerlma
 */
@RunWith (Parameterized.class)
public final class UserMessageManyAttachmentTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSOAPVersion;

  public UserMessageManyAttachmentTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageManyAttachmentsMimeSuccess () throws IOException, MessagingException
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSOAPVersion,
                                                                            MockMessages.createUserMessageNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments)
                                                                                        .getAsSoapDocument (),
                                                                            aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageManyAttachmentsSignedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSOAPVersion,
                                                                            AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                                                                           aMsg.getAsSoapDocument (),
                                                                                                           m_eSOAPVersion,
                                                                                                           aMsg.getMessagingID (),
                                                                                                           aAttachments,
                                                                                                           s_aResMgr,
                                                                                                           false,
                                                                                                           AS4SigningParams.createDefault ()),
                                                                            aAttachments);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }

  @Test
  public void testUserMessageManyAttachmentsEncryptedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSOAPVersion,
                                                                       MockMessages.createUserMessageNotSigned (m_eSOAPVersion,
                                                                                                                null,
                                                                                                                aAttachments)
                                                                                   .getAsSoapDocument (),
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       false,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageManyAttachmentsSignedEncryptedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_TEST_IMG_JPG))
                                                                                         .mimeType (CMimeType.IMAGE_JPG)
                                                                                         .build (),
                                                                    s_aResMgr));
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML2_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments);
    final Document aDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                         aMsg.getAsSoapDocument (),
                                                         m_eSOAPVersion,
                                                         aMsg.getMessagingID (),
                                                         aAttachments,
                                                         s_aResMgr,
                                                         false,
                                                         AS4SigningParams.createDefault ());
    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSOAPVersion,
                                                                       aDoc,
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       false,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }
}
