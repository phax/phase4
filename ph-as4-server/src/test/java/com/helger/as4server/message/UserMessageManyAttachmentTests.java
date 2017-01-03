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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.outgoing.AS4OutgoingFileAttachment;
import com.helger.as4lib.attachment.outgoing.IAS4OutgoingAttachment;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;

/**
 * Run with many attachments. <br>
 * Note: these tests will fail when testing against Holodeck with
 * ESens-Connector enabled because it can only take exactly one payload!
 *
 * @author bayerlma
 */
@RunWith (Parameterized.class)
public class UserMessageManyAttachmentTests extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageManyAttachmentTests (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageManyAttachmentsMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                                     CMimeType.IMAGE_JPG,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml2.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     null,
                                                                                                                                     aAttachments),
                                                                                          aAttachments,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageManyAttachmentsSignedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                                     CMimeType.IMAGE_JPG,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml2.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                                                  null,
                                                                                                                                                                  aAttachments),
                                                                                                                       m_eSOAPVersion,
                                                                                                                       aAttachments,
                                                                                                                       s_aResMgr,
                                                                                                                       false,
                                                                                                                       ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                                                       ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT),
                                                                                          aAttachments,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageManyAttachmentsEncryptedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                                     CMimeType.IMAGE_JPG,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml2.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));

    final MimeMessage aMsg = new EncryptionCreator ().encryptMimeMessage (m_eSOAPVersion,
                                                                          TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                     null,
                                                                                                                     aAttachments),
                                                                          false,
                                                                          aAttachments,
                                                                          s_aResMgr);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageManyAttachmentsSignedEncryptedMimeSuccess () throws WSSecurityException, Exception
  {
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/test-img.jpg"),
                                                     CMimeType.IMAGE_JPG,
                                                     s_aResMgr));
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml2.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     s_aResMgr));

    final SignedMessageCreator aSigned = new SignedMessageCreator ();
    final Document aDoc = aSigned.createSignedMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
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
