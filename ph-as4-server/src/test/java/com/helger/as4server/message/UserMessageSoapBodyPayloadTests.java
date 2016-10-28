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

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.encrypt.EncryptionCreator;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

@RunWith (Parameterized.class)
public class UserMessageSoapBodyPayloadTests extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageSoapBodyPayloadTests (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testSendUnsignedMessageSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    final Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedMimeSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testSignedUserMessage (m_eSOAPVersion,
                                                                                                                              aPayload,
                                                                                                                              null),
                                                                                          null,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadEncryptSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    Document aDoc = TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion, aPayload, aAttachments);
    aDoc = new EncryptionCreator ().encryptSoapBodyPayload (m_eSOAPVersion, aDoc, false);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testUserMessageSOAPBodyPayloadSignedEncryptedSuccess () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final ICommonsList <IAS4Attachment> aAttachments = new CommonsArrayList<> ();
    Document aDoc = TestMessages.testSignedUserMessage (m_eSOAPVersion, aPayload, aAttachments);
    aDoc = new EncryptionCreator ().encryptSoapBodyPayload (m_eSOAPVersion, aDoc, false);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }
}
