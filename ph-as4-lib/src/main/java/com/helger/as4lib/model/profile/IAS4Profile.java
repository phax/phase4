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
package com.helger.as4lib.model.profile;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.commons.id.IHasID;
import com.helger.commons.name.IHasDisplayName;

public interface IAS4Profile extends IHasID <String>, IHasDisplayName, Serializable
{
  /**
   * @return A non-<code>null</<code> validator. May not validate anything but
   *         must be present.
   */
  @Nonnull
  IAS4ProfileValidator getValidator ();

  /**
   * @return A PMode config that is NOT yet in the manager!
   */
  @Nonnull
  IPModeConfig createDefaultPModeConfig ();
}
