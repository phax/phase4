package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;

/**
 * Base interface for AS4 profile registrar
 * 
 * @author Philip Helger
 */
public interface IAS4ProfileRegistrar
{
  void registerProfile (@Nonnull IAS4Profile aAS4Profile);
}
