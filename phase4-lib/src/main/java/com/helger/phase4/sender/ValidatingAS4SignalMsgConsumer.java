/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.sender;

import java.util.ArrayList;
import java.util.List;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.collection.CollectionFind;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.MessagePartNRInformation;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.util.Phase4Exception;
import com.helger.xsds.xmldsig.ReferenceType;
import com.helger.xsds.xmldsig.TransformType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Specific wrapped {@link IAS4SignalMessageConsumer} that verifies the DSig References between the
 * sent message and received Receipt is identical.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class ValidatingAS4SignalMsgConsumer implements IAS4SignalMessageConsumer
{
  // The default instance doing some logging only
  private static final IAS4SignalMessageValidationResultHandler DEFAULT_RES_HDL = new LoggingAS4SignalMsgValidationResultHandler ();

  private final AS4ClientSentMessage <?> m_aClientSetMsg;
  private final IAS4SignalMessageConsumer m_aOriginalConsumer;
  private final IAS4SignalMessageValidationResultHandler m_aResultHandler;

  /**
   * Constructor
   *
   * @param aClientSetMsg
   *        The original message sent, that contains the DSig references in the built message.
   *        Non-<code>null</code>.
   * @param aOriginalConsumer
   *        The original signal message consumer to be invoked after the reference check. May be
   *        null.
   * @param aResultHandler
   *        The result handler to be invoked. May be <code>null</code> in which case some default
   *        messages will be logged.
   */
  public ValidatingAS4SignalMsgConsumer (@Nonnull final AS4ClientSentMessage <?> aClientSetMsg,
                                         @Nullable final IAS4SignalMessageConsumer aOriginalConsumer,
                                         @Nullable final IAS4SignalMessageValidationResultHandler aResultHandler)
  {
    ValueEnforcer.notNull (aClientSetMsg, "ClientSetMsg");
    m_aClientSetMsg = aClientSetMsg;
    m_aOriginalConsumer = aOriginalConsumer;
    m_aResultHandler = aResultHandler != null ? aResultHandler : DEFAULT_RES_HDL;
  }

  /**
   * This method compares XMLDSig references based on the following parameters:
   * <ul>
   * <li>Reference URI</li>
   * <li>Transform Algorithms (count and algorithms)</li>
   * <li>DigestMethod Algorithm</li>
   * <li>DigestValue</li>
   * </ul>
   * <br>
   * This is based on a real world problem, where the outbound message used
   *
   * <pre>
  &lt;ds:Reference URI="#my-msg-6311136f-ff8e-4d84-9ca4-6ba68939680e">
   &lt;ds:Transforms>
    &lt;ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     &lt;ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="S12"/>
    &lt;/ds:Transform>
   &lt;/ds:Transforms>
   &lt;ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
   &lt;ds:DigestValue>LceN50/wXDPAvAvitk+EtQHANOxac2zVrWTCHm3P2UA=&lt;/ds:DigestValue>
  &lt;/ds:Reference>
   * </pre>
   *
   * but the returned reference looked like this:
   *
   * <pre>
  &lt;ns4:Reference URI="#my-msg-6311136f-ff8e-4d84-9ca4-6ba68939680e">
    &lt;ns4:Transforms>
        &lt;ns4:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
    &lt;/ns4:Transforms>
    &lt;ns4:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
    &lt;ns4:DigestValue>LceN50/wXDPAvAvitk+EtQHANOxac2zVrWTCHm3P2UA=&lt;/ns4:DigestValue>
  &lt;/ns4:Reference>
   * </pre>
   *
   * @param aRef1
   *        First reference. May not be <code>null</code>.
   * @param aRef2
   *        Second reference. May not be <code>null</code>.
   * @return <code>true</code> if they are equivalent, <code>false</code> if not.
   * @since 3.0.7
   */
  public static boolean areSemanticallyEquivalent (@Nonnull final ReferenceType aRef1,
                                                   @Nonnull final ReferenceType aRef2)
  {
    // Reference URI
    if (!EqualsHelper.equals (aRef1.getURI (), aRef2.getURI ()))
      return false;

    // Transform algorithms
    final List <TransformType> aTransforms1 = aRef1.getTransforms () == null ? new ArrayList <> ()
                                                                             : aRef1.getTransforms ().getTransform ();
    final List <TransformType> aTransforms2 = aRef2.getTransforms () == null ? new ArrayList <> ()
                                                                             : aRef2.getTransforms ().getTransform ();
    if (aTransforms1.size () != aTransforms2.size ())
      return false;
    for (final TransformType aTransform1 : aTransforms1)
      if (!CollectionFind.containsAny (aTransforms2,
                                       x -> EqualsHelper.equals (x.getAlgorithm (), aTransform1.getAlgorithm ())))
        return false;

    // Digest algorithm
    if (!EqualsHelper.equals (aRef1.getDigestMethod ().getAlgorithm (), aRef2.getDigestMethod ().getAlgorithm ()))
      return false;

    // Digest algorithm
    if (!EqualsHelper.equals (aRef1.getDigestValue (), aRef2.getDigestValue ()))
      return false;

    return true;
  }

  public void handleSignalMessage (@Nonnull final Ebms3SignalMessage aEbmsSignalMsg,
                                   @Nonnull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                   @Nonnull final IAS4IncomingMessageState aIncomingState) throws Phase4Exception
  {
    boolean bComparedReferences = false;
    if (m_aClientSetMsg.getBuiltMessage ().hasDSReferences () &&
      aEbmsSignalMsg != null &&
      aEbmsSignalMsg.getReceipt () != null &&
      aEbmsSignalMsg.getReceipt ().hasAnyEntries ())
    {
      // Verify that stored references match the ones contained in the NRR
      // of the signal message
      NonRepudiationInformation aReceivedNRR = null;
      final List <Object> aAnyList = aEbmsSignalMsg.getReceipt ().getAny ();
      for (final var aAnyItem : aAnyList)
        if (aAnyItem instanceof NonRepudiationInformation)
        {
          aReceivedNRR = (NonRepudiationInformation) aAnyItem;
          break;
        }

      if (aReceivedNRR != null)
      {
        bComparedReferences = true;

        final ICommonsList <ReferenceType> aSentDSRefs = m_aClientSetMsg.getBuiltMessage ().getAllDSReferences ();
        if (aSentDSRefs.size () != aReceivedNRR.getMessagePartNRInformationCount ())
        {
          m_aResultHandler.onError ("The UserMessage sent out contains " +
                                    aSentDSRefs.size () +
                                    " DSig references, wheres the received Receipt contains " +
                                    aReceivedNRR.getMessagePartNRInformationCount () +
                                    " DSig references. This will lead to follow-up errors.");
        }

        int nRefsFound = 0;
        for (final MessagePartNRInformation aInfo : aReceivedNRR.getMessagePartNRInformation ())
        {
          final ReferenceType aReceivedRef = aInfo.getReference ();

          // Find by content
          final ReferenceType aMatchingSentRef = aSentDSRefs.findFirst (x -> areSemanticallyEquivalent (aReceivedRef,
                                                                                                        x));
          if (aMatchingSentRef == null)
            m_aResultHandler.onError ("The received DSig reference was not found in the source list: " + aReceivedRef);
          else
            nRefsFound++;
        }

        if (nRefsFound != aSentDSRefs.size ())
          m_aResultHandler.onError ("No all sent DSig references were found in the received AS4 Receipt message");
        else
          m_aResultHandler.onSuccess ();
      }
    }

    if (!bComparedReferences)
      m_aResultHandler.onNotApplicable ();

    if (m_aOriginalConsumer != null)
      m_aOriginalConsumer.handleSignalMessage (aEbmsSignalMsg, aIncomingMessageMetadata, aIncomingState);
  }
}
