/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.servlet;

import org.apache.http.HttpEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.server.message.MockMessages;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.xml.serialize.read.DOMReader;

public final class UserMessageDuplicateTest extends AbstractUserMessageTestSetUpExt
{
  private final ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;

  @Test
  public void sendDuplicateMessageOnlyGetOneReceipt () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.testUserMessageSoapNotSigned (m_eSoapVersion, aPayload, null).getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    sendPlainMessageAndWait (aEntity, true, null);

    sendPlainMessageAndWait (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }

  // Only use if you need to test the feature, takes a long time
  @Ignore
  @Test
  public void sendDuplicateMessageTestDisposalFeature () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.testUserMessageSoapNotSigned (m_eSoapVersion, aPayload, null).getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    sendPlainMessageAndWait (aEntity, true, null);

    // Making sure the message gets disposed off
    // 60 000 = 1 minute, *2 and + 10000 are a buffer
    // test file is configured for 1 minute can take LONGER if configured
    // differently
    Thread.sleep (AS4ServerConfiguration.getIncomingDuplicateDisposalMinutes () * 60000 * 2 + 10000);

    sendPlainMessageAndWait (aEntity, true, null);
  }
}
