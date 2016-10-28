/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.lang.GenericReflection;
import com.helger.photon.security.object.AbstractObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class AS4ProfileMicroTypeConverter extends AbstractObjectMicroTypeConverter
{
  private static final String ATTR_DISPLAYNAME = "Displayname";
  private static final String ATTR_CLASS = "Class";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final AS4Profile aValue = (AS4Profile) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ID, aValue.getID ());
    ret.setAttribute (ATTR_DISPLAYNAME, aValue.getDisplayName ());
    ret.setAttribute (ATTR_CLASS, aValue.getValidatorClassName ());
    return ret;
  }

  @Nonnull
  public AS4Profile convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sID = aElement.getAttributeValue (ATTR_ID);
    final String sDisplayName = aElement.getAttributeValue (ATTR_DISPLAYNAME);
    final String sClass = aElement.getAttributeValue (ATTR_CLASS);

    return new AS4Profile (sID, sDisplayName, GenericReflection.getClassFromNameSafe (sClass));

  }
}
