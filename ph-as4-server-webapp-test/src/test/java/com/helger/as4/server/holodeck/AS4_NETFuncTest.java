/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.holodeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.CEF.AbstractCEFTestSetUp;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.impl.ICommonsList;

public final class AS4_NETFuncTest extends AbstractCEFTestSetUp
{
  /** TThe test URL for AS4.NET */
  public static final String DEFAULT_AS4_NET_URI = "http://eessidev10.westeurope.cloudapp.azure.com:7070/as4-net-c3";
  private static final String COLLABORATION_INFO_SERVICE = "SRV_SIMPLE_ONEWAY_DYN";
  private static final String COLLABORATION_INFO_ACTION = "ACT_SIMPLE_ONEWAY_DYN";

  @Test
  public void sendToAS4_NET () throws Exception
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    AS4ServerConfiguration.getMutableSettings ().putIn ("server.jetty.enabled", false);
    AS4ServerConfiguration.getMutableSettings ().putIn ("server.address", DEFAULT_AS4_NET_URI);

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (m_aPayload, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo (COLLABORATION_INFO_ACTION,
                                                                              AS4TestConstants.TEST_SERVICE_TYPE,
                                                                              COLLABORATION_INFO_SERVICE,
                                                                              AS4TestConstants.TEST_CONVERSATION_ID,
                                                                              m_aESENSOneWayPMode.getID (),
                                                                              MockEbmsHelper.DEFAULT_AGREEMENT);
    aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                              "ph-as4-sender",
                                                              CAS4.DEFAULT_RESPONDER_URL,
                                                              "ph-as4-receiver");

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);
    final String sTrackerIdentifier = "trackingidentifier";
    aEbms3MessageProperties.addProperty (MessageHelperMethods.createEbms3Property (sTrackerIdentifier, "tracker"));

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     m_eSOAPVersion)
                                                 .setMustUnderstand (true);
    final SignedMessageCreator aClient = new SignedMessageCreator (AS4CryptoFactory.DEFAULT_INSTANCE);

    final Document aSignedDoc = aClient.createSignedMessage (aDoc.getAsSOAPDocument (m_aPayload),
                                                             m_eSOAPVersion,
                                                             null,
                                                             new AS4ResourceManager (),
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final NodeList nList = aSignedDoc.getElementsByTagName ("eb:MessageProperties");
    assertEquals (nList.item (0).getLastChild ().getAttributes ().getNamedItem ("name").getTextContent (),
                  sTrackerIdentifier);

    final String sResponse = sendPlainMessage (new HttpXMLEntity (aSignedDoc, m_eSOAPVersion), true, null);
    assertTrue (sResponse.contains (AS4TestConstants.NON_REPUDIATION_INFORMATION));
    System.out.println (sResponse);
  }
}
