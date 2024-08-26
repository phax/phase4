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
package com.helger.phase4.messaging.http;

import java.math.BigDecimal;
import java.time.Duration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.math.MathHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * An object encapsulating the HTTP retry settings. By default retries are
 * disabled.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
@NotThreadSafe
public class HttpRetrySettings
{
  public static final int DEFAULT_MAX_RETRIES = 0;
  public static final Duration DEFAULT_RETRY_DURATION = Duration.ofSeconds (10);
  public static final BigDecimal DEFAULT_RETRY_INCREASE_FACTOR = BigDecimal.ONE;

  private int m_nMaxRetries = DEFAULT_MAX_RETRIES;
  private Duration m_aDurationBeforeRetry = DEFAULT_RETRY_DURATION;
  private BigDecimal m_aRetryIncreaseFactor = DEFAULT_RETRY_INCREASE_FACTOR;

  public HttpRetrySettings ()
  {}

  /**
   * @return <code>true</code> if retries are enabled, <code>false</code> if
   *         not. Only if <code>true</code> is returned further calls to
   *         {@link #getMaxRetries()}, {@link #getDurationBeforeRetry()} and
   *         {@link #getRetryIncreaseFactor()} make sense.
   */
  public boolean isRetryEnabled ()
  {
    return m_nMaxRetries > 0;
  }

  /**
   * @return The maximum number of retries. Only if this value is &gt; 0,
   *         retries are enabled.
   * @see #isRetryEnabled()
   */
  public final int getMaxRetries ()
  {
    return m_nMaxRetries;
  }

  /**
   * Set the maximum number of retries.
   *
   * @param nMaxRetries
   *        New maximum. Any value &le; 0 means "no retries".
   * @return this for chaining
   */
  @Nonnull
  public final HttpRetrySettings setMaxRetries (final int nMaxRetries)
  {
    m_nMaxRetries = nMaxRetries;
    return this;
  }

  /**
   * @return The duration before the first retry. Never <code>null</code>. The
   *         caller needs to ensure to increase the duration with the factor
   *         provided from {@link #getRetryIncreaseFactor()}. The
   *         {@link #getIncreased(Duration, BigDecimal)} utility method may be
   *         used to perform the necessary increase.
   * @see #getRetryIncreaseFactor()
   * @see #getIncreased(Duration, BigDecimal)
   */
  @Nonnull
  public final Duration getDurationBeforeRetry ()
  {
    return m_aDurationBeforeRetry;
  }

  /**
   * Set the duration before the first retry.
   *
   * @param aDurationBeforeRetry
   *        The duration to use. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final HttpRetrySettings setDurationBeforeRetry (@Nonnull final Duration aDurationBeforeRetry)
  {
    ValueEnforcer.notNull (aDurationBeforeRetry, "DurationBeforeRetry");
    m_aDurationBeforeRetry = aDurationBeforeRetry;
    return this;
  }

  /**
   * @return The retry increase factory, that should be applied for every retry
   *         after the first one. Never <code>null</code>. An increase factor of
   *         1 means no increase. An increase factor of 2 means the waiting time
   *         doubles. The default is 1. Return values are always &gt; 0.
   */
  @Nonnull
  @Nonnegative
  public final BigDecimal getRetryIncreaseFactor ()
  {
    return m_aRetryIncreaseFactor;
  }

  /**
   * Set the retry increase factor to use. 1 means no increase. 2 means the
   * waiting time doubles every time. Only value &gt; 0 are allowed.
   *
   * @param aRetryIncreaseFactor
   *        The retry increase factor. May not be <code>null</code> and must be
   *        &gt; 0.
   * @return this for chaining
   */
  @Nonnull
  public final HttpRetrySettings setRetryIncreaseFactor (@Nonnull final BigDecimal aRetryIncreaseFactor)
  {
    ValueEnforcer.isGT0 (aRetryIncreaseFactor, "RetryIncreaseFactor");
    m_aRetryIncreaseFactor = aRetryIncreaseFactor;
    return this;
  }

  @Nonnull
  public static Duration getIncreased (@Nonnull final Duration aDuration,
                                       @Nonnull final BigDecimal aRetryIncreaseFactor)
  {
    if (MathHelper.isEQ0 (aRetryIncreaseFactor))
      return Duration.ZERO;
    if (MathHelper.isEQ1 (aRetryIncreaseFactor))
      return aDuration;
    return Duration.ofNanos (aRetryIncreaseFactor.multiply (BigDecimal.valueOf (aDuration.toNanos ())).longValue ());
  }

  public final void assignFrom (@Nonnull final HttpRetrySettings aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    setMaxRetries (aOther.getMaxRetries ());
    setDurationBeforeRetry (aOther.getDurationBeforeRetry ());
    setRetryIncreaseFactor (aOther.getRetryIncreaseFactor ());
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final HttpRetrySettings rhs = (HttpRetrySettings) o;
    return m_nMaxRetries == rhs.m_nMaxRetries &&
           m_aDurationBeforeRetry.equals (rhs.m_aDurationBeforeRetry) &&
           EqualsHelper.equals (m_aRetryIncreaseFactor, rhs.m_aRetryIncreaseFactor);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_nMaxRetries)
                                       .append (m_aDurationBeforeRetry)
                                       .append (m_aRetryIncreaseFactor)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MaxRetries", m_nMaxRetries)
                                       .append ("DurationBeforeRetry", m_aDurationBeforeRetry)
                                       .append ("RetryIncreaseFactor", m_aRetryIncreaseFactor)
                                       .getToString ();
  }
}
