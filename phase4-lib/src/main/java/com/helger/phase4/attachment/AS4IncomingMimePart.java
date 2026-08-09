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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.WrappedInputStream;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.http.CHttpHeader;

import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.HeaderTokenizer;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeUtility;

/**
 * Represents a single MIME part of an incoming multipart message. In contrast to
 * {@link jakarta.mail.internet.MimeBodyPart} the part content is NOT read into memory. Only the
 * header section is parsed eagerly - the content stays on the underlying stream and can be consumed
 * exactly once, in a streaming way. See issue #382.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@NotThreadSafe
public final class AS4IncomingMimePart
{
  /** The default MIME type, if no Content-Type header is present. Same as in MimeBodyPart. */
  public static final String DEFAULT_CONTENT_TYPE = "text/plain";

  /**
   * An InputStream wrapper that throws an {@link IOException} as soon as more than the provided
   * number of bytes were read. Used to limit the size of the MIME part header section.
   *
   * @author Philip Helger
   */
  private static final class SizeLimitedInputStream extends WrappedInputStream
  {
    private final int m_nMaxBytes;
    private int m_nReadBytes = 0;

    SizeLimitedInputStream (@NonNull final InputStream aSrcIS, final int nMaxBytes)
    {
      super (aSrcIS);
      m_nMaxBytes = nMaxBytes;
    }

    private void _checkLimit (final int nBytesRead) throws IOException
    {
      m_nReadBytes += nBytesRead;
      if (m_nReadBytes > m_nMaxBytes)
        throw new IOException ("The MIME part header section exceeds the maximum allowed size of " +
                               m_nMaxBytes +
                               " bytes");
    }

    @Override
    public int read () throws IOException
    {
      final int ret = super.read ();
      if (ret >= 0)
        _checkLimit (1);
      return ret;
    }

    @Override
    public int read (final byte [] aBuf, final int nOfs, final int nLen) throws IOException
    {
      final int ret = super.read (aBuf, nOfs, nLen);
      if (ret > 0)
        _checkLimit (ret);
      return ret;
    }
  }

  private final InternetHeaders m_aHeaders;
  private final InputStream m_aRawContentIS;

  private AS4IncomingMimePart (@NonNull final InternetHeaders aHeaders, @NonNull final InputStream aRawContentIS)
  {
    m_aHeaders = aHeaders;
    m_aRawContentIS = aRawContentIS;
  }

  /**
   * @return An enumeration of all headers of this MIME part. Never <code>null</code>.
   */
  @NonNull
  public Enumeration <Header> getAllHeaders ()
  {
    return m_aHeaders.getAllHeaders ();
  }

  /**
   * @return The value of the <code>Content-Type</code> header of this MIME part or
   *         {@value #DEFAULT_CONTENT_TYPE} if not present. Never <code>null</code>.
   */
  @NonNull
  public String getContentType ()
  {
    final String ret = m_aHeaders.getHeader (CHttpHeader.CONTENT_TYPE, null);
    return ret != null ? ret : DEFAULT_CONTENT_TYPE;
  }

  /**
   * @return The value of the <code>Content-ID</code> header of this MIME part, usually including
   *         the surrounding angle brackets. May be <code>null</code>.
   */
  @Nullable
  public String getContentID ()
  {
    return m_aHeaders.getHeader (CHttpHeader.CONTENT_ID, null);
  }

  /**
   * @return The content transfer encoding of this MIME part, determined with the same semantics as
   *         <code>MimeBodyPart.getEncoding ()</code>. May be <code>null</code>.
   * @throws MessagingException
   *         If the Content-Transfer-Encoding header cannot be tokenized.
   */
  @Nullable
  public String getContentTransferEncoding () throws MessagingException
  {
    String sCTE = m_aHeaders.getHeader (CHttpHeader.CONTENT_TRANSFER_ENCODING, null);
    if (sCTE == null)
      return null;

    sCTE = sCTE.trim ();
    if (sCTE.isEmpty ())
      return null;

    // Quick check for the well-known values, to avoid the tokenizer
    if (sCTE.equalsIgnoreCase ("7bit") ||
      sCTE.equalsIgnoreCase ("8bit") ||
      sCTE.equalsIgnoreCase ("binary") ||
      sCTE.equalsIgnoreCase ("base64") ||
      sCTE.equalsIgnoreCase ("quoted-printable"))
      return sCTE;

    // Tokenize the header to obtain the encoding (skipping comments)
    final HeaderTokenizer aTokenizer = new HeaderTokenizer (sCTE, HeaderTokenizer.MIME);
    while (true)
    {
      final HeaderTokenizer.Token aToken = aTokenizer.next ();
      final int nTokenType = aToken.getType ();
      if (nTokenType == HeaderTokenizer.Token.EOF)
        break;
      if (nTokenType == HeaderTokenizer.Token.ATOM)
        return aToken.getValue ();
      // Invalid token - skip it
    }
    return sCTE;
  }

  /**
   * @return The raw, potentially still transfer-encoded, content of this MIME part. The stream can
   *         be consumed only once. Never <code>null</code>.
   */
  @NonNull
  public InputStream getRawContentStream ()
  {
    return m_aRawContentIS;
  }

  /**
   * @return The content of this MIME part with the content transfer encoding applied in a streaming
   *         way (relevant e.g. for <code>base64</code>). The stream can be consumed only once.
   *         Never <code>null</code>.
   * @throws MessagingException
   *         If the content transfer encoding is unknown.
   */
  @NonNull
  public InputStream getDecodedContentStream () throws MessagingException
  {
    final String sCTE = getContentTransferEncoding ();
    if (StringHelper.isEmpty (sCTE))
      return m_aRawContentIS;
    return MimeUtility.decode (m_aRawContentIS, sCTE);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Headers", m_aHeaders)
                                       .append ("RawContentIS", m_aRawContentIS)
                                       .getToString ();
  }

  /**
   * Parse the header section of a single MIME part from the provided stream. Only the header
   * section (until the first empty line) is read from the stream - the part content stays on the
   * stream and must be consumed via {@link #getDecodedContentStream()} or
   * {@link #getRawContentStream()} while the stream is still open.
   *
   * @param aPartIS
   *        The stream containing a single MIME part (headers and content, without the multipart
   *        boundaries). May not be <code>null</code>.
   * @param nMaxHeaderSizeBytes
   *        The maximum number of bytes the header section may have. Must be &gt; 0.
   * @return The parsed MIME part. Never <code>null</code>.
   * @throws MessagingException
   *         If reading the headers fails or the header section exceeds the provided maximum size.
   */
  @NonNull
  public static AS4IncomingMimePart parse (@NonNull @WillNotClose final InputStream aPartIS,
                                           final int nMaxHeaderSizeBytes) throws MessagingException
  {
    ValueEnforcer.notNull (aPartIS, "PartIS");
    ValueEnforcer.isGT0 (nMaxHeaderSizeBytes, "MaxHeaderSizeBytes");

    // This reads exactly the header section from the stream, as the line based
    // reading never reads ahead of the current line
    final InternetHeaders aHeaders = new InternetHeaders (new SizeLimitedInputStream (aPartIS, nMaxHeaderSizeBytes));
    return new AS4IncomingMimePart (aHeaders, aPartIS);
  }
}
