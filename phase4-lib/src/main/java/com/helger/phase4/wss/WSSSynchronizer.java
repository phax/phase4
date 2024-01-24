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
package com.helger.phase4.wss;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.functional.IThrowingSupplier;
import com.helger.phase4.config.AS4Configuration;

/**
 * A helper class to run all WSS stuff in a lock. {@link WSSConfig#init()} and
 * {@link WSSConfig#cleanUp()} is called for every invocation.<br>
 * Note: this class may only be invoked if
 * {@link AS4Configuration#isWSS4JSynchronizedSecurity()} returns
 * <code>true</code>.
 *
 * @author Philip Helger
 * @since 0.11.0
 */
@ThreadSafe
public final class WSSSynchronizer
{
  private static final Lock LOCK = new ReentrantLock ();

  private WSSSynchronizer ()
  {}

  /**
   * A wrapper around {@link #call(IThrowingSupplier)} swallowing the return
   * value
   *
   * @param aRunnable
   *        The runnable to be run. May not be <code>null</code>.
   */
  public static void run (@Nonnull final Runnable aRunnable)
  {
    ValueEnforcer.notNull (aRunnable, "Runnable");

    // Wrap Runnable in Supplier
    call ( () -> {
      aRunnable.run ();
      return null;
    });
  }

  @Nullable
  public static <T, EX extends Exception> T call (@Nonnull final IThrowingSupplier <T, EX> aSupplier) throws EX
  {
    ValueEnforcer.notNull (aSupplier, "Supplier");

    // Lock
    LOCK.lock ();
    try
    {
      // Register
      WSSConfig.init ();
      try
      {
        // Perform
        return aSupplier.get ();
      }
      finally
      {
        // Unregister
        WSSConfig.cleanUp ();
      }
    }
    finally
    {
      // Unlock
      LOCK.unlock ();
    }
  }
}
