package com.helger.as4.esens;

import javax.annotation.Nonnull;

import com.helger.as4lib.model.profile.AS4Profile;
import com.helger.as4lib.model.profile.IAS4ProfileRegistrar;
import com.helger.as4lib.model.profile.IAS4ProfileRegistrarSPI;
import com.helger.commons.annotation.IsSPIImplementation;

@IsSPIImplementation
public class AS4ESENSProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    aRegistrar.registerProfile (new AS4Profile ("esens", "e-SENS", ESENSCompatibilityValidator.class));
  }
}
