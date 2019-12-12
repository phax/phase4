/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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

import org.apache.http.HttpEntity;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.http.HttpXMLEntity;
import com.helger.phase4.messaging.domain.AS4PullRequestMessage;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.profile.cef.CEFPMode;
import com.helger.phase4.server.standalone.RunInJettyAS4TEST9090;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.xml.serialize.read.DOMReader;

public class TwoWayAsyncPushPullTest extends AbstractUserMessageTestSetUpExt
{
  private final ESOAPVersion m_eSoapVersion = ESOAPVersion.AS4_DEFAULT;
  private PMode m_aPMode;

  @BeforeClass
  public static void startServerNinety () throws Exception
  {
    WebAppListener.setOnlyOneInstanceAllowed (false);
    RunInJettyAS4TEST9090.startNinetyServer ();
  }

  @AfterClass
  public static void shutDownServerNinety () throws Exception
  {
    // reset
    RunInJettyAS4TEST9090.stopNinetyServer ();
    WebAppListener.setOnlyOneInstanceAllowed (true);
  }

  @Before
  public void createTwoWayPMode ()
  {
    final PMode aPMode = CEFPMode.createCEFPMode (AS4TestConstants.TEST_INITIATOR,
                                                  AS4TestConstants.TEST_RESPONDER,
                                                  AS4ServerConfiguration.getSettings ()
                                                                        .getAsString ("server.address",
                                                                                      AS4TestConstants.DEFAULT_SERVER_ADDRESS),
                                                  (i, r) -> "pmode" + GlobalIDFactory.getNewPersistentLongID (),
                                                  false);
    // Setting second leg to the same as first
    final PModeLeg aLeg2 = aPMode.getLeg1 ();

    // ESENS PMode is One Way on default settings need to change to two way
    m_aPMode = new PMode ( (i, r) -> aPMode.getID (),
                           PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                           PModeParty.createSimple (DEFAULT_PARTY_ID + "1", CAS4.DEFAULT_ROLE),
                           aPMode.getAgreement (),
                           EMEP.TWO_WAY,
                           EMEPBinding.PUSH_PULL,
                           aPMode.getLeg1 (),
                           aLeg2,
                           aPMode.getPayloadService (),
                           aPMode.getReceptionAwareness ());

    // Delete old PMode since it is getting created in the ESENS createPMode
    MetaAS4Manager.getPModeMgr ().deletePMode (aPMode.getID ());
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (m_aPMode);
  }

  @Test
  public void pushPullSuccess () throws Exception
  {
    // Needs to be cleared so we can exactly see if two messages are contained
    // in the duplicate manager
    final IAS4DuplicateManager aIncomingDuplicateMgr = MetaAS4Manager.getIncomingDuplicateMgr ();
    aIncomingDuplicateMgr.clearCache ();
    assertTrue (aIncomingDuplicateMgr.isEmpty ());
    Document aDoc = _modifyUserMessage (m_aPMode.getID (), null, null, _defaultProperties (), null, null, null);
    String sResponse = sendPlainMessage (new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ()), true, null);

    // Avoid stopping server to receive async response
    ThreadHelper.sleepSeconds (2);

    // Step one assertion for the sync part
    assertTrue (sResponse.contains (AS4TestConstants.RECEIPT_ASSERTCHECK));

    final NodeList nList = aDoc.getElementsByTagName ("eb:MessageId");
    // Should only be called once
    final String aID = nList.item (0).getTextContent ();

    assertTrue (aIncomingDuplicateMgr.findFirst (x -> x.getMessageID ().equals (aID)) != null);
    assertTrue (aIncomingDuplicateMgr.size () == 1);
    assertTrue (sResponse.contains ("<eb:RefToMessageId>" + aID));

    // Depending on the payload a different EMEPBinding get chosen by
    // @MockPullRequestProcessorSPI
    // To Test the pull request part of the EMEPBinding
    final Document aPayload = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/PushPull.xml"));
    final ICommonsList <Object> aAny = new CommonsArrayList <> ();
    aAny.add (aPayload.getDocumentElement ());

    // add the ID from the usermessage since its still one async message
    // transfer
    aDoc = AS4PullRequestMessage.create (m_eSoapVersion,
                                         MessageHelperMethods.createEbms3MessageInfo (aID),
                                         AS4TestConstants.DEFAULT_MPC,
                                         aAny)
                                .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc, m_eSoapVersion.getMimeType ());
    sResponse = sendPlainMessage (aEntity, true, null);

    final NodeList nUserList = aDoc.getElementsByTagName ("eb:MessageId");
    // Should only be called once
    final String aUserMsgID = nUserList.item (0).getTextContent ();

    assertTrue (sResponse.contains (aUserMsgID));
  }
}
