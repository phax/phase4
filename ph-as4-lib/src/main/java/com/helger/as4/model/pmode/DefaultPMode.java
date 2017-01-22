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

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.ETransportChannelBinding;
import com.helger.as4.model.mpc.MPCManager;
import com.helger.as4.model.pmode.config.IPModeConfig;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.soap.ESOAPVersion;

/**
 * Default MPC Specification from
 * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/os/ebms_core-3.0-spec-os.
 * pdf Only use if necessary and nothing is used/defined.
 *
 * @author bayerlma
 */
@Immutable
public final class DefaultPMode
{
  private DefaultPMode ()
  {}

  @Nonnull
  public static IPModeConfig getDefaultPModeConfig ()
  {
    final PModeConfig aDefaultConfig = new PModeConfig ("default-pmode");
    aDefaultConfig.setMEP (EMEP.ONE_WAY);
    aDefaultConfig.setMEPBinding (ETransportChannelBinding.PUSH);
    aDefaultConfig.setLeg1 (_generatePModeLeg ());
    // Leg 2 stays null, because we only use one-way
    MetaAS4Manager.getPModeConfigMgr ().createPModeConfigIfNotExisting (aDefaultConfig);
    return aDefaultConfig;
  }

  @Nonnull
  public static IPMode getDefaultPMode ()
  {
    return new PMode (_generateInitiatorOrResponder (true),
                      _generateInitiatorOrResponder (false),
                      getDefaultPModeConfig ());
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg ()
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_generatePModeLegProtocol (),
                         _generatePModeLegBusinessInformation (),
                         null,
                         aPModeLegReliability,
                         aPModeLegSecurity);
  }

  @Nonnull
  private static PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test",
                                            null,
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

  @Nonnull
  private static PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty (null,
                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultFrom",
                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                             null,
                             null);
    return new PModeParty (null,
                           "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultTo",
                           "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                           null,
                           null);
  }
}
