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
package com.helger.phase4.logging;

import java.util.concurrent.Callable;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The logging customizer for the phase4 library.
 *
 * @author Philip Helger
 * @since 3.1.0-beta3
 */
@ThreadSafe
public final class Phase4LogCustomizer
{
  private static final ThreadLocal <String> TL_PREFIX = new ThreadLocal <> ();
  private static final ThreadLocal <String> TL_SUFFIX = new ThreadLocal <> ();

  @Deprecated (forRemoval = false)
  private Phase4LogCustomizer ()
  {}

  /**
   * Get the current thread local log prefix.
   *
   * @return The current thread local log prefix. May be <code>null</code>.
   */
  @Nullable
  public static String getThreadLocalLogPrefix ()
  {
    return TL_PREFIX.get ();
  }

  /**
   * Set the current thread local log prefix.
   *
   * @param sPrefix
   *        The prefix to set. May be <code>null</code>.
   */
  public static void setThreadLocalLogPrefix (@Nullable final String sPrefix)
  {
    TL_PREFIX.set (sPrefix);
  }

  /**
   * Get the current thread local log suffix.
   *
   * @return The current thread local log suffix. May be <code>null</code>.
   */
  @Nullable
  public static String getThreadLocalLogSuffix ()
  {
    return TL_SUFFIX.get ();
  }

  /**
   * Set the current thread local log suffix.
   *
   * @param sSuffix
   *        The suffix to set. May be <code>null</code>.
   */
  public static void setThreadLocalLogSuffix (@Nullable final String sSuffix)
  {
    TL_SUFFIX.set (sSuffix);
  }

  /**
   * Clear the thread local log prefix and suffix.
   */
  public static void clearThreadLocals ()
  {
    TL_PREFIX.remove ();
    TL_SUFFIX.remove ();
  }

  /**
   * Run the passed runnable with the given prefix and suffix set as thread local.
   *
   * @param sPrefix
   *        The prefix to set. May be <code>null</code>.
   * @param sSuffix
   *        The suffix to set. May be <code>null</code>.
   * @param aRunnable
   *        The runnable to execute. May not be <code>null</code>.
   */
  public static void runWithLogPrefixAndSuffix (@Nullable final String sPrefix,
                                                @Nullable final String sSuffix,
                                                @Nonnull final Runnable aRunnable)
  {
    ValueEnforcer.notNull (aRunnable, "Runnable");

    setThreadLocalLogPrefix (sPrefix);
    setThreadLocalLogSuffix (sSuffix);
    try
    {
      aRunnable.run ();
    }
    finally
    {
      clearThreadLocals ();
    }
  }

  /**
   * Run the passed callable with the given prefix and suffix set as thread local.
   *
   * @param <T>
   *        The return type of the callable.
   * @param sPrefix
   *        The prefix to set. May be <code>null</code>.
   * @param sSuffix
   *        The suffix to set. May be <code>null</code>.
   * @param aCallable
   *        The callable to execute. May not be <code>null</code>.
   * @return The result of the callable.
   * @throws Exception
   *         If the callable throws an exception.
   */
  public static <T> T callWithLogPrefixAndSuffix (@Nullable final String sPrefix,
                                                  @Nullable final String sSuffix,
                                                  @Nonnull final Callable <T> aCallable) throws Exception
  {
    ValueEnforcer.notNull (aCallable, "Callable");

    setThreadLocalLogPrefix (sPrefix);
    setThreadLocalLogSuffix (sSuffix);
    try
    {
      return aCallable.call ();
    }
    finally
    {
      clearThreadLocals ();
    }
  }
}
