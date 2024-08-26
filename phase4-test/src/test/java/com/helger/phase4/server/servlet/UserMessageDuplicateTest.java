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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertTrue;

import org.apache.hc.core5.http.HttpEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.CGlobal;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.server.message.MockMessages;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Try a duplicate detection for a UserMessage
 *
 * @author Philip Helger
 */
public final class UserMessageDuplicateTest extends AbstractUserMessageTestSetUpExt
{
  private final ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;

  @Test
  public void testSendDuplicateMessageOnlyGetOneReceipt () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    // Send once
    sendPlainMessage (aEntity, true, null);

    // Send again
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }

  @Test
  @Ignore ("Only use if you need to test the feature, takes a long time")
  public void testSendDuplicateMessageTestDisposalFeature () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    sendPlainMessage (aEntity, true, null);

    // Making sure the message gets disposed off
    // 60 000 = 1 minute, *2 and + 10000 are a buffer
    // test file is configured for 1 minute can take LONGER if configured
    // differently
    ThreadHelper.sleep (AS4Configuration.getIncomingDuplicateDisposalMinutes () * CGlobal.MILLISECONDS_PER_MINUTE * 2 +
                        10 * CGlobal.MILLISECONDS_PER_SECOND);

    sendPlainMessage (aEntity, true, null);
  }

  @Test
  public void testDuplicateSignedMessage () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final ESoapVersion eSOAPVersion = ESoapVersion.AS4_DEFAULT;
    final Document aDoc = MockMessages.createUserMessageSigned (eSOAPVersion, aPayload, null, s_aResMgr);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, eSOAPVersion.getMimeType ());

    // Send first
    final String sResponse = sendPlainMessage (aEntity, true, null);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));

    // Send second
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }
}
