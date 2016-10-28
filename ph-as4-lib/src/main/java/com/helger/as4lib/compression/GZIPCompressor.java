/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.compression;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import com.helger.commons.io.stream.StreamHelper;

public class GZIPCompressor
{
  public static void compressPayload (@Nonnull @WillClose final OutputStream aOut, final File aFile) throws IOException
  {
    try (final GZIPOutputStream aGZIPOut = new GZIPOutputStream (aOut))
    {
      aGZIPOut.write (Files.readAllBytes (aFile.toPath ()));
    }
  }

  public static void decompressPayload (final InputStream aIn, final OutputStream aOut) throws IOException
  {
    final GZIPInputStream aGZIPIn = new GZIPInputStream (aIn);
    StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aGZIPIn, aOut);
  }
}
