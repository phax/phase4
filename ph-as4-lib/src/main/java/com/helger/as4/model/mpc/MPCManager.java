/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.model.mpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.CAS4;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.BusinessObjectHelper;

public final class MPCManager extends AbstractPhotonMapBasedWALDAO <IMPC, MPC>
{
  public MPCManager (@Nullable final String sFilename) throws DAOException
  {
    super (MPC.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onInit ()
  {
    // Create default MPC
    createMPC (new MPC (CAS4.DEFAULT_MPC_ID));
    return EChange.CHANGED;
  }

  @Nonnull
  public IMPC createMPC (@Nonnull final MPC aMPC)
  {
    ValueEnforcer.notNull (aMPC, "MPC");

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aMPC);
    });
    AuditHelper.onAuditCreateSuccess (MPC.OT, aMPC.getID ());
    return aMPC;
  }

  @Nonnull
  public EChange updateMPC (@Nonnull final IMPC aMPC)
  {
    ValueEnforcer.notNull (aMPC, "MPC");
    final MPC aRealMPC = getOfID (aMPC.getID ());
    if (aRealMPC == null)
    {
      AuditHelper.onAuditModifyFailure (MPC.OT, aMPC.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      BusinessObjectHelper.setLastModificationNow (aRealMPC);
      internalUpdateItem (aRealMPC);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (MPC.OT, "all", aRealMPC.getID ());

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange markMPCDeleted (@Nullable final String sMPCID)
  {
    final MPC aDeletedMPC = getOfID (sMPCID);
    if (aDeletedMPC == null)
    {
      AuditHelper.onAuditDeleteFailure (MPC.OT, "no-such-object-id", sMPCID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (BusinessObjectHelper.setDeletionNow (aDeletedMPC).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (MPC.OT, "already-deleted", sMPCID);
        return EChange.UNCHANGED;
      }
      internalMarkItemDeleted (aDeletedMPC);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (MPC.OT, sMPCID);

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteMPC (@Nullable final String sMPCID)
  {
    final MPC aDeletedMPC = getOfID (sMPCID);
    if (aDeletedMPC == null)
    {
      AuditHelper.onAuditDeleteFailure (MPC.OT, "no-such-object-id", sMPCID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalDeleteItem (sMPCID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (MPC.OT, sMPCID);

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IMPC> getAllMPCs ()
  {
    return getAll ();
  }

  @Nullable
  public IMPC getMPCOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nullable
  public IMPC getMPCOrDefaultOfID (@Nullable final String sID)
  {
    return getMPCOfID (StringHelper.hasNoText (sID) ? CAS4.DEFAULT_MPC_ID : sID);
  }
}
