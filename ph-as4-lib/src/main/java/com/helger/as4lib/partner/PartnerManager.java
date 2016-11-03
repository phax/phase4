/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import com.helger.as4lib.model.pmode.PMode;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.ObjectHelper;
import com.helger.xml.microdom.IMicroDocument;

public class PartnerManager extends AbstractMapBasedWALDAO <IPartner, Partner>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PartnerManager.class);
  private static final String ATTR_DEFAULT_ID = "defaultpmode";

  private String m_sDefaultID = null;

  public PartnerManager (@Nullable final String sFilename) throws DAOException
  {
    super (Partner.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    final EChange ret = super.onRead (aDoc);
    m_sDefaultID = aDoc.getDocumentElement ().getAttributeValue (ATTR_DEFAULT_ID);
    return ret;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument ret = super.createWriteData ();
    ret.getDocumentElement ().setAttribute (ATTR_DEFAULT_ID, m_sDefaultID);
    return ret;
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
  public EChange updatePartner (@Nonnull final IPartner aPartner)
  {
    ValueEnforcer.notNull (aPartner, "Partner");
    final Partner aRealPartner = getOfID (aPartner.getID ());
    if (aRealPartner == null)
    {
      AuditHelper.onAuditModifyFailure (PMode.OT, aPartner.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      ObjectHelper.setLastModificationNow (aRealPartner);
      internalUpdateItem (aRealPartner);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PMode.OT, "all", aRealPartner.getID ());
    s_aLogger.info ("Updated PMode with ID '" + aPartner.getID () + "'");

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deletePartner (@Nullable final String sPartnerID)
  {
    final Partner aDeletedPartner = getOfID (sPartnerID);
    if (aDeletedPartner == null)
    {
      AuditHelper.onAuditDeleteFailure (PMode.OT, "no-such-object-id", sPartnerID);
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
    AuditHelper.onAuditDeleteSuccess (PMode.OT, sPartnerID);

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
    IPartner ret = getOfID (sID);
    if (ret == null && m_sDefaultID != null)
    {
      // ID not found - try default
      ret = getOfID (m_sDefaultID);
    }
    return ret;
  }

  @Nullable
  public String getDefaultPartnerID ()
  {
    return m_sDefaultID;
  }

  public void setDefaultPartnerID (@Nullable final String sDefaultPartnerID)
  {
    m_sDefaultID = sDefaultPartnerID;
  }

}
