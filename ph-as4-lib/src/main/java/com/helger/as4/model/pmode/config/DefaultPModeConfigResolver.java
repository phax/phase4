/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4.model.pmode.config;

import javax.annotation.Nullable;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.DefaultPMode;
import com.helger.as4.profile.IAS4Profile;
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
      final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getDefaultProfile ();
      if (aProfile != null)
        return aProfile.createDefaultPModeConfig ();

      return DefaultPMode.getDefaultPModeConfig ();
    }

    // An ID is present - resolve this ID
    final PModeConfigManager aPModeConfigMgr = MetaAS4Manager.getPModeConfigMgr ();
    return aPModeConfigMgr.getPModeConfigOfID (sPModeConfigID);
  }
}
