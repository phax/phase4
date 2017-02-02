/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.partner;

import java.util.Map;

import com.helger.as4.util.StringMap;
import com.helger.photon.security.object.AbstractObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public final class PartnerMicroTypeConverter extends AbstractObjectMicroTypeConverter
{
  private static final String ELEMENT_PARTNER_ATTRIBUTES = "PartnerAttribute";
  private static final String ATTR_NAME = "name";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final Partner aValue = (Partner) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);

    for (final Map.Entry <String, String> aEntry : aValue.getAllAttributes ().entrySet ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_PARTNER_ATTRIBUTES)
         .setAttribute (ATTR_NAME, aEntry.getKey ())
         .appendText (aEntry.getValue ());
    }

    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final StringMap aSM = new StringMap ();

    for (final IMicroElement aAttribute : aElement.getAllChildElements (ELEMENT_PARTNER_ATTRIBUTES))
    {
      final String sName = aAttribute.getAttributeValue (ATTR_NAME);
      aSM.setAttribute (sName, aAttribute.getTextContentTrimmed ());
    }

    return new Partner (getStubObject (aElement), aSM);
  }

}
