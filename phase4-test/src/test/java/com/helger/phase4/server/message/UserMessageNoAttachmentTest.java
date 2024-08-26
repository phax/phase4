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

import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.AS4TestConstants;
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
import com.helger.phase4.server.external.IHolodeckTests;

@RunWith (Parameterized.class)
@Category (IHolodeckTests.class)
public final class UserMessageNoAttachmentTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSOAPVersion;

  public UserMessageNoAttachmentTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageMimeSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSOAPVersion,
                                                                            aMsg.getAsSoapDocument (),
                                                                            aAttachments);

    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageMimeSignedSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();

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
  @Ignore ("Makes no sense - nothing to encrypt")
  public void testUserMessageMimeEncryptedSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments);
    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSOAPVersion,
                                                                       aMsg.getAsSoapDocument (),
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       false,
                                                                       s_aResMgr,
                                                                       m_aCryptParams);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMimeMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  @Ignore ("Makes no sense - nothing to encrypt")
  public void testUserMessageMimeSignedEncryptedSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();

    final AS4UserMessage aMsg = MockMessages.createUserMessageNotSigned (m_eSOAPVersion, null, aAttachments);
    final Document aSignedDoc = AS4Signer.createSignedMessage (m_aCryptoFactory,
                                                               aMsg.getAsSoapDocument (),
                                                               m_eSOAPVersion,
                                                               aMsg.getMessagingID (),
                                                               aAttachments,
                                                               s_aResMgr,
                                                               false,
                                                               AS4SigningParams.createDefault ());

    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSOAPVersion,
                                                                       aSignedDoc,
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
