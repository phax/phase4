/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.resolve;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.DefaultPMode;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.profile.IAS4Profile;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Default implementation of {@link IPModeResolver} using the fixed ID only. If
 * no ID is provided the default pmode is used.
 *
 * @author bayerlma
 */
public class DefaultPModeResolver implements IPModeResolver
{
  public static final IPModeResolver DEFAULT_PMODE_RESOLVER = new DefaultPModeResolver (false);

  private final boolean m_bUseDefaultAsFallback;

  public DefaultPModeResolver (final boolean bUseDefaultAsFallback)
  {
    m_bUseDefaultAsFallback = bUseDefaultAsFallback;
  }

  public final boolean isUseDefaultAsFallback ()
  {
    return m_bUseDefaultAsFallback;
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sPModeID,
                              @Nonnull final String sService,
                              @Nonnull final String sAction,
                              @Nonnull @Nonempty final String sInitiatorID,
                              @Nonnull @Nonempty final String sResponderID,
                              @Nullable final String sAddress)
  {
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    IPMode ret = null;
    if (StringHelper.hasText (sPModeID))
    {
      // An ID is present - try to resolve this ID
      ret = aPModeMgr.getPModeOfID (sPModeID);
      if (ret != null)
        return ret;
    }

    // the PMode id field is empty or null (or invalid)
    // try a combination of service and action
    ret = aPModeMgr.getPModeOfServiceAndAction (sService, sAction);
    if (ret != null)
      return ret;

    // Use default pmode
    // 1. Based on profile
    // 2. Default default
    final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getDefaultProfile ();
    if (aProfile != null)
      return aProfile.createPModeTemplate (sInitiatorID, sResponderID, sAddress);

    if (!m_bUseDefaultAsFallback)
    {
      // Not found and no default -> null
      return null;
    }

    return DefaultPMode.getOrCreateDefaultPMode (sInitiatorID, sResponderID, sAddress, true);
  }
}
