/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.dbnalliance;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.IAS4ProfilePModeProvider;
import com.helger.phase4.profile.IAS4ProfileRegistrar;
import com.helger.phase4.profile.IAS4ProfileRegistrarSPI;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Philip Helger
 * @author Michael Riviera
 */
@IsSPIImplementation
public final class AS4DBNAllianceProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_ID = "dbnalliance";
  public static final String AS4_PROFILE_NAME = "DBN Alliance";
  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4DBNAllianceProfileRegistarSPI.class);

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IAS4ProfilePModeProvider aDefaultPModeProvider = (i,
                                                            r,
                                                            a) -> DBNAlliancePMode.createDBNAlliancePMode (i,
                                                                                                           r,
                                                                                                           a,
                                                                                                           PMODE_ID_PROVIDER,
                                                                                                           true);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID + "'");
    final AS4Profile aProfile = new AS4Profile (AS4_PROFILE_ID,
                                                AS4_PROFILE_NAME,
                                                DBNAllianceCompatibilityValidator::new,
                                                aDefaultPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false);
    aRegistrar.registerProfile (aProfile);
  }
}
