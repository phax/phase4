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
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.state.EChange;
import com.helger.dao.DAOException;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.security.object.BusinessObjectHelper;

/**
 * Persisting manager for {@link PMode} objects.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PModeManagerXML extends AbstractPhotonMapBasedWALDAO <IPMode, PMode> implements IPModeManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PModeManagerXML.class);

  public PModeManagerXML (@Nullable final String sFilename) throws DAOException
  {
    super (PMode.class, sFilename);
  }

  private void _validatePMode (@Nonnull final IPMode aPMode)
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
  private void _createPModeLocked (@Nonnull final PMode aPMode)
  {
    internalCreateItem (aPMode);
    AuditHelper.onAuditCreateSuccess (PMode.OT, aPMode.getID ());

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created PMode with ID '" + aPMode.getID () + "'");
  }

  public void createPMode (@Nonnull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    _validatePMode (aPMode);

    m_aRWLock.writeLocked ( () -> _createPModeLocked (aPMode));
  }

  @Nonnull
  public EChange updatePMode (@Nonnull final IPMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    _validatePMode (aPMode);

    final PMode aExistingPMode = getOfID (aPMode.getID ());
    if (aExistingPMode == null)
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPMode.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }
    if (aExistingPMode.isDeleted ())
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPMode.getID (), "already-deleted");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aExistingPMode.setInitiator (aPMode.getInitiator ()));
      eChange = eChange.or (aExistingPMode.setResponder (aPMode.getResponder ()));
      eChange = eChange.or (aExistingPMode.setAgreement (aPMode.getAgreement ()));
      eChange = eChange.or (aExistingPMode.setMEP (aPMode.getMEP ()));
      eChange = eChange.or (aExistingPMode.setMEPBinding (aPMode.getMEPBinding ()));
      eChange = eChange.or (aExistingPMode.setLeg1 (aPMode.getLeg1 ()));
      eChange = eChange.or (aExistingPMode.setLeg2 (aPMode.getLeg2 ()));
      eChange = eChange.or (aExistingPMode.setPayloadService (aPMode.getPayloadService ()));
      eChange = eChange.or (aExistingPMode.setReceptionAwareness (aPMode.getReceptionAwareness ()));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      BusinessObjectHelper.setLastModificationNow (aExistingPMode);
      internalUpdateItem (aExistingPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PMode.OT, "all", aExistingPMode.getID ());

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Updated PMode with ID '" + aPMode.getID () + "'");

    return EChange.CHANGED;
  }

  public void createOrUpdatePMode (@Nonnull final PMode aPMode)
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
      updatePMode (aExisting);
  }

  @Nonnull
  public EChange markPModeDeleted (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
    {
      AuditHelper.onAuditDeleteFailure (PMode.OT, "no-such-object-id", sPModeID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (BusinessObjectHelper.setDeletionNow (aDeletedPMode).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (PMode.OT, "already-deleted", sPModeID);
        return EChange.UNCHANGED;
      }
      internalMarkItemDeleted (aDeletedPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PMode.OT, sPModeID);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Marked PMode with ID '" + aDeletedPMode.getID () + "' as deleted");

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deletePMode (@Nullable final String sPModeID)
  {
    final PMode aDeletedPMode = getOfID (sPModeID);
    if (aDeletedPMode == null)
    {
      AuditHelper.onAuditDeleteFailure (PMode.OT, "no-such-object-id", sPModeID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalDeleteItem (sPModeID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PMode.OT, sPModeID);

    return EChange.CHANGED;
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
