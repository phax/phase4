package com.helger.as4lib.model.pmode.config;

import javax.annotation.Nullable;

/**
 * Resolve PMode from ID
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IPModeConfigResolver
{
  /**
   * Get the PMode config of the passed ID.
   * 
   * @param sID
   *        The ID to be resolved. May be <code>null</code>.
   * @return <code>null</code> if resolution failed.
   */
  @Nullable
  IPModeConfig getPModeConfigOfID (@Nullable String sID);
}
