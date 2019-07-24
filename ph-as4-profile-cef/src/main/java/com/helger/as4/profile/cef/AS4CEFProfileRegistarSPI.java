/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.profile.cef;

import javax.annotation.Nonnull;

import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.profile.AS4Profile;
import com.helger.as4.profile.IAS4ProfilePModeProvider;
import com.helger.as4.profile.IAS4ProfileRegistrar;
import com.helger.as4.profile.IAS4ProfileRegistrarSPI;
import com.helger.as4.profile.IAS4ProfileValidator;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.functional.ISupplier;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class AS4CEFProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  @Deprecated
  public static final String AS4_PROFILE_ID = "esens";
  @Deprecated
  public static final String AS4_PROFILE_NAME = "e-SENS";
  public static final String AS4_PROFILE_ID_NEW = "cef";
  public static final String AS4_PROFILE_NAME_NEW = "CEF";

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final IPModeIDProvider aPModeIDProvider = IPModeIDProvider.DEFAULT_DYNAMIC;
    final ISupplier <? extends IAS4ProfileValidator> aProfileValidatorProvider = () -> new CEFCompatibilityValidator ();
    final IAS4ProfilePModeProvider aDefaultPModeProvider = (i, r, a) -> CEFPMode.createCEFPMode (i,
                                                                                                 r,
                                                                                                 a,
                                                                                                 aPModeIDProvider,
                                                                                                 true);
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID,
                                                AS4_PROFILE_NAME,
                                                aProfileValidatorProvider,
                                                aDefaultPModeProvider,
                                                aPModeIDProvider,
                                                true));
    final AS4Profile aProfile = new AS4Profile (AS4_PROFILE_ID_NEW,
                                                AS4_PROFILE_NAME_NEW,
                                                aProfileValidatorProvider,
                                                aDefaultPModeProvider,
                                                aPModeIDProvider,
                                                false);
    aRegistrar.registerProfile (aProfile);
    aRegistrar.setDefaultProfile (aProfile);
  }
}
