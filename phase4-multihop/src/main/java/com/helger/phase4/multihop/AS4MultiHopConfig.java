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
package com.helger.phase4.multihop;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsOrderedSet;

/**
 * Configuration of the AS4 Multi-Hop endpoint support.<br>
 * On the <b>sending</b> side, multi-hop is opt-in per PMode ID - only messages of a PMode listed
 * via {@link #addAddActorOrRoleAttributePModeID(String)} get the <code>nextmsh</code> role/actor
 * attribute (AS4 Profile section 4.3 <code>AddActorOrRoleAttribute</code>).<br>
 * On the <b>responding</b> side no configuration exists at all - detection is purely based on the
 * role/actor attribute of the incoming message, as required by AS4 Profile section 4.2.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class AS4MultiHopConfig
{
  /** Default number of remembered sent messages */
  public static final int DEFAULT_SENT_MESSAGE_STORE_MAX_SIZE = 10_000;

  /** Default time a sent message is remembered */
  public static final Duration DEFAULT_SENT_MESSAGE_STORE_MAX_AGE = Duration.ofDays (7);

  private static final class SingletonHolder
  {
    static final AS4MultiHopConfig INSTANCE = new AS4MultiHopConfig ();
  }

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsOrderedSet <String> m_aPModeIDs = new CommonsLinkedHashSet <> ();
  @GuardedBy ("m_aRWLock")
  private String m_sReverseActionSuffixReceipt = CAS4MultiHop.DEFAULT_ACTION_SUFFIX;
  @GuardedBy ("m_aRWLock")
  private String m_sReverseActionSuffixError = CAS4MultiHop.DEFAULT_ACTION_SUFFIX;
  @GuardedBy ("m_aRWLock")
  private boolean m_bSignAddressingHeaders = true;
  @GuardedBy ("m_aRWLock")
  private int m_nSentMessageStoreMaxSize = DEFAULT_SENT_MESSAGE_STORE_MAX_SIZE;
  @GuardedBy ("m_aRWLock")
  private Duration m_aSentMessageStoreMaxAge = DEFAULT_SENT_MESSAGE_STORE_MAX_AGE;

  /**
   * Default constructor. Use {@link #getDefaultInstance()} for the globally shared instance; create
   * dedicated instances for tests or for multi-tenant setups.
   */
  public AS4MultiHopConfig ()
  {}

  /**
   * @return The global default configuration instance. Never <code>null</code>.
   */
  @NonNull
  public static AS4MultiHopConfig getDefaultInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  /**
   * Enable multi-hop sending for the provided PMode ID.
   *
   * @param sPModeID
   *        The PMode ID. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig addAddActorOrRoleAttributePModeID (@NonNull @Nonempty final String sPModeID)
  {
    ValueEnforcer.notEmpty (sPModeID, "PModeID");
    m_aRWLock.writeLocked ( () -> m_aPModeIDs.add (sPModeID));
    return this;
  }

  /**
   * Disable multi-hop sending for the provided PMode ID.
   *
   * @param sPModeID
   *        The PMode ID. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig removeAddActorOrRoleAttributePModeID (@Nullable final String sPModeID)
  {
    if (sPModeID != null)
      m_aRWLock.writeLocked ( () -> m_aPModeIDs.remove (sPModeID));
    return this;
  }

  /**
   * @return All PMode IDs for which multi-hop sending is enabled. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public final ICommonsOrderedSet <String> getAllAddActorOrRoleAttributePModeIDs ()
  {
    return m_aRWLock.readLockedGet (m_aPModeIDs::getClone);
  }

  /**
   * @param sPModeID
   *        The PMode ID to check. May be <code>null</code>.
   * @return <code>true</code> if the <code>nextmsh</code> role/actor attribute must be added to
   *         outgoing User Messages of that PMode.
   */
  public final boolean isAddActorOrRoleAttribute (@Nullable final String sPModeID)
  {
    if (sPModeID == null)
      return false;
    return m_aRWLock.readLockedBoolean ( () -> m_aPModeIDs.contains (sPModeID));
  }

  /**
   * @return The suffix appended to the Action of an inferred reverse RoutingInput of a Receipt.
   *         Never <code>null</code>.
   */
  @NonNull
  public final String getReverseActionSuffixReceipt ()
  {
    return m_aRWLock.readLockedGet ( () -> m_sReverseActionSuffixReceipt);
  }

  /**
   * @param s
   *        The new suffix. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig setReverseActionSuffixReceipt (@NonNull @Nonempty final String s)
  {
    ValueEnforcer.notEmpty (s, "Suffix");
    m_aRWLock.writeLocked ( () -> m_sReverseActionSuffixReceipt = s);
    return this;
  }

  /**
   * @return The suffix appended to the Action of an inferred reverse RoutingInput of an Error.
   *         Never <code>null</code>.
   */
  @NonNull
  public final String getReverseActionSuffixError ()
  {
    return m_aRWLock.readLockedGet ( () -> m_sReverseActionSuffixError);
  }

  /**
   * @param s
   *        The new suffix. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig setReverseActionSuffixError (@NonNull @Nonempty final String s)
  {
    ValueEnforcer.notEmpty (s, "Suffix");
    m_aRWLock.writeLocked ( () -> m_sReverseActionSuffixError = s);
    return this;
  }

  /**
   * @return <code>true</code> if the WS-Addressing headers and the RoutingInput are to be included
   *         in the signature of a signed response. Default is <code>true</code>.
   */
  public final boolean isSignAddressingHeaders ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_bSignAddressingHeaders);
  }

  /**
   * @param b
   *        <code>true</code> to sign the added headers.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig setSignAddressingHeaders (final boolean b)
  {
    m_aRWLock.writeLocked ( () -> m_bSignAddressingHeaders = b);
    return this;
  }

  /**
   * @return The maximum number of entries in the sent message store.
   */
  public final int getSentMessageStoreMaxSize ()
  {
    return m_aRWLock.readLockedInt ( () -> m_nSentMessageStoreMaxSize);
  }

  /**
   * @param n
   *        The new maximum number of entries. Must be &gt; 0.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig setSentMessageStoreMaxSize (final int n)
  {
    ValueEnforcer.isGT0 (n, "MaxSize");
    m_aRWLock.writeLocked ( () -> m_nSentMessageStoreMaxSize = n);
    return this;
  }

  /**
   * @return The maximum age of an entry in the sent message store. Never <code>null</code>.
   */
  @NonNull
  public final Duration getSentMessageStoreMaxAge ()
  {
    return m_aRWLock.readLockedGet ( () -> m_aSentMessageStoreMaxAge);
  }

  /**
   * @param a
   *        The new maximum age. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public final AS4MultiHopConfig setSentMessageStoreMaxAge (@NonNull final Duration a)
  {
    ValueEnforcer.notNull (a, "MaxAge");
    m_aRWLock.writeLocked ( () -> m_aSentMessageStoreMaxAge = a);
    return this;
  }
}
