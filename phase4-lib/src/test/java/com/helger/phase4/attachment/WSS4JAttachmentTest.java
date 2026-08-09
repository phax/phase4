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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.mime.CMimeType;
import com.helger.phase4.util.AS4ResourceHelper;

import jakarta.mail.MessagingException;

/**
 * Test class for class {@link WSS4JAttachment}.
 *
 * @author Philip Helger
 */
public final class WSS4JAttachmentTest
{
  @Rule
  public final TemporaryFolder m_aRule = new TemporaryFolder ();

  @SuppressWarnings ("deprecation")
  @Test
  public void testBasic ()
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final WSS4JAttachment a = new WSS4JAttachment (aResHelper, CMimeType.APPLICATION_XML.getAsString ());
      assertNotNull (a.getResHelper ());
      assertNull (a.getId ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getMimeType ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      a.setUniqueID ();
      assertNotNull (a.getId ());

      try
      {
        a.setMimeType ("foo/bar");
        fail ();
      }
      catch (final UnsupportedOperationException ex)
      {
        // expected
      }

      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getMimeType ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      a.overwriteMimeType ("foo/bar");
      assertEquals ("foo/bar", a.getMimeType ());
      assertEquals ("foo/bar", a.getUncompressedMimeType ());
    }
  }

  @Test
  public void testOutgoingBytes () throws IOException
  {
    final byte [] aBytes = "<?xml version='1.0'?>".getBytes (StandardCharsets.UTF_8);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4OutgoingAttachment aOA = AS4OutgoingAttachment.builder ().data (aBytes).mimeTypeXML ().build ();
      final WSS4JAttachment a = WSS4JAttachment.createOutgoingFileAttachment (aOA, aResHelper);
      assertNotNull (a);
      // Random ID generated if null
      assertNotNull (a.getId ());

      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getMimeType ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      // Not compressed - no compressed data present
      assertNull (a.getCompressedSourceStreamProvider ());

      // Read content
      final byte [] aRead = StreamHelper.getAllBytes (a.getSourceStream ());
      assertEquals (new String (aBytes, StandardCharsets.UTF_8), new String (aRead, StandardCharsets.UTF_8));
    }
  }

  @Test
  public void testOutgoingFile () throws IOException
  {
    final File f = m_aRule.newFile ("test.xml");
    final String sXMLContent = "<?xml version='1.0'?>";
    SimpleFileIO.writeFile (f, sXMLContent.getBytes (StandardCharsets.UTF_8));

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4OutgoingAttachment aOA = AS4OutgoingAttachment.builder ()
                                                             .data (f)
                                                             .mimeTypeXML ()
                                                             .contentID ("cid1")
                                                             .build ();
      final WSS4JAttachment a = WSS4JAttachment.createOutgoingFileAttachment (aOA, aResHelper);
      assertNotNull (a);
      assertEquals ("cid1", a.getId ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getMimeType ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      // Not compressed - no compressed data present
      assertNull (a.getCompressedSourceStreamProvider ());

      // Read content
      final byte [] aRead = StreamHelper.getAllBytes (a.getSourceStream ());
      assertEquals (sXMLContent, new String (aRead, StandardCharsets.UTF_8));
    }
  }

  @Test
  public void testOutgoingCompression () throws IOException
  {
    final byte [] aXmlBytes = "<?xml version='1.0'?>".getBytes (StandardCharsets.UTF_8);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4OutgoingAttachment aOA = AS4OutgoingAttachment.builder ()
                                                             .data (aXmlBytes)
                                                             .mimeTypeXML ()
                                                             .compressionGZIP ()
                                                             .build ();
      final WSS4JAttachment a = WSS4JAttachment.createOutgoingFileAttachment (aOA, aResHelper);
      assertNotNull (a);
      // Main MIME type is GZIP
      assertEquals (EAS4CompressionMode.GZIP.getMimeType ().getAsString (), a.getMimeType ());
      // Uncompressed MIME type is XML
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      // Read content - should be compressed
      final byte [] aRead = StreamHelper.getAllBytes (a.getSourceStream ());
      assertNotNull (aRead);
      assertTrue (aRead.length > 0);

      // It is definitely not the XML
      assertNotEquals (new String (aRead, StandardCharsets.UTF_8), new String (aXmlBytes, StandardCharsets.UTF_8));

      // The compressed data must be preserved (issue #361)
      final IHasInputStream aCompressedISP = a.getCompressedSourceStreamProvider ();
      assertNotNull (aCompressedISP);

      // It must be identical to the source stream content
      assertArrayEquals (aRead, StreamHelper.getAllBytes (aCompressedISP.getInputStream ()));

      // Decompressing the preserved compressed data must yield the original
      // payload
      final byte [] aDecompressed = StreamHelper.getAllBytes (EAS4CompressionMode.GZIP.getDecompressStream (aCompressedISP.getInputStream ()));
      assertArrayEquals (aXmlBytes, aDecompressed);
    }
  }

  @NonNull
  private static AS4IncomingMimePart _createIncomingMimePart (final byte [] aContent) throws MessagingException
  {
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      aBAOS.write (("Content-Type: application/octet-stream\r\n" +
                    "Content-ID: <mycid@phase4>\r\n" +
                    "Content-Transfer-Encoding: binary\r\n" +
                    "\r\n").getBytes (StandardCharsets.ISO_8859_1));
      aBAOS.write (aContent);
      return AS4IncomingMimePart.parse (aBAOS.getAsInputStream (), 64 * 1024);
    }
  }

  @Test
  public void testIncomingInMemory () throws IOException, MessagingException
  {
    // Content is below the threshold - must be kept in memory
    final byte [] aContent = new byte [1024];
    ThreadLocalRandom.current ().nextBytes (aContent);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final WSS4JAttachment a = WSS4JAttachment.createIncomingFileAttachment (_createIncomingMimePart (aContent),
                                                                              aResHelper);
      assertNotNull (a);
      // Content-ID is stripped of the angle brackets
      assertEquals ("mycid@phase4", a.getId ());
      assertEquals ("application/octet-stream", a.getMimeType ());

      // The content must be readable multiple times
      assertTrue (a.getInputStreamProvider ().isReadMultiple ());
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
    }
  }

  @Test
  public void testIncomingSpillToTempFile () throws IOException, MessagingException
  {
    // Content exceeds the in-memory threshold - must be spilled to a temporary
    // file
    final byte [] aContent = new byte [WSS4JAttachment.MAX_IN_MEMORY_BYTES + 1024];
    ThreadLocalRandom.current ().nextBytes (aContent);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final WSS4JAttachment a = WSS4JAttachment.createIncomingFileAttachment (_createIncomingMimePart (aContent),
                                                                              aResHelper);
      assertNotNull (a);
      assertEquals ("mycid@phase4", a.getId ());

      // The content must be readable multiple times
      assertTrue (a.getInputStreamProvider ().isReadMultiple ());
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
    }
  }

  @Test
  public void testIncomingEmpty () throws IOException, MessagingException
  {
    // Empty content - must be kept in memory
    final byte [] aContent = new byte [0];

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final WSS4JAttachment a = WSS4JAttachment.createIncomingFileAttachment (_createIncomingMimePart (aContent),
                                                                              aResHelper);
      assertNotNull (a);
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
    }
  }

  @Test
  public void testIncomingExactlyThresholdSize () throws IOException, MessagingException
  {
    // Content with exactly the threshold size - must be kept in memory
    final byte [] aContent = new byte [WSS4JAttachment.MAX_IN_MEMORY_BYTES];
    ThreadLocalRandom.current ().nextBytes (aContent);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final WSS4JAttachment a = WSS4JAttachment.createIncomingFileAttachment (_createIncomingMimePart (aContent),
                                                                              aResHelper);
      assertNotNull (a);
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
      assertArrayEquals (aContent, StreamHelper.getAllBytes (a.getSourceStream ()));
    }
  }
}
