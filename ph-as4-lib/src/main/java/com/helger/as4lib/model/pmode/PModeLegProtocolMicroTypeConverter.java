package com.helger.as4lib.model.pmode;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeLegProtocolMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_ADDRESS = "Address";
  private static final String ATTR_SOAP_VERSION = "SOAPVersion";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegProtocol aValue = (PModeLegProtocol) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ADDRESS, aValue.getAddress ());
    ret.setAttribute (ATTR_SOAP_VERSION, aValue.getSOAPVersion ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {

    final String sAddress = aElement.getAttributeValue (ATTR_ADDRESS);

    final String sSOAPVersion = aElement.getAttributeValue (ATTR_SOAP_VERSION);

    return new PModeLegProtocol (sAddress, sSOAPVersion);
  }

}
