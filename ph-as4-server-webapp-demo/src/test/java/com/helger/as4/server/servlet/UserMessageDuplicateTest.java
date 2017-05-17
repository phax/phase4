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
package com.helger.as4.server.servlet;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.server.message.MockMessages;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public final class UserMessageDuplicateTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void sendDuplicateMessageOnlyGetOneReceipt () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = MockMessages.testUserMessageSoapNotSigned (ESOAPVersion.AS4_DEFAULT, aPayload, null);

    final HttpEntity aEntity = new StringEntity (AS4XMLHelper.serializeXML (aDoc));

    sendPlainMessage (aEntity, true, null);

    sendPlainMessage (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }

  // Only use if you need to test the feature, takes a long time
  @Ignore
  @Test
  public void sendDuplicateMessageTestDisposalFeature () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    final Document aDoc = MockMessages.testUserMessageSoapNotSigned (ESOAPVersion.AS4_DEFAULT, aPayload, null);

    final HttpEntity aEntity = new StringEntity (AS4XMLHelper.serializeXML (aDoc));

    sendPlainMessage (aEntity, true, null);

    // Making sure the message gets disposed off
    // 60 000 = 1 minute, *2 and + 10000 are a buffer
    // test file is configured for 1 minute can take LONGER if configured
    // differently
    Thread.sleep (AS4ServerConfiguration.getIncomingDuplicateDisposalMinutes () * 60000 * 2 + 10000);

    sendPlainMessage (aEntity, true, null);
  }
}
