package com.helger.as4lib.model.pmode;

import javax.annotation.Nullable;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.photon.basic.object.IObject;

public interface IPMode extends IObject
{
  @Nullable
  String getAgreement ();

  @Nullable
  EMEP getMEP ();

  @Nullable
  default String getMEPID ()
  {
    final EMEP eMEP = getMEP ();
    return eMEP == null ? null : eMEP.getID ();
  }

  @Nullable
  ETransportChannelBinding getMEPBinding ();

  @Nullable
  default String getMEPBindingID ()
  {
    final ETransportChannelBinding eMEPBinding = getMEPBinding ();
    return eMEPBinding == null ? null : eMEPBinding.getID ();
  }

  @Nullable
  PModeParty getInitiator ();

  @Nullable
  PModeParty getResponder ();

  @Nullable
  PModeLeg getLeg1 ();

  @Nullable
  PModeLeg getLeg2 ();
}
