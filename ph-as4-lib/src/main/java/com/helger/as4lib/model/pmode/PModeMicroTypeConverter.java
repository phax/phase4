package com.helger.as4lib.model.pmode;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_ID = "ID";
  private static final String ATTR_AGREEMENT = "Agreement";
  private static final String ATTR_MEP = "MEP";
  private static final String ATTR_MEP_BINDING = "MEPBinding";
  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final String ELEMENT_LEG1 = "Leg1";
  private static final String ELEMENT_LEG2 = "Leg2";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PMode aValue = (PMode) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ID, aValue.getID ());
    ret.setAttribute (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.setAttribute (ATTR_MEP, aValue.getMEP ().getID ());
    ret.setAttribute (ATTR_MEP_BINDING, aValue.getMEPBinding ().getID ());
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
    final PMode ret = new PMode (aElement.getAttributeValue (ATTR_ID));
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
