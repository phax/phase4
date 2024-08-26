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

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;

/**
 * Interface for a manager for {@link PMode} objects.
 *
 * @author Philip Helger
 */
public interface IPModeManager
{
  /**
   * Create a new PMode.
   *
   * @param aPMode
   *        The PMode to be created. May not be <code>null</code>.
   */
  void createPMode (@Nonnull PMode aPMode);

  /**
   * Update an existing PMode.
   *
   * @param aPMode
   *        The PMode to be updated. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if something changed,
   *         {@link EChange#UNCHANGED} otherwise.
   */
  @Nonnull
  EChange updatePMode (@Nonnull IPMode aPMode);

  /**
   * Create or update the provided PMode.
   *
   * @param aPMode
   *        The PMode to be created or updated.
   */
  void createOrUpdatePMode (@Nonnull PMode aPMode);

  /**
   * Mark the provided PMode as deleted.
   *
   * @param sPModeID
   *        The ID of the PMode to be marked as deleted. May be
   *        <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  EChange markPModeDeleted (@Nullable String sPModeID);

  /**
   * Delete the provided PMode.
   *
   * @param sPModeID
   *        The ID of the PMode to be deleted. May be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  EChange deletePMode (@Nullable String sPModeID);

  /**
   * Find the first PMode matching the provided filter.
   *
   * @param aFilter
   *        The filter to be used. May not be <code>null</code>.
   * @return <code>null</code> if no such PMode exists.
   */
  @Nullable
  IPMode findFirst (@Nonnull Predicate <? super IPMode> aFilter);

  /**
   * Find the first PMode that has the provided service and action.
   *
   * @param sService
   *        The service to be searched. May be <code>null</code>.
   * @param sAction
   *        The action to be searched. May be <code>null</code>.
   * @return <code>null</code> if no such PMode exists.
   */
  @Nullable
  default IPMode getPModeOfServiceAndAction (@Nullable final String sService, @Nullable final String sAction)
  {
    return findFirst (x -> {
      final PModeLeg aLeg = x.getLeg1 ();
      if (aLeg != null)
      {
        final PModeLegBusinessInformation aBI = aLeg.getBusinessInfo ();
        if (aBI != null)
          return EqualsHelper.equals (aBI.getService (), sService) && EqualsHelper.equals (aBI.getAction (), sAction);
      }
      return false;
    });
  }

  /**
   * Get a predicate that matches a PMode by ID or initiator and responder
   * together.
   *
   * @param sID
   *        PMode ID to search. May be <code>null</code>.
   * @param aInitiator
   *        Initiator to search. May be <code>null</code>.
   * @param aResponder
   *        Responder to search. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  static Predicate <IPMode> getPModeFilter (@Nonnull final String sID,
                                            @Nullable final PModeParty aInitiator,
                                            @Nullable final PModeParty aResponder)
  {
    // The same PMode exists either if the ID is identical or if Initiator and
    // Responder are identical
    return x -> x.getID ().equals (sID) ||
                (EqualsHelper.equals (x.getInitiator (), aInitiator) &&
                 EqualsHelper.equals (x.getResponder (), aResponder));
  }

  /**
   * Find the PMode with the provided ID
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such PMode exists.
   */
  @Nullable
  IPMode getPModeOfID (@Nullable String sID);

  /**
   * @return A non-<code>null</code> but maybe empty list of all contained
   *         PModes.
   */
  @Nonnull
  ICommonsList <IPMode> getAll ();

  /**
   * @return A non-<code>null</code> but maybe empty set of the IDs of all
   *         contained PModes.
   */
  @Nonnull
  ICommonsSet <String> getAllIDs ();

  /**
   * Validate, that the provided PMode domain object is consistent according to
   * the underlying requirements.
   *
   * @param aPMode
   *        The PMode to be validated. May be <code>null</code>.
   * @throws PModeValidationException
   *         in case the PMode is invalid.
   */
  default void validatePMode (@Nullable final IPMode aPMode) throws PModeValidationException
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    // Needs ID
    if (StringHelper.hasNoText (aPMode.getID ()))
      throw new PModeValidationException ("No PMode ID present");

    final PModeParty aInitiator = aPMode.getInitiator ();
    if (aInitiator != null)
    {
      // INITIATOR PARTY_ID
      if (StringHelper.hasNoText (aInitiator.getIDValue ()))
        throw new PModeValidationException ("No PMode Initiator ID present");

      // INITIATOR ROLE
      if (StringHelper.hasNoText (aInitiator.getRole ()))
        throw new PModeValidationException ("No PMode Initiator Role present");
    }

    final PModeParty aResponder = aPMode.getResponder ();
    if (aResponder != null)
    {
      // RESPONDER PARTY_ID
      if (StringHelper.hasNoText (aResponder.getIDValue ()))
        throw new PModeValidationException ("No PMode Responder ID present");

      // RESPONDER ROLE
      if (StringHelper.hasNoText (aResponder.getRole ()))
        throw new PModeValidationException ("No PMode Responder Role present");
    }

    if (aResponder == null && aInitiator == null)
      throw new PModeValidationException ("PMode is missing Initiator and/or Responder");
  }

  /**
   * Validate all contained PModes at once.
   *
   * @throws PModeValidationException
   *         In case at least one PMode is invalid
   */
  default void validateAllPModes () throws PModeValidationException
  {
    for (final IPMode aPMode : getAll ())
      validatePMode (aPMode);
  }
}
