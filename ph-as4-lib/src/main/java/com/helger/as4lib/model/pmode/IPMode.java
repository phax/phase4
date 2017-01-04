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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.model.pmode.config.IPModeConfig;
import com.helger.commons.annotation.Nonempty;
import com.helger.photon.basic.object.IObject;

public interface IPMode extends IObject
{
  @Nullable
  PModeParty getInitiator ();

  @Nullable
  default String getInitiatorID ()
  {
    final PModeParty aParty = getInitiator ();
    return aParty == null ? null : aParty.getID ();
  }

  @Nullable
  PModeParty getResponder ();

  @Nullable
  default String getResponderID ()
  {
    final PModeParty aParty = getResponder ();
    return aParty == null ? null : aParty.getID ();
  }

  @Nonnull
  IPModeConfig getConfig ();

  @Nonnull
  @Nonempty
  default String getConfigID ()
  {
    return getConfig ().getID ();
  }
}
