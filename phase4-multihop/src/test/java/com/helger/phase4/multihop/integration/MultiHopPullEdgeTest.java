/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import org.apache.wss4j.dom.WSConstants;
import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.phase4.CAS4;
import com.helger.phase4.client.AS4ClientBuiltMessage;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4PullRequestMessage;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.callback.AS4MultiHopReceiptSender;
import com.helger.phase4.multihop.incoming.MultiHopSentMessageStore;
import com.helger.phase4.multihop.model.MultiHopRoutingInputMarshaller;
import com.helger.phase4.multihop.sender.MultiHopBuildMessageCallback;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Scenario S3 "First-push-last-pull" (ebMS3 Part 2 section 2.4.7.1 case 2) plus the unit level
 * checks of {@link AS4MultiHopReceiptSender}.
 *
 * @author Philip Helger
 */
public final class MultiHopPullEdgeTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  private static final String PARTY_A = "pull-a";
  private static final String PARTY_B = "pull-b";
  private static final String PMODE_ID = "pull-test-pmode";
  private static final String CT = ESoapVersion.SOAP_12.getMimeType ().getAsString ();

  private static KeyStore s_aKeyStore;

  private AS4MultiHopConfig m_aConfig;
  private TestForwardingIntermediary m_aICloud;
  private IAS4CryptoFactory m_aCryptoA;
  private IAS4CryptoFactory m_aCryptoB;

  @BeforeClass
  public static void loadKeyStore () throws Exception
  {
    s_aKeyStore = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.PKCS12,
                                                      "keys/mh-test.p12",
                                                      "mh-test".toCharArray ());
    assertNotNull (s_aKeyStore);
  }

  @Before
  public void setUp ()
  {
    MultiHopSentMessageStore.getDefaultInstance ().clear ();

    m_aConfig = new AS4MultiHopConfig ();
    m_aConfig.addAddActorOrRoleAttributePModeID (PMODE_ID);

    m_aICloud = new TestForwardingIntermediary ();
    m_aCryptoA = new AS4CryptoFactoryInMemoryKeyStore (s_aKeyStore, "mh-a", "mh-test".toCharArray (), s_aKeyStore);
    m_aCryptoB = new AS4CryptoFactoryInMemoryKeyStore (s_aKeyStore, "mh-b", "mh-test".toCharArray (), s_aKeyStore);

    MultiHopTestPModes.createAndStore (PMODE_ID + CAS4MultiHop.PMODE_SUFFIX_RESP, PARTY_A, PARTY_B);
  }

  @NonNull
  private static Ebms3UserMessage _createUserMessage ()
  {
    final Ebms3UserMessage ret = new Ebms3UserMessage ();
    ret.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    ret.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                  PARTY_A,
                                                                  CAS4.DEFAULT_RESPONDER_URL,
                                                                  PARTY_B));
    ret.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo (PMODE_ID,
                                                                                 "urn:as4:agreement",
                                                                                 null,
                                                                                 "svcType",
                                                                                 "svc",
                                                                                 "theAction",
                                                                                 "cid"));
    ret.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    ret.setMpc (CAS4.DEFAULT_MPC_ID);
    return ret;
  }

  private static byte @NonNull [] _toBytes (@NonNull final AS4ClientBuiltMessage aMsg) throws Exception
  {
    try (final ByteArrayOutputStream aBAOS = new ByteArrayOutputStream ())
    {
      aMsg.getHttpEntity ().writeTo (aBAOS);
      return aBAOS.toByteArray ();
    }
  }

  /**
   * R2 - a Pull Request never carries the nextmsh role or a RoutingInput, even when the endpoint
   * is fully multi-hop enabled.
   */
  @Test
  public void testPullRequestIsPlain () throws Exception
  {
    final Ebms3SignalMessage aSignal = new Ebms3SignalMessage ();
    aSignal.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    final Ebms3PullRequest aPR = new Ebms3PullRequest ();
    aPR.setMpc (CAS4.DEFAULT_MPC_ID);
    aSignal.setPullRequest (aPR);

    final AS4PullRequestMessage aMsg = new AS4PullRequestMessage (ESoapVersion.SOAP_12, aSignal);
    new MultiHopBuildMessageCallback (null, true).onAS4Message (aMsg);

    final Document aDoc = aMsg.getAsSoapDocument ();
    assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());
    assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.WSA_NS, "To").getLength ());
  }

  /**
   * S3 "First-push-last-pull".
   * <ol>
   * <li>A pushes a multi-hop User Message into the I-Cloud, which queues it</li>
   * <li>B pulls it</li>
   * <li>B answers with a <b>callback</b> Receipt that carries the routing headers (R4)</li>
   * <li>The I-Cloud queues that Receipt on the ".receipt" MPC</li>
   * <li>A pulls the Receipt back and it is unchanged (R13)</li>
   * </ol>
   */
  @Test
  public void testS3FirstPushLastPull () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // (1) A builds and pushes the User Message
      final Ebms3UserMessage aEbms3UserMsg = _createUserMessage ();
      final AS4UserMessage aUserMsg = AS4UserMessage.create (ESoapVersion.SOAP_12, aEbms3UserMsg);
      new MultiHopBuildMessageCallback (null, true).onAS4Message (aUserMsg);

      MultiHopSentMessageStore.getDefaultInstance ()
                              .rememberSentMessage (aEbms3UserMsg.getMessageInfo ().getMessageId (),
                                                    PMODE_ID + CAS4MultiHop.PMODE_SUFFIX_RESP);

      final byte [] aUserMsgBytes = AS4XMLHelper.serializeXML (AS4Signer.createSignedMessage (m_aCryptoA,
                                                                                               aUserMsg.getAsSoapDocument (null),
                                                                                               ESoapVersion.SOAP_12,
                                                                                               aUserMsg.getMessagingID (),
                                                                                               null,
                                                                                               aResHelper,
                                                                                               false,
                                                                                               AS4SigningParams.createDefault ()))
                                                .getBytes (StandardCharsets.UTF_8);

      // (2) B pulls it - the I-Cloud hands out the identical bytes
      m_aICloud.getQueue (CAS4.DEFAULT_MPC_ID)
               .add (new TestForwardingIntermediary.StoredMessage (aUserMsgBytes, CT));
      final TestForwardingIntermediary.StoredMessage aPulledUserMsg = m_aICloud.pull (CAS4.DEFAULT_MPC_ID);
      assertNotNull ("B could not pull the User Message", aPulledUserMsg);
      assertArrayEquals ("Pulling changed the bytes", aUserMsgBytes, aPulledUserMsg.getBytes ());

      // (3) B answers with a callback Receipt
      final Document aPulledDoc = DOMReader.readXMLDOM (aPulledUserMsg.getBytes ());
      assertNotNull (aPulledDoc);

      final AS4ClientBuiltMessage aReceipt = AS4MultiHopReceiptSender.createCallbackReceipt (aEbms3UserMsg,
                                                                                             aPulledDoc,
                                                                                             MessageHelperMethods.createRandomMessageID (),
                                                                                             ESoapVersion.SOAP_12,
                                                                                             m_aCryptoB,
                                                                                             aResHelper,
                                                                                             m_aConfig);
      final byte [] aReceiptBytes = _toBytes (aReceipt);
      final Document aReceiptDoc = DOMReader.readXMLDOM (aReceiptBytes);
      assertNotNull (aReceiptDoc);

      // R4 - the callback Receipt carries the same three headers as a synchronous one
      assertEquals (1, aReceiptDoc.getElementsByTagNameNS (CAS4MultiHop.WSA_NS, "To").getLength ());
      assertEquals (1, aReceiptDoc.getElementsByTagNameNS (CAS4MultiHop.WSA_NS, "Action").getLength ());
      assertEquals (1, aReceiptDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());

      // R9 - and they are covered by the signature
      final var aRefURIs = new CommonsArrayList <String> ();
      final NodeList aRefs = aReceiptDoc.getElementsByTagNameNS (WSConstants.SIG_NS, "Reference");
      for (int i = 0; i < aRefs.getLength (); ++i)
        aRefURIs.add (((Element) aRefs.item (i)).getAttribute ("URI"));

      for (final String [] aPair : new String [] [] { { CAS4MultiHop.WSA_NS, "To" },
                                                      { CAS4MultiHop.WSA_NS, "Action" },
                                                      { CAS4MultiHop.EBINT_NS, "RoutingInput" } })
      {
        final Element aElement = (Element) aReceiptDoc.getElementsByTagNameNS (aPair[0], aPair[1]).item (0);
        assertTrue (aPair[1] + " is not covered by the signature. References: " + aRefURIs,
                    aRefURIs.contains ("#" + aElement.getAttributeNS (CAS4.WSU_NS, "Id")));
      }

      // (4) The I-Cloud queues the Receipt on the .receipt MPC
      final String sReceiptMPC = m_aICloud.queueForPull (aReceiptBytes, CT);
      assertNotNull ("The callback Receipt carries no RoutingInput MPC", sReceiptMPC);
      assertEquals (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_RECEIPT, sReceiptMPC);

      // (5) A pulls the Receipt back, unchanged - R13
      final TestForwardingIntermediary.StoredMessage aPulledReceipt = m_aICloud.pull (sReceiptMPC);
      assertNotNull ("A could not pull the Receipt", aPulledReceipt);
      assertArrayEquals ("Pulling changed the Receipt bytes", aReceiptBytes, aPulledReceipt.getBytes ());

      // The routing input of the pulled Receipt addresses A again (R6)
      final var aParsed = new MultiHopRoutingInputMarshaller ().read ((Element) DOMReader.readXMLDOM (aPulledReceipt.getBytes ())
                                                                                          .getElementsByTagNameNS (CAS4MultiHop.EBINT_NS,
                                                                                                                   "RoutingInput")
                                                                                          .item (0));
      assertNotNull (aParsed);
      assertEquals (PARTY_A, aParsed.getUserMessage ().getPartyInfo ().getTo ().getPartyIdAtIndex (0).getValue ());
      assertEquals (PARTY_B, aParsed.getUserMessage ().getPartyInfo ().getFrom ().getPartyIdAtIndex (0).getValue ());
    }
  }
}
