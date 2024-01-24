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
package com.helger.phase4.model.pmode;

import java.io.Serializable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.ToStringGenerator;

@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeReceptionAwareness implements Serializable
{
  public static final boolean DEFAULT_RECEPTION_AWARENESS = true;
  public static final boolean DEFAULT_RETRY = true;
  public static final int DEFAULT_MAX_RETRIES = 1;
  public static final long DEFAULT_RETRY_INTERVAL_MS = 10 * CGlobal.MILLISECONDS_PER_SECOND;
  public static final boolean DEFAULT_DUPLICATE_DETECTION = true;

  private ETriState m_eReceptionAwareness = ETriState.UNDEFINED;
  private ETriState m_eRetry = ETriState.UNDEFINED;
  private int m_nMaxRetries;
  private long m_nRetryIntervalMS;
  private ETriState m_eDuplicateDetection = ETriState.UNDEFINED;

  public PModeReceptionAwareness (@Nonnull final ETriState eReceptionAwareness,
                                  @Nonnull final ETriState eRetry,
                                  final int nMaxRetries,
                                  final long nRetryIntervalMS,
                                  @Nonnull final ETriState eDuplicateDetection)
  {
    setReceptionAwareness (eReceptionAwareness);
    setRetry (eRetry);
    setMaxRetries (nMaxRetries);
    setRetryIntervalMS (nRetryIntervalMS);
    setDuplicateDetection (eDuplicateDetection);
  }

  /**
   * @return <code>true</code> if reception awareness is defined,
   *         <code>false</code> if it is undefined. If it is undefined, the
   *         default value ({@link #DEFAULT_RECEPTION_AWARENESS}) will be used.
   */
  public final boolean isReceptionAwarenessDefined ()
  {
    return m_eReceptionAwareness.isDefined ();
  }

  /**
   * @return <code>true</code> if reception awareness is enabled,
   *         <code>false</code> of not.
   */
  public final boolean isReceptionAwareness ()
  {
    return m_eReceptionAwareness.getAsBooleanValue (DEFAULT_RECEPTION_AWARENESS);
  }

  /**
   * Set the reception awareness
   *
   * @param bReceptionAwareness
   *        <code>true</code> to enable it, <code>false</code> to disable it.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setReceptionAwareness (final boolean bReceptionAwareness)
  {
    return setReceptionAwareness (ETriState.valueOf (bReceptionAwareness));
  }

  /**
   * Set the reception awareness
   *
   * @param eReceptionAwareness
   *        Value to use. May not be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setReceptionAwareness (@Nonnull final ETriState eReceptionAwareness)
  {
    ValueEnforcer.notNull (eReceptionAwareness, "ReceptionAwareness");
    if (eReceptionAwareness.equals (m_eReceptionAwareness))
      return EChange.UNCHANGED;
    m_eReceptionAwareness = eReceptionAwareness;
    return EChange.CHANGED;
  }

  /**
   * @return <code>true</code> if retry is defined, <code>false</code> if it is
   *         undefined. If it is undefined, the default value
   *         ({@link #DEFAULT_RETRY}) will be used.
   */
  public final boolean isRetryDefined ()
  {
    return m_eRetry.isDefined ();
  }

  /**
   * @return <code>true</code> if retry is enabled, <code>false</code> of not.
   */
  public final boolean isRetry ()
  {
    return m_eRetry.getAsBooleanValue (DEFAULT_RETRY);
  }

  /**
   * Set enable retry
   *
   * @param bRetry
   *        <code>true</code> to enable it, <code>false</code> to disable it.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setRetry (final boolean bRetry)
  {
    return setRetry (ETriState.valueOf (bRetry));
  }

  /**
   * Set enable retry
   *
   * @param eRetry
   *        Value to use. May not be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setRetry (@Nonnull final ETriState eRetry)
  {
    ValueEnforcer.notNull (eRetry, "Retry");
    if (eRetry.equals (m_eRetry))
      return EChange.UNCHANGED;
    m_eRetry = eRetry;
    return EChange.CHANGED;
  }

  /**
   * @return The maximum number of retries. Always &ge; 0.
   */
  @Nonnegative
  public final int getMaxRetries ()
  {
    return m_nMaxRetries;
  }

  /**
   * Set the maximum number of retries. This value does not include the original
   * try.
   *
   * @param nMaxRetries
   *        The maximum number of retries to use. Must be &ge; 0.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setMaxRetries (@Nonnegative final int nMaxRetries)
  {
    ValueEnforcer.isGE0 (nMaxRetries, "MaxRetries");
    if (nMaxRetries == m_nMaxRetries)
      return EChange.UNCHANGED;
    m_nMaxRetries = nMaxRetries;
    return EChange.CHANGED;
  }

  /**
   * @return The milliseconds to wait between retries. Must be &ge; 0.
   */
  @Nonnegative
  public final long getRetryIntervalMS ()
  {
    return m_nRetryIntervalMS;
  }

  /**
   * Set the milliseconds to wait between retries. This should also be used to
   * define the interval between the original try and the first retry.
   *
   * @param nRetryIntervalMS
   *        Milliseconds to wait. Must be &ge; 0.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setRetryIntervalMS (@Nonnegative final long nRetryIntervalMS)
  {
    ValueEnforcer.isGE0 (nRetryIntervalMS, "RetryIntervalMS");
    if (nRetryIntervalMS == m_nRetryIntervalMS)
      return EChange.UNCHANGED;
    m_nRetryIntervalMS = nRetryIntervalMS;
    return EChange.CHANGED;
  }

  /**
   * @return <code>true</code> if duplication detection is defined,
   *         <code>false</code> if it is undefined. If it is undefined, the
   *         default value ({@link #DEFAULT_DUPLICATE_DETECTION}) will be used.
   */
  public final boolean isDuplicateDetectionDefined ()
  {
    return m_eDuplicateDetection.isDefined ();
  }

  /**
   * @return <code>true</code> if duplicate detection is enabled,
   *         <code>false</code> of not.
   */
  public final boolean isDuplicateDetection ()
  {
    return m_eDuplicateDetection.getAsBooleanValue (DEFAULT_DUPLICATE_DETECTION);
  }

  /**
   * Set duplicate detection
   *
   * @param bDuplicateDetection
   *        <code>true</code> to enable it, <code>false</code> to disable it.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setDuplicateDetection (final boolean bDuplicateDetection)
  {
    return setDuplicateDetection (ETriState.valueOf (bDuplicateDetection));
  }

  /**
   * Set duplicate detection
   *
   * @param eDuplicateDetection
   *        Value to use. May not be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setDuplicateDetection (@Nonnull final ETriState eDuplicateDetection)
  {
    ValueEnforcer.notNull (eDuplicateDetection, "DuplicateDetection");
    if (eDuplicateDetection.equals (m_eDuplicateDetection))
      return EChange.UNCHANGED;
    m_eDuplicateDetection = eDuplicateDetection;
    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeReceptionAwareness rhs = (PModeReceptionAwareness) o;
    return m_eReceptionAwareness.equals (rhs.m_eReceptionAwareness) &&
           m_eRetry.equals (rhs.m_eRetry) &&
           m_nMaxRetries == rhs.m_nMaxRetries &&
           m_nRetryIntervalMS == rhs.m_nRetryIntervalMS &&
           m_eDuplicateDetection.equals (rhs.m_eDuplicateDetection);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eReceptionAwareness)
                                       .append (m_eRetry)
                                       .append (m_nMaxRetries)
                                       .append (m_nRetryIntervalMS)
                                       .append (m_eDuplicateDetection)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ReceptionAwareness", m_eReceptionAwareness)
                                       .append ("Retry", m_eRetry)
                                       .append ("MaxRetries", m_nMaxRetries)
                                       .append ("RetryIntervalMS", m_nRetryIntervalMS)
                                       .append ("DuplicateDetection", m_eDuplicateDetection)
                                       .getToString ();
  }

  /**
   * @return A new {@link PModeReceptionAwareness} using the default values.
   * @see #DEFAULT_RECEPTION_AWARENESS
   * @see #DEFAULT_RETRY
   * @see #DEFAULT_MAX_RETRIES
   * @see #DEFAULT_RETRY_INTERVAL_MS
   * @see #DEFAULT_DUPLICATE_DETECTION
   */
  @Nonnull
  public static PModeReceptionAwareness createDefault ()
  {
    return new PModeReceptionAwareness (ETriState.valueOf (DEFAULT_RECEPTION_AWARENESS),
                                        ETriState.valueOf (DEFAULT_RETRY),
                                        DEFAULT_MAX_RETRIES,
                                        DEFAULT_RETRY_INTERVAL_MS,
                                        ETriState.valueOf (DEFAULT_DUPLICATE_DETECTION));
  }
}
