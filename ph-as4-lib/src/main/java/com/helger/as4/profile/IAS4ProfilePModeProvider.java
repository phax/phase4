package com.helger.as4.profile;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.pmode.PMode;
import com.helger.commons.annotation.Nonempty;

@FunctionalInterface
public interface IAS4ProfilePModeProvider extends Serializable
{
  @Nullable
  PMode getOrCreatePMode (@Nonnull @Nonempty String sInitiatorID,
                          @Nonnull @Nonempty String sResponderID,
                          @Nullable String sResponderAddress);
}
