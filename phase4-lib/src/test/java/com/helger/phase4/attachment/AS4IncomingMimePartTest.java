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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;

import jakarta.mail.MessagingException;

/**
 * Test class for class {@link AS4IncomingMimePart}.
 *
 * @author Philip Helger
 */
public final class AS4IncomingMimePartTest
{
  private static final int DEFAULT_MAX_HEADER_SIZE_BYTES = 64 * 1024;

  @NonNull
  private static byte [] _createPartBytes (@NonNull final String sHeaderSection, final byte @NonNull [] aContent)
  {
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      aBAOS.write (sHeaderSection.getBytes (StandardCharsets.ISO_8859_1));
      aBAOS.write ("\r\n".getBytes (StandardCharsets.ISO_8859_1));
      aBAOS.write (aContent);
      return aBAOS.toByteArray ();
    }
  }

  @Test
  public void testBasic () throws MessagingException
  {
    final byte [] aContent = "Hello World".getBytes (StandardCharsets.ISO_8859_1);
    final byte [] aPart = _createPartBytes ("Content-Type: application/xml\r\n" +
                                            "Content-ID: <mycid@phase4>\r\n" +
                                            "Content-Transfer-Encoding: binary\r\n",
                                            aContent);

    final AS4IncomingMimePart aMimePart = AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart),
                                                                     DEFAULT_MAX_HEADER_SIZE_BYTES);
    assertNotNull (aMimePart);
    assertEquals ("application/xml", aMimePart.getContentType ());
    assertEquals ("<mycid@phase4>", aMimePart.getContentID ());
    assertEquals ("binary", aMimePart.getContentTransferEncoding ());

    // The content must be exactly the bytes after the empty line
    assertArrayEquals (aContent, StreamHelper.getAllBytes (aMimePart.getDecodedContentStream ()));
  }

  @Test
  public void testDefaultsWithoutHeaders () throws MessagingException
  {
    final byte [] aContent = "anything".getBytes (StandardCharsets.ISO_8859_1);
    final byte [] aPart = _createPartBytes ("", aContent);

    final AS4IncomingMimePart aMimePart = AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart),
                                                                     DEFAULT_MAX_HEADER_SIZE_BYTES);
    assertEquals (AS4IncomingMimePart.DEFAULT_CONTENT_TYPE, aMimePart.getContentType ());
    assertNull (aMimePart.getContentID ());
    assertNull (aMimePart.getContentTransferEncoding ());
    assertArrayEquals (aContent, StreamHelper.getAllBytes (aMimePart.getDecodedContentStream ()));
  }

  @Test
  public void testBase64Decoding () throws MessagingException
  {
    final byte [] aPayload = "Streaming Base64 content for issue #382".getBytes (StandardCharsets.ISO_8859_1);
    final byte [] aEncoded = Base64.getMimeEncoder ().encode (aPayload);
    final byte [] aPart = _createPartBytes ("Content-Type: application/octet-stream\r\n" +
                                            "Content-Transfer-Encoding: base64\r\n",
                                            aEncoded);

    final AS4IncomingMimePart aMimePart = AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart),
                                                                     DEFAULT_MAX_HEADER_SIZE_BYTES);
    assertEquals ("base64", aMimePart.getContentTransferEncoding ());
    // The raw content is still encoded
    assertArrayEquals (aEncoded, StreamHelper.getAllBytes (aMimePart.getRawContentStream ()));

    // Parse again for the decoded variant, as the content can only be read once
    final AS4IncomingMimePart aMimePart2 = AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart),
                                                                      DEFAULT_MAX_HEADER_SIZE_BYTES);
    assertArrayEquals (aPayload, StreamHelper.getAllBytes (aMimePart2.getDecodedContentStream ()));
  }

  @Test
  public void testContentTransferEncodingWithComment () throws MessagingException
  {
    final byte [] aPart = _createPartBytes ("Content-Transfer-Encoding: (a comment) base64\r\n", new byte [0]);

    final AS4IncomingMimePart aMimePart = AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart),
                                                                     DEFAULT_MAX_HEADER_SIZE_BYTES);
    assertEquals ("base64", aMimePart.getContentTransferEncoding ());
  }

  @Test
  public void testHeaderSectionTooLarge ()
  {
    // Header section of ~1200 bytes but limit of 100 bytes
    final StringBuilder aHeaders = new StringBuilder ();
    for (int i = 0; i < 20; ++i)
      aHeaders.append ("X-Custom-Header-").append (i).append (": some rather long header value\r\n");
    final byte [] aPart = _createPartBytes (aHeaders.toString (), "content".getBytes (StandardCharsets.ISO_8859_1));

    try
    {
      AS4IncomingMimePart.parse (new NonBlockingByteArrayInputStream (aPart), 100);
      fail ();
    }
    catch (final MessagingException ex)
    {
      // expected - the limiting stream throws an IOException that is wrapped
      // into a MessagingException by InternetHeaders
    }
  }
}
