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
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.server.external.IHolodeckTests;
import com.helger.xml.serialize.read.DOMReader;

@RunWith (Parameterized.class)
@Category (IHolodeckTests.class)
public final class ReceiptMessageTest extends AbstractUserMessageTestSetUp
{
  @Parameters (name = "{index}: {0}")
  public static Collection <Object []> data ()
  {
    return new CommonsArrayList <> (ESoapVersion.values (), x -> new Object [] { x });
  }

  private final ESoapVersion m_eSoapVersion;

  public ReceiptMessageTest (@Nonnull final ESoapVersion eSOAPVersion)
  {
    m_eSoapVersion = eSOAPVersion;
  }

  @Test
  public void testReceiptReceivedFromUserMessageWithoutWSSecurity () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageNotSigned (m_eSoapVersion, aPayload, null)
                                      .getAsSoapDocument (aPayload);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.USERMESSAGE_ASSERTCHECK));
  }

  @Test
  public void testReceiptReceivedFromUserMessageWithWSSecurity () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, aPayload, null, s_aResMgr);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }

  @Test
  public void testShouldNotGetAResponse () throws Exception
  {
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    final Document aDoc = MockMessages.createUserMessageSigned (m_eSoapVersion, aPayload, null, s_aResMgr);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
  }
}
