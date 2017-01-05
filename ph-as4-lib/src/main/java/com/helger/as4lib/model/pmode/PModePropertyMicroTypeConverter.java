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
package com.helger.as4lib.model.pmode;

import com.helger.commons.state.EMandatory;
import com.helger.commons.string.StringParser;
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
    final PModeProperty aValue = (PModeProperty) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_NAME, aValue.getName ());
    ret.setAttribute (ATTR_DESCRIPTION, aValue.getDescription ());
    ret.setAttribute (ATTR_DATA_TYPE, aValue.getDataType ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isMandatory ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final String sDescription = aElement.getAttributeValue (ATTR_DESCRIPTION);
    final String sDataType = aElement.getAttributeValue (ATTR_DATA_TYPE);
    final EMandatory eMandatory = EMandatory.valueOf (StringParser.parseBool (aElement.getAttributeValue (ATTR_MANDATORY),
                                                                              false));
    return new PModeProperty (sName, sDescription, sDataType, eMandatory);
  }

}
