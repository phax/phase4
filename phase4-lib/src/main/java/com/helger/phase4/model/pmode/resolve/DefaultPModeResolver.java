/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.resolve;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.DefaultPMode;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4Profile;

/**
 * Default implementation of {@link IPModeResolver} using the fixed ID only. If
 * no ID is provided the default pmode is used.
 *
 * @author bayerlma
 * @author Philip Helger
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
                              @Nullable final String sAgreementRef,
                              @Nullable final String sAddress)
  {
    final IPModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
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

    // Use default pmode based on profile
    final IAS4Profile aProfile = MetaAS4Manager.getProfileMgr ().getDefaultProfileOrNull ();
    if (aProfile != null)
      return aProfile.createPModeTemplate (sInitiatorID, sResponderID, sAddress);

    if (!m_bUseDefaultAsFallback)
    {
      // Not found and no default -> null
      return null;
    }

    // 2. Default default PMode
    return DefaultPMode.getOrCreateDefaultPMode (sInitiatorID, sResponderID, sAddress, true);
  }
}
