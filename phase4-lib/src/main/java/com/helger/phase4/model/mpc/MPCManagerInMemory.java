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
package com.helger.phase4.model.mpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;
import com.helger.photon.security.object.BusinessObjectHelper;

/**
 * Manager for {@link MPC} objects.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class MPCManagerInMemory implements IMPCManager
{
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, MPC> m_aMap = new CommonsHashMap <> ();

  public MPCManagerInMemory ()
  {
    // Create default MPC
    createMPC (new MPC (CAS4.DEFAULT_MPC_ID));
  }

  public final void createMPC (@Nonnull final MPC aMPC)
  {
    ValueEnforcer.notNull (aMPC, "MPC");

    m_aRWLock.writeLocked ( () -> {
      final String sID = aMPC.getID ();
      if (m_aMap.containsKey (sID))
        throw new IllegalArgumentException ("An object with ID '" + sID + "' is already contained!");
      m_aMap.put (sID, aMPC);
    });
  }

  @Nonnull
  public EChange updateMPC (@Nonnull final IMPC aMPC)
  {
    ValueEnforcer.notNull (aMPC, "MPC");
    final MPC aRealMPC = getOfID (aMPC.getID ());
    if (aRealMPC == null || aRealMPC.isDeleted ())
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      BusinessObjectHelper.setLastModificationNow (aRealMPC);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange markMPCDeleted (@Nullable final String sMPCID)
  {
    final MPC aDeletedMPC = getOfID (sMPCID);
    if (aDeletedMPC == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (BusinessObjectHelper.setDeletionNow (aDeletedMPC).isUnchanged ())
        return EChange.UNCHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteMPC (@Nullable final String sMPCID)
  {
    final MPC aDeletedMPC = getOfID (sMPCID);
    if (aDeletedMPC == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aMap.remove (sMPCID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    return EChange.CHANGED;
  }

  @Nullable
  MPC getOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;
    return m_aRWLock.readLockedGet ( () -> m_aMap.get (sID));
  }

  @Nullable
  public IMPC getMPCOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public boolean containsWithID (final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;
    return m_aRWLock.readLockedBoolean ( () -> m_aMap.containsKey (sID));
  }
}
