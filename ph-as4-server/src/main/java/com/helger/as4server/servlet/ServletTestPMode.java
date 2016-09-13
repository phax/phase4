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
  public static PMode getTestPMode ()
  {
    final PMode aTestPmode = new PMode ("pm-esens-generic-resp");
    aTestPmode.setMEP (EMEP.ONE_WAY);
    aTestPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aTestPmode.setInitiator (_generateInitiatorOrResponder (true));
    aTestPmode.setResponder (_generateInitiatorOrResponder (false));
    aTestPmode.setLeg1 (_generatePModeLeg ());
    // Leg 2 stays null, because we only use one-way
    return aTestPmode;
  }

  @Nonnull
  public static PMode getTestPModeWithSecurity ()
  {
    final PMode aTestPmode = getTestPMode ();
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_11.getVersion ());
    aTestPmode.setLeg1 (new PModeLeg (_generatePModeLegProtocol (),
                                      _generatePModeLegBusinessInformation (),
                                      null,
                                      null,
                                      aPModeLegSecurity));
    // Leg 2 stays null, because we only use one-way
    return aTestPmode;
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
                                            "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");
  }

  @Nonnull
  private static PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("http://localhost:8080", ESOAPVersion.AS4_DEFAULT);
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
