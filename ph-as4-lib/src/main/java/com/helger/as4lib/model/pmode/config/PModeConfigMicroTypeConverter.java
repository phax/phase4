/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.as4lib.model.pmode.leg.PModeLeg;
import com.helger.photon.security.object.AbstractObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeConfigMicroTypeConverter extends AbstractObjectMicroTypeConverter
{
  private static final String ATTR_AGREEMENT = "Agreement";
  private static final String ATTR_MEP = "MEP";
  private static final String ATTR_MEP_BINDING = "MEPBinding";
  private static final String ELEMENT_LEG1 = "Leg1";
  private static final String ELEMENT_LEG2 = "Leg2";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IPModeConfig aValue = (IPModeConfig) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);
    ret.setAttribute (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.setAttribute (ATTR_MEP, aValue.getMEPID ());
    ret.setAttribute (ATTR_MEP_BINDING, aValue.getMEPBindingID ());
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg1 (), sNamespaceURI, ELEMENT_LEG1));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg2 (), sNamespaceURI, ELEMENT_LEG2));

    return ret;
  }

  @Nonnull
  public PModeConfig convertToNative (@Nonnull final IMicroElement aElement)
  {
    final PModeConfig ret = new PModeConfig (getStubObject (aElement));
    ret.setAgreement (aElement.getAttributeValue (ATTR_AGREEMENT));
    ret.setMEP (EMEP.getFromIDOrNull (aElement.getAttributeValue (ATTR_MEP)));
    ret.setMEPBinding (ETransportChannelBinding.getFromIDOrNull (aElement.getAttributeValue (ATTR_MEP_BINDING)));
    ret.setLeg1 (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG1), PModeLeg.class));
    ret.setLeg2 (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG2), PModeLeg.class));
    return ret;
  }
}
