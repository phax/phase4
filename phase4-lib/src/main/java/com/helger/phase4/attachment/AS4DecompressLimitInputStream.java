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
import java.util.function.LongSupplier;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.CheckForSigned;
import com.helger.base.CGlobal;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.WrappedInputStream;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * An {@link InputStream} wrapper around a decompressing stream that limits the total number of
 * decompressed bytes as well as the effective compression ratio. This avoids "decompression bomb"
 * attacks, where a very small compressed attachment expands to an arbitrary large amount of data.
 * See issue #318.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public class AS4DecompressLimitInputStream extends WrappedInputStream
{
  /** The value to be used to disable a limit */
  public static final long NO_LIMIT = CGlobal.ILLEGAL_ULONG;

  /**
   * The number of decompressed bytes that are always accepted, before the compression ratio is
   * evaluated at all. This avoids false positives caused by the read ahead buffering of the
   * underlying decompressing stream, that would otherwise lead to an extreme ratio for the first
   * few bytes.
   */
  public static final long RATIO_CHECK_GRACE_BYTES = CGlobal.BYTES_PER_MEGABYTE;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4DecompressLimitInputStream.class);

  private final LongSupplier m_aCompressedByteCountProvider;
  private final long m_nMaxDecompressedBytes;
  private final long m_nMaxCompressionRatio;
  private long m_nDecompressedBytes = 0;

  /**
   * Constructor
   *
   * @param aDecompressedIS
   *        The decompressing stream to be wrapped. May not be <code>null</code>.
   * @param aCompressedByteCountProvider
   *        Provider for the number of compressed bytes that were read so far. May not be
   *        <code>null</code>.
   * @param nMaxDecompressedBytes
   *        The maximum number of decompressed bytes. Use {@link #NO_LIMIT} to disable the check.
   * @param nMaxCompressionRatio
   *        The maximum ratio of decompressed to compressed bytes. Use {@link #NO_LIMIT} to disable
   *        the check.
   */
  public AS4DecompressLimitInputStream (@NonNull final InputStream aDecompressedIS,
                                        @NonNull final LongSupplier aCompressedByteCountProvider,
                                        @CheckForSigned final long nMaxDecompressedBytes,
                                        @CheckForSigned final long nMaxCompressionRatio)
  {
    super (aDecompressedIS);
    ValueEnforcer.notNull (aCompressedByteCountProvider, "CompressedByteCountProvider");
    m_aCompressedByteCountProvider = aCompressedByteCountProvider;
    m_nMaxDecompressedBytes = nMaxDecompressedBytes;
    m_nMaxCompressionRatio = nMaxCompressionRatio;
  }

  /**
   * @return The number of decompressed bytes that were read so far.
   */
  public final long getDecompressedBytes ()
  {
    return m_nDecompressedBytes;
  }

  private void _checkLimits () throws AS4SizeLimitException
  {
    if (m_nMaxDecompressedBytes >= 0 && m_nDecompressedBytes > m_nMaxDecompressedBytes)
    {
      final String sMsg = "The decompressed content of the incoming attachment exceeds the maximum allowed size of " +
                          m_nMaxDecompressedBytes +
                          " bytes";
      LOGGER.error (sMsg);
      throw new AS4SizeLimitException (sMsg);
    }

    if (m_nMaxCompressionRatio >= 0 && m_nDecompressedBytes > RATIO_CHECK_GRACE_BYTES)
    {
      final long nCompressedBytes = m_aCompressedByteCountProvider.getAsLong ();
      // Avoid a division by zero - if nothing was read yet, the ratio check is
      // pointless anyway
      if (nCompressedBytes > 0 && m_nDecompressedBytes > nCompressedBytes * m_nMaxCompressionRatio)
      {
        final String sMsg = "The incoming attachment exceeds the maximum allowed compression ratio of " +
                            m_nMaxCompressionRatio +
                            " (" +
                            m_nDecompressedBytes +
                            " decompressed bytes from " +
                            nCompressedBytes +
                            " compressed bytes)";
        LOGGER.error (sMsg);
        throw new AS4SizeLimitException (sMsg);
      }
    }
  }

  @Override
  public int read () throws IOException
  {
    final int ret = super.read ();
    if (ret != -1)
    {
      m_nDecompressedBytes++;
      _checkLimits ();
    }
    return ret;
  }

  @Override
  public int read (final byte [] aBuf, final int nOffset, final int nLength) throws IOException
  {
    final int ret = super.read (aBuf, nOffset, nLength);
    if (ret > 0)
    {
      m_nDecompressedBytes += ret;
      _checkLimits ();
    }
    return ret;
  }

  @Override
  public long skip (final long n) throws IOException
  {
    final long ret = super.skip (n);
    if (ret > 0)
    {
      m_nDecompressedBytes += ret;
      _checkLimits ();
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("MaxDecompressedBytes", m_nMaxDecompressedBytes)
                            .append ("MaxCompressionRatio", m_nMaxCompressionRatio)
                            .append ("DecompressedBytes", m_nDecompressedBytes)
                            .getToString ();
  }
}
