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
package com.helger.as4lib.partner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4lib.util.IStringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.ObjectHelper;

public class PartnerManager extends AbstractMapBasedWALDAO <IPartner, Partner>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PartnerManager.class);

  public PartnerManager (@Nullable final String sFilename) throws DAOException
  {
    super (Partner.class, sFilename);
  }

  @Nonnull
  public IPartner createPartner (@Nonnull final Partner aPartner)
  {
    ValueEnforcer.notNull (aPartner, "PMode");

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aPartner);
    });
    AuditHelper.onAuditCreateSuccess (Partner.OT, aPartner.getID ());
    s_aLogger.info ("Created PMode with ID '" + aPartner.getID () + "'");

    return aPartner;
  }

  @Nonnull
  public EChange updatePartner (@Nonnull final String sPartnerID, @Nonnull final IStringMap aNewAttrs)
  {
    ValueEnforcer.notNull (aNewAttrs, "NewAttrs");

    final Partner aPartner = getOfID (sPartnerID);
    if (aPartner == null)
    {
      AuditHelper.onAuditModifyFailure (Partner.OT, sPartnerID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      aPartner.setAllAttributes (aNewAttrs);
      ObjectHelper.setLastModificationNow (aPartner);
      internalUpdateItem (aPartner);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (Partner.OT, "all", sPartnerID);
    s_aLogger.info ("Updated PMode with ID '" + sPartnerID + "'");

    return EChange.CHANGED;
  }

  @Nonnull
  public Partner createOrUpdatePartner (@Nullable final String sID, final IStringMap aSM)
  {
    Partner ret = getOfID (sID);
    if (ret == null)
    {
      ret = new Partner (sID, aSM);
      createPartner (ret);
    }
    else
    {
      updatePartner (sID, aSM);
    }
    return ret;
  }

  @Nonnull
  public EChange deletePartner (@Nullable final String sPartnerID)
  {
    final Partner aDeletedPartner = getOfID (sPartnerID);
    if (aDeletedPartner == null)
    {
      AuditHelper.onAuditDeleteFailure (Partner.OT, "no-such-object-id", sPartnerID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalDeleteItem (sPartnerID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (Partner.OT, sPartnerID);

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPartner> getAllPartners ()
  {
    return getAll ();
  }

  @Nullable
  public IPartner getPartnerOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
