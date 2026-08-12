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
package com.helger.phase4.attachment;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.CountingInputStream;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * An {@link InputStream} wrapper that throws an {@link AS4SizeLimitException}, as soon as more than
 * the configured number of bytes was read from the underlying stream. Using {@link #NO_LIMIT} as
 * the maximum size disables the check, so that this class acts as a pure byte counter. See issue
 * #318.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public class AS4SizeLimitedInputStream extends CountingInputStream
{
  /** The value to be used to disable the size limit */
  public static final long NO_LIMIT = CGlobal.ILLEGAL_ULONG;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4SizeLimitedInputStream.class);

  private final String m_sWhat;
  private final long m_nMaxBytes;

  /**
   * Constructor
   *
   * @param aSrcIS
   *        The source stream to be wrapped. May not be <code>null</code>.
   * @param sWhat
   *        A human readable description of the limited content, used in the exception message. May
   *        neither be <code>null</code> nor empty.
   * @param nMaxBytes
   *        The maximum number of bytes that may be read. Use {@link #NO_LIMIT} to disable the
   *        check.
   */
  public AS4SizeLimitedInputStream (@NonNull final InputStream aSrcIS,
                                    @NonNull @Nonempty final String sWhat,
                                    @CheckForSigned final long nMaxBytes)
  {
    super (aSrcIS);
    ValueEnforcer.notEmpty (sWhat, "What");
    m_sWhat = sWhat;
    m_nMaxBytes = nMaxBytes;
  }

  /**
   * @return <code>true</code> if a size limit is effectively applied, <code>false</code> if this
   *         stream is a pure byte counter.
   */
  public final boolean isLimited ()
  {
    return m_nMaxBytes >= 0;
  }

  /**
   * @return The maximum number of bytes that may be read or {@link #NO_LIMIT}.
   */
  public final long getMaxBytes ()
  {
    return m_nMaxBytes;
  }

  /**
   * @return <code>true</code> if the configured size limit was exceeded. This is needed, because
   *         some consumers (like <code>DOMReader</code>) silently swallow the
   *         {@link AS4SizeLimitException} thrown while reading.
   */
  public final boolean isLimitExceeded ()
  {
    return isLimited () && getPosition () > m_nMaxBytes;
  }

  /**
   * @return A human readable description of the limited content. Neither <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public final String getWhat ()
  {
    return m_sWhat;
  }

  private void _checkLimit () throws AS4SizeLimitException
  {
    if (isLimitExceeded ())
    {
      final String sMsg = m_sWhat + " exceeds the maximum allowed size of " + m_nMaxBytes + " bytes";
      LOGGER.error (sMsg);
      throw new AS4SizeLimitException (sMsg);
    }
  }

  @Override
  public int read () throws IOException
  {
    final int ret = super.read ();
    _checkLimit ();
    return ret;
  }

  @Override
  public int read (final byte [] aBuf, final int nOffset, final int nLength) throws IOException
  {
    final int ret = super.read (aBuf, nOffset, nLength);
    _checkLimit ();
    return ret;
  }

  @Override
  public long skip (final long n) throws IOException
  {
    final long ret = super.skip (n);
    _checkLimit ();
    return ret;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("What", m_sWhat)
                            .append ("MaxBytes", m_nMaxBytes)
                            .getToString ();
  }
}
