package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.soap.ESOAPVersion;

/**
 * Default MPC Specification from
 * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/os/ebms_core-3.0-spec-os.
 * pdf Only use if necessary and nothing is used/defined.
 *
 * @author bayerlma
 */
public class DefaultPMode
{
  private DefaultPMode ()
  {}

  @Nonnull
  public static PMode getDefaultPmode ()
  {
    final PMode aDefaultPmode = new PMode ();
    aDefaultPmode.setID ("default-pmode");
    aDefaultPmode.setMEP (EMEP.ONE_WAY);
    aDefaultPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aDefaultPmode.setInitiator (_generateInitiatorOrResponder (true));
    aDefaultPmode.setResponder (_generateInitiatorOrResponder (false));
    aDefaultPmode.setLeg1 (_generatePModeLeg ());
    // Leg 2 stays null, because we only use one-way
    return aDefaultPmode;
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
