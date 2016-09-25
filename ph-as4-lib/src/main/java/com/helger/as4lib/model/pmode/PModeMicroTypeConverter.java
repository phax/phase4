/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.photon.security.object.AbstractObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeMicroTypeConverter extends AbstractObjectMicroTypeConverter
{
  private static final String ATTR_AGREEMENT = "Agreement";
  private static final String ATTR_MEP = "MEP";
  private static final String ATTR_MEP_BINDING = "MEPBinding";
  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final String ELEMENT_LEG1 = "Leg1";
  private static final String ELEMENT_LEG2 = "Leg2";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final IPMode aValue = (IPMode) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);
    ret.setAttribute (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.setAttribute (ATTR_MEP, aValue.getMEPID ());
    ret.setAttribute (ATTR_MEP_BINDING, aValue.getMEPBindingID ());
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getInitiator (),
                                                               sNamespaceURI,
                                                               ELEMENT_INITIATOR));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getResponder (),
                                                               sNamespaceURI,
                                                               ELEMENT_RESPONDER));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg1 (), sNamespaceURI, ELEMENT_LEG1));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg2 (), sNamespaceURI, ELEMENT_LEG2));

    return ret;
  }

  public PMode convertToNative (final IMicroElement aElement)
  {
    final PMode ret = new PMode (getStubObject (aElement));
    ret.setAgreement (aElement.getAttributeValue (ATTR_AGREEMENT));
    ret.setMEP (EMEP.getFromIDOrNull (aElement.getAttributeValue (ATTR_MEP)));
    ret.setMEPBinding (ETransportChannelBinding.getFromIDOrNull (aElement.getAttributeValue (ATTR_MEP_BINDING)));
    ret.setResponder (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RESPONDER),
                                                          PModeParty.class));
    ret.setLeg1 (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG1), PModeLeg.class));
    ret.setLeg2 (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG2), PModeLeg.class));
    ret.setInitiator (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_INITIATOR),
                                                          PModeParty.class));
    return ret;
  }
}
