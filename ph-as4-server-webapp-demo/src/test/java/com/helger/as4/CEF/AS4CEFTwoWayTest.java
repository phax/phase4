package com.helger.as4.CEF;

import static org.junit.Assert.assertTrue;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;

public class AS4CEFTwoWayTest extends AbstractCEFTwoWayTestSetUp
{
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
   */
  @Test
  public void AS4_TA01 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, new AS4ResourceManager ());
    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);

    assertTrue (sResponse.contains ("ConversationId"));
    assertTrue (sResponse.contains (CONVERSATION_ID));
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
   */
  @Test
  public void AS4_TA02 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, new AS4ResourceManager ());
    final NodeList nList = aDoc.getElementsByTagName ("eb:MessageId");

    // Should only be called once
    final String aID = nList.item (0).getTextContent ();
    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);

    assertTrue (sResponse.contains ("eb:RefToMessageId"));
    assertTrue (sResponse.contains (aID));
    assertTrue (sResponse.contains ("ConversationId"));
    assertTrue (sResponse.contains (CONVERSATION_ID));
  }
}
