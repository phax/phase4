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
package com.helger.as4.duplicate;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * Micro type converter for class {@link AS4DuplicateItem}.
 * 
 * @author Philip Helger
 */
public final class AS4DuplicateItemMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_DT = "dt";
  private static final String ATTR_MESSAGE_ID = "msgid";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final AS4DuplicateItem aValue = (AS4DuplicateItem) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttributeWithConversion (ATTR_DT, aValue.getDateTime ());
    ret.setAttribute (ATTR_MESSAGE_ID, aValue.getMessageID ());
    return ret;
  }

  @Nonnull
  public AS4DuplicateItem convertToNative (@Nonnull final IMicroElement aElement)
  {
    final LocalDateTime aLDT = aElement.getAttributeValueWithConversion (ATTR_DT, LocalDateTime.class);
    final String sMsgID = aElement.getAttributeValue (ATTR_MESSAGE_ID);
    return new AS4DuplicateItem (aLDT, sMsgID);
  }
}
