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
package com.helger.as4.server.servlet;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractUserMessageTestSetUpExt extends AbstractUserMessageTestSetUp
{

  /**
   * Modify the standard user message to try special cases or provoke failure
   * messages.
   *
   * @param sAnotherOrWrongPModeConfigID
   * @param sAnotherOrWrongPartyIdInitiator
   * @param sAnotherOrWrongPartyIdResponder
   * @param aEbms3MessageProperties
   *        Default should be with _defaultProperties(), only if you do not want
   *        them change this
   * @return
   * @throws Exception
   */
  @Nonnull
  protected Document _modifyUserMessage (@Nullable final String sAnotherOrWrongPModeConfigID,
                                         @Nullable final String sAnotherOrWrongPartyIdInitiator,
                                         @Nullable final String sAnotherOrWrongPartyIdResponder,
                                         @Nullable final Ebms3MessageProperties aEbms3MessageProperties) throws Exception
  {
    return _modifyUserMessage (sAnotherOrWrongPModeConfigID,
                               sAnotherOrWrongPartyIdInitiator,
                               sAnotherOrWrongPartyIdResponder,
                               aEbms3MessageProperties,
                               null);
  }

  /**
   * Modify the standard user message to try special cases or provoke failure
   * messages.
   *
   * @param sAnotherOrWrongPModeConfigID
   * @param sAnotherOrWrongPartyIdInitiator
   * @param sAnotherOrWrongPartyIdResponder
   * @param aEbms3MessageProperties
   *        Default should be with _defaultProperties(), only if you do not want
   *        them change this
   * @return
   * @throws Exception
   */
  @Nonnull
  protected Document _modifyUserMessage (@Nullable final String sAnotherOrWrongPModeConfigID,
                                         @Nullable final String sAnotherOrWrongPartyIdInitiator,
                                         @Nullable final String sAnotherOrWrongPartyIdResponder,
                                         @Nullable final Ebms3MessageProperties aEbms3MessageProperties,
                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments) throws Exception
  {
    // If argument is set replace the default one
    final String sSetPartyIDInitiator = sAnotherOrWrongPartyIdInitiator == null ? MockEbmsHelper.DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdInitiator;
    final String sSetPartyIDResponder = sAnotherOrWrongPartyIdResponder == null ? MockEbmsHelper.DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdResponder;
    Ebms3PayloadInfo aEbms3PayloadInfo;
    Node aPayload = null;
    if (aAttachments == null)
    {
      aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
      aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, null);
    }
    else
    {
      aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (null, aAttachments);
    }

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                           "MyServiceTypes",
                                                                                                           "QuoteToCollect",
                                                                                                           "4321",
                                                                                                           sAnotherOrWrongPModeConfigID,
                                                                                                           MockEbmsHelper.DEFAULT_AGREEMENT);
    final Ebms3PartyInfo aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                                   sSetPartyIDInitiator,
                                                                                   CAS4.DEFAULT_RESPONDER_URL,
                                                                                   sSetPartyIDResponder);

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     ESOAPVersion.AS4_DEFAULT)
                                                 .setMustUnderstand (true);

    return aAttachments != null ? aDoc.getAsSOAPDocument (null) : aDoc.getAsSOAPDocument (aPayload);
  }

  @Nonnull
  protected static Predicate <IPMode> _getFirstPModeWithID (@Nonnull final String sID)
  {
    return p -> p.getID ().equals (sID);
  }

  protected Ebms3MessageProperties _defaultProperties ()
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (aEbms3Properties);
    return aEbms3MessageProperties;
  }
}
