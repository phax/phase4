/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4Profile;

/**
 * Default implementation of {@link IAS4PModeResolver} based on an AS4 Profile ID. If no PMode is
 * present, the respective PMode template from the selected AS4 profile is used instead.
 *
 * @author bayerlma
 * @author Philip Helger
 */
public class AS4DefaultPModeResolver implements IAS4PModeResolver
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4DefaultPModeResolver.class);

  private final String m_sAS4ProfileID;
  private final IAS4Profile m_aAS4Profile;

  public AS4DefaultPModeResolver (@Nullable final String sAS4ProfileID)
  {
    m_sAS4ProfileID = sAS4ProfileID;
    m_aAS4Profile = MetaAS4Manager.getProfileMgr ().getProfileOfID (sAS4ProfileID);
    if (m_aAS4Profile == null && StringHelper.isNotEmpty (sAS4ProfileID))
      LOGGER.error ("Failed to resolved the AS4 profile ID '" + sAS4ProfileID + "'");
  }

  /**
   * @return The AS4 profile ID that was provided in the constructor. May be <code>null</code>.
   * @since 2.8.2
   */
  @Nullable
  public final String getAS4ProfileID ()
  {
    return m_sAS4ProfileID;
  }

  /**
   * @return The resolved AS4 profile based on the ID provided in the constructor. May be
   *         <code>null</code>.
   * @since 3.0.0
   */
  @Nullable
  protected final IAS4Profile getAS4Profile ()
  {
    return m_aAS4Profile;
  }

  /**
   * Create a default PMode template for the provided parameters.
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sAddress
   *        Address string
   * @return <code>null</code> if no AS4 profile is present.
   * @deprecated Since 4.6.1 - override
   *             {@link #createDefaultPMode(String, String, String, String, String)} instead. This
   *             method is no longer called by
   *             {@link #findPMode(String, String, String, String, String, String, String)}.
   */
  @Nullable
  @OverrideOnDemand
  @Deprecated (since = "4.6.1", forRemoval = true)
  protected IPMode createDefaultPMode (@NonNull @Nonempty final String sInitiatorID,
                                       @NonNull @Nonempty final String sResponderID,
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

  /**
   * Create a default PMode template for the provided parameters, including the Service and the
   * Action of the message the PMode is created for. Without these two values, a synthesized PMode
   * template can never satisfy a profile validator that checks
   * <code>PMode.Leg[x].BusinessInfo.Service</code> or <code>...Action</code>. See issue #213.
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sAddress
   *        Address string
   * @param sService
   *        The Service of the message the PMode is created for. May be <code>null</code>.
   * @param sAction
   *        The Action of the message the PMode is created for. May be <code>null</code>.
   * @return <code>null</code> if no AS4 profile is present.
   * @since 4.6.1
   */
  @Nullable
  @OverrideOnDemand
  protected IPMode createDefaultPMode (@NonNull @Nonempty final String sInitiatorID,
                                       @NonNull @Nonempty final String sResponderID,
                                       @Nullable final String sAddress,
                                       @Nullable final String sService,
                                       @Nullable final String sAction)
  {
    if (m_aAS4Profile != null)
    {
      // Create a default PMode template
      return m_aAS4Profile.createPModeTemplate (sInitiatorID, sResponderID, sAddress, sService, sAction);
    }

    // Nothing to create
    return null;
  }

  @Nullable
  public IPMode findPMode (@Nullable final String sPModeID,
                           @NonNull final String sService,
                           @NonNull final String sAction,
                           @NonNull @Nonempty final String sInitiatorID,
                           @NonNull @Nonempty final String sResponderID,
                           @Nullable final String sAgreementRef,
                           @Nullable final String sAddress)
  {
    final IPModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();

    IPMode ret = null;
    if (StringHelper.isNotEmpty (sPModeID))
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
    // Try to resolve a default PMode from the other parameters. Service and Action are passed in,
    // because a synthesized template has no other source for them (see issue #213)
    return createDefaultPMode (sInitiatorID, sResponderID, sAddress, sService, sAction);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AS4ProfileID", m_sAS4ProfileID).getToString ();
  }
}
