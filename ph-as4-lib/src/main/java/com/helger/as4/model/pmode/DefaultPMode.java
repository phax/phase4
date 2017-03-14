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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.as4.CAS4;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.commons.state.ETriState;

/**
 * Default PMode configuration Specification from
 * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/os/ebms_core-3.0-spec-os.
 * pdf <br>
 * Automatically generated in PModeConfigManager upon initialization
 *
 * @author bayerlma
 * @author Philip Helger
 */
@Immutable
public final class DefaultPMode
{
  public static final String DEFAULT_PMODE_ID = "default-pmode";

  private DefaultPMode ()
  {}

  @Nonnull
  public static PMode createDefaultPMode (@Nullable final String sAddress)
  {
    final PMode aDefaultPMode = new PMode (DEFAULT_PMODE_ID,
                                           PModeParty.createSimple (MockEbmsHelper.DEFAULT_PARTY_ID,
                                                                    CAS4.DEFAULT_SENDER_URL),
                                           PModeParty.createSimple (MockEbmsHelper.DEFAULT_PARTY_ID,
                                                                    CAS4.DEFAULT_SENDER_URL),
                                           MockEbmsHelper.DEFAULT_AGREEMENT,
                                           EMEP.ONE_WAY,
                                           EMEPBinding.PUSH,
                                           _generatePModeLeg (sAddress),
                                           null,
                                           null,
                                           null);
    // Leg 2 stays null, because we only use one-way
    return aDefaultPMode;
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg (@Nullable final String sAddress)
  {
    final PModeLegReliability aReliability = null;
    final PModeLegSecurity aSecurity = new PModeLegSecurity ();
    {
      aSecurity.setSendReceipt (true);
      aSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    }
    final PModeLegErrorHandling aErrorHandler = new PModeLegErrorHandling (null,
                                                                           null,
                                                                           ETriState.TRUE,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED,
                                                                           ETriState.UNDEFINED);
    return new PModeLeg (_generatePModeLegProtocol (sAddress),
                         _generatePModeLegBusinessInformation (),
                         aErrorHandler,
                         aReliability,
                         aSecurity);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation (CAS4.DEFAULT_SERVICE_URL,
                                            CAS4.DEFAULT_ACTION_URL,
                                            null,
                                            CAS4.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nullable final String sAddress)
  {
    return PModeLegProtocol.createForDefaultSOAPVersion (sAddress);
  }
}
