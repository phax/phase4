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
package com.helger.phase4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.photon.security.object.AbstractBusinessObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * XML converter for objects of class {@link PMode}.
 *
 * @author Philip Helger
 */
public final class PModeMicroTypeConverter extends AbstractBusinessObjectMicroTypeConverter <PMode>
{
  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final IMicroQName ATTR_AGREEMENT = new MicroQName ("Agreement");
  private static final IMicroQName ATTR_MEP = new MicroQName ("MEP");
  private static final IMicroQName ATTR_MEP_BINDING = new MicroQName ("MEPBinding");
  private static final String ELEMENT_LEG1 = "Leg1";
  private static final String ELEMENT_LEG2 = "Leg2";
  private static final String ELEMENT_PAYLOADSERVICE = "PayloadServices";
  private static final String ELEMENT_RECEPETIONAWARENESS = "RecepetionAwareness";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PMode aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getInitiator (),
                                                               sNamespaceURI,
                                                               ELEMENT_INITIATOR));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getResponder (),
                                                               sNamespaceURI,
                                                               ELEMENT_RESPONDER));
    ret.setAttribute (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.setAttribute (ATTR_MEP, aValue.getMEPID ());
    ret.setAttribute (ATTR_MEP_BINDING, aValue.getMEPBindingID ());
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg1 (), sNamespaceURI, ELEMENT_LEG1));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getLeg2 (), sNamespaceURI, ELEMENT_LEG2));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getPayloadService (),
                                                               sNamespaceURI,
                                                               ELEMENT_PAYLOADSERVICE));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReceptionAwareness (),
                                                               sNamespaceURI,
                                                               ELEMENT_RECEPETIONAWARENESS));
    return ret;
  }

  @Nonnull
  public PMode convertToNative (@Nonnull final IMicroElement aElement)
  {
    final PModeParty aInitiator = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_INITIATOR),
                                                                      PModeParty.class);
    final PModeParty aResponder = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RESPONDER),
                                                                      PModeParty.class);

    final String sAgreement = aElement.getAttributeValue (ATTR_AGREEMENT);

    final String sMEP = aElement.getAttributeValue (ATTR_MEP);
    final EMEP eMEP = EMEP.getFromIDOrNull (sMEP);
    if (eMEP == null)
      throw new IllegalStateException ("Failed to resolve MEP '" + sMEP + "'");

    final String sMEPBinding = aElement.getAttributeValue (ATTR_MEP_BINDING);
    final EMEPBinding eMEPBinding = EMEPBinding.getFromIDOrNull (sMEPBinding);
    if (eMEPBinding == null)
      throw new IllegalStateException ("Failed to resolve MEPBinding '" + sMEPBinding + "'");

    final PModeLeg aLeg1 = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG1),
                                                               PModeLeg.class);
    final PModeLeg aLeg2 = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_LEG2),
                                                               PModeLeg.class);

    final PModePayloadService aPayloadService = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PAYLOADSERVICE),
                                                                                    PModePayloadService.class);
    final PModeReceptionAwareness aReceptionAwareness = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RECEPETIONAWARENESS),
                                                                                            PModeReceptionAwareness.class);
    return new PMode (getStubObject (aElement),
                      aInitiator,
                      aResponder,
                      sAgreement,
                      eMEP,
                      eMEPBinding,
                      aLeg1,
                      aLeg2,
                      aPayloadService,
                      aReceptionAwareness);
  }
}
