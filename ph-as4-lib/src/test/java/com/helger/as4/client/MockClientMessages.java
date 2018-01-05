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
package com.helger.as4.client;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.error.EEbmsError;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.ErrorMessageCreator;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.domain.ReceiptMessageCreator;
import com.helger.as4.messaging.domain.UserMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Only used for SOAPClientSAAJ.java as test message constructor.
 *
 * @author bayerlma
 */
final class MockClientMessages
{
  private MockClientMessages ()
  {}

  public static Document testSignedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                @Nullable final Node aPayload,
                                                @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException
  {
    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          testUserMessageSoapNotSigned (eSOAPVersion,
                                                                                                        aPayload,
                                                                                                        aAttachments),
                                                                          eSOAPVersion,
                                                                          aAttachments,
                                                                          aResMgr,
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                           @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException
  {
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList <> (EEbmsError.EBMS_INVALID_HEADER.getAsEbms3Error (Locale.US,
                                                                                                                               null));
    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          ErrorMessageCreator.createErrorMessage (eSOAPVersion,
                                                                                                                  MessageHelperMethods.createEbms3MessageInfo (),
                                                                                                                  aEbms3ErrorList)
                                                                                             .setMustUnderstand (true)
                                                                                             .getAsSOAPDocument (),
                                                                          eSOAPVersion,
                                                                          aAttachments,
                                                                          aResMgr,
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                             @Nullable final Node aPayload,
                                             @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                             @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException,
                                                                                        DOMException
  {
    final Document aUserMessage = testSignedUserMessage (eSOAPVersion, aPayload, aAttachments, aResMgr);

    final Document aDoc = ReceiptMessageCreator.createReceiptMessage (eSOAPVersion,
                                                                      MessageHelperMethods.createRandomMessageID (),
                                                                      null,
                                                                      aUserMessage,
                                                                      true)
                                               .setMustUnderstand (true)
                                               .getAsSOAPDocument ();

    final Document aSignedDoc = SignedMessageCreator.createSignedMessage (AS4CryptoFactory.DEFAULT_INSTANCE,
                                                                          aDoc,
                                                                          eSOAPVersion,
                                                                          aAttachments,
                                                                          aResMgr,
                                                                          false,
                                                                          ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                          ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testUserMessageSoapNotSigned (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       @Nullable final Node aPayload,
                                                       @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ProcessInst", "PurchaseOrder:123456"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ContextID", "987654321"));

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                            "MyServiceTypes",
                                                                                                            "QuoteToCollect",
                                                                                                            "4321",
                                                                                                            "pmode-twoway",
                                                                                                            MockEbmsHelper.DEFAULT_AGREEMENT);
    final Ebms3PartyInfo aEbms3PartyInfo = UserMessageCreator.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                                    "1234",
                                                                                    CAS4.DEFAULT_RESPONDER_URL,
                                                                                    "5678");
    final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = UserMessageCreator.createUserMessage (aEbms3MessageInfo,
                                                                      aEbms3PayloadInfo,
                                                                      aEbms3CollaborationInfo,
                                                                      aEbms3PartyInfo,
                                                                      aEbms3MessageProperties,
                                                                      eSOAPVersion)
                                                  .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  public static Document testUserMessageSoapNotSignedNotPModeConform (@Nonnull final ESOAPVersion eSOAPVersion,
                                                                      @Nullable final Node aPayload,
                                                                      @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ProcessInst", "PurchaseOrder:123456"));
    aEbms3Properties.add (MessageHelperMethods.createEbms3Property ("ContextID", "987654321"));

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                            "MyServiceTypes",
                                                                                                            "QuoteToCollect",
                                                                                                            "4321",
                                                                                                            "pm-esens-generic-resp",
                                                                                                            MockEbmsHelper.DEFAULT_AGREEMENT);
    final Ebms3PartyInfo aEbms3PartyInfo = UserMessageCreator.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                                    "testt",
                                                                                    CAS4.DEFAULT_RESPONDER_URL,
                                                                                    "testt");
    final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = UserMessageCreator.createUserMessage (aEbms3MessageInfo,
                                                                      aEbms3PayloadInfo,
                                                                      aEbms3CollaborationInfo,
                                                                      aEbms3PartyInfo,
                                                                      aEbms3MessageProperties,
                                                                      eSOAPVersion)
                                                  .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  @Nullable
  @SuppressFBWarnings ("NP_NONNULL_PARAM_VIOLATION")
  public static Document emptyUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nullable final Node aPayload,
                                           @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3Properties.add (aEbms3PropertyProcess);

    // Use an empty message info by purpose
    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = UserMessageCreator.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = UserMessageCreator.createEbms3CollaborationInfo (null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null);
    final Ebms3PartyInfo aEbms3PartyInfo = UserMessageCreator.createEbms3PartyInfo (null, null, null, null);
    final Ebms3MessageProperties aEbms3MessageProperties = UserMessageCreator.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = UserMessageCreator.createUserMessage (aEbms3MessageInfo,
                                                                      aEbms3PayloadInfo,
                                                                      aEbms3CollaborationInfo,
                                                                      aEbms3PartyInfo,
                                                                      aEbms3MessageProperties,
                                                                      eSOAPVersion)
                                                  .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }
}
