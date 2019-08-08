/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.server.message;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.server.external.IHolodeckTests;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.messaging.domain.AS4UserMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.MimeMessageCreator;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;

@RunWith (Parameterized.class)
@Category (IHolodeckTests.class)
public final class UserMessageOneAttachmentTest extends AbstractUserMessageTestSetUp
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
    final AS4ResourceHelper aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final AS4UserMessage aMsg = MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion,
                                                                            aMsg.getAsSOAPDocument (),
                                                                            aAttachments);

    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageOneAttachmentSignedMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceHelper aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final AS4UserMessage aMsg = MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion,
                                                                            AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                                                                           aMsg.getAsSOAPDocument (),
                                                                                                           m_eSOAPVersion,
                                                                                                           aMsg.getMessagingID (),
                                                                                                           aAttachments,
                                                                                                           s_aResMgr,
                                                                                                           false,
                                                                                                           AS4SigningParams.createDefault ()),
                                                                            aAttachments);

    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }

  @Test
  public void testUserMessageOneAttachmentEncryptedMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceHelper aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final AS4UserMessage aMsg = MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptMimeMessage (m_eSOAPVersion,
                                                                     aMsg.getAsSOAPDocument (),
                                                                     aAttachments,
                                                                     m_aCryptoFactory,
                                                                     false,
                                                                     s_aResMgr,
                                                                     m_aCryptParams);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageMimeSignedEncryptedSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4ResourceHelper aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final AS4UserMessage aMsg = MockMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, null, aAttachments);
    final Document aSignedDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                               aMsg.getAsSOAPDocument (),
                                                               m_eSOAPVersion,
                                                               aMsg.getMessagingID (),
                                                               aAttachments,
                                                               s_aResMgr,
                                                               false,
                                                               AS4SigningParams.createDefault ());

    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptMimeMessage (m_eSOAPVersion,
                                                                     aSignedDoc,
                                                                     aAttachments,
                                                                     m_aCryptoFactory,
                                                                     false,
                                                                     s_aResMgr,
                                                                     m_aCryptParams);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }
}
