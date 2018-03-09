/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.CEF;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public abstract class AbstractCEFTestSetUp extends AbstractUserMessageTestSetUp
{
  protected static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  protected PMode m_aESENSOneWayPMode;
  protected ESOAPVersion m_eSOAPVersion;
  protected Node m_aPayload;

  @Before
  public void setUpCEF ()
  {
    m_aESENSOneWayPMode = ESENSPMode.createESENSPMode (AS4TestConstants.CEF_INITIATOR_ID,
                                                       AS4TestConstants.CEF_RESPONDER_ID,
                                                       AS4TestConstants.DEFAULT_SERVER_ADDRESS,
                                                       IPModeIDProvider.DEFAULT_DYNAMIC,
                                                       true);

    m_eSOAPVersion = m_aESENSOneWayPMode.getLeg1 ().getProtocol ().getSOAPVersion ();
    try
    {
      m_aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
    }
    catch (final SAXException ex)
    {
      throw new IllegalStateException ("Failed to parse example XML", ex);
    }
  }

  @Nonnull
  protected Document testSignedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                            @Nullable final Node aPayload,
                                            @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                            @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException
  {
    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          testUserMessageSoapNotSigned (aPayload,
                                                                                                        aAttachments),
                                                                          eSOAPVersion,
                                                                          aAttachments,
                                                                          aResMgr,
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  protected Document testUserMessageSoapNotSigned (@Nullable final Node aPayload,
                                                   @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload, aAttachments);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (m_aESENSOneWayPMode.getID (),
                                                                                 DEFAULT_AGREEMENT,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                 AS4TestConstants.CEF_INITIATOR_ID,
                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                 AS4TestConstants.CEF_RESPONDER_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       m_eSOAPVersion)
                                              .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

}
