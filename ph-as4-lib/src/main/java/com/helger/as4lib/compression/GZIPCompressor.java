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
