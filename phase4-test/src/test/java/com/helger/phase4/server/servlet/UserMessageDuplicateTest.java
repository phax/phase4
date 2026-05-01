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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertTrue;

import org.apache.hc.core5.http.HttpEntity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.io.resource.ClassPathResource;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (UserMessageDuplicateTest.class);

  private final ESoapVersion m_eSoapVersion = ESoapVersion.AS4_DEFAULT;

  @Test
  public void testSendDuplicateMessageOnlyGetOneReceipt () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    // Send once
    sendPlainMessageExpectSuccess (aEntity);

    // Send again
    sendPlainMessageExpectError (aEntity, EEbmsError.EBMS_PHASE4_DUPLICATE.getErrorCode ());
  }

  @Test
  public void testSendDuplicateMessageTestDisposalFeature () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());

    sendPlainMessageExpectSuccess (aEntity);

    LOGGER.info ("Waiting until incoming message is evicted from duplicate cache; scheduled for " +
                 AS4Configuration.getIncomingDuplicateDisposal ());

    // Making sure the duplicate stuff gets eliminated
    // Test configuration is for a few seconds only
    // As the server is started per class and not per test, we need to wait at least twice the
    // amount of time (and add 0,5 seconds to be sure)
    ThreadHelper.sleep (AS4Configuration.getIncomingDuplicateDisposal ().multipliedBy (2).plusMillis (500).toMillis ());

    sendPlainMessageExpectSuccess (aEntity);
  }

  @Test
  public void testDuplicateSignedMessage () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final ESoapVersion eSOAPVersion = ESoapVersion.AS4_DEFAULT;
    final Document aDoc = MockMessages.createUserMessageSigned (eSOAPVersion, aPayload, null, s_aResMgr);

    final HttpEntity aEntity = new HttpXMLEntity (aDoc, eSOAPVersion.getMimeType ());

    // Send first
    final String sResponse = sendPlainMessageExpectSuccess (aEntity);

    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));

    // Send second
    sendPlainMessageExpectError (aEntity, EEbmsError.EBMS_PHASE4_DUPLICATE.getErrorCode ());
  }
}
