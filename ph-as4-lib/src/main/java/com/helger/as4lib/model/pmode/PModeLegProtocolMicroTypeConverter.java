package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeLegProtocolMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_ADDRESS = "Address";
  private static final String ATTR_SOAP_VERSION = "SOAPVersion";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final PModeLegProtocol aValue = (PModeLegProtocol) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ADDRESS, aValue.getAddress ());
    ret.setAttribute (ATTR_SOAP_VERSION, aValue.getSOAPVersion ().getVersion ());
    return ret;
  }

  @Nonnull
  public PModeLegProtocol convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sAddress = aElement.getAttributeValue (ATTR_ADDRESS);

    final String sSOAPVersion = aElement.getAttributeValue (ATTR_SOAP_VERSION);
    final ESOAPVersion eSOAPVersion = ESOAPVersion.getFromVersionOrDefault (sSOAPVersion, ESOAPVersion.AS4_DEFAULT);

    return new PModeLegProtocol (sAddress, eSOAPVersion);
  }
}
