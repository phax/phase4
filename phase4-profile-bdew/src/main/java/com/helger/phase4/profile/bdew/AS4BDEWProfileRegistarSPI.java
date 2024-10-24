/*
 * Copyright (C) 2023-2024 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.profile.bdew;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.IAS4ProfilePModeProvider;
import com.helger.phase4.profile.IAS4ProfileRegistrar;
import com.helger.phase4.profile.IAS4ProfileRegistrarSPI;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Gregor Scholtysik
 * @since 2.1.0
 */
@IsSPIImplementation
public final class AS4BDEWProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_ID = "bdew";
  public static final String AS4_PROFILE_NAME = "BDEW";
  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4BDEWProfileRegistarSPI.class);

  @Nonnull
  @Nonempty
  private static String _getTypeFromID (@Nonnull @Nonempty final String sID)
  {
    if (sID.startsWith ("99"))
      return BDEWPMode.BDEW_PARTY_ID_TYPE_BDEW;
    if (sID.startsWith ("98"))
      return BDEWPMode.BDEW_PARTY_ID_TYPE_DVGW;
    if (sID.startsWith ("19"))
      return BDEWPMode.BDEW_PARTY_ID_TYPE_BAHN;
    return BDEWPMode.BDEW_PARTY_ID_TYPE_GLN;
  }

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IAS4ProfilePModeProvider aDefaultPModeProvider = (i, r, a) -> BDEWPMode.createBDEWPMode (i,
                                                                                                   _getTypeFromID (i),
                                                                                                   r,
                                                                                                   _getTypeFromID (r),
                                                                                                   a,
                                                                                                   PMODE_ID_PROVIDER,
                                                                                                   true);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID + "'");
    final AS4Profile aProfile = new AS4Profile (AS4_PROFILE_ID,
                                                AS4_PROFILE_NAME,
                                                BDEWCompatibilityValidator::new,
                                                aDefaultPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                true);
    aRegistrar.registerProfile (aProfile);
  }

}
