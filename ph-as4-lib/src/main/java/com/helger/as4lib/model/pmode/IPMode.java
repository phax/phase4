/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import javax.annotation.Nullable;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.ETransportChannelBinding;
import com.helger.photon.basic.object.IObject;

public interface IPMode extends IObject
{
  @Nullable
  String getAgreement ();

  @Nullable
  EMEP getMEP ();

  @Nullable
  default String getMEPID ()
  {
    final EMEP eMEP = getMEP ();
    return eMEP == null ? null : eMEP.getID ();
  }

  @Nullable
  ETransportChannelBinding getMEPBinding ();

  @Nullable
  default String getMEPBindingID ()
  {
    final ETransportChannelBinding eMEPBinding = getMEPBinding ();
    return eMEPBinding == null ? null : eMEPBinding.getID ();
  }

  @Nullable
  PModeParty getInitiator ();

  @Nullable
  PModeParty getResponder ();

  @Nullable
  PModeLeg getLeg1 ();

  @Nullable
  PModeLeg getLeg2 ();
}
