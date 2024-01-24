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
package com.helger.phase4.duplicate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ContainsSoftMigration;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * Micro type converter for class {@link AS4DuplicateItem}.
 *
 * @author Philip Helger
 */
public final class AS4DuplicateItemMicroTypeConverter implements IMicroTypeConverter <AS4DuplicateItem>
{
  private static final String ATTR_DT = "dt";
  private static final String ATTR_MESSAGE_ID = "msgid";
  private static final String ATTR_PROFILE_ID = "profileid";
  private static final String ATTR_PMODE_ID = "pmodeid";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final AS4DuplicateItem aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttributeWithConversion (ATTR_DT, aValue.getDateTime ());
    ret.setAttribute (ATTR_MESSAGE_ID, aValue.getMessageID ());
    ret.setAttribute (ATTR_PROFILE_ID, aValue.getProfileID ());
    ret.setAttribute (ATTR_PMODE_ID, aValue.getPModeID ());
    return ret;
  }

  @Nonnull
  @ContainsSoftMigration
  public AS4DuplicateItem convertToNative (@Nonnull final IMicroElement aElement)
  {
    OffsetDateTime aODT = aElement.getAttributeValueWithConversion (ATTR_DT, OffsetDateTime.class);
    if (aODT == null)
    {
      // Soft migration
      final LocalDateTime aLDT = aElement.getAttributeValueWithConversion (ATTR_DT, LocalDateTime.class);
      if (aLDT != null)
        aODT = OffsetDateTime.of (aLDT, ZoneOffset.UTC);
    }
    final String sMsgID = aElement.getAttributeValue (ATTR_MESSAGE_ID);
    final String sProfileID = aElement.getAttributeValue (ATTR_PROFILE_ID);
    final String sPModeID = aElement.getAttributeValue (ATTR_PMODE_ID);
    return new AS4DuplicateItem (aODT, sMsgID, sProfileID, sPModeID);
  }
}
