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
package com.helger.as4.server.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Node;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  public static final String MPC_FAILURE = "failure";
  public static final String MPC_EMPTY = "empty";
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    if (aPMode.getMEPBinding ().equals (EMEPBinding.PUSH_PUSH))
    {
      return AS4MessageProcessorResult.createSuccess (aIncomingAttachments,
                                                      true ? "http://localhost:9090/as4"
                                                           : AS4ServerConfiguration.getServerAddress ());
    }

    return AS4MessageProcessorResult.createSuccess (aIncomingAttachments, null);
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4MessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    // Must be a pull-request
    final Ebms3PullRequest aPullRequest = aSignalMessage.getPullRequest ();
    if (aPullRequest != null)
    {
      if (aPullRequest.getMpc ().equals (MPC_FAILURE))
      {
        return AS4SignalMessageProcessorResult.createFailure ("Error in creating the usermessage - mock MPC 'failure' was used!");
      }

      // Empty MPC
      if (aPullRequest.getMpc ().equals (MPC_EMPTY))
      {
        return AS4SignalMessageProcessorResult.createSuccess ();
      }
    }

    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4TestConstants.getEBMSProperties ();

    final Ebms3MessageInfo aMessageInfo = aSignalMessage.getMessageInfo ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (aMessageInfo.getMessageId ());
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo ("PullPMode",
                                                                                 DEFAULT_AGREEMENT,
                                                                                 AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                 MockPModeGenerator.SOAP11_SERVICE,
                                                                                 AS4TestConstants.TEST_ACTION,
                                                                                 AS4TestConstants.TEST_CONVERSATION_ID);
    aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                 "pullinitiator",
                                                                 CAS4.DEFAULT_RESPONDER_URL,
                                                                 "pullresponder");

    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final Ebms3UserMessage aUserMessage = new Ebms3UserMessage ();
    aUserMessage.setCollaborationInfo (aEbms3CollaborationInfo);
    aUserMessage.setMessageInfo (aEbms3MessageInfo);
    aUserMessage.setMessageProperties (aEbms3MessageProperties);
    aUserMessage.setPartyInfo (aEbms3PartyInfo);
    aUserMessage.setPayloadInfo (aEbms3PayloadInfo);
    if (aPullRequest != null)
      aUserMessage.setMpc (aPullRequest.getMpc ());

    return AS4SignalMessageProcessorResult.createSuccess (null, null, aUserMessage);
  }
}
