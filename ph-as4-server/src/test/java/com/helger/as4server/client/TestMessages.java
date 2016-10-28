/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.client;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateErrorMessage;
import com.helger.as4lib.message.CreateReceiptMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

final class TestMessages
{
  public static Document testSignedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                @Nullable final Node aPayload,
                                                @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException
  {
    final SignedMessageCreator aClient = new SignedMessageCreator ();

    final Document aSignedDoc = aClient.createSignedMessage (testUserMessageSoapNotSigned (eSOAPVersion,
                                                                                           aPayload,
                                                                                           aAttachments),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testErrorMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException
  {
    final CreateErrorMessage aErrorMessage = new CreateErrorMessage ();
    final SignedMessageCreator aClient = new SignedMessageCreator ();
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList<> (EEbmsError.EBMS_INVALID_HEADER.getAsEbms3Error (Locale.US));
    final Document aSignedDoc = aClient.createSignedMessage (aErrorMessage.createErrorMessage (eSOAPVersion,
                                                                                               aErrorMessage.createEbms3MessageInfo ("AS4-Server"),
                                                                                               aEbms3ErrorList)
                                                                          .setMustUnderstand (false)
                                                                          .getAsSOAPDocument (),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                             @Nullable final Node aPayload,
                                             @Nullable final Iterable <? extends IAS4Attachment> aAttachments) throws WSSecurityException,
                                                                                                               DOMException
  {
    final Document aUserMessage = testSignedUserMessage (eSOAPVersion, aPayload, aAttachments);

    final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
    final SignedMessageCreator aClient = new SignedMessageCreator ();
    final Document aDoc = aReceiptMessage.createReceiptMessage (eSOAPVersion,
                                                                aReceiptMessage.createEbms3MessageInfo ("AS4-Server",
                                                                                                        null),
                                                                null,
                                                                aUserMessage)
                                         .setMustUnderstand (false)
                                         .getAsSOAPDocument ();

    final Document aSignedDoc = aClient.createSignedMessage (aDoc,
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  public static Document testUserMessageSoapNotSigned (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       @Nullable final Node aPayload,
                                                       @Nullable final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("AS4-Server");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      "pm-esens-generic-resp",
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              "APP_1000000101",
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              "APP_1000000101");
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSOAPVersion)
                                            .setMustUnderstand (false);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  public static Document testUserMessageSoapNotSignedNotPModeConform (@Nonnull final ESOAPVersion eSOAPVersion,
                                                                      @Nullable final Node aPayload,
                                                                      @Nullable final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("AS4-Server");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      "pm-esens-generic-resp",
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              "testt",
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              "testt");
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSOAPVersion)
                                            .setMustUnderstand (false);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  @Nullable
  @SuppressFBWarnings ("NP_NONNULL_PARAM_VIOLATION")
  public static Document emptyUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                           @Nullable final Node aPayload,
                                           @Nullable final Iterable <? extends IAS4Attachment> aAttachments)
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3Properties.add (aEbms3PropertyProcess);

    // Use an empty message info by purpose
    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo (null);
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo (null,
                                                                                                      null,
                                                                                                      null,
                                                                                                      null,
                                                                                                      null,
                                                                                                      null);
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo (null, null, null, null);
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSOAPVersion)
                                            .setMustUnderstand (false);
    return aDoc.getAsSOAPDocument (aPayload);
  }
}
