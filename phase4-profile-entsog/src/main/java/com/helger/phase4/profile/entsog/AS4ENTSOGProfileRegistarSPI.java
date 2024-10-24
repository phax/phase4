/*
 * Copyright (C) 2015-2021 Pavel Rotek
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Copyright (C) 2021-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.entsog;

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
 * @author Pavel Rotek
 */
@IsSPIImplementation
public final class AS4ENTSOGProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_ID = "entsog";
  public static final String AS4_PROFILE_NAME = "ENTSOG";
  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ENTSOGProfileRegistarSPI.class);

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IAS4ProfilePModeProvider aDefaultPModeProvider = (i,
                                                            r,
                                                            a) -> ENTSOGPMode.createENTSOGPMode (i,
                                                                                                 r,
                                                                                                 a,
                                                                                                 PMODE_ID_PROVIDER,
                                                                                                 true);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID + "'");
    final AS4Profile aProfile = new AS4Profile (AS4_PROFILE_ID,
                                                AS4_PROFILE_NAME,
                                                ENTSOGCompatibilityValidator::new,
                                                aDefaultPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false);
    aRegistrar.registerProfile (aProfile);
  }
}
