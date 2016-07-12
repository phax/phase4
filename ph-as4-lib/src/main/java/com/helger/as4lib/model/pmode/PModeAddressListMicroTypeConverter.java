package com.helger.as4lib.model.pmode;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeAddressListMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_ADDRESSES = "Addresses";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegSecurity aValue = (PModeLegSecurity) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sEncrypt : aValue.getX509EncryptionEncrypt ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_ADDRESSES).appendText (sEncrypt);
    }
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final ICommonsList <String> aAddresses = new CommonsArrayList<> ();
    for (final IMicroElement aEncryptElement : aElement.getAllChildElements (ELEMENT_ADDRESSES))
    {
      aAddresses.add (aEncryptElement.getTextContentTrimmed ());
    }

    return new PModeAddressList (aAddresses);
  }

}
