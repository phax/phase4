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
package com.helger.phase4.sender;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.NonRepudiationInformation;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4SignalMessageConsumer;
import com.helger.phase4.util.Phase4Exception;
import com.helger.xsds.xmldsig.ReferenceType;

/**
 * Specific wrapped {@link IAS4SignalMessageConsumer} that verifies the DSig
 * References between the sent message and received Receipt is identical.
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
   *        The original message sent, that contains the DSig references in the
   *        built message. Non-<code>null</code>.
   * @param aOriginalConsumer
   *        The original signal message consumer to be invoked after the
   *        reference check. May be null.
   * @param aResultHandler
   *        The result handler to be invoked. May be <code>null</code> in which
   *        case some default messages will be logged.
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
          m_aResultHandler.onError ("The UserMessage sent out contains " +
                                    aSentDSRefs.size () +
                                    " DSig references, wheres the received Receipt contains " +
                                    aReceivedNRR.getMessagePartNRInformationCount () +
                                    " DSig references. This will lead to follow-up errors.");
        for (final var aInfo : aReceivedNRR.getMessagePartNRInformation ())
        {
          final ReferenceType aReceivedRef = aInfo.getReference ();
          if (aSentDSRefs.removeObject (aReceivedRef).isUnchanged ())
          {
            m_aResultHandler.onError ("The received DSig reference was not found in the source list: " + aReceivedRef);
          }
        }
        if (aSentDSRefs.isNotEmpty ())
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
