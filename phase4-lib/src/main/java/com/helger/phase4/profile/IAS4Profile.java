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
package com.helger.phase4.profile;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.id.IHasID;
import com.helger.base.name.IHasDisplayName;
import com.helger.base.string.StringHelper;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;

/**
 * Base interface for an AS4 profile - a group of settings that outline what features of AS4 are
 * used.
 *
 * @author Philip Helger
 */
@MustImplementEqualsAndHashcode
public interface IAS4Profile extends IHasID <String>, IHasDisplayName
{
  /**
   * @return An optional validator. May be <code>null</code>.
   */
  @Nullable
  IAS4ProfileValidator getValidator ();

  /**
   * Create a PMode for the provided parameters.
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sAddress
   *        Address string
   * @return A PMode that is NOT yet in the manager and is not complete! The following information
   *         is most likely not contained: URLs, certificates.
   */
  @NonNull
  PMode createPModeTemplate (@NonNull @Nonempty String sInitiatorID,
                             @NonNull @Nonempty String sResponderID,
                             @Nullable String sAddress);

  /**
   * Create a PMode for the provided parameters, including the Service and the Action of the message
   * the PMode is created for. This overload exists, because a PMode template that is synthesized
   * for an incoming message has no other source for these two values, and a profile validator that
   * checks <code>PMode.Leg[x].BusinessInfo.Service</code> or <code>...Action</code> can therefore
   * never succeed on such a template. See issue #213.
   * <p>
   * The default implementation calls {@link #createPModeTemplate(String, String, String)} and
   * afterwards copies the provided Service and Action into the business information of leg 1, but
   * only if the created template left them empty. Implementations that need a different behaviour -
   * e.g. because they use two legs - must override this method.
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
   * @return A PMode that is NOT yet in the manager and is not complete! The following information
   *         is most likely not contained: URLs, certificates.
   * @since 4.6.1
   */
  @NonNull
  default PMode createPModeTemplate (@NonNull @Nonempty final String sInitiatorID,
                                     @NonNull @Nonempty final String sResponderID,
                                     @Nullable final String sAddress,
                                     @Nullable final String sService,
                                     @Nullable final String sAction)
  {
    final PMode ret = createPModeTemplate (sInitiatorID, sResponderID, sAddress);

    // Only fill what the template left open
    final PModeLeg aLeg1 = ret.getLeg1 ();
    if (aLeg1 != null)
    {
      final PModeLegBusinessInformation aBusinessInfo = aLeg1.getBusinessInfo ();
      if (aBusinessInfo != null)
      {
        if (StringHelper.isEmpty (aBusinessInfo.getService ()) && StringHelper.isNotEmpty (sService))
          aBusinessInfo.setService (sService);
        if (StringHelper.isEmpty (aBusinessInfo.getAction ()) && StringHelper.isNotEmpty (sAction))
          aBusinessInfo.setAction (sAction);
      }
    }
    return ret;
  }

  /**
   * @return The PMode ID provider to be used for this profile. May not be <code>null</code>.
   */
  @NonNull
  IPModeIDProvider getPModeIDProvider ();

  /**
   * @return <code>true</code> if this AS4 profile is deprecated and should therefore not be used,
   *         or <code>false</code> if not.
   */
  boolean isDeprecated ();

  /**
   * @return <code>true</code> if this profile wants to handle Ping messages inside the custom SPI
   *         handler. This was introduced for sole usage in BDEW profile.
   * @since v2.5.3
   */
  boolean isInvokeSPIForPingMessage ();
}
