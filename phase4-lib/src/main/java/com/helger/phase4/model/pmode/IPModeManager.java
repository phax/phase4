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
package com.helger.phase4.model.pmode;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.functional.IPredicate;
import com.helger.commons.state.EChange;
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
  @Nonnull
  default void createOrUpdatePMode (@Nonnull final PMode aPMode)
  {
    final IPMode aExisting = findFirst (getPModeFilter (aPMode.getID (),
                                                        aPMode.getInitiatorID (),
                                                        aPMode.getResponderID ()));
    if (aExisting == null)
      createPMode (aPMode);
    else
      updatePMode (aPMode);
  }

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
   * Get a predicate that matches a PMode by ID, initiator ID and responder ID?
   *
   * @param sID
   *        PMode ID to search. May be <code>null</code>.
   * @param sInitiatorID
   *        Initiator ID to search. May be <code>null</code>.
   * @param sResponderID
   *        Responder ID to search. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  static IPredicate <IPMode> getPModeFilter (@Nonnull final String sID,
                                             @Nullable final String sInitiatorID,
                                             @Nullable final String sResponderID)
  {
    return x -> x.getID ().equals (sID) && x.hasInitiatorID (sInitiatorID) && x.hasResponderID (sResponderID);
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
  void validatePMode (@Nullable IPMode aPMode) throws PModeValidationException;

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
