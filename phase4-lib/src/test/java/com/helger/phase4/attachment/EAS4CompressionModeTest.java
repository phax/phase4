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
package com.helger.phase4.attachment;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;

/**
 * Test class for class {@link EAS4CompressionMode}.
 *
 * @author Philip Helger
 */
public final class EAS4CompressionModeTest
{
  private static void _compressPayload (@Nonnull @WillClose final InputStream aUncompressed,
                                        @Nonnull @WillClose final OutputStream aOut) throws IOException
  {
    try (final InputStream aSrc = aUncompressed; final GZIPOutputStream aGZIPOut = new GZIPOutputStream (aOut))
    {
      StreamHelper.copyInputStreamToOutputStream (aSrc, aGZIPOut);
    }
  }

  private static void _decompressPayload (@Nonnull @WillClose final InputStream aIn,
                                          @Nonnull @WillClose final OutputStream aOut) throws IOException
  {
    try (final GZIPInputStream aGZIPIn = new GZIPInputStream (aIn))
    {
      StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aGZIPIn, aOut);
    }
  }

  @Test
  public void testCompressionStandalone () throws IOException
  {
    final byte [] aSrc = StreamHelper.getAllBytes (ClassPathResource.getInputStream ("SOAPBodyPayload.xml"));
    assertNotNull (aSrc);

    // Compression
    final NonBlockingByteArrayOutputStream aCompressedOS = new NonBlockingByteArrayOutputStream ();
    _compressPayload (new NonBlockingByteArrayInputStream (aSrc), aCompressedOS);
    final byte [] aCompressed = aCompressedOS.toByteArray ();

    // DECOMPRESSION
    final NonBlockingByteArrayOutputStream aDecompressedOS = new NonBlockingByteArrayOutputStream ();
    _decompressPayload (new NonBlockingByteArrayInputStream (aCompressed), aDecompressedOS);
    final byte [] aDecompressed = aDecompressedOS.toByteArray ();

    assertArrayEquals (aSrc, aDecompressed);
  }

  @Test
  public void testCompressionModes () throws IOException
  {
    final byte [] aSrc = StreamHelper.getAllBytes (ClassPathResource.getInputStream ("SOAPBodyPayload.xml"));
    assertNotNull (aSrc);

    for (final EAS4CompressionMode eMode : EAS4CompressionMode.values ())
    {
      // Compression
      final NonBlockingByteArrayOutputStream aCompressedOS = new NonBlockingByteArrayOutputStream ();
      try (final InputStream aIS = new NonBlockingByteArrayInputStream (aSrc);
           final OutputStream aOS = eMode.getCompressStream (aCompressedOS))
      {
        StreamHelper.copyInputStreamToOutputStream (aIS, aOS);
      }
      final byte [] aCompressed = aCompressedOS.toByteArray ();

      // Decompression
      final NonBlockingByteArrayOutputStream aDecompressedOS = new NonBlockingByteArrayOutputStream ();
      try (final InputStream aIS = eMode.getDecompressStream (new NonBlockingByteArrayInputStream (aCompressed));
           final OutputStream aOS = aDecompressedOS)
      {
        StreamHelper.copyInputStreamToOutputStream (aIS, aOS);
      }
      final byte [] aDecompressed = aDecompressedOS.toByteArray ();

      assertArrayEquals (aSrc, aDecompressed);
    }
  }
}
