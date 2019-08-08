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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.functional.IPredicate;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.photon.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.object.BusinessObjectHelper;

/**
 * Persisting manager for {@link PMode} objects.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PModeManager extends AbstractPhotonMapBasedWALDAO <IPMode, PMode>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PModeManager.class);

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
    LOGGER.info ("Created PMode with ID '" + aPMode.getID () + "'");

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
    if (aRealPMode.isDeleted ())
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPMode.getID (), "already-deleted");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aRealPMode.setInitiator (aPMode.getInitiator ()));
      eChange = eChange.or (aRealPMode.setResponder (aPMode.getResponder ()));
      eChange = eChange.or (aRealPMode.setAgreement (aPMode.getAgreement ()));
      eChange = eChange.or (aRealPMode.setMEP (aPMode.getMEP ()));
      eChange = eChange.or (aRealPMode.setMEPBinding (aPMode.getMEPBinding ()));
      eChange = eChange.or (aRealPMode.setLeg1 (aPMode.getLeg1 ()));
      eChange = eChange.or (aRealPMode.setLeg2 (aPMode.getLeg2 ()));
      eChange = eChange.or (aRealPMode.setPayloadService (aPMode.getPayloadService ()));
      eChange = eChange.or (aRealPMode.setReceptionAwareness (aPMode.getReceptionAwareness ()));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      BusinessObjectHelper.setLastModificationNow (aRealPMode);
      internalUpdateItem (aRealPMode);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PMode.OT, "all", aRealPMode.getID ());
    LOGGER.info ("Updated PMode with ID '" + aPMode.getID () + "'");

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
    LOGGER.info ("Marked PMode with ID '" + aDeletedPMode.getID () + "' as deleted");

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
  public static IPredicate <IPMode> getPModeFilter (@Nonnull final String sID,
                                                    @Nullable final String sInitiatorID,
                                                    @Nullable final String sResponderID)
  {
    return x -> x.getID ().equals (sID) && x.hasInitiatorID (sInitiatorID) && x.hasResponderID (sResponderID);
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
          return EqualsHelper.equals (aBI.getService (), sService) && EqualsHelper.equals (aBI.getAction (), sAction);
      }
      return false;
    });
  }

  @Nonnull
  public IPMode createOrUpdatePMode (@Nonnull final PMode aPMode)
  {
    IPMode ret = findFirst (getPModeFilter (aPMode.getID (), aPMode.getInitiatorID (), aPMode.getResponderID ()));
    if (ret == null)
    {
      createPMode (aPMode);
      ret = aPMode;
    }
    else
    {
      updatePMode (aPMode);
    }
    return ret;
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nonnull
  public ESuccess validatePMode (@Nullable final IPMode aPMode) throws IllegalStateException
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    // Needs ID
    if (StringHelper.hasNoText (aPMode.getID ()))
      throw new IllegalStateException ("No PMode ID present");

    final PModeParty aInitiator = aPMode.getInitiator ();
    if (aInitiator != null)
    {
      // INITIATOR PARTY_ID
      if (StringHelper.hasNoText (aInitiator.getIDValue ()))
        throw new IllegalStateException ("No PMode Initiator ID present");

      // INITIATOR ROLE
      if (StringHelper.hasNoText (aInitiator.getRole ()))
        throw new IllegalStateException ("No PMode Initiator Role present");
    }

    final PModeParty aResponder = aPMode.getResponder ();
    if (aResponder != null)
    {
      // RESPONDER PARTY_ID
      if (StringHelper.hasNoText (aResponder.getIDValue ()))
        throw new IllegalStateException ("No PMode Responder ID present");

      // RESPONDER ROLE
      if (StringHelper.hasNoText (aResponder.getRole ()))
        throw new IllegalStateException ("No PMode Responder Role present");
    }

    if (aResponder == null && aInitiator == null)
      throw new IllegalStateException ("PMode is missing Initiator and/or Responder");

    return ESuccess.SUCCESS;
  }

  public void validateAllPModes () throws IllegalStateException
  {
    for (final IPMode aPMode : getAll ())
      validatePMode (aPMode);
  }
}
