/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.config;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.as4lib.model.mpc.MPC;
import com.helger.as4lib.model.mpc.MPCMicroTypeConverter;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeAddressList;
import com.helger.as4lib.model.pmode.PModeAddressListMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegBusinessInformation;
import com.helger.as4lib.model.pmode.PModeLegBusinessInformationMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLegErrorHandling;
import com.helger.as4lib.model.pmode.PModeLegErrorHandlingMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLegMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeLegProtocolMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLegReliability;
import com.helger.as4lib.model.pmode.PModeLegReliabilityMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeLegSecurity;
import com.helger.as4lib.model.pmode.PModeLegSecurityMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeParty;
import com.helger.as4lib.model.pmode.PModePartyMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModePayloadProfile;
import com.helger.as4lib.model.pmode.PModePayloadProfileMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModePayloadService;
import com.helger.as4lib.model.pmode.PModePayloadServiceMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeProperty;
import com.helger.as4lib.model.pmode.PModePropertyMicroTypeConverter;
import com.helger.as4lib.model.pmode.PModeReceptionAwareness;
import com.helger.as4lib.model.pmode.PModeReceptionAwarenessMicroTypeConverter;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerMicroTypeConverter;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistry;

@Immutable
@IsSPIImplementation
public final class PModeMicroTypeConverterRegistrar implements IMicroTypeConverterRegistrarSPI
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

    aRegistry.registerMicroElementTypeConverter (Partner.class, new PartnerMicroTypeConverter ());
  }
}
