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
package com.helger.as4.model.pmode;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.ObjectHelper;

public class PModeManager extends AbstractMapBasedWALDAO <IPMode, PMode>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PModeManager.class);

  public PModeManager (@Nullable final String sFilename) throws DAOException
  {
    super (PMode.class, sFilename);
  }

  @Nonnull
  public IPMode createPMode (@Nonnull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    // If not valid throws IllegalStateException
    validatePMode (aPMode);

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aPMode);
    });
    AuditHelper.onAuditCreateSuccess (PMode.OT, aPMode.getID ());
    s_aLogger.info ("Created PMode with ID '" + aPMode.getID () + "'");

    return aPMode;
  }

  @Nonnull
  public EChange updatePMode (@Nonnull final IPMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    final PMode aRealPMode = getOfID (aPMode.getID ());
    if (aRealPMode == null)
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPMode.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      ObjectHelper.setLastModificationNow (aRealPMode);
      internalUpdateItem (aRealPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PMode.OT, "all", aRealPMode.getID ());
    s_aLogger.info ("Updated PMode with ID '" + aPMode.getID () + "'");

    return EChange.CHANGED;
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
      if (ObjectHelper.setDeletionNow (aDeletedPMode).isUnchanged ())
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
    s_aLogger.info ("Marked PMode with ID '" + aDeletedPMode.getID () + "' as deleted");

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

  @Nonnull
  public static Predicate <IPMode> getPModeFilter (@Nonnull final String sID,
                                                   @Nullable final String sInitiatorID,
                                                   @Nullable final String sResponderID)
  {
    return p -> p.getID ().equals (sID) &&
                EqualsHelper.equals (p.getInitiatorID (), sInitiatorID) &&
                EqualsHelper.equals (p.getResponderID (), sResponderID);
  }

  @Nullable
  public IPMode getPModeOfServiceAndAction (@Nullable final String sService, @Nullable final String sAction)
  {
    return findFirst (x -> {
      final PModeLeg aLeg = x.getLeg1 ();
      if (aLeg != null)
      {
        final PModeLegBusinessInformation aBI = aLeg.getBusinessInfo ();
        if (aBI != null)
        {
          return EqualsHelper.equals (aBI.getService (), sService) && EqualsHelper.equals (aBI.getAction (), sAction);
        }
      }
      return false;
    });
  }

  @Nonnull
  public IPMode createOrUpdatePMode (@Nonnull final PMode aPMode)
  {
    PMode ret = (PMode) findFirst (getPModeFilter (aPMode.getID (),
                                                   aPMode.getInitiatorID (),
                                                   aPMode.getResponderID ()));

    if (ret == null)
    {
      ret = (PMode) createPMode (aPMode);
    }
    else
    {
      updatePMode (aPMode);
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPMode> getAllPModes ()
  {
    return getAll ();
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nonnull
  public ESuccess validatePMode (@Nullable final IPMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    // Needs ID
    if (aPMode.getID () == null)
    {
      throw new IllegalStateException ("No PMode ID present");
    }

    final PModeParty aInitiator = aPMode.getInitiator ();
    if (aInitiator != null)
    {
      // INITIATOR PARTY_ID
      if (aInitiator.getIDValue () == null)
      {
        throw new IllegalStateException ("No PMode Initiator ID present");
      }

      // INITIATOR ROLE
      if (aInitiator.getRole () == null)
      {
        throw new IllegalStateException ("No PMode Initiator Role present");
      }
    }

    final PModeParty aResponder = aPMode.getResponder ();
    if (aResponder != null)
    {
      // RESPONDER PARTY_ID
      if (aResponder.getIDValue () == null)
      {
        throw new IllegalStateException ("No PMode Responder ID present");
      }

      // RESPONDER ROLE
      if (aResponder.getRole () == null)
      {
        throw new IllegalStateException ("No PMode Responder Role present");
      }
    }

    if (aResponder == null && aInitiator == null)
    {
      throw new IllegalStateException ("PMode is missing Initiator and/or Responder");
    }

    return ESuccess.SUCCESS;
  }

  public void validateAllPModes ()
  {
    for (final IPMode aPMode : getAll ())
      validatePMode (aPMode);
  }
}
