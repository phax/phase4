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
package com.helger.phase4.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.io.file.SimpleFileIO;

/**
 * Test class for class {@link MarkableFileInputStream}.
 */
public final class MarkableFileInputStreamTest
{
  @Rule
  public final TemporaryFolder m_aRule = new TemporaryFolder ();

  @NonNull
  private static byte [] _createRandomBytes (final int nLen)
  {
    final byte [] ret = new byte [nLen];
    // Deterministic seed
    new Random (20260803L).nextBytes (ret);
    return ret;
  }

  @NonNull
  private File _createFile (final byte [] aContent) throws IOException
  {
    final File ret = m_aRule.newFile ();
    SimpleFileIO.writeFile (ret, aContent);
    return ret;
  }

  /**
   * Consume the stream exactly the way WSS4J's
   * <code>AttachmentContentSignatureTransform#processAttachment</code> does when calculating the
   * digest of a signed attachment: <code>mark (Integer.MAX_VALUE)</code>, read to the end,
   * <code>reset ()</code>. Afterwards WSS4J re-uses the same stream object for the attachment
   * content, so a subsequent full read must deliver the identical bytes from the start.
   */
  @NonNull
  private static byte [] _digestLikeWSS4J (@NonNull final InputStream aIS) throws IOException
  {
    assertTrue ("WSS4J relies on mark/reset support", aIS.markSupported ());
    aIS.mark (Integer.MAX_VALUE);
    final byte [] ret = _readAll (aIS);
    aIS.reset ();
    return ret;
  }

  @NonNull
  private static byte [] _readAll (@NonNull final InputStream aIS) throws IOException
  {
    try (final NonBlockingByteArrayOutputStream aAll = new NonBlockingByteArrayOutputStream ())
    {
      int nBytes;
      // Same buffer size as WSS4J uses
      final byte [] aBuf = new byte [8192];
      while ((nBytes = aIS.read (aBuf)) != -1)
        aAll.write (aBuf, 0, nBytes);
      return aAll.toByteArray ();
    }
  }

  @Test
  public void testDigestThenReRead () throws IOException
  {
    // Clearly larger than the internal buffer
    final byte [] aPayload = _createRandomBytes (1024 * 1024 + 17);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f))
    {
      assertArrayEquals ("The digested bytes must be the file content", aPayload, _digestLikeWSS4J (aIS));

      // Re-read the same stream object from the beginning
      assertArrayEquals ("The content after reset must be identical", aPayload, _readAll (aIS));
    }
  }

  @Test
  public void testMarkAtNonZeroOffset () throws IOException
  {
    final byte [] aPayload = _createRandomBytes (100_000);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f))
    {
      // Read some bytes, crossing the internal buffer boundary
      final byte [] aHead = new byte [MarkableFileInputStream.DEFAULT_BUFFER_SIZE + 100];
      int nRead = 0;
      while (nRead < aHead.length)
        nRead += aIS.read (aHead, nRead, aHead.length - nRead);
      assertArrayEquals (Arrays.copyOf (aPayload, aHead.length), aHead);

      aIS.mark (Integer.MAX_VALUE);
      final byte [] aTail1 = _readAll (aIS);
      aIS.reset ();
      final byte [] aTail2 = _readAll (aIS);
      assertArrayEquals (Arrays.copyOfRange (aPayload, aHead.length, aPayload.length), aTail1);
      assertArrayEquals (aTail1, aTail2);
    }
  }

  @Test
  public void testResetWithoutMarkReReadsFromStart () throws IOException
  {
    final byte [] aPayload = _createRandomBytes (1000);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f))
    {
      assertArrayEquals (aPayload, _readAll (aIS));
      aIS.reset ();
      assertArrayEquals (aPayload, _readAll (aIS));
    }
  }

  @Test
  public void testSingleByteRead () throws IOException
  {
    // Small file, read byte by byte
    final byte [] aPayload = _createRandomBytes (100);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f, 8))
    {
      for (final byte b : aPayload)
        assertEquals (b & 0xff, aIS.read ());
      assertEquals (-1, aIS.read ());
    }
  }

  @Test
  public void testEmptyFile () throws IOException
  {
    final File f = _createFile (new byte [0]);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f))
    {
      assertEquals (0, aIS.available ());
      assertEquals (-1, aIS.read ());
      aIS.mark (Integer.MAX_VALUE);
      aIS.reset ();
      assertEquals (-1, aIS.read ());
    }
  }

  @Test
  public void testSkipAndAvailable () throws IOException
  {
    final byte [] aPayload = _createRandomBytes (50_000);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f))
    {
      assertEquals (aPayload.length, aIS.available ());
      assertEquals (0, aIS.skip (0));
      assertEquals (0, aIS.skip (-1));
      assertEquals (10_000, aIS.skip (10_000));
      assertEquals (aPayload.length - 10_000, aIS.available ());
      assertEquals (aPayload[10_000] & 0xff, aIS.read ());
      // Skipping beyond EOF is truncated
      assertEquals (aPayload.length - 10_001, aIS.skip (Long.MAX_VALUE));
      assertEquals (0, aIS.skip (1));
      assertEquals (-1, aIS.read ());
      assertEquals (0, aIS.available ());
    }
  }

  @Test
  public void testReadLargerThanBuffer () throws IOException
  {
    // Requests larger than the internal buffer are served from the channel
    // directly
    final byte [] aPayload = _createRandomBytes (70_000);
    final File f = _createFile (aPayload);

    try (final MarkableFileInputStream aIS = new MarkableFileInputStream (f, 1024))
    {
      aIS.mark (Integer.MAX_VALUE);
      final byte [] aBuf = new byte [aPayload.length];
      int nTotal = 0;
      int nRead;
      while (nTotal < aBuf.length && (nRead = aIS.read (aBuf, nTotal, aBuf.length - nTotal)) != -1)
        nTotal += nRead;
      assertEquals (aPayload.length, nTotal);
      assertArrayEquals (aPayload, aBuf);
      assertEquals (-1, aIS.read ());

      // The mark must still be valid, no matter how many bytes were read
      aIS.reset ();
      assertArrayEquals (aPayload, _readAll (aIS));
    }
  }

  @Test
  public void testCreateNonExistingFile ()
  {
    assertNull (MarkableFileInputStream.create (new File (m_aRule.getRoot (), "does-not-exist.bin")));
  }
}
