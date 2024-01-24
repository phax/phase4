/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * XML converter for objects of class {@link PModeParty}.
 *
 * @author Philip Helger
 */
public final class PModePartyMicroTypeConverter implements IMicroTypeConverter <PModeParty>
{
  private static final IMicroQName ATTR_ID_TYPE = new MicroQName ("IDType");
  private static final IMicroQName ATTR_ID_VALUE = new MicroQName ("IDValue");
  private static final IMicroQName ATTR_ROLE = new MicroQName ("Role");
  private static final IMicroQName ATTR_USER_NAME = new MicroQName ("Username");
  private static final IMicroQName ATTR_PASSWORD = new MicroQName ("Password");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeParty aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
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
