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
import javax.annotation.concurrent.Immutable;

import com.helger.as4.CAS4;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.mpc.MPCManager;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.soap.ESOAPVersion;
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
  public static PModeConfig createDefaultPModeConfig ()
  {
    final PModeConfig aDefaultConfig = new PModeConfig (DEFAULT_PMODE_ID);
    aDefaultConfig.setMEP (EMEP.ONE_WAY);
    aDefaultConfig.setMEPBinding (EMEPBinding.PUSH);
    aDefaultConfig.setLeg1 (_generatePModeLeg ());
    // Leg 2 stays null, because we only use one-way
    return aDefaultConfig;
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg ()
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
    return new PModeLeg (_generatePModeLegProtocol (),
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
                                            null,
                                            null,
                                            MPCManager.DEFAULT_MPC_ID);
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("HTTP 1.1", ESOAPVersion.AS4_DEFAULT);
  }
}
