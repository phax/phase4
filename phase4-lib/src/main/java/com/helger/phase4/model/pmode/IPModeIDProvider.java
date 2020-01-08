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

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * A nice little helper interface to dynamically create new PMode IDs based on
 * Initiator ID and Responder ID.
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IPModeIDProvider extends Serializable
{
  /**
   * The default implementation combines initiator ID and responder ID with a
   * minus sign
   */
  static IPModeIDProvider DEFAULT_DYNAMIC = (i, r) -> i + "-" + r;

  /**
   * Create a PMode ID from initiator ID and responder ID.
   * 
   * @param sInitiatorID
   *        Non-<code>null</code> and non-empty initiator ID.
   * @param sResponderID
   *        Non-<code>null</code> and non-empty responder ID.
   * @return The created non-<code>null</code> PMode ID.
   */
  @Nonnull
  String getPModeID (@Nonnull @Nonempty String sInitiatorID, @Nonnull @Nonempty String sResponderID);
}
