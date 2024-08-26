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
package com.helger.phase4.messaging.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3Receipt;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.ebms3header.MessagePartNRInformation;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.phase4.marshaller.DSigReferenceMarshaller;
import com.helger.phase4.marshaller.NonRepudiationInformationMarshaller;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.xml.ChildElementIterator;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * AS4 receipt message
 *
 * @author Philip Helger
 */
public class AS4ReceiptMessage extends AbstractAS4Message <AS4ReceiptMessage>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ReceiptMessage.class);

  private final Ebms3SignalMessage m_aSignalMessage;

  public AS4ReceiptMessage (@Nonnull final ESoapVersion eSoapVersion, @Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    super (eSoapVersion, EAS4MessageType.RECEIPT);

    ValueEnforcer.notNull (aSignalMessage, "SignalMessage");
    m_aMessaging.addSignalMessage (aSignalMessage);

    m_aSignalMessage = aSignalMessage;
  }

  /**
   * @return The {@link Ebms3SignalMessage} passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final Ebms3SignalMessage getEbms3SignalMessage ()
  {
    return m_aSignalMessage;
  }

  /**
   * Extract all "ds:Reference" nodes from the passed SOAP document. This method
   * search ins
   * "{soapDocument}/Envelop/Header/Security/Signature/SignedInfo".<br>
   * Note: use <code> XMLDSigReaderBuilder.dsigReference ().read (aRef)</code>
   * to read the content
   *
   * @param aSoapDocument
   *        The SOAP document to search in. May be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of Reference nodes.
   * @see #getAllDSigReferences(Node)
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <Node> getAllDSigReferenceNodes (@Nullable final Node aSoapDocument)
  {
    final ICommonsList <Node> aDSRefs = new CommonsArrayList <> ();
    Node aNext = XMLHelper.getFirstChildElementOfName (aSoapDocument, "Envelope");
    if (aNext != null)
    {
      aNext = XMLHelper.getFirstChildElementOfName (aNext, "Header");
      if (aNext != null)
      {
        aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.WSSE_NS, "Security");
        if (aNext != null)
        {
          aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.DS_NS, "Signature");
          if (aNext != null)
          {
            aNext = XMLHelper.getFirstChildElementOfName (aNext, CAS4.DS_NS, "SignedInfo");
            if (aNext != null)
            {
              new ChildElementIterator (aNext).findAll (XMLHelper.filterElementWithNamespaceAndLocalName (CAS4.DS_NS,
                                                                                                          "Reference"),
                                                        aDSRefs::add);
            }
          }
        }
      }
    }
    return aDSRefs;
  }

  /**
   * This is the typed version of {@link #getAllDSigReferenceNodes(Node)}
   *
   * @param aSoapDocument
   *        The SOAP document to search in. May be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of
   *         {@link ReferenceType} object.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <ReferenceType> getAllDSigReferences (@Nullable final Node aSoapDocument)
  {
    final ICommonsList <ReferenceType> ret = new CommonsArrayList <> ();
    for (final Node aRefNode : getAllDSigReferenceNodes (aSoapDocument))
    {
      // Read XMLDsig Reference
      final ReferenceType aRefObj = new DSigReferenceMarshaller ().read (aRefNode);
      if (aRefObj == null)
      {
        LOGGER.error ("Failed to read the content of the 'Reference' node as an XMLDsig Reference object: " +
                      aRefNode +
                      " / " +
                      XMLWriter.getNodeAsString (aRefNode));
        LOGGER.error ("This will most likely end in invalid non-repudiation of receipt information!");
      }
      else
      {
        // Safe to remember
        ret.add (aRefObj);
      }
    }
    return ret;
  }

  /**
   * This method creates a receipt message.
   *
   * @param eSoapVersion
   *        SOAP Version which should be used
   * @param sMessageID
   *        Message ID to use. May neither be <code>null</code> nor empty.
   * @param aEbms3UserMessageToRespond
   *        The received usermessage which should be responded too. May be
   *        <code>null</code>.
   * @param aSoapDocument
   *        If the SOAPDocument has WSS4j elements and the following parameter
   *        is true NonRepudiation will be used if the message is signed
   * @param bShouldUseNonRepudiation
   *        If NonRepudiation should be used or not
   * @return AS4ReceiptMessage
   */
  @Nonnull
  public static AS4ReceiptMessage create (@Nonnull final ESoapVersion eSoapVersion,
                                          @Nonnull @Nonempty final String sMessageID,
                                          @Nullable final Ebms3UserMessage aEbms3UserMessageToRespond,
                                          @Nullable final Node aSoapDocument,
                                          final boolean bShouldUseNonRepudiation)
  {
    // Only for signed messages
    final ICommonsList <ReferenceType> aDSRefs = getAllDSigReferences (aSoapDocument);

    final Ebms3SignalMessage aSignalMessage = new Ebms3SignalMessage ();

    // Message Info
    {
      // Always use "now" as date time
      final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sMessageID,
                                                                                              aEbms3UserMessageToRespond !=
                                                                                                          null ? aEbms3UserMessageToRespond.getMessageInfo ()
                                                                                                                                           .getMessageId ()
                                                                                                               : null);
      aSignalMessage.setMessageInfo (aEbms3MessageInfo);
    }

    final Ebms3Receipt aEbms3Receipt = new Ebms3Receipt ();
    if (aDSRefs.isNotEmpty () && bShouldUseNonRepudiation)
    {
      final NonRepudiationInformation aNonRepudiationInformation = new NonRepudiationInformation ();
      for (final ReferenceType aRef : aDSRefs)
      {
        // Add to NR response
        final MessagePartNRInformation aMessagePartNRInformation = new MessagePartNRInformation ();
        aMessagePartNRInformation.setReference (aRef);
        aNonRepudiationInformation.addMessagePartNRInformation (aMessagePartNRInformation);
      }

      final Element aElement = new NonRepudiationInformationMarshaller ().getAsElement (aNonRepudiationInformation);
      if (aElement == null)
        LOGGER.error ("Failed to serialize NonRepudiationInformation object");
      else
        aEbms3Receipt.addAny (aElement);
    }
    else
    {
      if (aDSRefs.isEmpty ())
        LOGGER.info ("Found no ds:Reference elements in the source message, hence returning the source UserMessage in the Receipt");
      else
        LOGGER.info ("Non-repudiation is disabled, hence returning the source UserMessage in the Receipt");

      // If the original usermessage is not signed, the receipt will contain the
      // original message part without wss4j security
      aEbms3Receipt.addAny (AS4UserMessage.create (eSoapVersion, aEbms3UserMessageToRespond)
                                          .getAsSoapDocument ()
                                          .getDocumentElement ());
    }
    aSignalMessage.setReceipt (aEbms3Receipt);

    return new AS4ReceiptMessage (eSoapVersion, aSignalMessage);
  }
}
