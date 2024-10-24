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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4Profile;

/**
 * Default implementation of {@link IAS4PModeResolver} based on an AS4 Profile
 * ID. If no PMode is present, the respective PMode template from the selected
 * AS4 profile is used instead.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class AS4DefaultPModeResolver implements IAS4PModeResolver
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4DefaultPModeResolver.class);

  private final String m_sAS4ProfileID;
  private final IAS4Profile m_aAS4Profile;

  public AS4DefaultPModeResolver (@Nullable final String sAS4ProfileID)
  {
    m_sAS4ProfileID = sAS4ProfileID;
    m_aAS4Profile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sAS4ProfileID);
    if (m_aAS4Profile == null && StringHelper.hasText (sAS4ProfileID))
      LOGGER.error ("Failed to resolved the AS4 profile ID '" + sAS4ProfileID + "'");
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

  /**
   * @return The resolved AS4 profile based on the ID provided in the
   *         constructor. May be <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  protected final IAS4Profile getAS4Profile ()
  {
    return m_aAS4Profile;
  }

  @Nullable
  @OverrideOnDemand
  protected IPMode createDefaultPMode (@Nonnull @Nonempty final String sInitiatorID,
                                       @Nonnull @Nonempty final String sResponderID,
                                       @Nullable final String sAddress)
  {
    if (m_aAS4Profile != null)
    {
      // Create a default PMode template
      return m_aAS4Profile.createPModeTemplate (sInitiatorID, sResponderID, sAddress);
    }

    // Nothing to create
    return null;
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

    // the PMode ID field is empty or null or invalid
    // try a combination of Service and Action
    ret = aPModeMgr.getPModeOfServiceAndAction (sService, sAction);
    if (ret != null)
      return ret;

    // No existing PMode was found
    // Try to resolve a default PMode from the other parameters
    return createDefaultPMode (sInitiatorID, sResponderID, sAddress);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AS4ProfileID", m_sAS4ProfileID).getToString ();
  }
}
