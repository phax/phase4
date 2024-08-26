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
package com.helger.phase4.CEF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.commons.concurrent.ThreadHelper;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.messaging.http.HttpXMLEntity;
import com.helger.phase4.mgr.MetaAS4Manager;

public final class AS4CEFTwoWayFuncTest extends AbstractCEFTwoWayTestSetUp
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4CEFTwoWayFuncTest.class);

  public AS4CEFTwoWayFuncTest ()
  {
    // No retries
    super (0);
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: Two-Way/Push-and-Push MEP. SMSH sends an AS4 User Message
   * (M1) associated to a specific conversation through variable (element)
   * CONVERSATIONIDM1 (set by the producer). The consumer replies to the message
   * M1.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a User Message (M2) with element CONVERSATIONIDM2 equal
   * to ConversationIdM1 (set by the consumer).
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA01 () throws Exception
  {
    // Needs to be cleared so we can exactly see if two messages are contained
    // in the duplicate manager
    final IAS4DuplicateManager aIncomingDuplicateMgr = MetaAS4Manager.getIncomingDuplicateMgr ();
    aIncomingDuplicateMgr.clearCache ();
    assertTrue (aIncomingDuplicateMgr.isEmpty ());

    final Document aDoc = testSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    // Avoid stopping server to receive async response
    LOGGER.info ("Waiting for 1 second");
    ThreadHelper.sleepSeconds (1);

    // Step one assertion for the sync part
    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));

    final NodeList aNL = aDoc.getElementsByTagName ("eb:MessageId");
    // Should only be called once
    final String aID = aNL.item (0).getTextContent ();

    assertNotNull (aIncomingDuplicateMgr.getItemOfMessageID (aID));
    assertEquals (2, aIncomingDuplicateMgr.getAll ().size ());
  }

  /**
   * Prerequisite:<br>
   * SMSH and RMSH are configured to exchange AS4 messages according to the
   * e-SENS profile: Two-Way/Push-and-Push MEP. SMSH sends an AS4 User Message
   * (M1 with ID MessageId) that requires a consumer response to the RMSH.
   * Additionally, the message is associated to a specific conversation through
   * variable (element) CONVERSATIONIDM1 (set by the producer). The consumer
   * replies to the message M1.<br>
   * <br>
   * Predicate: <br>
   * The RMSH sends back a User Message (M2) with element REFTOMESSAGEID set to
   * MESSAGEID (of M1) and with element CONVERSATIONIDM2 equal to
   * ConversationIdM1.
   *
   * @throws Exception
   *         In case of error
   */
  @Test
  public void testAS4_TA02 () throws Exception
  {
    // Needs to be cleared so we can exactly see if two messages are contained
    // in the duplicate manager
    final IAS4DuplicateManager aIncomingDuplicateMgr = MetaAS4Manager.getIncomingDuplicateMgr ();
    aIncomingDuplicateMgr.clearCache ();
    assertTrue (aIncomingDuplicateMgr.isEmpty ());

    final Document aDoc = testSignedUserMessage (m_eSoapVersion, m_aPayload, null, s_aResMgr);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    // Avoid stopping server to receive async response
    LOGGER.info ("Waiting for 1 second");
    ThreadHelper.sleepSeconds (1);

    final NodeList aNL = aDoc.getElementsByTagName ("eb:MessageId");
    // Should only be called once
    final String aID = aNL.item (0).getTextContent ();
    assertTrue (sResponse.contains ("eb:RefToMessageId"));
    assertTrue (sResponse.contains (aID));
    assertNotNull (aIncomingDuplicateMgr.getItemOfMessageID (aID));
    assertEquals (2, aIncomingDuplicateMgr.getAll ().size ());
  }
}
