package com.helger.as4lib.model.pmode;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

public class PModeLegSecurityMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_X509_ENCRYPTION_ENCRYPT = "X509EncryptionEncrypt";
  private static final String ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH = "X509EncryptionMinimumStrength";
  private static final String ELEMENT_X509_SIGN = "X509Sign";
  private static final String ATTR_PMODE_AUTHORIZE = "PModeAuthorize";
  private static final String ATTR_PASSWORD = "Password";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeParty aValue = (PModeParty) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_X509_ENCRYPTION_ENCRYPT, aValue.getIDType ());
    ret.setAttribute (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH, aValue.getIDValue ());
    ret.setAttribute (ATTR_X509_SIGN, aValue.getRole ());
    ret.setAttribute (ATTR_PMODE_AUTHORIZE, aValue.getUserName ());
    ret.setAttribute (ATTR_PASSWORD, aValue.getPassword ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final String sIDType = aElement.getAttributeValue (ATTR_X509_ENCRYPTION_ENCRYPT);
    final String sIDValue = aElement.getAttributeValue (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH);
    final String sRole = aElement.getAttributeValue (ATTR_X509_SIGN);
    final String sUserName = aElement.getAttributeValue (ATTR_PMODE_AUTHORIZE);
    final String sPassword = aElement.getAttributeValue (ATTR_PASSWORD);
    return new PModeParty (sIDType, sIDValue, sRole, sUserName, sPassword);
  }

}
