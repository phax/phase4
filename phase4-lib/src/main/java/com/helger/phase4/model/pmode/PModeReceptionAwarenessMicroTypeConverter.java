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

import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;

/**
 * XML converter for objects of class {@link PModeReceptionAwareness}.
 *
 * @author Philip Helger
 */
public class PModeReceptionAwarenessMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeReceptionAwareness>
{
  private static final IMicroQName ATTR_RECEPTION_AWARENESS = new MicroQName ("ReceptionAwareness");
  private static final IMicroQName ATTR_RETRY = new MicroQName ("Retry");
  private static final IMicroQName ATTR_MAX_RETRIES = new MicroQName ("MaxRetries");
  private static final IMicroQName ATTR_RETRY_INTERVAL_MS = new MicroQName ("RetryIntervalMS");
  private static final IMicroQName ATTR_DUPLICATE_DETECTION = new MicroQName ("DuplicateDetection");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeReceptionAwareness aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    if (aValue.isReceptionAwarenessDefined ())
      ret.setAttribute (ATTR_RECEPTION_AWARENESS, aValue.isReceptionAwareness ());
    if (aValue.isRetryDefined ())
      ret.setAttribute (ATTR_RETRY, aValue.isRetry ());
    ret.setAttribute (ATTR_MAX_RETRIES, aValue.getMaxRetries ());
    ret.setAttribute (ATTR_RETRY_INTERVAL_MS, aValue.getRetryIntervalMS ());
    if (aValue.isDuplicateDetectionDefined ())
      ret.setAttribute (ATTR_DUPLICATE_DETECTION, aValue.isDuplicateDetection ());
    return ret;
  }

  @Nonnull
  public PModeReceptionAwareness convertToNative (final IMicroElement aElement)
  {
    final ETriState eReceptionAwareness = getTriState (aElement.getAttributeValue (ATTR_RECEPTION_AWARENESS),
                                                       PModeReceptionAwareness.DEFAULT_RECEPTION_AWARENESS);
    final ETriState eRetry = getTriState (aElement.getAttributeValue (ATTR_RETRY),
                                          PModeReceptionAwareness.DEFAULT_RETRY);
    final int nMaxRetries = aElement.getAttributeValueAsInt (ATTR_MAX_RETRIES,
                                                             PModeReceptionAwareness.DEFAULT_MAX_RETRIES);
    // Was a typo
    final long nRetryIntervalMS = aElement.getAttributeValueAsLong (ATTR_RETRY_INTERVAL_MS,
                                                                    aElement.getAttributeValueAsLong ("MayRetries",
                                                                                                      PModeReceptionAwareness.DEFAULT_RETRY_INTERVAL_MS));
    final ETriState eDuplicateDetection = getTriState (aElement.getAttributeValue (ATTR_DUPLICATE_DETECTION),
                                                       PModeReceptionAwareness.DEFAULT_DUPLICATE_DETECTION);
    return new PModeReceptionAwareness (eReceptionAwareness,
                                        eRetry,
                                        nMaxRetries,
                                        nRetryIntervalMS,
                                        eDuplicateDetection);
  }
}
