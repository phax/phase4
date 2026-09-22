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
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
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
 * End-to-end scenarios of the AS4 Multi-Hop endpoint support.
 * <p>
 * <b>Scope note.</b> The endpoints are driven through {@code AS4RequestHandler} rather than
 * through an HTTP servlet container. Multi-hop changes nothing about the HTTP layer, and the
 * servlet path is already covered by the phase4-test suite. What is exercised here is the part
 * multi-hop actually changes: the role attribute, the three routing headers, the signature
 * coverage across a byte transparent intermediary, and the asynchronous receipt path.
 * </p>
 *
 * @author Philip Helger
 */
public final class MultiHopIntegrationTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  private static final String PARTY_A = "phase4-multihop-a";
  private static final String PARTY_B = "phase4-multihop-b";
  private static final String PMODE_ID = "multihop-test-pmode";
  private static final String SOAP_CONTENT_TYPE = ESoapVersion.SOAP_12.getMimeType ().getAsString ();

  private static KeyStore s_aKeyStore;

  private AS4MultiHopConfig m_aConfig;
  private TestForwardingIntermediary m_aICloud;
  private MultiHopTestEndpoint m_aEndpointA;
  private MultiHopTestEndpoint m_aEndpointB;

  @BeforeClass
  public static void loadKeyStore () throws Exception
  {
    s_aKeyStore = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.PKCS12,
                                                      "keys/mh-test.p12",
                                                      "mh-test".toCharArray ());
    assertNotNull ("Failed to load the test key store", s_aKeyStore);
  }

  @Before
  public void setUp ()
  {
    m_aConfig = new AS4MultiHopConfig ();
    m_aConfig.addAddActorOrRoleAttributePModeID (PMODE_ID);

    MultiHopSentMessageStore.getDefaultInstance ().clear ();
    m_aICloud = new TestForwardingIntermediary ();
    m_aEndpointA = new MultiHopTestEndpoint ("A", s_aKeyStore, "mh-a", "mh-test", m_aConfig, true);
    m_aEndpointB = new MultiHopTestEndpoint ("B", s_aKeyStore, "mh-b", "mh-test", m_aConfig, true);

    m_aICloud.registerEndpoint (PARTY_A, m_aEndpointA);
    m_aICloud.registerEndpoint (PARTY_B, m_aEndpointB);

    // D7 - the PMode is stored under the .resp unit ID, while the messages carry the ID
    // without the suffix (R11). Only MultiHopPModeResolver can bridge that.
    MultiHopTestPModes.createAndStore (PMODE_ID + CAS4MultiHop.PMODE_SUFFIX_RESP, PARTY_A, PARTY_B);
    MultiHopTestPModes.createAndStore (PMODE_ID + CAS4MultiHop.PMODE_SUFFIX_RESP, PARTY_B, PARTY_A);
  }

  @NonNull
  private static Ebms3UserMessage _createEbms3UserMessage (@NonNull final String sFrom, @NonNull final String sTo)
  {
    final Ebms3UserMessage ret = new Ebms3UserMessage ();
    ret.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    ret.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                  sFrom,
                                                                  CAS4.DEFAULT_RESPONDER_URL,
                                                                  sTo));
    ret.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo (PMODE_ID,
                                                                                 "urn:as4:agreement",
                                                                                 null,
                                                                                 "svcType",
                                                                                 "svc",
                                                                                 "theAction",
                                                                                 MessageHelperMethods.createRandomConversationID ()));
    ret.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    return ret;
  }

  /**
   * Build a signed User Message that is routed through the I-Cloud, exactly as the sender side of
   * this module would.
   */
  private byte @NonNull [] _buildMultiHopUserMessage (@NonNull final MultiHopTestEndpoint aSender,
                                                      @NonNull final String sFrom,
                                                      @NonNull final String sTo,
                                                      final boolean bMultiHop) throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final Ebms3UserMessage aEbms3 = _createEbms3UserMessage (sFrom, sTo);
      final AS4UserMessage aMsg = AS4UserMessage.create (ESoapVersion.SOAP_12, aEbms3);

      // R1 - the nextmsh role attribute
      new MultiHopBuildMessageCallback (null, bMultiHop).onAS4Message (aMsg);

      // What AS4MultiHopSender.configure() does: remember the message, so that an
      // asynchronously arriving Receipt can be related back to the PMode (C3 / D12)
      MultiHopSentMessageStore.getDefaultInstance ()
                              .rememberSentMessage (aEbms3.getMessageInfo ().getMessageId (),
                                                    PMODE_ID + CAS4MultiHop.PMODE_SUFFIX_RESP);

      final Document aSigned = AS4Signer.createSignedMessage (aSender.getCryptoFactory (),
                                                               aMsg.getAsSoapDocument (null),
                                                               ESoapVersion.SOAP_12,
                                                               aMsg.getMessagingID (),
                                                               null,
                                                               aResHelper,
                                                               false,
                                                               AS4SigningParams.createDefault ());

      // Canonicalization aware serialization - anything else breaks the signature
      return AS4XMLHelper.serializeXML (aSigned).getBytes (StandardCharsets.UTF_8);
    }
  }

  @NonNull
  private static Element _getFirst (@NonNull final Document aDoc,
                                    @NonNull final String sNamespace,
                                    @NonNull final String sLocalName)
  {
    final NodeList aList = aDoc.getElementsByTagNameNS (sNamespace, sLocalName);
    assertTrue ("No " + sLocalName + " found in the response", aList.getLength () > 0);
    return (Element) aList.item (0);
  }

  private static void _assertIsSignedRoutedReceipt (final byte @NonNull [] aBytes,
                                                    @NonNull final String sExpectedAction,
                                                    @NonNull final String sExpectedMPCSuffix)
  {
    final Document aDoc = DOMReader.readXMLDOM (aBytes);
    assertNotNull ("The response is not XML", aDoc);

    // It is signed
    assertTrue ("The routed signal is not signed",
                aDoc.getElementsByTagNameNS (WSConstants.SIG_NS, "Signature").getLength () > 0);

    // R4 - all three headers
    assertEquals (CAS4MultiHop.ICLOUD_URI, _getFirst (aDoc, CAS4MultiHop.WSA_NS, "To").getTextContent ());
    assertEquals (sExpectedAction, _getFirst (aDoc, CAS4MultiHop.WSA_NS, "Action").getTextContent ());

    final Element aRI = _getFirst (aDoc, CAS4MultiHop.EBINT_NS, "RoutingInput");

    // R9 - all three are covered by the signature
    final NodeList aRefs = aDoc.getElementsByTagNameNS (WSConstants.SIG_NS, "Reference");
    final var aRefURIs = new CommonsArrayList <String> ();
    for (int i = 0; i < aRefs.getLength (); ++i)
      aRefURIs.add (((Element) aRefs.item (i)).getAttribute ("URI"));

    for (final String sLocalName : new String [] { "To", "Action" })
    {
      final String sID = _getFirst (aDoc, CAS4MultiHop.WSA_NS, sLocalName).getAttributeNS (CAS4.WSU_NS, "Id");
      assertTrue ("wsa:" + sLocalName + " is not covered by the signature. References: " + aRefURIs,
                  aRefURIs.contains ("#" + sID));
    }
    assertTrue ("ebint:RoutingInput is not covered by the signature. References: " + aRefURIs,
                aRefURIs.contains ("#" + aRI.getAttributeNS (CAS4.WSU_NS, "Id")));

    // R6 - the MPC suffix
    final var aParsed = new MultiHopRoutingInputMarshaller ().read (aRI);
    assertNotNull ("The RoutingInput could not be parsed back", aParsed);
    assertTrue ("Wrong MPC: " + aParsed.getUserMessage ().getMpc (),
                aParsed.getUserMessage ().getMpc ().endsWith (sExpectedMPCSuffix));
  }

  /**
   * S1 "First-and-last-push" (ebMS3 Part 2 section 2.4.7.1 case 1).<br>
   * A sends through the I-Cloud, B answers synchronously, and the intermediary streams the
   * response back. The Receipt must be signed by B and carry the three routing headers.
   */
  @Test
  public void testS1FirstAndLastPush () throws Exception
  {
    final byte [] aRequest = _buildMultiHopUserMessage (m_aEndpointA, PARTY_A, PARTY_B, true);

    final MultiHopTestEndpoint.Response aResponse = m_aICloud.pushSync (aRequest, SOAP_CONTENT_TYPE);

    assertTrue ("B did not answer at all", aResponse.hasBytes ());
    _assertIsSignedRoutedReceipt (aResponse.getBytes (),
                                  CAS4MultiHop.WSA_ACTION_ONEWAY_RECEIPT,
                                  CAS4MultiHop.MPC_SUFFIX_RECEIPT);

    // The intermediary forwarded the identical bytes
    assertEquals (1, m_aICloud.getAllForwardedMessages ().size ());
    assertArrayEquals ("The I-Cloud did not forward the identical bytes",
                       aRequest,
                       m_aICloud.getAllForwardedMessages ().getFirstOrNull ().getBytes ());
  }

  /**
   * S2 - the same, but the routed Receipt is delivered back to A asynchronously instead of being
   * streamed through. A must accept it via the servlet path, which is only possible because of
   * the C3 core extension point.
   */
  @Test
  public void testS2AsyncReceiptDelivery () throws Exception
  {
    final byte [] aRequest = _buildMultiHopUserMessage (m_aEndpointA, PARTY_A, PARTY_B, true);

    // The edge intermediary accepted the message; B answers to the I-Cloud
    final MultiHopTestEndpoint.Response aFromB = m_aICloud.pushSync (aRequest, SOAP_CONTENT_TYPE);
    assertTrue (aFromB.hasBytes ());

    // The I-Cloud now routes the Receipt onwards, based on its RoutingInput.
    // The reversed To party is A - proving the inferred reverse routing works end to end (R6).
    final MultiHopTestEndpoint.Response aAtA = m_aICloud.pushSync (aFromB.getBytes (), SOAP_CONTENT_TYPE);

    // A accepted the routed Receipt. It answers nothing - a Receipt is never answered (R3).
    assertTrue ("A rejected the routed Receipt: " +
                (aAtA.hasBytes () ? new String (aAtA.getBytes (), StandardCharsets.UTF_8) : "<empty>"),
                !aAtA.hasBytes () || !new String (aAtA.getBytes (), StandardCharsets.UTF_8).contains ("EBMS:"));

    assertEquals (2, m_aICloud.getAllForwardedMessages ().size ());
  }

  /**
   * S2b - the routed Receipt can also be queued on its MPC for later pulling, which is what
   * happens when the receiving endpoint is not addressable (R13 / D12 groundwork).
   */
  @Test
  public void testS2bReceiptCanBeQueuedForPull () throws Exception
  {
    final byte [] aRequest = _buildMultiHopUserMessage (m_aEndpointA, PARTY_A, PARTY_B, true);
    final MultiHopTestEndpoint.Response aFromB = m_aICloud.pushSync (aRequest, SOAP_CONTENT_TYPE);

    final String sMPC = m_aICloud.queueForPull (aFromB.getBytes (), SOAP_CONTENT_TYPE);
    assertNotNull ("The Receipt carries no RoutingInput MPC", sMPC);
    assertTrue ("The Receipt was not queued on a .receipt MPC but on " + sMPC,
                sMPC.endsWith (CAS4MultiHop.MPC_SUFFIX_RECEIPT));

    final TestForwardingIntermediary.StoredMessage aPulled = m_aICloud.pull (sMPC);
    assertNotNull ("Nothing could be pulled from " + sMPC, aPulled);
    assertArrayEquals ("Pulling changed the bytes", aFromB.getBytes (), aPulled.getBytes ());

    // The queue is empty afterwards
    assertEquals (0, m_aICloud.getQueue (sMPC).size ());
  }

  /**
   * S5 - regression. Without the nextmsh role the exchange behaves exactly as a plain phase4:
   * the Receipt carries none of the multi-hop headers.
   */
  @Test
  public void testS5NoMultiHopIsUnchanged () throws Exception
  {
    // Endpoints without the multi-hop customizer, i.e. a phase4 without this module
    final MultiHopTestEndpoint aPlainB = new MultiHopTestEndpoint ("plain-B",
                                                                    s_aKeyStore,
                                                                    "mh-b",
                                                                    "mh-test",
                                                                    m_aConfig,
                                                                    false);

    final byte [] aRequest = _buildMultiHopUserMessage (m_aEndpointA, PARTY_A, PARTY_B, false);
    final MultiHopTestEndpoint.Response aResponse = aPlainB.receive (aRequest, SOAP_CONTENT_TYPE);

    assertTrue ("B did not answer at all", aResponse.hasBytes ());

    final Document aDoc = DOMReader.readXMLDOM (aResponse.getBytes ());
    assertNotNull (aDoc);
    assertEquals ("A wsa:To was added although multi-hop is not in play",
                  0,
                  aDoc.getElementsByTagNameNS (CAS4MultiHop.WSA_NS, "To").getLength ());
    assertEquals ("A RoutingInput was added although multi-hop is not in play",
                  0,
                  aDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());
  }

  /**
   * S5b - even with the multi-hop customizer installed, a message that did NOT arrive with the
   * nextmsh role gets an unchanged response (R5). This is the invariant that keeps the module
   * safe to deploy.
   */
  @Test
  public void testS5bCustomizerInstalledButNoRole () throws Exception
  {
    final byte [] aRequest = _buildMultiHopUserMessage (m_aEndpointA, PARTY_A, PARTY_B, false);
    final MultiHopTestEndpoint.Response aResponse = m_aEndpointB.receive (aRequest, SOAP_CONTENT_TYPE);

    assertTrue (aResponse.hasBytes ());

    final Document aDoc = DOMReader.readXMLDOM (aResponse.getBytes ());
    assertNotNull (aDoc);
    assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.WSA_NS, "To").getLength ());
    assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());
  }
}
