package com.helger.as4.CEF;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.server.standalone.RunInJettyAS49090;
import com.helger.commons.thread.ThreadHelper;
import com.helger.photon.core.servlet.WebAppListener;

public final class AS4CEFTwoWayTest extends AbstractCEFTwoWayTestSetUp
{
  public AS4CEFTwoWayTest ()
  {
    // No retries
    super (0);
  }

  @BeforeClass
  public static void startServerNinety () throws Exception
  {
    WebAppListener.setOnlyOneInstanceAllowed (false);
    RunInJettyAS49090.startNinetyServer ();
  }

  @AfterClass
  public static void shutDownServerNinety () throws Exception
  {
    // reset
    RunInJettyAS49090.stopNinetyServer ();
    WebAppListener.setOnlyOneInstanceAllowed (true);
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
  public void AS4_TA01 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, s_aResMgr);
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc), true, null);

    // Avoid stopping server to receive async response
    ThreadHelper.sleepSeconds (2);

    // Step one assertion for the sync part
    assertTrue (sResponse.contains ("Receipt"));

    final NodeList nList = aDoc.getElementsByTagName ("eb:MessageId");
    // Should only be called once
    final String aID = nList.item (0).getTextContent ();

    // <item dt="2017-06-22T14:53:40.091"
    // msgid="ph-as4@005ed363-bc75-4dab-a58e-d7bdc12b5699"
    // pmodeid="APP_000000000011-APP_000000000011" />

    assertTrue (MetaAS4Manager.getIncomingDuplicateMgr ().findFirst (x -> x.getMessageID ().equals (aID)) != null);
    assertTrue (MetaAS4Manager.getIncomingDuplicateMgr ().getAll ().size () > 1);
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
  // TODO async has to work
  public void AS4_TA02 () throws Exception
  {
    final Document aDoc = testSignedUserMessage (m_eSOAPVersion, m_aPayload, null, s_aResMgr);
    final NodeList nList = aDoc.getElementsByTagName ("eb:MessageId");

    // Should only be called once
    final String aID = nList.item (0).getTextContent ();
    final String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc), true, null);

    assertTrue (sResponse.contains ("eb:RefToMessageId"));
    assertTrue (sResponse.contains (aID));
  }
}
