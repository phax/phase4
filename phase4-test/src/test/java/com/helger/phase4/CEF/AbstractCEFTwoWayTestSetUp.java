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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestConstants;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.server.MockPModeGenerator;
import com.helger.phase4.server.message.AbstractUserMessageTestSetUp;
import com.helger.phase4.server.standalone.RunInJettyAS4TEST9090;
import com.helger.phase4.test.profile.TestPMode;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.xml.serialize.read.DOMReader;

public abstract class AbstractCEFTwoWayTestSetUp extends AbstractUserMessageTestSetUp
{
  protected static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  protected PMode m_aESENSTwoWayPMode;
  protected ESoapVersion m_eSoapVersion;
  protected Node m_aPayload;

  protected AbstractCEFTwoWayTestSetUp ()
  {
    super ();
  }

  protected AbstractCEFTwoWayTestSetUp (@Nonnegative final int nRetries)
  {
    super (nRetries);
  }

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
  public void setUpCEF ()
  {
    m_aESENSTwoWayPMode = TestPMode.createTestPModeTwoWay (AS4TestConstants.CEF_INITIATOR_ID,
                                                           AS4TestConstants.CEF_RESPONDER_ID,
                                                           AS4TestConstants.DEFAULT_SERVER_ADDRESS,
                                                           IPModeIDProvider.DEFAULT_DYNAMIC,
                                                           true);

    m_eSoapVersion = m_aESENSTwoWayPMode.getLeg1 ().getProtocol ().getSoapVersion ();
    m_aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
  }

  @Nonnull
  protected Document testSignedUserMessage (@Nonnull final ESoapVersion eSOAPVersion,
                                            @Nullable final Node aPayload,
                                            @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                            @Nonnull @WillNotClose final AS4ResourceHelper aResMgr) throws WSSecurityException
  {
    final AS4UserMessage aMsg = testUserMessageSoapNotSigned (aPayload, aAttachments);
    return AS4Signer.createSignedMessage (m_aCryptoFactory,
                                          aMsg.getAsSoapDocument (aPayload),
                                          eSOAPVersion,
                                          aMsg.getMessagingID (),
                                          aAttachments,
                                          aResMgr,
                                          true,
                                          AS4SigningParams.createDefault ());
  }

  @Nonnull
  protected AS4UserMessage testUserMessageSoapNotSigned (@Nullable final Node aPayload,
                                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null,
                                                                                            aAttachments);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (m_aESENSTwoWayPMode.getID (),
                                                                                 DEFAULT_AGREEMENT,
                                                                                 null,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                 AS4TestConstants.CEF_INITIATOR_ID,
                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                 AS4TestConstants.CEF_RESPONDER_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    return AS4UserMessage.create (aEbms3MessageInfo,
                                  aEbms3PayloadInfo,
                                  aEbms3CollaborationInfo,
                                  aEbms3PartyInfo,
                                  aEbms3MessageProperties,
                                  null,
                                  m_eSoapVersion)
                         .setMustUnderstand (true);
  }
}
