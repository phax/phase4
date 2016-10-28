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
package com.helger.as4lib.model.pmode;

import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeReceptionAwarenessMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_RECEPTION_AWARENESS = "WSSVersion";
  private static final String ATTR_RETRY = "X509Sign";
  private static final String ATTR_DOUBLE_DETECTION = "X509SignatureCertificate";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeReceptionAwareness aValue = (PModeReceptionAwareness) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    if (aValue.isReceptionAwarenessDefined ())
      ret.setAttribute (ATTR_RECEPTION_AWARENESS, aValue.isReceptionAwareness ());
    if (aValue.isRetryDefined ())
      ret.setAttribute (ATTR_RETRY, aValue.isRetry ());
    if (aValue.isDuplicateDetectionDefined ())
      ret.setAttribute (ATTR_DOUBLE_DETECTION, aValue.isDuplicateDetection ());

    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {

    final ETriState eReceptionAwareness = getTriState (aElement.getAttributeValue (ATTR_RECEPTION_AWARENESS),
                                                       PModeReceptionAwareness.DEFAULT_RECEPTION_AWARENESS);
    final ETriState eRetry = getTriState (aElement.getAttributeValue (ATTR_RETRY),
                                          PModeReceptionAwareness.DEFAULT_RETRY);
    final ETriState eDoubleDetection = getTriState (aElement.getAttributeValue (ATTR_DOUBLE_DETECTION),
                                                    PModeReceptionAwareness.DEFAULT_DUPLICATE_DETECTION);

    return new PModeReceptionAwareness (eReceptionAwareness, eRetry, eDoubleDetection);
  }

}
