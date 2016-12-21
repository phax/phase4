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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.attachment.outgoing.AS4OutgoingFileAttachment;
import com.helger.as4lib.attachment.outgoing.IAS4OutgoingAttachment;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;
import com.helger.xml.serialize.read.DOMReader;

@RunWith (Parameterized.class)
public class UserMessageCompressionTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return CollectionHelper.newListMapped (ESOAPVersion.values (), x -> new Object [] { x });
  }

  private final ESOAPVersion m_eSOAPVersion;

  public UserMessageCompressionTest (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = eSOAPVersion;
  }

  @Test
  public void testUserMessageWithCompressedAttachmentSuccessful () throws Exception
  {
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     EAS4CompressionMode.GZIP,
                                                     s_aResMgr));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     null,
                                                                                                                                     aAttachments),

                                                                                          aAttachments,
                                                                                          null);

    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }

  @Test
  public void testUserMessageWithCompressedAttachmentFailureNoBodyPayloadAllowed () throws Exception
  {

    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final ICommonsList <IAS4OutgoingAttachment> aAttachments = new CommonsArrayList<> ();
    aAttachments.add (new AS4OutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                     CMimeType.APPLICATION_XML,
                                                     EAS4CompressionMode.GZIP,
                                                     s_aResMgr));

    final MimeMessage aMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (TestMessages.testUserMessageSoapNotSigned (m_eSOAPVersion,
                                                                                                                                     aPayload,
                                                                                                                                     aAttachments),

                                                                                          aAttachments,
                                                                                          null);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void testUserMessageWithWrongCompressionType () throws Exception
  {
    final MimeMessage aMsg = new MimeMessage (null,
                                              new ClassPathResource ("testfiles/WrongCompression.mime").getInputStream ());

    sendMimeMessage (new HttpMimeMessageEntity (aMsg), false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }
}
