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
package com.helger.phase4.duplicate;

import java.time.OffsetDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * This is the duplicate checker for avoiding duplicate messages.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class AS4DuplicateManagerXML extends AbstractPhotonMapBasedWALDAO <IAS4DuplicateItem, AS4DuplicateItem>
                                    implements
                                    IAS4DuplicateManager
{
  public AS4DuplicateManagerXML (@Nullable final String sFilename) throws DAOException
  {
    super (AS4DuplicateItem.class, sFilename);
  }

  @Nonnull
  public EContinue registerAndCheck (@Nullable final String sMessageID,
                                     @Nullable final String sProfileID,
                                     @Nullable final String sPModeID)
  {
    if (StringHelper.hasNoText (sMessageID))
    {
      // No message ID present - don't check for duplication
      return EContinue.CONTINUE;
    }

    final AS4DuplicateItem aItem = new AS4DuplicateItem (sMessageID, sProfileID, sPModeID);
    try
    {
      m_aRWLock.writeLocked ( () -> internalCreateItem (aItem));
    }
    catch (final IllegalArgumentException ex)
    {
      // ID already in use
      return EContinue.BREAK;
    }
    return EContinue.CONTINUE;
  }

  @Nonnull
  public EChange clearCache ()
  {
    return m_aRWLock.writeLockedGet (this::internalRemoveAllItemsNoCallback);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> evictAllItemsBefore (@Nonnull final OffsetDateTime aRefDT)
  {
    // Get all message IDs to be removed
    final ICommonsList <String> aEvictItems = getAllMapped (x -> x.getDateTime ().isBefore (aRefDT),
                                                            IAS4DuplicateItem::getMessageID);
    if (aEvictItems.isNotEmpty ())
      // Bulk erase all
      m_aRWLock.writeLocked ( () -> {
        for (final String sItemID : aEvictItems)
          internalDeleteItem (sItemID);
      });
    return aEvictItems;
  }

  @Nullable
  public IAS4DuplicateItem getItemOfMessageID (@Nullable final String sMessageID)
  {
    if (StringHelper.hasNoText (sMessageID))
      return null;

    return findFirst (x -> x.getMessageID ().equals (sMessageID));
  }
}
