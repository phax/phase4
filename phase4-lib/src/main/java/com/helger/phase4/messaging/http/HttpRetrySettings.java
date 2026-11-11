/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
import java.util.concurrent.ThreadLocalRandom;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.numeric.BigHelper;
import com.helger.base.tostring.ToStringGenerator;

/**
 * An object encapsulating the HTTP retry settings. By default retries are disabled.<br>
 * Since 4.9.0 only failures that are classified as retriable by {@link HttpRetryClassifier} are
 * retried.
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
  /** AS4 receivers detect duplicates based on the message ID, so replaying is safe */
  public static final boolean DEFAULT_REPLAY_SAFE = true;
  public static final int DEFAULT_MAX_RETRIES_UNKNOWN_HOST = 1;
  /** No jitter by default */
  public static final BigDecimal DEFAULT_JITTER_FACTOR = BigDecimal.ZERO;
  public static final Duration DEFAULT_MAX_DURATION_BEFORE_RETRY = Duration.ofMinutes (5);
  public static final Duration DEFAULT_MAX_RETRY_AFTER = Duration.ofMinutes (5);

  private int m_nMaxRetries = DEFAULT_MAX_RETRIES;
  private Duration m_aDurationBeforeRetry = DEFAULT_RETRY_DURATION;
  private BigDecimal m_aRetryIncreaseFactor = DEFAULT_RETRY_INCREASE_FACTOR;
  private boolean m_bReplaySafe = DEFAULT_REPLAY_SAFE;
  private int m_nMaxRetriesUnknownHost = DEFAULT_MAX_RETRIES_UNKNOWN_HOST;
  private BigDecimal m_aJitterFactor = DEFAULT_JITTER_FACTOR;
  private Duration m_aMaxDurationBeforeRetry = DEFAULT_MAX_DURATION_BEFORE_RETRY;
  private Duration m_aMaxRetryAfter = DEFAULT_MAX_RETRY_AFTER;

  public HttpRetrySettings ()
  {}

  /**
   * @return <code>true</code> if retries are enabled, <code>false</code> if not. Only if
   *         <code>true</code> is returned further calls to {@link #getMaxRetries()},
   *         {@link #getDurationBeforeRetry()} and {@link #getRetryIncreaseFactor()} make sense.
   */
  public boolean isRetryEnabled ()
  {
    return m_nMaxRetries > 0;
  }

  /**
   * @return The maximum number of retries. Only if this value is &gt; 0, retries are enabled.
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
  @NonNull
  public final HttpRetrySettings setMaxRetries (final int nMaxRetries)
  {
    m_nMaxRetries = nMaxRetries;
    return this;
  }

  /**
   * @return The duration before the first retry. Never <code>null</code>. The caller needs to
   *         ensure to increase the duration with the factor provided from
   *         {@link #getRetryIncreaseFactor()}. The {@link #getIncreased(Duration, BigDecimal)}
   *         utility method may be used to perform the necessary increase.
   * @see #getRetryIncreaseFactor()
   * @see #getIncreased(Duration, BigDecimal)
   */
  @NonNull
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
  @NonNull
  public final HttpRetrySettings setDurationBeforeRetry (@NonNull final Duration aDurationBeforeRetry)
  {
    ValueEnforcer.notNull (aDurationBeforeRetry, "DurationBeforeRetry");
    m_aDurationBeforeRetry = aDurationBeforeRetry;
    return this;
  }

  /**
   * @return The retry increase factory, that should be applied for every retry after the first one.
   *         Never <code>null</code>. An increase factor of 1 means no increase. An increase factor
   *         of 2 means the waiting time doubles. The default is 1. Return values are always &gt; 0.
   */
  @NonNull
  @Nonnegative
  public final BigDecimal getRetryIncreaseFactor ()
  {
    return m_aRetryIncreaseFactor;
  }

  /**
   * Set the retry increase factor to use. 1 means no increase. 2 means the waiting time doubles
   * every time. Only value &gt; 0 are allowed.
   *
   * @param aRetryIncreaseFactor
   *        The retry increase factor. May not be <code>null</code> and must be &gt; 0.
   * @return this for chaining
   */
  @NonNull
  public final HttpRetrySettings setRetryIncreaseFactor (@NonNull final BigDecimal aRetryIncreaseFactor)
  {
    ValueEnforcer.isGT0 (aRetryIncreaseFactor, "RetryIncreaseFactor");
    m_aRetryIncreaseFactor = aRetryIncreaseFactor;
    return this;
  }

  /**
   * @return <code>true</code> if replaying a request that may already have reached the server is
   *         safe, <code>false</code> if not. Default is {@link #DEFAULT_REPLAY_SAFE}.
   * @since 4.9.0
   */
  public final boolean isReplaySafe ()
  {
    return m_bReplaySafe;
  }

  /**
   * Define whether replaying a request that may already have reached the server is safe. This is
   * the case if the receiver detects duplicates, as AS4 receivers do based on the message ID. If
   * set to <code>false</code>, only failures where the request was provably never sent are retried,
   * as well as the HTTP status codes 408, 425, 429 and 503.
   *
   * @param bReplaySafe
   *        <code>true</code> if replaying is safe, <code>false</code> if not.
   * @return this for chaining
   * @since 4.9.0
   */
  @NonNull
  public final HttpRetrySettings setReplaySafe (final boolean bReplaySafe)
  {
    m_bReplaySafe = bReplaySafe;
    return this;
  }

  /**
   * @return The maximum number of retries after an unknown host error. This is often a
   *         configuration error rather than a transient one. Default is
   *         {@link #DEFAULT_MAX_RETRIES_UNKNOWN_HOST}.
   * @since 4.9.0
   */
  @Nonnegative
  public final int getMaxRetriesUnknownHost ()
  {
    return m_nMaxRetriesUnknownHost;
  }

  /**
   * Set the maximum number of retries after an unknown host error. The overall maximum number of
   * retries is applied in addition.
   *
   * @param nMaxRetriesUnknownHost
   *        The maximum to use. Must be &ge; 0.
   * @return this for chaining
   * @since 4.9.0
   */
  @NonNull
  public final HttpRetrySettings setMaxRetriesUnknownHost (@Nonnegative final int nMaxRetriesUnknownHost)
  {
    ValueEnforcer.isGE0 (nMaxRetriesUnknownHost, "MaxRetriesUnknownHost");
    m_nMaxRetriesUnknownHost = nMaxRetriesUnknownHost;
    return this;
  }

  /**
   * @return The jitter factor to be applied to the duration before a retry. 0 means no jitter. 0.2
   *         means that the duration is randomly changed by up to &plusmn;20%. Never
   *         <code>null</code>. Default is {@link #DEFAULT_JITTER_FACTOR}.
   * @since 4.9.0
   */
  @NonNull
  @Nonnegative
  public final BigDecimal getJitterFactor ()
  {
    return m_aJitterFactor;
  }

  /**
   * Set the jitter factor to be applied to the duration before a retry. Jitter avoids that many
   * senders retry at exactly the same time.
   *
   * @param aJitterFactor
   *        The jitter factor. May not be <code>null</code> and must be between 0 and 1 (both
   *        inclusive). 0 means no jitter.
   * @return this for chaining
   * @since 4.9.0
   */
  @NonNull
  public final HttpRetrySettings setJitterFactor (@NonNull final BigDecimal aJitterFactor)
  {
    ValueEnforcer.isBetweenInclusive (aJitterFactor, "JitterFactor", BigDecimal.ZERO, BigDecimal.ONE);
    m_aJitterFactor = aJitterFactor;
    return this;
  }

  /**
   * @return The maximum duration before a retry, that limits the increase via
   *         {@link #getRetryIncreaseFactor()}. A duration before the first retry that is longer than
   *         this value is never shortened. The jitter (see {@link #getJitterFactor()}) is applied on
   *         top of the limited duration, so the effective waiting time may exceed this value by up to
   *         the jitter factor. Never <code>null</code>. Default is
   *         {@link #DEFAULT_MAX_DURATION_BEFORE_RETRY}.
   * @since 4.9.0
   */
  @NonNull
  public final Duration getMaxDurationBeforeRetry ()
  {
    return m_aMaxDurationBeforeRetry;
  }

  /**
   * Set the maximum duration before a retry, that limits the increase via
   * {@link #getRetryIncreaseFactor()}. The jitter is applied on top of the limited duration.
   *
   * @param aMaxDurationBeforeRetry
   *        The maximum duration to use. May not be <code>null</code>.
   * @return this for chaining
   * @since 4.9.0
   */
  @NonNull
  public final HttpRetrySettings setMaxDurationBeforeRetry (@NonNull final Duration aMaxDurationBeforeRetry)
  {
    ValueEnforcer.notNull (aMaxDurationBeforeRetry, "MaxDurationBeforeRetry");
    m_aMaxDurationBeforeRetry = aMaxDurationBeforeRetry;
    return this;
  }

  /**
   * @return The maximum duration of an HTTP <code>Retry-After</code> response header that is
   *         honoured. If the server asks for a longer duration, no retry happens at all. Never
   *         <code>null</code>. Default is {@link #DEFAULT_MAX_RETRY_AFTER}.
   * @since 4.9.0
   */
  @NonNull
  public final Duration getMaxRetryAfter ()
  {
    return m_aMaxRetryAfter;
  }

  /**
   * Set the maximum duration of an HTTP <code>Retry-After</code> response header that is honoured.
   * If the server asks for a longer duration, no retry happens at all.
   *
   * @param aMaxRetryAfter
   *        The maximum duration to use. May not be <code>null</code>.
   * @return this for chaining
   * @since 4.9.0
   */
  @NonNull
  public final HttpRetrySettings setMaxRetryAfter (@NonNull final Duration aMaxRetryAfter)
  {
    ValueEnforcer.notNull (aMaxRetryAfter, "MaxRetryAfter");
    m_aMaxRetryAfter = aMaxRetryAfter;
    return this;
  }

  @NonNull
  public static Duration getIncreased (@NonNull final Duration aDuration,
                                       @NonNull final BigDecimal aRetryIncreaseFactor)
  {
    if (BigHelper.isEQ0 (aRetryIncreaseFactor))
      return Duration.ZERO;
    if (BigHelper.isEQ1 (aRetryIncreaseFactor))
      return aDuration;
    return Duration.ofNanos (aRetryIncreaseFactor.multiply (BigDecimal.valueOf (aDuration.toNanos ())).longValue ());
  }

  /**
   * Apply the jitter factor to the provided duration.
   *
   * @param aDuration
   *        The duration to modify. May not be <code>null</code>.
   * @param aJitterFactor
   *        The jitter factor between 0 and 1. May not be <code>null</code>.
   * @return The duration randomly changed by up to &plusmn;jitter factor. Never <code>null</code>
   *         and never negative.
   * @since 4.9.0
   */
  @NonNull
  public static Duration getWithJitter (@NonNull final Duration aDuration, @NonNull final BigDecimal aJitterFactor)
  {
    if (BigHelper.isEQ0 (aJitterFactor) || aDuration.isZero ())
      return aDuration;

    final double dJitter = aJitterFactor.doubleValue ();
    final double dFactor = 1 + ThreadLocalRandom.current ().nextDouble (-dJitter, dJitter);
    return Duration.ofMillis (Math.max (0, Math.round (aDuration.toMillis () * dFactor)));
  }

  public final void assignFrom (@NonNull final HttpRetrySettings aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    setMaxRetries (aOther.getMaxRetries ());
    setDurationBeforeRetry (aOther.getDurationBeforeRetry ());
    setRetryIncreaseFactor (aOther.getRetryIncreaseFactor ());
    setReplaySafe (aOther.isReplaySafe ());
    setMaxRetriesUnknownHost (aOther.getMaxRetriesUnknownHost ());
    setJitterFactor (aOther.getJitterFactor ());
    setMaxDurationBeforeRetry (aOther.getMaxDurationBeforeRetry ());
    setMaxRetryAfter (aOther.getMaxRetryAfter ());
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
           EqualsHelper.equals (m_aRetryIncreaseFactor, rhs.m_aRetryIncreaseFactor) &&
           m_bReplaySafe == rhs.m_bReplaySafe &&
           m_nMaxRetriesUnknownHost == rhs.m_nMaxRetriesUnknownHost &&
           EqualsHelper.equals (m_aJitterFactor, rhs.m_aJitterFactor) &&
           m_aMaxDurationBeforeRetry.equals (rhs.m_aMaxDurationBeforeRetry) &&
           m_aMaxRetryAfter.equals (rhs.m_aMaxRetryAfter);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_nMaxRetries)
                                       .append (m_aDurationBeforeRetry)
                                       .append (m_aRetryIncreaseFactor)
                                       .append (m_bReplaySafe)
                                       .append (m_nMaxRetriesUnknownHost)
                                       .append (m_aJitterFactor)
                                       .append (m_aMaxDurationBeforeRetry)
                                       .append (m_aMaxRetryAfter)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MaxRetries", m_nMaxRetries)
                                       .append ("DurationBeforeRetry", m_aDurationBeforeRetry)
                                       .append ("RetryIncreaseFactor", m_aRetryIncreaseFactor)
                                       .append ("ReplaySafe", m_bReplaySafe)
                                       .append ("MaxRetriesUnknownHost", m_nMaxRetriesUnknownHost)
                                       .append ("JitterFactor", m_aJitterFactor)
                                       .append ("MaxDurationBeforeRetry", m_aMaxDurationBeforeRetry)
                                       .append ("MaxRetryAfter", m_aMaxRetryAfter)
                                       .getToString ();
  }
}
