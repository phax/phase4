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
package com.helger.as4.server.external;

import javax.mail.internet.MimeMessage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.CEF.AbstractCEFTestSetUp;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.http.HttpMimeMessageEntity;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.server.MockJettySetup;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;

@Ignore ("Axis2 bug in Holodeck! Requires external proxy and PEPPOL pilot certificate!")
public final class HolodeckFuncTest extends AbstractCEFTestSetUp
{
  /** The test URL for Holodeck */
  public static final String DEFAULT_AS4_NET_URI = "http://interop.holodeck-b2b.com:8080/msh";
  private static final String COLLABORATION_INFO_SERVICE = "SRV_SIMPLE_ONEWAY_DYN";
  private static final String COLLABORATION_INFO_SERVICE_TYPE = null;
  private static final String COLLABORATION_INFO_ACTION = "ACT_SIMPLE_ONEWAY_DYN";
  private static final String TO_PARTY_ID = "holodeck-c3";

  @BeforeClass
  public static void noJetty ()
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    AS4ServerConfiguration.getMutableSettings ().putIn (MockJettySetup.SETTINGS_SERVER_JETTY_ENABLED, false);
    AS4ServerConfiguration.getMutableSettings ().putIn (MockJettySetup.SETTINGS_SERVER_ADDRESS, DEFAULT_AS4_NET_URI);
    AS4HttpDebug.setEnabled (true);
  }

  @AfterClass
  public static void disableDebug ()
  {
    AS4HttpDebug.setEnabled (false);
  }

  @Test
  public void sendToHolodeck () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    s_aResMgr));

    // New message ID
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (false, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (true ? null
                                                                                                                   : m_aESENSOneWayPMode.getID (),
                                                                                                              true ? null
                                                                                                                   : DEFAULT_AGREEMENT,
                                                                                                              COLLABORATION_INFO_SERVICE_TYPE,
                                                                                                              COLLABORATION_INFO_SERVICE,
                                                                                                              COLLABORATION_INFO_ACTION,
                                                                                                              AS4TestConstants.TEST_CONVERSATION_ID);
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                                      "ph-as4-sender",
                                                                                      CAS4.DEFAULT_RESPONDER_URL,
                                                                                      TO_PARTY_ID);

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MessageHelperMethods.createEmbs3PropertiesOriginalSenderFinalRecipient ("C1",
                                                                                                                                  "C4");
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("trackingidentifier", "tracker"));
    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aUserMsg = AS4UserMessage.create (aEbms3MessageInfo,
                                                           aEbms3PayloadInfo,
                                                           aEbms3CollaborationInfo,
                                                           aEbms3PartyInfo,
                                                           aEbms3MessageProperties,
                                                           m_eSOAPVersion)
                                                  .setMustUnderstand (true);

    // Sign payload document
    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          aUserMsg.getAsSOAPDocument (),
                                                                          m_eSOAPVersion,
                                                                          aUserMsg.getMessagingID (),
                                                                          aAttachments,
                                                                          new AS4ResourceManager (),
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    final MimeMessage aMsg = MimeMessageCreator.generateMimeMessage (m_eSOAPVersion, aSignedDoc, aAttachments);
    sendMimeMessage (new HttpMimeMessageEntity (aMsg), true, null);
  }
}
