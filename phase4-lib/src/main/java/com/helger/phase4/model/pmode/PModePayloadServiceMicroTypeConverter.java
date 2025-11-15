/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * XML converter for objects of class {@link PModePayloadService}.
 * 
 * @author Philip Helger
 */
public final class PModePayloadServiceMicroTypeConverter implements IMicroTypeConverter <PModePayloadService>
{
  private static final IMicroQName ATTR_COMPRESSION_MODE = new MicroQName ("CompressionMode");

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final PModePayloadService aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_COMPRESSION_MODE, aValue.getCompressionModeID ());
    return ret;
  }

  @NonNull
  public PModePayloadService convertToNative (@NonNull final IMicroElement aElement)
  {
    final String sCompressionModeID = aElement.getAttributeValue (ATTR_COMPRESSION_MODE);
    final EAS4CompressionMode eCompressionMode = EAS4CompressionMode.getFromIDOrNull (sCompressionModeID);
    if (sCompressionModeID != null && eCompressionMode == null)
      throw new IllegalStateException ("Invalid compression mode ID '" + sCompressionModeID + "' provided!");

    return new PModePayloadService (eCompressionMode);
  }
}
