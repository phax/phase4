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
