/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.base.io.stream.StreamHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.mime.CMimeType;
import com.helger.phase4.util.AS4ResourceHelper;

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
    final byte [] aBytes = "<?xml version='1.0'?>".getBytes (StandardCharsets.ISO_8859_1);

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4OutgoingAttachment aOA = AS4OutgoingAttachment.builder ().data (aBytes).mimeTypeXML ().build ();
      final WSS4JAttachment a = WSS4JAttachment.createOutgoingFileAttachment (aOA, aResHelper);
      assertNotNull (a);
      // Random ID generated if null
      assertNotNull (a.getId ());

      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getMimeType ());
      assertEquals (CMimeType.APPLICATION_XML.getAsString (), a.getUncompressedMimeType ());

      // Read content
      final byte [] aRead = StreamHelper.getAllBytes (a.getSourceStream ());
      assertEquals (new String (aBytes, StandardCharsets.ISO_8859_1), new String (aRead, StandardCharsets.ISO_8859_1));
    }
  }

  @Test
  public void testOutgoingFile () throws IOException
  {
    final File f = m_aRule.newFile ("test.xml");
    final String sXMLContent = "<?xml version='1.0'?>";
    SimpleFileIO.writeFile (f, sXMLContent.getBytes (StandardCharsets.ISO_8859_1));

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

      // Read content
      final byte [] aRead = StreamHelper.getAllBytes (a.getSourceStream ());
      assertEquals (sXMLContent, new String (aRead, StandardCharsets.ISO_8859_1));
    }
  }

  @Test
  public void testOutgoingCompression () throws IOException
  {
    final byte [] aXmlBytes = "<?xml version='1.0'?>".getBytes (StandardCharsets.ISO_8859_1);

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
      assertNotEquals (new String (aRead, StandardCharsets.ISO_8859_1),
                       new String (aXmlBytes, StandardCharsets.ISO_8859_1));
    }
  }
}
