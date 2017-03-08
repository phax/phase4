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
package com.helger.as4.model.pmode.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.commons.annotation.Nonempty;
import com.helger.photon.basic.object.IObject;

public interface IPModeConfig extends IObject
{
  @Nullable
  String getAgreement ();

  @Nonnull
  EMEP getMEP ();

  @Nonnull
  @Nonempty
  default String getMEPID ()
  {
    return getMEP ().getID ();
  }

  @Nonnull
  EMEPBinding getMEPBinding ();

  @Nonnull
  @Nonempty
  default String getMEPBindingID ()
  {
    return getMEPBinding ().getID ();
  }

  @Nullable
  PModeLeg getLeg1 ();

  @Nullable
  PModeLeg getLeg2 ();

  @Nullable
  PModePayloadService getPayloadService ();

  @Nullable
  PModeReceptionAwareness getReceptionAwareness ();
}
