/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;

public class AS4ProfileManager extends AbstractMapBasedWALDAO <IAS4Profile, AS4Profile>
{
  public AS4ProfileManager (@Nullable final String sFilename) throws DAOException
  {
    super (AS4Profile.class, sFilename);
  }

  @Nonnull
  public IAS4Profile createAS4Profile (@Nonnull final AS4Profile aAS4Profile)
  {
    ValueEnforcer.notNull (aAS4Profile, "AS4Profile");

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aAS4Profile);
    });
    AuditHelper.onAuditCreateSuccess (AS4Profile.OT, aAS4Profile.getID ());

    return aAS4Profile;
  }

  @Nonnull
  public EChange updateAS4Profile (@Nonnull final IAS4Profile aAS4Profile)
  {
    ValueEnforcer.notNull (aAS4Profile, "AS4Profile");
    final AS4Profile aRealAS4Profile = getOfID (aAS4Profile.getID ());
    if (aRealAS4Profile == null)
    {
      AuditHelper.onAuditModifyFailure (AS4Profile.OT, aAS4Profile.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalUpdateItem (aRealAS4Profile);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (AS4Profile.OT, "all", aRealAS4Profile.getID ());

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAS4Profile> getAllAS4Profiles ()
  {
    return getAll ();
  }

  @Nullable
  public IAS4Profile getAS4ProfileOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
