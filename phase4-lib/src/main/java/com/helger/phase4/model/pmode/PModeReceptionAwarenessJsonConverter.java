/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.state.ETriState;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * JSON converter for objects of class {@link PModeReceptionAwareness}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public class PModeReceptionAwarenessJsonConverter
{
  private static final String ATTR_RECEPTION_AWARENESS = "ReceptionAwareness";
  private static final String ATTR_RETRY = "Retry";
  private static final String ATTR_MAX_RETRIES = "MaxRetries";
  private static final String ATTR_RETRY_INTERVAL_MS = "RetryIntervalMS";
  private static final String ATTR_DUPLICATE_DETECTION = "DuplicateDetection";

  private PModeReceptionAwarenessJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeReceptionAwareness aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.isReceptionAwarenessDefined ())
      ret.add (ATTR_RECEPTION_AWARENESS, aValue.isReceptionAwareness ());
    if (aValue.isRetryDefined ())
      ret.add (ATTR_RETRY, aValue.isRetry ());
    ret.add (ATTR_MAX_RETRIES, aValue.getMaxRetries ());
    ret.add (ATTR_RETRY_INTERVAL_MS, aValue.getRetryIntervalMS ());
    if (aValue.isDuplicateDetectionDefined ())
      ret.add (ATTR_DUPLICATE_DETECTION, aValue.isDuplicateDetection ());
    return ret;
  }

  @Nonnull
  public static PModeReceptionAwareness convertToNative (final IJsonObject aElement)
  {
    final ETriState eReceptionAwareness = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_RECEPTION_AWARENESS),
                                                                                       PModeReceptionAwareness.DEFAULT_RECEPTION_AWARENESS);
    final ETriState eRetry = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_RETRY),
                                                                          PModeReceptionAwareness.DEFAULT_RETRY);
    final int nMaxRetries = aElement.getAsInt (ATTR_MAX_RETRIES, PModeReceptionAwareness.DEFAULT_MAX_RETRIES);
    final int nRetryIntervalMS = aElement.getAsInt (ATTR_RETRY_INTERVAL_MS,
                                                    PModeReceptionAwareness.DEFAULT_RETRY_INTERVAL_MS);
    final ETriState eDuplicateDetection = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_DUPLICATE_DETECTION),
                                                                                       PModeReceptionAwareness.DEFAULT_DUPLICATE_DETECTION);
    return new PModeReceptionAwareness (eReceptionAwareness,
                                        eRetry,
                                        nMaxRetries,
                                        nRetryIntervalMS,
                                        eDuplicateDetection);
  }
}
