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
package com.helger.phase4.client;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.message.AS4ErrorMessage;
import com.helger.phase4.model.message.AS4ReceiptMessage;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Only used for {@link MainOldAS4Client} as test message constructor.
 *
 * @author bayerlma
 * @author Philip Helger
 */
final class MockClientMessages
{
  private static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  private MockClientMessages ()
  {}

  @Nonnull
  public static Document createUserMessageSigned (@Nonnull final ESoapVersion eSoapVersion,
                                                  @Nullable final Node aPayload,
                                                  @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                  @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws WSSecurityException
  {
    final AS4UserMessage aMsg = createUserMessageNotSigned (eSoapVersion, aPayload, aAttachments);
    return AS4Signer.createSignedMessage (AS4CryptoFactoryConfiguration.getDefaultInstance (),
                                          aMsg.getAsSoapDocument (aPayload),
                                          eSoapVersion,
                                          aMsg.getMessagingID (),
                                          aAttachments,
                                          aResHelper,
                                          false,
                                          AS4SigningParams.createDefault ());
  }

  @Nonnull
  public static Document createErrorMessageSigned (@Nonnull final ESoapVersion eSoapVersion,
                                                   @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                   @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws WSSecurityException
  {
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList <> (EEbmsError.EBMS_INVALID_HEADER.errorBuilder (Locale.US)
                                                                                                             .build ());
    final AS4ErrorMessage aErrorMsg = AS4ErrorMessage.create (eSoapVersion, "srcmsgid", aEbms3ErrorList)
                                                     .setMustUnderstand (true);
    final Document aSignedDoc = AS4Signer.createSignedMessage (AS4CryptoFactoryConfiguration.getDefaultInstance (),
                                                               aErrorMsg.getAsSoapDocument (),
                                                               eSoapVersion,
                                                               aErrorMsg.getMessagingID (),
                                                               aAttachments,
                                                               aResHelper,
                                                               false,
                                                               AS4SigningParams.createDefault ());
    return aSignedDoc;
  }

  @Nonnull
  public static Document createReceiptMessageSigned (@Nonnull final ESoapVersion eSoapVersion,
                                                     @Nullable final Node aPayload,
                                                     @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                     @Nonnull @WillNotClose final AS4ResourceHelper aResHelper) throws WSSecurityException,
                                                                                                                DOMException
  {
    final Document aUserMessage = createUserMessageSigned (eSoapVersion, aPayload, aAttachments, aResHelper);

    final AS4ReceiptMessage aReceiptMsg = AS4ReceiptMessage.create (eSoapVersion,
                                                                    MessageHelperMethods.createRandomMessageID (),
                                                                    null,
                                                                    aUserMessage,
                                                                    true,
                                                                    null)
                                                           .setMustUnderstand (true);
    final Document aDoc = aReceiptMsg.getAsSoapDocument ();

    return AS4Signer.createSignedMessage (AS4CryptoFactoryConfiguration.getDefaultInstance (),
                                          aDoc,
                                          eSoapVersion,
                                          aReceiptMsg.getMessagingID (),
                                          aAttachments,
                                          aResHelper,
                                          false,
                                          AS4SigningParams.createDefault ());
  }

  @Nonnull
  public static AS4UserMessage createUserMessageNotSigned (@Nonnull final ESoapVersion eSoapVersion,
                                                           @Nullable final Node aPayload,
                                                           @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ProcessInst", "PurchaseOrder:123456"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ContextID", "987654321"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER, "C1 OS"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT, "C4 FR"));

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null,
                                                                                            aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo ("pmode-twoway",
                                                                                                              DEFAULT_AGREEMENT,
                                                                                                              null,
                                                                                                              "MyServiceTypes",
                                                                                                              "QuoteToCollect",
                                                                                                              "NewPurchaseOrder",
                                                                                                              "4321");
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                                      "1234",
                                                                                      CAS4.DEFAULT_RESPONDER_URL,
                                                                                      "5678");
    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       null,
                                                       eSoapVersion)
                                              .setMustUnderstand (true);
    return aDoc;
  }

  @Nonnull
  public static Document createUserMessageSoapNotSignedNotPModeConform (@Nonnull final ESoapVersion eSoapVersion,
                                                                        @Nullable final Node aPayload,
                                                                        @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ProcessInst", "PurchaseOrder:123456"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ContextID", "987654321"));

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null,
                                                                                            aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo ("pm-esens-generic-resp",
                                                                                                              DEFAULT_AGREEMENT,
                                                                                                              null,
                                                                                                              "MyServiceTypes",
                                                                                                              "QuoteToCollect",
                                                                                                              "NewPurchaseOrder",
                                                                                                              "4321");
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_INITIATOR_URL,
                                                                                      "testt",
                                                                                      CAS4.DEFAULT_RESPONDER_URL,
                                                                                      "testt");
    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       null,
                                                       eSoapVersion)
                                              .setMustUnderstand (true);
    return aDoc.getAsSoapDocument (aPayload);
  }

  @Nonnull
  @SuppressFBWarnings ("NP_NONNULL_PARAM_VIOLATION")
  public static Document createEmptyUserMessage (@Nonnull final ESoapVersion eSoapVersion,
                                                 @Nullable final Node aPayload,
                                                 @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3Properties.add (aEbms3PropertyProcess);

    // Use an empty message info by purpose
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null,
                                                                                            aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (null,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null,
                                                                                                              "svc",
                                                                                                              "act",
                                                                                                              "conv");
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo ("fid", "frole", "tid", "trole");
    final Ebms3MessageProperties aEbms3MessageProperties = MessageHelperMethods.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       null,
                                                       eSoapVersion)
                                              .setMustUnderstand (true);
    return aDoc.getAsSoapDocument (aPayload);
  }
}
