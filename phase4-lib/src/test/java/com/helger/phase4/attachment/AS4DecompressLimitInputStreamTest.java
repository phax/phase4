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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.CGlobal;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;

/**
 * Test class for class {@link AS4DecompressLimitInputStream}.
 *
 * @author Philip Helger
 */
public final class AS4DecompressLimitInputStreamTest
{
  @NonNull
  private static byte [] _gzip (final byte [] aSrc) throws IOException
  {
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      try (final OutputStream aOS = EAS4CompressionMode.GZIP.getCompressStream (aBAOS))
      {
        aOS.write (aSrc);
      }
      return aBAOS.getBufferOrCopy ();
    }
  }

  /**
   * @param nUncompressedBytes
   *        The number of uncompressed bytes to create.
   * @param nMaxDecompressedBytes
   *        The maximum number of decompressed bytes.
   * @param nMaxCompressionRatio
   *        The maximum compression ratio.
   * @return The number of decompressed bytes that could be read.
   * @throws IOException
   *         on error
   */
  private static long _readAll (final int nUncompressedBytes,
                                final long nMaxDecompressedBytes,
                                final long nMaxCompressionRatio) throws IOException
  {
    // Highly compressible content - this is the "decompression bomb"
    final byte [] aCompressed = _gzip (new byte [nUncompressedBytes]);

    final AS4SizeLimitedInputStream aCompressedIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (aCompressed),
                                                                                   "Test data",
                                                                                   AS4SizeLimitedInputStream.NO_LIMIT);
    try (final InputStream aIS = new AS4DecompressLimitInputStream (EAS4CompressionMode.GZIP.getDecompressStream (aCompressedIS),
                                                                    aCompressedIS::getPosition,
                                                                    nMaxDecompressedBytes,
                                                                    nMaxCompressionRatio))
    {
      // Read manually, because StreamHelper swallows the exception
      long nTotal = 0;
      final byte [] aBuf = new byte [8192];
      int nRead;
      while ((nRead = aIS.read (aBuf, 0, aBuf.length)) > 0)
        nTotal += nRead;
      return nTotal;
    }
  }

  @Test
  public void testWithinLimits () throws IOException
  {
    final int nBytes = 4 * CGlobal.BYTES_PER_MEGABYTE;
    // No limits at all
    assertEquals (nBytes,
                  _readAll (nBytes, AS4DecompressLimitInputStream.NO_LIMIT, AS4DecompressLimitInputStream.NO_LIMIT));
  }

  @Test
  public void testMaxDecompressedSizeExceeded () throws IOException
  {
    try
    {
      _readAll (4 * CGlobal.BYTES_PER_MEGABYTE,
                CGlobal.BYTES_PER_MEGABYTE,
                AS4DecompressLimitInputStream.NO_LIMIT);
      fail ();
    }
    catch (final AS4SizeLimitException ex)
    {
      assertTrue (ex.getMessage ().contains ("maximum allowed size"));
    }
  }

  @Test
  public void testMaxCompressionRatioExceeded () throws IOException
  {
    // 16 MB of zeros compress to a few KB, so the ratio is way beyond 10
    try
    {
      _readAll (16 * CGlobal.BYTES_PER_MEGABYTE, AS4DecompressLimitInputStream.NO_LIMIT, 10);
      fail ();
    }
    catch (final AS4SizeLimitException ex)
    {
      assertTrue (ex.getMessage ().contains ("maximum allowed compression ratio"));
    }
  }

  @Test
  public void testRatioCheckGraceIsHonoured () throws IOException
  {
    // Everything up to the grace limit is accepted, independent of the ratio
    final int nBytes = (int) AS4DecompressLimitInputStream.RATIO_CHECK_GRACE_BYTES;
    assertEquals (nBytes, _readAll (nBytes, AS4DecompressLimitInputStream.NO_LIMIT, 1));
  }
}
