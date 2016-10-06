/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import javax.annotation.Nonnull;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegBusinessInformation;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegReliability;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.wss.EWSSVersion;

public class ServletTestPMode
{
  private ServletTestPMode ()
  {}

  @Nonnull
  public static PMode getTestPMode (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PMode aTestPmode;
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_12))
      aTestPmode = new PMode ("pm-esens-generic-resp");
    else
      aTestPmode = new PMode ("pm-esens-generic-resp11");
    aTestPmode.setMEP (EMEP.ONE_WAY);
    aTestPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aTestPmode.setInitiator (_generateInitiatorOrResponder (true));
    aTestPmode.setResponder (_generateInitiatorOrResponder (false));
    aTestPmode.setLeg1 (_generatePModeLeg (eSOAPVersion));
    // Leg 2 stays null, because we only use one-way
    return aTestPmode;
  }

  @Nonnull
  public static PMode getTestPModeSetID (@Nonnull final ESOAPVersion eSOAPVersion, final String sPModeID)
  {
    final PMode aTestPmode = new PMode (sPModeID);
    aTestPmode.setMEP (EMEP.ONE_WAY);
    aTestPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aTestPmode.setInitiator (_generateInitiatorOrResponder (true));
    aTestPmode.setResponder (_generateInitiatorOrResponder (false));
    aTestPmode.setLeg1 (_generatePModeLeg (eSOAPVersion));
    // Leg 2 stays null, because we only use one-way
    return aTestPmode;
  }

  @Nonnull
  public static PMode getTestPModeWithSecurity (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PMode aTestPmode = getTestPMode (eSOAPVersion);
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_11.getVersion ());
    aTestPmode.setLeg1 (new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
                                      _generatePModeLegBusinessInformation (),
                                      null,
                                      null,
                                      aPModeLegSecurity));
    // Leg 2 stays null, because we only use one-way
    return aTestPmode;
  }

  @Nonnull
  private static PModeLeg _generatePModeLeg (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new PModeLeg (_generatePModeLegProtocol (eSOAPVersion),
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
                                            "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    return new PModeLegProtocol ("http://localhost:8080", eSOAPVersion);
  }

  @Nonnull
  private static PModeParty _generateInitiatorOrResponder (final boolean bInitiator)
  {
    if (bInitiator)
      return new PModeParty (null,
                             "APP_1000000101",
                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                             null,
                             null);
    return new PModeParty (null,
                           "APP_1000000101",
                           "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                           null,
                           null);
  }
}
