package com.helger.as4.model.pmode;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

@FunctionalInterface
public interface IPModeIDProvider extends Serializable
{
  static IPModeIDProvider DEFAULT_DYNAMIC = (i, r) -> i + "-" + r;

  @Nonnull
  String getPModeID (@Nonnull @Nonempty String sInitiatorID, @Nonnull @Nonempty String sResponderID);
}
