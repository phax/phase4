/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.edelivery2;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.IAS4ProfilePModeProvider;
import com.helger.phase4.profile.IAS4ProfileRegistrar;
import com.helger.phase4.profile.IAS4ProfileRegistrarSPI;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI} for eDelivery AS4 2.0.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
@IsSPIImplementation
public final class AS4EDelivery2ProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  /** Profile ID for eDelivery 2.0 Common Usage Profile (EdDSA/X25519) - four corner model */
  public static final String AS4_PROFILE_ID_EDDSA_FOUR_CORNER = "edelivery2-eddsa";
  public static final String AS4_PROFILE_NAME_EDDSA_FOUR_CORNER = "eDelivery AS4 2.0 EdDSA (four corner)";

  /** Profile ID for eDelivery 2.0 Common Usage Profile (EdDSA/X25519) - two corner model */
  public static final String AS4_PROFILE_ID_EDDSA_TWO_CORNER = "edelivery2-eddsa-two-corner";
  public static final String AS4_PROFILE_NAME_EDDSA_TWO_CORNER = "eDelivery AS4 2.0 EdDSA (two corner)";

  /** Profile ID for eDelivery 2.0 Alternative EC Profile (ECDSA/ECDH-ES) - four corner model */
  public static final String AS4_PROFILE_ID_ECDSA_FOUR_CORNER = "edelivery2-ecdsa";
  public static final String AS4_PROFILE_NAME_ECDSA_FOUR_CORNER = "eDelivery AS4 2.0 ECDSA (four corner)";

  /** Profile ID for eDelivery 2.0 Alternative EC Profile (ECDSA/ECDH-ES) - two corner model */
  public static final String AS4_PROFILE_ID_ECDSA_TWO_CORNER = "edelivery2-ecdsa-two-corner";
  public static final String AS4_PROFILE_NAME_ECDSA_TWO_CORNER = "eDelivery AS4 2.0 ECDSA (two corner)";

  public static final IPModeIDProvider PMODE_ID_PROVIDER = IPModeIDProvider.DEFAULT_DYNAMIC;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4EDelivery2ProfileRegistarSPI.class);

  public void registerAS4Profile (@NonNull final IAS4ProfileRegistrar aRegistrar)
  {
    // EdDSA/X25519 PMode provider (Common Usage Profile)
    final IAS4ProfilePModeProvider aEdDSAPModeProvider = (i, r, a) -> EDelivery2PMode.createEDelivery2PMode (i,
                                                                                                             r,
                                                                                                             a,
                                                                                                             PMODE_ID_PROVIDER,
                                                                                                             true,
                                                                                                             EDelivery2PMode.generatePModeLegSecurityEdDSA ());

    // ECDSA/ECDH-ES PMode provider (Alternative EC Profile)
    final IAS4ProfilePModeProvider aECDSAPModeProvider = (i, r, a) -> EDelivery2PMode.createEDelivery2PMode (i,
                                                                                                             r,
                                                                                                             a,
                                                                                                             PMODE_ID_PROVIDER,
                                                                                                             true,
                                                                                                             EDelivery2PMode.generatePModeLegSecurityECDSA ());

    // Register EdDSA four corner profile
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_EDDSA_FOUR_CORNER + "'");
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID_EDDSA_FOUR_CORNER,
                                                AS4_PROFILE_NAME_EDDSA_FOUR_CORNER,
                                                () -> new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (true)
                                                                                             .setAllowECDSA (false),
                                                aEdDSAPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false));

    // Register EdDSA two corner profile
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_EDDSA_TWO_CORNER + "'");
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID_EDDSA_TWO_CORNER,
                                                AS4_PROFILE_NAME_EDDSA_TWO_CORNER,
                                                () -> new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false)
                                                                                             .setAllowECDSA (false),
                                                aEdDSAPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false));

    // Register ECDSA four corner profile
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_ECDSA_FOUR_CORNER + "'");
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID_ECDSA_FOUR_CORNER,
                                                AS4_PROFILE_NAME_ECDSA_FOUR_CORNER,
                                                () -> new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (true)
                                                                                             .setAllowECDSA (true),
                                                aECDSAPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false));

    // Register ECDSA two corner profile
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registering phase4 profile '" + AS4_PROFILE_ID_ECDSA_TWO_CORNER + "'");
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID_ECDSA_TWO_CORNER,
                                                AS4_PROFILE_NAME_ECDSA_TWO_CORNER,
                                                () -> new EDelivery2CompatibilityValidator ().setExpectFourCornerModel (false)
                                                                                             .setAllowECDSA (true),
                                                aECDSAPModeProvider,
                                                PMODE_ID_PROVIDER,
                                                false,
                                                false));
  }
}
