package com.helger.as4lib.model.pmode;

import com.helger.commons.mime.IMimeType;
import com.helger.commons.state.EMandatory;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModePayloadProfileMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_MAX_SIZE_KB = "MaxSizeKB";
  private static final String ATTR_MIME_TYPE = "MimeType";
  private static final String ATTR_MANDATORY = "Mandatory";
  private static final String ATTR_NAME = "Name";
  private static final String ATTR_XSD_FILENAME = "XSDFilename";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegReliability aValue = (PModeLegReliability) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_MAX_SIZE_KB, aValue.isAtLeastOnceAckOnDelivery ());
    ret.setAttribute (ATTR_MIME_TYPE, aValue.isAtLeastOnceContract ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isAtLeastOnceContractAckResponse ());
    ret.setAttribute (ATTR_NAME, aValue.getAtLeastOnceReplyPattern ());
    ret.setAttribute (ATTR_XSD_FILENAME, aValue.isAtMostOnceContract ());

    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final IMimeType aMimeType = aElement.getAttributeValueWithConversion (ATTR_MIME_TYPE, IMimeType.class);
    final String sXSDFilename = aElement.getAttributeValue (ATTR_XSD_FILENAME);
    final Integer aMaxSizeKB = aElement.getAttributeValueWithConversion (ATTR_MAX_SIZE_KB, Integer.class);
    final EMandatory eMandatory = aElement.getAttributeValueWithConversion (ATTR_MANDATORY, EMandatory.class);

    return new PModePayloadProfile (sName, aMimeType, sXSDFilename, aMaxSizeKB, eMandatory);
  }

}
