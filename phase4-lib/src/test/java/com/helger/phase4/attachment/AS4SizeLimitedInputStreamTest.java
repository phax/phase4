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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.annotation.WillNotClose;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;

/**
 * Test class for class {@link AS4SizeLimitedInputStream}.
 *
 * @author Philip Helger
 */
public final class AS4SizeLimitedInputStreamTest
{
  private static final byte [] SRC = new byte [1000];

  /**
   * Read the whole stream, so that the {@link AS4SizeLimitException} is not swallowed as it would
   * be by <code>StreamHelper</code>.
   *
   * @param aIS
   *        The stream to read. May not be <code>null</code>.
   * @return All read bytes. Never <code>null</code>.
   * @throws IOException
   *         on error
   */
  @NonNull
  private static byte [] _readFully (@NonNull @WillNotClose final InputStream aIS) throws IOException
  {
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      final byte [] aBuf = new byte [128];
      int nRead;
      while ((nRead = aIS.read (aBuf, 0, aBuf.length)) > 0)
        aBAOS.write (aBuf, 0, nRead);
      return aBAOS.getBufferOrCopy ();
    }
  }

  @Test
  public void testBelowLimit () throws IOException
  {
    try (final AS4SizeLimitedInputStream aIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (SRC),
                                                                              "Test data",
                                                                              SRC.length))
    {
      assertTrue (aIS.isLimited ());
      assertArrayEquals (SRC, _readFully (aIS));
      assertFalse (aIS.isLimitExceeded ());
      assertEquals (SRC.length, aIS.getPosition ());
    }
  }

  @Test
  public void testAboveLimit () throws IOException
  {
    try (final AS4SizeLimitedInputStream aIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (SRC),
                                                                              "Test data",
                                                                              SRC.length - 1))
    {
      try
      {
        _readFully (aIS);
        fail ();
      }
      catch (final AS4SizeLimitException ex)
      {
        // Expected
      }
      assertTrue (aIS.isLimitExceeded ());
    }
  }

  @Test
  public void testAboveLimitSingleByteRead () throws IOException
  {
    try (final AS4SizeLimitedInputStream aIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (SRC),
                                                                              "Test data",
                                                                              10))
    {
      try
      {
        for (int i = 0; i < SRC.length; ++i)
          aIS.read ();
        fail ();
      }
      catch (final AS4SizeLimitException ex)
      {
        // Expected
      }
      assertTrue (aIS.isLimitExceeded ());
    }
  }

  @Test
  public void testNoLimit () throws IOException
  {
    try (final AS4SizeLimitedInputStream aIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (SRC),
                                                                              "Test data",
                                                                              AS4SizeLimitedInputStream.NO_LIMIT))
    {
      assertFalse (aIS.isLimited ());
      assertArrayEquals (SRC, _readFully (aIS));
      assertFalse (aIS.isLimitExceeded ());
      // Acts as a pure byte counter
      assertEquals (SRC.length, aIS.getPosition ());
    }
  }

  @Test
  public void testSkip () throws IOException
  {
    try (final InputStream aIS = new AS4SizeLimitedInputStream (new NonBlockingByteArrayInputStream (SRC),
                                                                "Test data",
                                                                10))
    {
      try
      {
        aIS.skip (SRC.length);
        fail ();
      }
      catch (final AS4SizeLimitException ex)
      {
        // Expected
      }
    }
  }
}
