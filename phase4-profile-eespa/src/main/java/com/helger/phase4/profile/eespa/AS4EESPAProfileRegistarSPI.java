/*
 * Copyright (C) 2020-2024 OpusCapita
 * Copyright (C) 2022 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.eespa;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.IAS4ProfilePModeProvider;
import com.helger.phase4.profile.IAS4ProfileRegistrar;
import com.helger.phase4.profile.IAS4ProfileRegistrarSPI;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}.
 *
 * @author OpusCapita
 */
@IsSPIImplementation
public final class AS4EESPAProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  // The IDs are unchanged for backwards compatibility
  public static final String AS4_PROFILE_ID_ACCEPTANCE = "eespa-acc";
  public static final String AS4_PROFILE_NAME_ACCEPTANCE = "GENA Acceptance";
  public static final String AS4_PROFILE_ID_PROD = "eespa-prod";
  public static final String AS4_PROFILE_NAME_PROD = "GENA Production";

  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4EESPAProfileRegistarSPI.class);

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IAS4ProfilePModeProvider aDefaultPModeProviderAcc = (i,
                                                               r,
                                                               a) -> EESPAPMode.createEESPAPMode (i,
                                                                                                  r,
                                                                                                  a,
                                                                                                  PMODE_ID_PROVIDER,
                                                                                                  true,
                                                                                                  false);
    final IAS4ProfilePModeProvider aDefaultPModeProviderProd = (i,
                                                                r,
                                                                a) -> EESPAPMode.createEESPAPMode (i,
                                                                                                   r,
                                                                                                   a,
                                                                                                   PMODE_ID_PROVIDER,
                                                                                                   false,
                                                                                                   false);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_ACCEPTANCE + "'");
    final AS4Profile aProfileAcc = new AS4Profile (AS4_PROFILE_ID_ACCEPTANCE,
                                                   AS4_PROFILE_NAME_ACCEPTANCE,
                                                   EESPACompatibilityValidator::new,
                                                   aDefaultPModeProviderAcc,
                                                   PMODE_ID_PROVIDER,
                                                   false,
                                                   false);
    aRegistrar.registerProfile (aProfileAcc);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_PROD + "'");
    final AS4Profile aProfileProd = new AS4Profile (AS4_PROFILE_ID_PROD,
                                                    AS4_PROFILE_NAME_PROD,
                                                    EESPACompatibilityValidator::new,
                                                    aDefaultPModeProviderProd,
                                                    PMODE_ID_PROVIDER,
                                                    false,
                                                    false);
    aRegistrar.registerProfile (aProfileProd);
  }
}
