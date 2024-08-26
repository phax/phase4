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

import java.io.InputStream;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.server.external.IHolodeckTests;

@RunWith (Parameterized.class)
public final class UserMessageCompressionTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSOAPVersion;

  public UserMessageCompressionTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Category (IHolodeckTests.class)
  @Test
  public void testUserMessageWithCompressedAttachmentSuccessful () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSOAPVersion,
                                                                            MockMessages.createUserMessageNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments)
                                                                                        .getAsSoapDocument (),
                                                                            aAttachments);

    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);
  }

  @Test
  public void testUserMessageWithCompressedSignedSuccessful () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
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
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSOAPVersion, aDoc, aAttachments);

    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);
  }

  @Test
  public void testUserMessageCompressedEncrpytedSuccessful () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
                                                                                         .build (),
                                                                    s_aResMgr));

    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments)
                                      .getAsSoapDocument ();

    final AS4MimeMessage aMsg = AS4Encryptor.encryptToMimeMessage (m_eSOAPVersion,
                                                                   aDoc,
                                                                   aAttachments,
                                                                   m_aCryptoFactory,
                                                                   false,
                                                                   s_aResMgr,
                                                                   m_aCryptParams);
    sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);
  }

  @Test
  public void testUserMessageCompressedSignedEncrpytedSuccessful () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .compressionGZIP ()
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
    sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);
  }

  @Test
  public void testUserMessageWithWrongCompressionType () throws Exception
  {
    try (final InputStream aIS = ClassPathResource.getInputStream ("testfiles/WrongCompression.mime"))
    {
      // Read an existing MimeMessage
      final AS4MimeMessage aMsg = new AS4MimeMessage (null, aIS);

      sendMimeMessage (HttpMimeMessageEntity.create (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
    }
  }
}
