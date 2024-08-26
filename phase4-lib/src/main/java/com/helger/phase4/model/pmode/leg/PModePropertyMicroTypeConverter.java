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
package com.helger.phase4.model.pmode.leg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.EMandatory;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;

/**
 * XML converter for objects of class {@link PModeProperty}.
 *
 * @author Philip Helger
 */
public class PModePropertyMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeProperty>
{
  private static final IMicroQName ATTR_NAME = new MicroQName ("Name");
  private static final IMicroQName ATTR_DESCRIPTION = new MicroQName ("Description");
  private static final IMicroQName ATTR_DATA_TYPE = new MicroQName ("DataType");
  private static final IMicroQName ATTR_MANDATORY = new MicroQName ("Mandatory");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeProperty aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_NAME, aValue.getName ());
    ret.setAttribute (ATTR_DESCRIPTION, aValue.getDescription ());
    ret.setAttribute (ATTR_DATA_TYPE, aValue.getDataType ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isMandatory ());
    return ret;
  }

  @Nonnull
  public PModeProperty convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final String sDescription = aElement.getAttributeValue (ATTR_DESCRIPTION);
    final String sDataType = aElement.getAttributeValue (ATTR_DATA_TYPE);
    final EMandatory eMandatory = EMandatory.valueOf (aElement.getAttributeValueAsBool (ATTR_MANDATORY,
                                                                                        PModeProperty.DEFAULT_MANDATORY));

    return new PModeProperty (sName, sDescription, sDataType, eMandatory);
  }
}
