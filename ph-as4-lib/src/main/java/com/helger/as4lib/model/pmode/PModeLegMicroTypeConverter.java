package com.helger.as4lib.model.pmode;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeLegMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_BUSINESS_INFORMATION = "BusinessInfo";
  private static final String ELEMENT_ERROR_HANDLING = "mErrorHandling";
  private static final String ELEMENT_PROTOCOL = "Protocol";
  private static final String ELEMENT_RELIABILITY = "Reliability";
  private static final String ELEMENT_SECURITY = "Security";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLeg aValue = (PModeLeg) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getBusinessInfo (),
                                                               sNamespaceURI,
                                                               ELEMENT_BUSINESS_INFORMATION));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getErrorHandling (),
                                                               sNamespaceURI,
                                                               ELEMENT_ERROR_HANDLING));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getProtocol (), sNamespaceURI, ELEMENT_PROTOCOL));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReliability (),
                                                               sNamespaceURI,
                                                               ELEMENT_RELIABILITY));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getSecurity (), sNamespaceURI, ELEMENT_SECURITY));
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {

    final PModeLegBusinessInformation aBusinessInformation = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_BUSINESS_INFORMATION),
                                                                                                 PModeLegBusinessInformation.class);
    final PModeLegErrorHandling aErrorHandling = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_ERROR_HANDLING),
                                                                                     PModeLegErrorHandling.class);
    final PModeLegProtocol aProtocol = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PROTOCOL),
                                                                           PModeLegProtocol.class);
    final PModeLegReliability aReliability = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RELIABILITY),
                                                                                 PModeLegReliability.class);
    final PModeLegSecurity aSecurity = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_SECURITY),
                                                                           PModeLegSecurity.class);
    return new PModeLeg (aBusinessInformation, aErrorHandling, aProtocol, aReliability, aSecurity);
  }

}
