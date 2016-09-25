/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.stream.StreamHelper;

public class CompressionTest
{

  @Test
  public void testCompression () throws IOException
  {
    final File aFile = new FileSystemResource ("data/test.xml").getAsFile ();
    try (final OutputStream oos = new FileOutputStream (aFile))
    {
      compressPayload (oos, new ClassPathResource ("PayloadXML.xml").getAsFile ());
    }

    // DECOMPRESSION
    final File aFileDecompressed = new FileSystemResource ("data/result.xml").getAsFile ();
    final OutputStream aDecompress = new FileOutputStream (aFileDecompressed);

    try (final InputStream aIn = new FileInputStream (aFile))
    {
      decompressPayload (aIn, aDecompress);
    }
  }

  public void compressPayload (@Nonnull @WillClose final OutputStream aOut, final File aFile) throws IOException
  {
    try (final GZIPOutputStream aGZIPOut = new GZIPOutputStream (aOut))
    {
      aGZIPOut.write (Files.readAllBytes (aFile.toPath ()));
    }
  }

  public void decompressPayload (final InputStream aIn, final OutputStream aOut) throws IOException
  {
    final GZIPInputStream aGZIPIn = new GZIPInputStream (aIn);
    StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aGZIPIn, aOut);
  }

}
