/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.profile;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.name.IHasDisplayName;

/**
 * Base interface for an AS4 profile - a group of settings that outline what
 * features of AS4 are used.
 *
 * @author Philip Helger
 */
public interface IAS4Profile extends IHasID <String>, IHasDisplayName, Serializable
{
  /**
   * @return An optional validator. May be <code>null</code>.
   */
  @Nullable
  IAS4ProfileValidator getValidator ();

  /**
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sAddress
   *        Address string
   * @return A PMode that is NOT yet in the manager and is not complete! The
   *         following information is most likely not contained: initiator,
   *         responder, URLs, certificates.
   */
  @Nonnull
  PMode createPModeTemplate (@Nonnull @Nonempty String sInitiatorID,
                             @Nonnull @Nonempty String sResponderID,
                             @Nullable String sAddress);

  @Nonnull
  IPModeIDProvider getPModeIDProvider ();

  boolean isDeprecated ();
}
