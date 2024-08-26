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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.messaging.crypto.AS4Encryptor;
import com.helger.phase4.messaging.http.HttpMimeMessageEntity;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.AS4MimeMessageHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.server.external.IHolodeckTests;
import com.helger.xml.serialize.read.DOMReader;

@RunWith (Parameterized.class)
@Category (IHolodeckTests.class)
public final class UserMessageSoapBodyPayloadTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSoapVersion;

  public UserMessageSoapBodyPayloadTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSoapVersion = eSOAPVersion;
  }

  @Test
  public void testSendUnsignedMessageSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, aPayload, aAttachments, s_aResMgr);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedMimeSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final AS4MimeMessage aMsg = AS4MimeMessageHelper.generateMimeMessage (m_eSoapVersion,
                                                                        MockMessages.createUserMessageSigned (m_eSoapVersion,
                                                                                                              aPayload,
                                                                                                              null,
                                                                                                              s_aResMgr),
                                                                        null);
    final String sResponse = sendMimeMessage (HttpMimeMessageEntity.create (aMsg), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }

  @Test
  public void testUserMessageSOAPBodyPayloadEncryptSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, aAttachments)
                                .getAsSoapDocument (aPayload);
    aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory, m_eSoapVersion, aDoc, false, m_aCryptParams);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedEncryptedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, aPayload, aAttachments, s_aResMgr);
    aDoc = AS4Encryptor.encryptSoapBodyPayload (m_aCryptoFactory, m_eSoapVersion, aDoc, false, m_aCryptParams);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
    assertTrue (sResponse.contains (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT.getAlgorithmURI ()));
    assertTrue (sResponse.contains (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT.getAlgorithmURI ()));
  }
}
