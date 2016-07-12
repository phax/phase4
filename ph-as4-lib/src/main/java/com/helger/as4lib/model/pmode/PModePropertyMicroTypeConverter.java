package com.helger.as4lib.model.pmode;

import com.helger.commons.state.EMandatory;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModePropertyMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_DESCRIPTION = "Description";
  private static final String ATTR_DATA_TYPE = "DataType";
  private static final String ATTR_MANDATORY = "Mandatory";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegReliability aValue = (PModeLegReliability) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_NAME, aValue.getAtLeastOnceReplyPattern ());
    ret.setAttribute (ATTR_DESCRIPTION, aValue.isAtMostOnceContract ());
    ret.setAttribute (ATTR_DATA_TYPE, aValue.isAtLeastOnceAckOnDelivery ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isAtLeastOnceContractAckResponse ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final String sDescription = aElement.getAttributeValue (ATTR_DESCRIPTION);
    final String sDataType = aElement.getAttributeValue (ATTR_DATA_TYPE);
    final EMandatory eMandatory = aElement.getAttributeValueWithConversion (ATTR_MANDATORY, EMandatory.class);
    return new PModeProperty (sName, sDescription, sDataType, eMandatory);
  }

}
