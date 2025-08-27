/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.util.function.Predicate;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.state.EChange;
import com.helger.base.state.EContinue;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.CollectionFind;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This is the duplicate checker for avoiding duplicate messages.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class AS4DuplicateManagerInMemory implements IAS4DuplicateManager
{
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, AS4DuplicateItem> m_aMap = new CommonsHashMap <> ();

  public AS4DuplicateManagerInMemory ()
  {}

  @Nonnull
  public EContinue registerAndCheck (@Nullable final String sMessageID,
                                     @Nullable final String sProfileID,
                                     @Nullable final String sPModeID)
  {
    if (StringHelper.isEmpty (sMessageID))
    {
      // No message ID present - don't check for duplication
      return EContinue.CONTINUE;
    }

    final AS4DuplicateItem aItem = new AS4DuplicateItem (sMessageID, sProfileID, sPModeID);
    m_aRWLock.writeLock ().lock ();
    try
    {
      final String sID = aItem.getID ();
      if (m_aMap.containsKey (sID))
      {
        // ID already in use
        return EContinue.BREAK;
      }
      m_aMap.put (sID, aItem);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return EContinue.CONTINUE;
  }

  @Nonnull
  public EChange clearCache ()
  {
    return m_aRWLock.writeLockedGet (m_aMap::removeAll);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> evictAllItemsBefore (@Nonnull final OffsetDateTime aRefDT)
  {
    // Get all message IDs to be removed
    final ICommonsList <String> aEvictItems = new CommonsArrayList <> ();
    m_aRWLock.readLocked ( () -> m_aMap.forEachValue (x -> x.getDateTime ().isBefore (aRefDT),
                                                      x -> aEvictItems.add (x.getMessageID ())));
    if (aEvictItems.isNotEmpty ())
    {
      // Bulk erase all
      m_aRWLock.writeLock ().lock ();
      try
      {
        m_aMap.removeIfKey (aEvictItems::contains);
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }
    return aEvictItems;
  }

  public boolean isEmpty ()
  {
    return m_aRWLock.readLockedBoolean (m_aMap::isEmpty);
  }

  @Nonnegative
  public int size ()
  {
    return m_aRWLock.readLockedInt (m_aMap::size);
  }

  @Nullable
  public IAS4DuplicateItem findFirst (@Nonnull final Predicate <? super IAS4DuplicateItem> aFilter)
  {
    return m_aRWLock.readLockedGet ( () -> CollectionFind.findFirst (m_aMap.values (), aFilter));
  }

  @Nullable
  public IAS4DuplicateItem getItemOfMessageID (@Nullable final String sMessageID)
  {
    if (StringHelper.isEmpty (sMessageID))
      return null;

    return findFirst (x -> x.getMessageID ().equals (sMessageID));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAS4DuplicateItem> getAll ()
  {
    return m_aRWLock.readLockedGet ( () -> new CommonsArrayList <> (m_aMap.values ()));
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Map", m_aMap).getToString ();
  }
}
