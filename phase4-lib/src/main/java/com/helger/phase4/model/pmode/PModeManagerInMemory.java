/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.MustBeLocked;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.CollectionFind;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.photon.security.object.BusinessObjectHelper;

/**
 * Persisting manager for {@link PMode} objects.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PModeManagerInMemory implements IPModeManager
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (PModeManagerInMemory.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, PMode> m_aMap = new CommonsHashMap <> ();

  public PModeManagerInMemory ()
  {}

  private void _validatePMode (@NonNull final IPMode aPMode)
  {
    try
    {
      validatePMode (aPMode);
    }
    catch (final PModeValidationException ex)
    {
      throw new IllegalArgumentException ("PMode is invalid", ex);
    }
  }

  @MustBeLocked (ELockType.WRITE)
  private void _createPModeLocked (@NonNull final PMode aPMode)
  {
    final String sID = aPMode.getID ();
    if (m_aMap.containsKey (sID))
      throw new IllegalArgumentException ("An object with ID '" + sID + "' is already contained!");
    m_aMap.put (sID, aPMode);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created PMode with ID '" + aPMode.getID () + "'");
  }

  public void createPMode (@NonNull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    _validatePMode (aPMode);

    m_aRWLock.writeLocked ( () -> _createPModeLocked (aPMode));
  }

  @NonNull
  public EChange updatePMode (@NonNull final IPMode aNewPMode)
  {
    ValueEnforcer.notNull (aNewPMode, "PMode");
    _validatePMode (aNewPMode);

    final PMode aExistingPMode = getOfID (aNewPMode.getID ());
    if (aExistingPMode == null || aExistingPMode.isDeleted ())
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aExistingPMode.setInitiator (aNewPMode.getInitiator ()));
      eChange = eChange.or (aExistingPMode.setResponder (aNewPMode.getResponder ()));
      eChange = eChange.or (aExistingPMode.setAgreement (aNewPMode.getAgreement ()));
      eChange = eChange.or (aExistingPMode.setMEP (aNewPMode.getMEP ()));
      eChange = eChange.or (aExistingPMode.setMEPBinding (aNewPMode.getMEPBinding ()));
      eChange = eChange.or (aExistingPMode.setLeg1 (aNewPMode.getLeg1 ()));
      eChange = eChange.or (aExistingPMode.setLeg2 (aNewPMode.getLeg2 ()));
      eChange = eChange.or (aExistingPMode.setPayloadService (aNewPMode.getPayloadService ()));
      eChange = eChange.or (aExistingPMode.setReceptionAwareness (aNewPMode.getReceptionAwareness ()));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      BusinessObjectHelper.setLastModificationNow (aExistingPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Updated PMode with ID '" + aNewPMode.getID () + "'");

    return EChange.CHANGED;
  }

  public void createOrUpdatePMode (@NonNull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    _validatePMode (aPMode);

    // Try in read-lock
    final Predicate <IPMode> aFilter = IPModeManager.getPModeFilter (aPMode.getID (),
                                                                     aPMode.getInitiator (),
                                                                     aPMode.getResponder ());
    IPMode aExisting = findFirst (aFilter);
    if (aExisting == null)
    {
      m_aRWLock.writeLock ().lock ();
      try
      {
        // Try again in write lock
        aExisting = findFirst (aFilter);
        if (aExisting == null)
        {
          // Create a new one
          // Ensure "existing" stays null
          _createPModeLocked (aPMode);
        }
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }

    if (aExisting != null)
    {
      updatePMode (aExisting);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Updated PMode with ID '" + aPMode.getID () + "'");
    }
  }

  @NonNull
  public EChange markPModeDeleted (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (BusinessObjectHelper.setDeletionNow (aDeletedPMode).isUnchanged ())
        return EChange.UNCHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Marked PMode with ID '" + aDeletedPMode.getID () + "' as deleted");

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deletePMode (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aMap.remove (sPModeID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    return EChange.CHANGED;
  }

  @Nullable
  PMode getOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    return m_aRWLock.readLockedGet ( () -> m_aMap.get (sID));
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nullable
  public IPMode findFirst (@NonNull final Predicate <? super IPMode> aFilter)
  {
    return m_aRWLock.readLockedGet ( () -> CollectionFind.findFirst (m_aMap.values (), aFilter));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IPMode> getAll ()
  {
    return m_aRWLock.readLockedGet ( () -> new CommonsArrayList <> (m_aMap.values ()));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllIDs ()
  {
    return m_aRWLock.readLockedGet (m_aMap::copyOfKeySet);
  }
}
