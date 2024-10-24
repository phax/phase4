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

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.test.profile.AS4TestProfileRegistarSPI;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test for the CEF PMode
 *
 * @author Philip Helger
 */
public final class CEFProfileTest extends AbstractUserMessageTestSetUpExt
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CEFProfileTest.class);

  private ESoapVersion m_eSoapVersion;
  private Ebms3UserMessage m_aEbms3UserMessage;
  private Node m_aPayload;

  @Before
  public void before ()
  {
    m_eSoapVersion = ESoapVersion.AS4_DEFAULT;
    m_aEbms3UserMessage = new Ebms3UserMessage ();

    // Default Payload for testing
    m_aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    if (m_aPayload == null)
      LOGGER.warn ("SOAPBodyPayload.xml could not be found no payload attached in PModeCheckTest setup");
    m_aEbms3UserMessage.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (m_aPayload != null, null));

    // Default MessageInfo for testing
    m_aEbms3UserMessage.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());

    // Default CollaborationInfo for testing
    m_aEbms3UserMessage.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo (DEFAULT_PARTY_ID +
                                                                                                 "12-" +
                                                                                                 DEFAULT_PARTY_ID +
                                                                                                 "12",
                                                                                                 DEFAULT_AGREEMENT,
                                                                                                 null,
                                                                                                 null,
                                                                                                 CAS4.DEFAULT_SERVICE_URL,
                                                                                                 CAS4.DEFAULT_ACTION_URL,
                                                                                                 AS4TestConstants.TEST_CONVERSATION_ID));

    // Default PartyInfo for testing
    m_aEbms3UserMessage.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                                 DEFAULT_PARTY_ID,
                                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                                 DEFAULT_PARTY_ID));
    // Default MessageProperties for testing
    m_aEbms3UserMessage.setMessageProperties (createDefaultProperties ());

    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4TestProfileRegistarSPI.AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT);
  }

  @After
  public void after ()
  {
    // Reset to default
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (null);
  }

  @Test
  public void testUserMessageFinalRecipientButNoOriginalSender () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();
    aEbms3Properties.removeIf (x -> x.getName ().equals (CAS4.ORIGINAL_SENDER));

    assertEquals (1, aEbms3Properties.size ());

    aEbms3Properties.add (_createRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    m_aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = AS4UserMessage.create (m_eSoapVersion, m_aEbms3UserMessage)
                                        .setMustUnderstand (true)
                                        .getAsSoapDocument (m_aPayload);

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      "'originalSender' property is empty or not existant but mandatory");
  }

  @Test
  public void testUserMessageOriginalSenderButNoFinalRecipient () throws Exception
  {
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();
    aEbms3Properties.removeIf (x -> x.getName ().equals (CAS4.FINAL_RECIPIENT));

    assertEquals (1, aEbms3Properties.size ());

    aEbms3Properties.add (_createRandomProperty ());
    aEbms3MessageProperties.setProperty (aEbms3Properties);

    m_aEbms3UserMessage.setMessageProperties (aEbms3MessageProperties);
    final Document aDoc = AS4UserMessage.create (m_eSoapVersion, m_aEbms3UserMessage)
                                        .setMustUnderstand (true)
                                        .getAsSoapDocument (m_aPayload);

    sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()),
                      false,
                      "'finalRecipient' property is empty or not existant but mandatory");
  }

  @Nonnull
  private static Ebms3Property _createRandomProperty ()
  {
    return MessageHelperMethods.createEbms3Property ("randomname" + UUID.randomUUID (),
                                                     "randomvalue" + UUID.randomUUID ());
  }
}
