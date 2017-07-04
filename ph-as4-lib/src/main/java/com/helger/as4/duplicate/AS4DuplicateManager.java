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
package com.helger.as4.duplicate;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;
import com.helger.commons.string.StringHelper;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;

/**
 * This is the duplicate checker for avoiding duplicate messages.
 *
 * @author Philip Helger
 */
public final class AS4DuplicateManager extends AbstractMapBasedWALDAO <IAS4DuplicateItem, AS4DuplicateItem>
{
  public AS4DuplicateManager (@Nullable final String sFilename) throws DAOException
  {
    super (AS4DuplicateItem.class, sFilename);
  }

  /**
   * Check if the passed message ID was already handled.
   *
   * @param sMessageID
   *        Message ID to check. May be <code>null</code>.
   * @param sProfileID
   *        Active AS4 profile ID. May be used to define the PMode further. May
   *        be <code>null</code>.
   * @param sPModeID
   *        Active AS4 PMode ID. May be <code>null</code>.
   * @return {@link EContinue#CONTINUE} to continue
   */
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
      m_aRWLock.writeLocked ( () -> {
        internalCreateItem (aItem);
      });
    }
    catch (final IllegalArgumentException ex)
    {
      // ID already in use
      return EContinue.BREAK;
    }
    return EContinue.CONTINUE;
  }

  /**
   * Remove all entries in the cache.
   *
   * @return {@link EChange}
   */
  @Nonnull
  public EChange clearCache ()
  {
    return m_aRWLock.writeLocked ( () -> internalRemoveAllItemsNoCallback ());
  }

  /**
   * Delete all duplicate items that were created before the provided time.
   *
   * @param aRefDT
   *        The reference date time to compare to. May not be <code>null</code>.
   * @return A non-<code>null</code> list of all evicted message IDs.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> evictAllItemsBefore (@Nonnull final LocalDateTime aRefDT)
  {
    final ICommonsList <String> aEvictItems = getAllMapped (x -> x.getDateTime ().isBefore (aRefDT),
                                                            IAS4DuplicateItem::getMessageID);
    if (aEvictItems.isNotEmpty ())
      m_aRWLock.writeLocked ( () -> {
        for (final String sItemID : aEvictItems)
          internalDeleteItem (sItemID);
      });
    return aEvictItems;
  }

  /**
   * @return Get the amount of messages that already are safed in the file.
   */
  public int getCountOfMessages ()
  {
    return this.getCount ();
  }
}
