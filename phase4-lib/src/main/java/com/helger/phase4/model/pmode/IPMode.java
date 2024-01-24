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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.string.StringHelper;
import com.helger.json.IJsonObject;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.tenancy.IBusinessObject;

/**
 * Base read-only interface for a single {@link PMode}.
 *
 * @author Philip Helger
 */
public interface IPMode extends IBusinessObject
{
  /**
   * @return The initiator party. May be <code>null</code>.
   */
  @Nullable
  PModeParty getInitiator ();

  /**
   * @return <code>true</code> if an initiator party is present,
   *         <code>false</code> if not.
   */
  default boolean hasInitiator ()
  {
    return getInitiator () != null;
  }

  /**
   * @return The initiator party ID or <code>null</code> if no initiator is
   *         present.
   * @see #getInitiator()
   */
  @Nullable
  default String getInitiatorID ()
  {
    final PModeParty aParty = getInitiator ();
    return aParty == null ? null : aParty.getID ();
  }

  /**
   * Check if this PMode has the provided initiator party ID.
   *
   * @param sID
   *        The ID to check against. May be <code>null</code>.
   * @return <code>true</code> if this PMode has the provided initiator ID,
   *         <code>false</code> if not. If <code>null</code> is passed and no
   *         initiator party is present, the result will be <code>true</code>.
   * @see #getInitiatorID()
   */
  default boolean hasInitiatorID (@Nullable final String sID)
  {
    return EqualsHelper.equals (sID, getInitiatorID ());
  }

  /**
   * @return The responder party. May be <code>null</code>.
   */
  @Nullable
  PModeParty getResponder ();

  /**
   * @return <code>true</code> if an responder party is present,
   *         <code>false</code> if not.
   */
  default boolean hasResponder ()
  {
    return getResponder () != null;
  }

  /**
   * @return The responder party ID or <code>null</code> if no responder is
   *         present.
   * @see #getResponder()
   */
  @Nullable
  default String getResponderID ()
  {
    final PModeParty aParty = getResponder ();
    return aParty == null ? null : aParty.getID ();
  }

  /**
   * Check if this PMode has the provided responder party ID.
   *
   * @param sID
   *        The ID to check against. May be <code>null</code>.
   * @return <code>true</code> if this PMode has the provided responder ID,
   *         <code>false</code> if not. If <code>null</code> is passed and no
   *         responder party is present, the result will be <code>true</code>.
   * @see #getResponderID()
   */
  default boolean hasResponderID (@Nullable final String sID)
  {
    return EqualsHelper.equals (sID, getResponderID ());
  }

  /**
   * @return The PMode agreement to use. May be <code>null</code>.
   */
  @Nullable
  String getAgreement ();

  /**
   * @return <code>true</code> if a PMode agreement is present,
   *         <code>false</code> if not.
   */
  default boolean hasAgreement ()
  {
    return StringHelper.hasText (getAgreement ());
  }

  /**
   * @return The Message Exchange Profile (MEP) to be used. May not be
   *         <code>null</code>.
   */
  @Nonnull
  EMEP getMEP ();

  /**
   * @return The ID of the Message Exchange Profile to be used. May neither be
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  default String getMEPID ()
  {
    return getMEP ().getID ();
  }

  /**
   * @return The MEP binding to be used. May not be <code>null</code>.
   */
  @Nonnull
  EMEPBinding getMEPBinding ();

  /**
   * @return The ID of the MEP binding to be used. May neither be
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  default String getMEPBindingID ()
  {
    return getMEPBinding ().getID ();
  }

  /**
   * @return The first leg of the PMode for the first interaction. May be
   *         <code>null</code>.
   */
  @Nullable
  PModeLeg getLeg1 ();

  /**
   * @return <code>true</code> if this PMode has a first leg, <code>false</code>
   *         if not.
   */
  default boolean hasLeg1 ()
  {
    return getLeg1 () != null;
  }

  /**
   * @return The second leg of the PMode for the first interaction. May be
   *         <code>null</code>.
   */
  @Nullable
  PModeLeg getLeg2 ();

  /**
   * @return <code>true</code> if this PMode has a second leg,
   *         <code>false</code> if not.
   */
  default boolean hasLeg2 ()
  {
    return getLeg2 () != null;
  }

  /**
   * @return The PMode payload service. May be <code>null</code>.
   */
  @Nullable
  PModePayloadService getPayloadService ();

  /**
   * @return <code>true</code> if the PMode payload service is set,
   *         <code>false</code> if not.
   */
  default boolean hasPayloadService ()
  {
    return getPayloadService () != null;
  }

  /**
   * @return The PMode reception awareness. May be <code>null</code>.
   */
  @Nullable
  PModeReceptionAwareness getReceptionAwareness ();

  /**
   * @return <code>true</code> if this PMode reception awareness is set,
   *         <code>false</code> if not.
   */
  default boolean hasReceptionAwareness ()
  {
    return getReceptionAwareness () != null;
  }

  /**
   * @return The JSON representation of the PMode. Never <code>null</code>.
   * @since 0.12.0
   */
  @Nonnull
  default IJsonObject getAsJson ()
  {
    return PModeJsonConverter.convertToJson (this);
  }
}
