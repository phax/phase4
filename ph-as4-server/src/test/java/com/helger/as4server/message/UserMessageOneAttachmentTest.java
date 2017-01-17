/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.as4server.holodeck.IHolodeckTests;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;

@RunWith (Parameterized.class)
@Category (IHolodeckTests.class)
public class UserMessageOneAttachmentTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageOneAttachmentTest (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageOneAttachmentMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"), CMimeType.APPLICATION_XML, null, aResMgr));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     null,
                                                                                                                                     aAttachments),

                                                                                          aAttachments);

    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageOneAttachmentSignedMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"), CMimeType.APPLICATION_XML, null, aResMgr));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSigned.createSignedMessage (MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                                                  null,
                                                                                                                                                                  aAttachments),
                                                                                                                       m_eSOAPVersion,
                                                                                                                       aAttachments,
                                                                                                                       s_aResMgr,
                                                                                                                       false,
                                                                                                                       ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                                                       ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT),
                                                                                          aAttachments);

    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageOneAttachmentEncryptedMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"), CMimeType.APPLICATION_XML, null, aResMgr));

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion,
                                                                          MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments),
                                                                          false,
                                                                          aAttachments,
                                                                          s_aResMgr);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageMimeSignedEncryptedSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"), CMimeType.APPLICATION_XML, null, aResMgr));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final Document aDoc = aSigned.createSignedMessage (MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                  null,
                                                                                                  aAttachments),
                                                       m_eSOAPVersion,
                                                       aAttachments,
                                                       s_aResMgr,
                                                       false,
                                                       ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                       ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion,
                                                                          aDoc,
                                                                          false,
                                                                          aAttachments,
                                                                          s_aResMgr);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }
}
