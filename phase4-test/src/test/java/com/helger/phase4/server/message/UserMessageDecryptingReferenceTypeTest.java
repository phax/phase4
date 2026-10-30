/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertSame;

import java.util.Collection;

import org.apache.wss4j.dom.str.STRParser;
import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.server.spi.MockMessageProcessorCheckingStreamsSPI;

/**
 * Test that the certificate reference type of the encrypted key is what is remembered as the
 * decrypting certificate reference type, independent of the signing.
 *
 * @author Philip Helger
 */
@RunWith (Parameterized.class)
public final class UserMessageDecryptingReferenceTypeTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSoapVersion;

  public UserMessageDecryptingReferenceTypeTest (@NonNull final ESoapVersion eSOAPVersion)
  {
    m_eSoapVersion = eSOAPVersion;
  }

  private void _sendEncrypted (@NonNull final ECryptoKeyIdentifierType eKeyIdentifierType) throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (AS4OutgoingAttachment.builder ()
                                                                                         .data (ClassPathResource.getAsFile (AS4TestConstants.ATTACHMENT_SHORTXML_XML))
                                                                                         .mimeTypeXML ()
                                                                                         .build (), s_aResMgr));

    final AS4CryptParams aCryptParams = AS4CryptParams.createDefault ()
                                                      .setAlias ("ph-as4")
                                                      .setKeyIdentifierType (eKeyIdentifierType);
    final AS4MimeMessage aMimeMsg = AS4Encryptor.encryptToMimeMessage (m_eSoapVersion,
                                                                       MockMessages.createUserMessageNotSigned (m_eSoapVersion,
                                                                                                                null,
                                                                                                                aAttachments)
                                                                                   .getAsSoapDocument (),
                                                                       aAttachments,
                                                                       m_aCryptoFactory,
                                                                       true,
                                                                       s_aResMgr,
                                                                       aCryptParams);
    sendMimeMessageExpectSuccess (HttpMimeMessageEntity.create (aMimeMsg));
  }

  @Test
  public void testBinarySecurityToken () throws Exception
  {
    _sendEncrypted (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE);
    assertSame (STRParser.REFERENCE_TYPE.DIRECT_REF,
                MockMessageProcessorCheckingStreamsSPI.getLastDecryptingReferenceType ());
  }

  @Test
  public void testIssuerSerial () throws Exception
  {
    _sendEncrypted (ECryptoKeyIdentifierType.ISSUER_SERIAL);
    assertSame (STRParser.REFERENCE_TYPE.ISSUER_SERIAL,
                MockMessageProcessorCheckingStreamsSPI.getLastDecryptingReferenceType ());
  }
}
