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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.string.StringHelper;
import com.helger.tenancy.IBusinessObject;

/**
 * Base read-only interface for a single {@link PMode}.
 *
 * @author Philip Helger
 */
public interface IPMode extends IBusinessObject
{
  @Nullable
  PModeParty getInitiator ();

  default boolean hasInitiator ()
  {
    return getInitiator () != null;
  }

  @Nullable
  default String getInitiatorID ()
  {
    final PModeParty aParty = getInitiator ();
    return aParty == null ? null : aParty.getID ();
  }

  default boolean hasInitiatorID (@Nullable final String sID)
  {
    return EqualsHelper.equals (sID, getInitiatorID ());
  }

  @Nullable
  PModeParty getResponder ();

  default boolean hasResponder ()
  {
    return getResponder () != null;
  }

  @Nullable
  default String getResponderID ()
  {
    final PModeParty aParty = getResponder ();
    return aParty == null ? null : aParty.getID ();
  }

  default boolean hasResponderID (@Nullable final String sID)
  {
    return EqualsHelper.equals (sID, getResponderID ());
  }

  @Nullable
  String getAgreement ();

  default boolean hasAgreement ()
  {
    return StringHelper.hasText (getAgreement ());
  }

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

  default boolean hasLeg1 ()
  {
    return getLeg1 () != null;
  }

  @Nullable
  PModeLeg getLeg2 ();

  default boolean hasLeg2 ()
  {
    return getLeg2 () != null;
  }

  @Nullable
  PModePayloadService getPayloadService ();

  default boolean hasPayloadService ()
  {
    return getPayloadService () != null;
  }

  @Nullable
  PModeReceptionAwareness getReceptionAwareness ();

  default boolean hasReceptionAwareness ()
  {
    return getReceptionAwareness () != null;
  }
}
