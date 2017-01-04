package com.helger.as4lib.model.pmode.config;

import javax.annotation.Nullable;

import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.pmode.DefaultPMode;
import com.helger.commons.string.StringHelper;

/**
 * Default implementation of {@link IPModeConfigResolver} using the fixed ID
 * only. If no ID is provided the default pmode is used.
 * 
 * @author bayerlma
 */
public class DefaultPModeConfigResolver implements IPModeConfigResolver
{
  @Nullable
  public IPModeConfig getPModeConfigOfID (@Nullable final String sPModeConfigID)
  {
    if (StringHelper.hasNoText (sPModeConfigID))
    {
      // If the pmodeconfig id field is empty or null, set default pmode
      // config
      return DefaultPMode.getDefaultPModeConfig ();
    }

    final PModeConfigManager aPModeConfigMgr = MetaAS4Manager.getPModeConfigMgr ();
    return aPModeConfigMgr.getPModeConfigOfID (sPModeConfigID);
  }
}
