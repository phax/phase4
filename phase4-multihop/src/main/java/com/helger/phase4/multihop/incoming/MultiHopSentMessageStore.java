/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.phase4.multihop.AS4MultiHopConfig;

/**
 * Remembers which PMode was used to send a message, so that an asynchronously arriving Receipt or
 * Error can be related back to it. This feeds
 * {@link MultiHopSignalPModeProviderSPI} and therefore the core C3 extension point.
 * <p>
 * <b>Limitation (D12):</b> this store is in-memory only. A restart loses all entries, and in a
 * clustered setup a routed signal that arrives on a different node cannot be resolved. A
 * persistent implementation is out of scope.
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class MultiHopSentMessageStore
{
  private static final class Entry
  {
    private final String m_sPModeID;
    private final Instant m_aCreationDT;

    Entry (@NonNull final String sPModeID)
    {
      m_sPModeID = sPModeID;
      m_aCreationDT = Instant.now ();
    }
  }

  private static final class SingletonHolder
  {
    static final MultiHopSentMessageStore INSTANCE = new MultiHopSentMessageStore (AS4MultiHopConfig.getDefaultInstance ());
  }

  private final AS4MultiHopConfig m_aConfig;
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final LinkedHashMap <String, Entry> m_aMap = new LinkedHashMap <> ();

  /**
   * Constructor.
   *
   * @param aConfig
   *        The configuration providing the size and age limits. May not be <code>null</code>.
   */
  public MultiHopSentMessageStore (@NonNull final AS4MultiHopConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    m_aConfig = aConfig;
  }

  /**
   * @return The global default instance, bound to {@link AS4MultiHopConfig#getDefaultInstance()}.
   *         Never <code>null</code>.
   */
  @NonNull
  public static MultiHopSentMessageStore getDefaultInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  /**
   * Remove all entries that are older than the configured maximum age. Must be called with the
   * write lock held.
   */
  @GuardedBy ("m_aRWLock")
  private void _expireOldEntries ()
  {
    final Duration aMaxAge = m_aConfig.getSentMessageStoreMaxAge ();
    final Instant aThreshold = Instant.now ().minus (aMaxAge);

    final Iterator <Map.Entry <String, Entry>> it = m_aMap.entrySet ().iterator ();
    while (it.hasNext ())
    {
      final Entry aEntry = it.next ().getValue ();
      if (aEntry.m_aCreationDT.isBefore (aThreshold))
        it.remove ();
      else
      {
        // LinkedHashMap keeps the insertion order, so everything after this is newer
        break;
      }
    }
  }

  /**
   * Remember that the provided message ID was sent using the provided PMode.
   *
   * @param sMessageID
   *        The AS4 message ID of the sent message. May neither be <code>null</code> nor empty.
   * @param sPModeID
   *        The ID of the PMode used. May neither be <code>null</code> nor empty.
   */
  public void rememberSentMessage (@NonNull @Nonempty final String sMessageID,
                                   @NonNull @Nonempty final String sPModeID)
  {
    ValueEnforcer.notEmpty (sMessageID, "MessageID");
    ValueEnforcer.notEmpty (sPModeID, "PModeID");

    m_aRWLock.writeLocked ( () -> {
      _expireOldEntries ();

      // Re-insert to keep the insertion order correct
      m_aMap.remove (sMessageID);
      m_aMap.put (sMessageID, new Entry (sPModeID));

      // Enforce the size limit, oldest first
      final int nMaxSize = m_aConfig.getSentMessageStoreMaxSize ();
      final Iterator <Map.Entry <String, Entry>> it = m_aMap.entrySet ().iterator ();
      while (m_aMap.size () > nMaxSize && it.hasNext ())
      {
        it.next ();
        it.remove ();
      }
    });
  }

  /**
   * @param sMessageID
   *        The message ID to look for. May be <code>null</code>.
   * @return The PMode ID that was used to send the message, or <code>null</code> if the message is
   *         unknown or its entry already expired.
   */
  @Nullable
  public String getPModeIDOfSentMessage (@Nullable final String sMessageID)
  {
    if (sMessageID == null)
      return null;

    return m_aRWLock.readLockedGet ( () -> {
      final Entry aEntry = m_aMap.get (sMessageID);
      if (aEntry == null)
        return null;

      // Do not hand out entries that are already too old
      if (aEntry.m_aCreationDT.isBefore (Instant.now ().minus (m_aConfig.getSentMessageStoreMaxAge ())))
        return null;

      return aEntry.m_sPModeID;
    });
  }

  /**
   * @return The current number of remembered messages.
   */
  public int size ()
  {
    return m_aRWLock.readLockedInt (m_aMap::size);
  }

  /**
   * Remove all entries.
   */
  public void clear ()
  {
    m_aRWLock.writeLocked (m_aMap::clear);
  }
}
