package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

public final class PModePartyMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_ID_TYPE = "IDType";
  private static final String ATTR_ID_VALUE = "IDValue";
  private static final String ATTR_ROLE = "Role";
  private static final String ATTR_USER_NAME = "Username";
  private static final String ATTR_PASSWORD = "Password";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final PModeParty aValue = (PModeParty) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ID_TYPE, aValue.getIDType ());
    ret.setAttribute (ATTR_ID_VALUE, aValue.getIDValue ());
    ret.setAttribute (ATTR_ROLE, aValue.getRole ());
    ret.setAttribute (ATTR_USER_NAME, aValue.getUserName ());
    ret.setAttribute (ATTR_PASSWORD, aValue.getPassword ());
    return ret;
  }

  @Nonnull
  public PModeParty convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sIDType = aElement.getAttributeValue (ATTR_ID_TYPE);
    final String sIDValue = aElement.getAttributeValue (ATTR_ID_VALUE);
    final String sRole = aElement.getAttributeValue (ATTR_ROLE);
    final String sUserName = aElement.getAttributeValue (ATTR_USER_NAME);
    final String sPassword = aElement.getAttributeValue (ATTR_PASSWORD);
    return new PModeParty (sIDType, sIDValue, sRole, sUserName, sPassword);
  }
}
