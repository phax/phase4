package com.helger.as4lib.model.pmode;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

/**
 * Default MPC Specification from
 * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/os/ebms_core-3.0-spec-os.
 * pdf Only use if necessary and nothing is used/defined.
 *
 * @author bayerlma
 */
public class DefaultPMode
{
  private PMode aDefaultPmode;

  public PMode getDefaultPmode ()
  {
    _configureDefaultPMode ();
    return aDefaultPmode;
  }

  private void _configureDefaultPMode ()
  {
    aDefaultPmode = new PMode ();
    aDefaultPmode.setMEP (EMEP.ONE_WAY);
    aDefaultPmode.setMEPBinding (ETransportChannelBinding.PUSH);
    aDefaultPmode.setLegs (_generatePModeLeg ());
    aDefaultPmode.setInitiator (_generateInitiatorOrResponder (true));
    aDefaultPmode.setResponder (_generateInitiatorOrResponder (false));
  }

  private ICommonsList <PModeLeg> _generatePModeLeg ()
  {
    final PModeLegReliability aPModeLegReliability = null;
    final PModeLegSecurity aPModeLegSecurity = null;
    return new CommonsArrayList<> (new PModeLeg (_generatePModeLegProtocol (),
                                                 _generatePModeLegBusinessInformation (),
                                                 null,
                                                 aPModeLegReliability,
                                                 aPModeLegSecurity));
  }

  private PModeLegBusinessInformation _generatePModeLegBusinessInformation ()
  {
    return new PModeLegBusinessInformation ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test",
                                            null,
                                            null,
                                            null,
                                            null,
                                            "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");
  }

  private PModeLegProtocol _generatePModeLegProtocol ()
  {
    return new PModeLegProtocol ("HTTP 1.1", "soap12");
  }

  private PModeParty _generateInitiatorOrResponder (final boolean bChoose)
  {
    if (bChoose)
      return new PModeParty ("",
                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultFrom",
                             "",
                             "",
                             "");
    return new PModeParty ("", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultTo", "", "", "");
  }
}
