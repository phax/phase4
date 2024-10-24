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
package com.helger.phase4.test.profile;

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
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}. This test
 * profile registrar was created after the profile usage was intensified and the
 * CEF profile would have had been chosen. The profile registered here is very
 * lax and tries to support as much as possible.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
@IsSPIImplementation
public final class AS4TestProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT = "phase4-unitest";

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4TestProfileRegistarSPI.class);

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IPModeIDProvider aPMIDProv = IPModeIDProvider.DEFAULT_DYNAMIC;

    final IAS4ProfilePModeProvider aDefaultPModeProvider = (i,
                                                            r,
                                                            a) -> TestPMode.createTestPMode (i, r, a, aPMIDProv, false);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT + "'");
    final AS4Profile aTestProfile = new AS4Profile (AS4_PROFILE_ID_MAY_SIGN_MAY_CRYPT,
                                                    "Unit Testing Profile",
                                                    TestProfileCompatibilityValidator::new,
                                                    aDefaultPModeProvider,
                                                    aPMIDProv,
                                                    false,
                                                    false);
    aRegistrar.registerProfile (aTestProfile);
  }
}
