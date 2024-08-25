/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.DefaultPMode;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.IAS4ProfileManager;

/**
 * Default implementation of {@link IPModeResolver} using the fixed ID only. If
 * no ID is provided the default PMode is used.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class DefaultPModeResolver implements IPModeResolver
{
  public static final IPModeResolver DEFAULT_PMODE_RESOLVER = new DefaultPModeResolver (null, false);

  private final String m_sAS4ProfileID;
  private final boolean m_bUseDefaultAsFallback;

  public DefaultPModeResolver (@Nullable final String sAS4ProfileID, final boolean bUseDefaultAsFallback)
  {
    m_sAS4ProfileID = sAS4ProfileID;
    m_bUseDefaultAsFallback = bUseDefaultAsFallback;
  }

  /**
   * @return The AS4 profile ID that was provided in the constructor. May be
   *         <code>null</code>.
   * @since 2.8.2
   */
  @Nullable
  public final String getAS4ProfileID ()
  {
    return m_sAS4ProfileID;
  }

  public final boolean isUseDefaultAsFallback ()
  {
    return m_bUseDefaultAsFallback;
  }

  @Nullable
  @OverrideOnDemand
  protected IPMode createDefaultPMode (@Nonnull @Nonempty final String sInitiatorID,
                                       @Nonnull @Nonempty final String sResponderID,
                                       @Nullable final String sAddress)
  {
    // Use default pmode based on profile
    final IAS4ProfileManager aProfileMgr = MetaAS4Manager.getProfileMgr ();
    IAS4Profile aProfile = null;
    if (StringHelper.hasText (m_sAS4ProfileID))
    {
      // Try to resolve ID from constructor
      aProfile = aProfileMgr.getProfileOfID (m_sAS4ProfileID);
    }
    if (aProfile == null)
    {
      // ID not provided or non-existing - use default
      aProfile = aProfileMgr.getDefaultProfileOrNull ();
    }
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

  @Nullable
  public IPMode findPMode (@Nullable final String sPModeID,
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

    // Try to resolve a default PMode from the other parameters
    return createDefaultPMode (sInitiatorID, sResponderID, sAddress);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AS4ProfileID", m_sAS4ProfileID)
                                       .append ("UseDefaultAsFallback", m_bUseDefaultAsFallback)
                                       .getToString ();
  }
}
