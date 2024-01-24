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

/**
 * A nice little helper interface to dynamically create new PMode IDs based on
 * Initiator ID and Responder ID.
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IPModeIDProvider
{
  /**
   * The default implementation combines initiator ID value and responder ID
   * value with a minus sign
   */
  IPModeIDProvider DEFAULT_DYNAMIC = (i, r) -> i.getIDValue () + "-" + r.getIDValue ();

  /**
   * Create a PMode ID from initiator and responder. This was changed in v2.2.0
   * to use the full PMode party instead of just the ID.
   *
   * @param aInitiator
   *        Non-<code>null</code> initiator party.
   * @param aResponder
   *        Non-<code>null</code> responder party.
   * @return The created non-<code>null</code> PMode ID.
   */
  @Nonnull
  String getPModeID (@Nonnull PModeParty aInitiator, @Nonnull PModeParty aResponder);
}
