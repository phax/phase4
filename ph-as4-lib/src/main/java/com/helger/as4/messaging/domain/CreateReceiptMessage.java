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
package com.helger.as4.messaging.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Node;

import com.helger.as4.CAS4;
import com.helger.as4.marshaller.Ebms3WriterBuilder;
import com.helger.as4.marshaller.XMLDSigReaderBuilder;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3Receipt;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.as4lib.ebms3header.MessagePartNRInformation;
import com.helger.as4lib.ebms3header.NonRepudiationInformation;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xsds.xmldsig.ReferenceType;

public final class CreateReceiptMessage
{
  private CreateReceiptMessage ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  private static ICommonsList <Node> _getAllReferences (@Nullable final Node aUserMessage)
  {
    final ICommonsList <Node> aDSRefs = new CommonsArrayList <> ();
    Node aNext = XMLHelper.getFirstChildElementOfName (aUserMessage, "Envelope");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, "Header");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.WSSE_NS, "Security");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.DS_NS, "Signature");
    aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.DS_NS, "SignedInfo");
    if (aNext != null)
    {
      new ChildElementIterator (aNext).findAll (XMLHelper.filterElementWithNamespaceAndLocalName (CAS4.DS_NS,
                                                                                                  "Reference"),
                                                aDSRefs::add);
    }
    return aDSRefs;
  }

  /**
   * This method creates a receipt message.
   *
   * @param eSOAPVersion
   *        SOAP Version which should be used
   * @param aEbms3MessageInfo
   *        MessageInfo comes from the received usermessage
   * @param aEbms3UserMessage
   *        The received usermessage which should be responded too
   * @param aSOAPDocument
   *        If the SOAPDocument has WSS4j elements and the following parameter
   *        is true NonRepudiation will be used if the message is signed
   * @param bShouldUseNonRepudiation
   *        If NonRepudiation should be used or not
   * @return AS4ReceiptMessage
   */
  @Nonnull
  public static AS4ReceiptMessage createReceiptMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                        @Nonnull final Ebms3MessageInfo aEbms3MessageInfo,
                                                        @Nullable final Ebms3UserMessage aEbms3UserMessage,
                                                        @Nullable final Node aSOAPDocument,
                                                        @Nonnull final boolean bShouldUseNonRepudiation)
  {
    if (aEbms3UserMessage != null)
      aEbms3MessageInfo.setRefToMessageId (aEbms3UserMessage.getMessageInfo ().getMessageId ());

    // Only for signed messages
    final ICommonsList <Node> aDSRefs = _getAllReferences (aSOAPDocument);

    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    aSignalMessage.setMessageInfo (aEbms3MessageInfo);

    final Ebms3Receipt aEbms3Receipt = new Ebms3Receipt ();
    // PullRequest
    if (aDSRefs.isNotEmpty () && bShouldUseNonRepudiation)
    {

      final NonRepudiationInformation aNonRepudiationInformation = new NonRepudiationInformation ();
      for (final Node aRef : aDSRefs)
      {
        final ReferenceType aRefObj = XMLDSigReaderBuilder.dsigReference ().read (aRef);

        final MessagePartNRInformation aMessagePartNRInformation = new MessagePartNRInformation ();
        aMessagePartNRInformation.setReference (aRefObj);

        aNonRepudiationInformation.addMessagePartNRInformation (aMessagePartNRInformation);
      }

      aEbms3Receipt.addAny (Ebms3WriterBuilder.nonRepudiationInformation ()
                                              .getAsDocument (aNonRepudiationInformation)
                                              .getDocumentElement ());
    }
    else
    {
      // If the original usermessage is not signed, the receipt will contain the
      // original message part with out wss4j security
      aEbms3Receipt.addAny (CreateUserMessage.getUserMessageAsAS4UserMessage (eSOAPVersion, aEbms3UserMessage)
                                             .getAsSOAPDocument ()
                                             .getDocumentElement ());
    }
    aSignalMessage.setReceipt (aEbms3Receipt);

    return new AS4ReceiptMessage (eSOAPVersion, aSignalMessage);
  }

  @Nonnull
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId)
  {
    return MessageHelperMethods.createEbms3MessageInfo (sMessageId);
  }
}
