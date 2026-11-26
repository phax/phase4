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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;

/**
 * Test class for class {@link AS4ReadErrorRecordingInputStream}.
 *
 * @author Philip Helger
 */
public final class AS4ReadErrorRecordingInputStreamTest
{
  @Test
  public void testNoError () throws IOException
  {
    try (final AS4ReadErrorRecordingInputStream aIS = new AS4ReadErrorRecordingInputStream (new NonBlockingByteArrayInputStream (new byte [] { 1,
                                                                                                                                                2 })))
    {
      assertEquals (2, aIS.readAllBytes ().length);
      assertFalse (aIS.hasReadError ());
      assertNull (aIS.getReadError ());
    }
  }

  @Test
  public void testError ()
  {
    final IOException aEx = new IOException ("bla");
    final InputStream aFailing = new InputStream ()
    {
      @Override
      public int read () throws IOException
      {
        throw aEx;
      }
    };
    final AS4ReadErrorRecordingInputStream aIS = new AS4ReadErrorRecordingInputStream (aFailing);
    try
    {
      aIS.read (new byte [10], 0, 10);
      fail ();
    }
    catch (final IOException ex)
    {
      assertSame (aEx, ex);
    }
    assertTrue (aIS.hasReadError ());
    assertSame (aEx, aIS.getReadError ());
  }
}
