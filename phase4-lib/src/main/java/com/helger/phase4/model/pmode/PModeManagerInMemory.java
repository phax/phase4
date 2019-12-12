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
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.photon.security.object.BusinessObjectHelper;

/**
 * Persisting manager for {@link PMode} objects.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PModeManagerInMemory implements IPModeManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PModeManagerInMemory.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, PMode> m_aMap = new CommonsHashMap <> ();

  public PModeManagerInMemory ()
  {}

  public void createPMode (@Nonnull final PMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");

    // If not valid throws IllegalStateException
    try
    {
      validatePMode (aPMode);
    }
    catch (final PModeValidationException ex)
    {
      throw new IllegalArgumentException ("PMode is invalid", ex);
    }

    m_aRWLock.writeLocked ( () -> {
      final String sID = aPMode.getID ();
      if (m_aMap.containsKey (sID))
        throw new IllegalArgumentException ("An object with ID '" + sID + "' is already contained!");
      m_aMap.put (sID, aPMode);
    });

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created PMode with ID '" + aPMode.getID () + "'");
  }

  @Nonnull
  public EChange updatePMode (@Nonnull final IPMode aPMode)
  {
    ValueEnforcer.notNull (aPMode, "PMode");
    final PMode aRealPMode = getOfID (aPMode.getID ());
    if (aRealPMode == null || aRealPMode.isDeleted ())
      return EChange.UNCHANGED;

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
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Updated PMode with ID '" + aPMode.getID () + "'");

    return EChange.CHANGED;
  }

  @Nonnull
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

  @Nonnull
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
    if (StringHelper.hasNoText (sID))
      return null;
    return m_aRWLock.readLocked ( () -> m_aMap.get (sID));
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nullable
  public IPMode findFirst (@Nonnull final Predicate <? super IPMode> aFilter)
  {
    return m_aRWLock.readLocked ( () -> CollectionHelper.findFirst (m_aMap.values (), aFilter));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPMode> getAll ()
  {
    return m_aRWLock.readLocked ( () -> new CommonsArrayList <> (m_aMap.values ()));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllIDs ()
  {
    return m_aRWLock.readLocked ( () -> m_aMap.copyOfKeySet ());
  }
}
