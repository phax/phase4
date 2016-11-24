package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;

@IsSPIInterface
public interface IAS4ProfileRegistrarSPI
{
  /**
   * Register AS4 profiles at the provided registrar.
   * 
   * @param aRegistrar
   *        The registrar to register at. Never <code>null</code>.
   */
  void registerAS4Profile (@Nonnull IAS4ProfileRegistrar aRegistrar);
}
