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
package com.helger.as4.dump;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.concurrent.SimpleReadWriteLock;

/**
 * This class holds the global stream dumper.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@ThreadSafe
public final class AS4DumpManager
{
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static IAS4IncomingDumper s_aIncomingDumper;
  @GuardedBy ("s_aRWLock")
  private static IAS4OutgoingDumper s_aOutgoingDumper;

  private AS4DumpManager ()
  {}

  /**
   * @return The incoming dumper. May be <code>null</code>.
   */
  @Nullable
  public static IAS4IncomingDumper getIncomingDumper ()
  {
    return s_aRWLock.readLocked ( () -> s_aIncomingDumper);
  }

  /**
   * Set the incoming dumper to be globally used.
   *
   * @param aIncomingDumper
   *        The new dumper. May be <code>null</code>.
   */
  @Nullable
  public static void setIncomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
  {
    s_aRWLock.writeLocked ( () -> s_aIncomingDumper = aIncomingDumper);
  }

  /**
   * @return The outgoing dumper. May be <code>null</code>.
   */
  @Nullable
  public static IAS4OutgoingDumper getOutgoingDumper ()
  {
    return s_aRWLock.readLocked ( () -> s_aOutgoingDumper);
  }

  /**
   * Set the outgoing dumper to be globally used.
   *
   * @param aOutgoingDumper
   *        The new dumper. May be <code>null</code>.
   */
  @Nullable
  public static void setOutgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
  {
    s_aRWLock.writeLocked ( () -> s_aOutgoingDumper = aOutgoingDumper);
  }
}
