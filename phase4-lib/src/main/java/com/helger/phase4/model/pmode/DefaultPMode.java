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
package com.helger.phase4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.CAS4;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;

/**
 * Default PMode configuration Specification from
 * <code>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/os/ebms_core-3.0-spec-os.pdf</code><br>
 * This PMode is NOT specific to any profile and only present as a fallback.
 *
 * @author bayerlma
 * @author Philip Helger
 */
@Immutable
public final class DefaultPMode
{
  private DefaultPMode ()
  {}

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nullable final String sAddress)
  {
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return PModeLegBusinessInformation.create (CAS4.DEFAULT_SERVICE_URL,
                                               CAS4.DEFAULT_ACTION_URL,
                                               null,
                                               CAS4.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg (@Nullable final String sAddress)
  {
    final PModeLegErrorHandling aErrorHandling = PModeLegErrorHandling.createUndefined ();
    aErrorHandling.setReportAsResponse (true);

    final PModeLegReliability aReliability = null;

    final PModeLegSecurity aSecurity = new PModeLegSecurity ();
    aSecurity.setSendReceipt (true);
    aSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);

    return new PModeLeg (_generatePModeLegProtocol (sAddress),
                         _generatePModeLegBusinessInformation (),
                         aErrorHandling,
                         aReliability,
                         aSecurity);
  }

  @Nonnull
  public static IPMode getOrCreateDefaultPMode (@Nonnull @Nonempty final String sInitiatorID,
                                                @Nonnull @Nonempty final String sResponderID,
                                                @Nullable final String sAddress,
                                                final boolean bPersist)
  {
    // The "default" type is important to differentiate it from other PModes for
    // the same mock entities
    final PModeParty aInitiator = new PModeParty ("default", sInitiatorID, CAS4.DEFAULT_INITIATOR_URL, null, null);
    final PModeParty aResponder = new PModeParty ("default", sResponderID, CAS4.DEFAULT_RESPONDER_URL, null, null);

    // ID: Add "default-" prefix to easily identify "default" PModes
    final String sPModeID = "default-" + IPModeIDProvider.DEFAULT_DYNAMIC.getPModeID (aInitiator, aResponder);
    // Leg 2 stays null, because we only use one-way
    final PMode aDefaultPMode = new PMode (sPModeID,
                                           aInitiator,
                                           aResponder,
                                           "urn:as4:agreement",
                                           EMEP.ONE_WAY,
                                           EMEPBinding.PUSH,
                                           _generatePModeLeg (sAddress),
                                           (PModeLeg) null,
                                           (PModePayloadService) null,
                                           (PModeReceptionAwareness) null);
    if (bPersist)
    {
      // Create or update
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aDefaultPMode);
    }
    return aDefaultPMode;
  }
}
