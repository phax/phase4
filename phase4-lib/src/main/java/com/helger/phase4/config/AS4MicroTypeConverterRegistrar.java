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
package com.helger.phase4.config;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.phase4.duplicate.AS4DuplicateItem;
import com.helger.phase4.duplicate.AS4DuplicateItemMicroTypeConverter;
import com.helger.phase4.model.mpc.MPC;
import com.helger.phase4.model.mpc.MPCMicroTypeConverter;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeMicroTypeConverter;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.PModePartyMicroTypeConverter;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModePayloadServiceMicroTypeConverter;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.PModeReceptionAwarenessMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeAddressList;
import com.helger.phase4.model.pmode.leg.PModeAddressListMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformationMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandlingMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLegMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegProtocolMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegReliabilityMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.model.pmode.leg.PModeLegSecurityMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModePayloadProfile;
import com.helger.phase4.model.pmode.leg.PModePayloadProfileMicroTypeConverter;
import com.helger.phase4.model.pmode.leg.PModeProperty;
import com.helger.phase4.model.pmode.leg.PModePropertyMicroTypeConverter;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistry;

/**
 * MicroType converter registry for this project. Invoked via SPI.
 * 
 * @author Philip Helger
 */
@Immutable
@IsSPIImplementation
public final class AS4MicroTypeConverterRegistrar implements IMicroTypeConverterRegistrarSPI
{
  public void registerMicroTypeConverter (@Nonnull final IMicroTypeConverterRegistry aRegistry)
  {
    // Register all MicroTypeConverter
    aRegistry.registerMicroElementTypeConverter (PModeParty.class, new PModePartyMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PMode.class, new PModeMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLegBusinessInformation.class,
                                                 new PModeLegBusinessInformationMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLegErrorHandling.class,
                                                 new PModeLegErrorHandlingMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLeg.class, new PModeLegMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLegProtocol.class, new PModeLegProtocolMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLegReliability.class,
                                                 new PModeLegReliabilityMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeLegSecurity.class, new PModeLegSecurityMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModePayloadProfile.class,
                                                 new PModePayloadProfileMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeProperty.class, new PModePropertyMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeAddressList.class, new PModeAddressListMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModePayloadService.class,
                                                 new PModePayloadServiceMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (PModeReceptionAwareness.class,
                                                 new PModeReceptionAwarenessMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (MPC.class, new MPCMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (AS4DuplicateItem.class, new AS4DuplicateItemMicroTypeConverter ());
  }
}
