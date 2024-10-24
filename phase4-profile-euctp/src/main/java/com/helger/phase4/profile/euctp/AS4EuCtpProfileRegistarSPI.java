/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.euctp;

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
 * @author Philip Helger
 */
@IsSPIImplementation
public final class AS4EuCtpProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_PUSH_ID = "euctp-push";
  public static final String AS4_PROFILE_PUSH_NAME = "EuCTP Push";

  public static final String AS4_PROFILE_PULL_ID = "euctp-pull";
  public static final String AS4_PROFILE_PULL_NAME = "EuCTP Pull";

  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4EuCtpProfileRegistarSPI.class);

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    // push
    final IAS4ProfilePModeProvider aPushPModeProvider = (i, r, a) -> EuCtpPMode.createEuCtpPushPMode (i,
                                                                                                      r,
                                                                                                      a,
                                                                                                      PMODE_ID_PROVIDER,
                                                                                                      true);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_PUSH_ID + "'");
    final AS4Profile aPushProfile = new AS4Profile (AS4_PROFILE_PUSH_ID,
                                                    AS4_PROFILE_PUSH_NAME,
                                                    EuCtpCompatibilityValidator::new,
                                                    aPushPModeProvider,
                                                    PMODE_ID_PROVIDER,
                                                    false,
                                                    false);
    aRegistrar.registerProfile (aPushProfile);

    // pull
    final IAS4ProfilePModeProvider aPullPModeProvider = (i, r, a) -> EuCtpPMode.createEuCtpPullPMode (i,
                                                                                                      r,
                                                                                                      a,
                                                                                                      PMODE_ID_PROVIDER,
                                                                                                      true);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_PULL_ID + "'");
    final AS4Profile aPullProfile = new AS4Profile (AS4_PROFILE_PULL_ID,
                                                    AS4_PROFILE_PULL_NAME,
                                                    EuCtpCompatibilityValidator::new,
                                                    aPullPModeProvider,
                                                    PMODE_ID_PROVIDER,
                                                    false,
                                                    false);
    aRegistrar.registerProfile (aPullProfile);
  }
}
