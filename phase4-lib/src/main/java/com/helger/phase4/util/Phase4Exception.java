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
package com.helger.phase4.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * phase4 base exception. It is just a in-between exception for easy catching of all phase4 related
 * exception.
 *
 * @author Philip Helger
 * @since 0.9.7
 */
public class Phase4Exception extends Exception
{
  public static final boolean DEFAULT_RETRY_FEASIBLE = true;

  private boolean m_bRetryFeasible = DEFAULT_RETRY_FEASIBLE;

  /**
   * @param sMessage
   *        Error message
   */
  public Phase4Exception (@Nonnull final String sMessage)
  {
    super (sMessage);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public Phase4Exception (@Nonnull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
  }

  /**
   * @return <code>true</code> if a retry is feasible, <code>false</code> if not.
   * @since 3.2.0
   */
  public final boolean isRetryFeasible ()
  {
    return m_bRetryFeasible;
  }

  /**
   * Set whether a retry might be feasible or not.
   *
   * @param b
   *        <code>true</code> if a retry is feasible, <code>false</code> if not
   * @return this for chaining
   * @since 3.2.0
   */
  @Nonnull
  public final Phase4Exception setRetryFeasible (final boolean b)
  {
    m_bRetryFeasible = b;
    return this;
  }
}
