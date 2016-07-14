package com.helger.as4lib.model.pmode;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_ID = "ID";
  private static final String ATTR_AGREEMENT = "Agreement";
  private static final String ELEMENT_MEP = "MEP";
  private static final String ELEMENT_MEP_BINDING = "MEPBinding";
  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final String ELEMENT_LEG = "Leg";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PMode aValue = (PMode) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ID, aValue.getID ());
    ret.setAttribute (ATTR_AGREEMENT, aValue.getAgreement ());
    ret.appendElement (ELEMENT_MEP).appendText (aValue.getMEP ().getURI ());
    ret.appendElement (ELEMENT_MEP_BINDING).appendText (aValue.getMEPBinding ().getURI ());
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getInitiator (),
                                                               sNamespaceURI,
                                                               ELEMENT_INITIATOR));

    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getResponder (),
                                                               sNamespaceURI,
                                                               ELEMENT_RESPONDER));
    for (final PModeLeg aPModeLeg : aValue.getLegs ())
    {
      ret.appendChild (MicroTypeConverter.convertToMicroElement (aPModeLeg, sNamespaceURI, ELEMENT_LEG));
    }

    return ret;
  }

  public PMode convertToNative (final IMicroElement aElement)
  {
    final PMode ret = new PMode ();
    ret.setID (aElement.getAttributeValue (ATTR_ID));
    ret.setInitiator (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_INITIATOR),
                                                          PModeParty.class));

    final ICommonsList <PModeLeg> aPModeLegs = new CommonsArrayList<> ();
    for (final IMicroElement aPModeElement : aElement.getAllChildElements (ELEMENT_LEG))
    {
      aPModeLegs.add (MicroTypeConverter.convertToNative (aPModeElement, PModeLeg.class));
    }
    ret.setLegs (aPModeLegs);
    ret.setResponder (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RESPONDER),
                                                          PModeParty.class));
    ret.setMEP (EMEP.getFromURIOrNull (aElement.getFirstChildElement (ELEMENT_MEP).getTextContentTrimmed ()));
    ret.setMEPBinding (ETransportChannelBinding.getFromURIOrNull (aElement.getFirstChildElement (ELEMENT_MEP_BINDING)
                                                                          .getTextContentTrimmed ()));
    ret.setAgreement (aElement.getAttributeValue (ATTR_AGREEMENT));

    return ret;
  }

}
