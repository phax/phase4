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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public final class PModePayloadServiceMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ELEMENT_COMPRESSION_MODE = "CompressionMode";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final PModePayloadService aValue = (PModePayloadService) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getCompressionMode (),
                                                               sNamespaceURI,
                                                               ELEMENT_COMPRESSION_MODE));
    return ret;
  }

  @Nonnull
  public PModePayloadService convertToNative (@Nonnull final IMicroElement aElement)
  {
    return new PModePayloadService (MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_COMPRESSION_MODE),
                                                                        EAS4CompressionMode.class));

  }
}
