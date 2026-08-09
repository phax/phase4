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
package com.helger.phase4.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * A buffered {@link InputStream} on a {@link File} that supports {@link #mark(int)} and
 * {@link #reset()} with constant heap usage, by re-positioning the underlying {@link FileChannel}
 * instead of buffering all the bytes read after the mark.<br>
 * This is relevant for the streams handed over to WSS4J: for every signed attachment,
 * <code>AttachmentContentSignatureTransform#processAttachment</code> calls
 * <code>mark (Integer.MAX_VALUE)</code> on the source stream, reads it to the end to calculate the
 * digest and calls <code>reset ()</code> afterwards, so that the attachment stays readable. On a
 * heap buffering stream (like <code>BufferedInputStream</code> or
 * <code>NonBlockingBufferedInputStream</code>) that mark/read/reset sequence keeps the complete
 * attachment on the heap, so the heap usage scales with the attachment size. With this class it
 * stays constant, and the 2 GB limit of heap buffering streams does not apply. See issue #380.<br>
 * The mark position is initially 0, so a {@link #reset()} without a preceding {@link #mark(int)}
 * re-reads from the beginning (like {@link java.io.ByteArrayInputStream}).
 *
 * @since 4.6.0
 */
@NotThreadSafe
public class MarkableFileInputStream extends InputStream
{
  /** The default size of the internal read buffer in bytes */
  public static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MarkableFileInputStream.class);

  private final FileChannel m_aChannel;
  // Read buffer; between reads always in "drain" mode: position..limit are the
  // unread bytes
  private final ByteBuffer m_aBuffer;
  // File offset of the next byte to be read from the channel (mirrors
  // FileChannel.position)
  private long m_nChannelPos = 0;
  // File offset to fall back to on reset
  private long m_nMarkPos = 0;

  /**
   * Constructor using {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @throws IOException
   *         If the file cannot be opened for reading.
   */
  public MarkableFileInputStream (@NonNull final File aFile) throws IOException
  {
    this (aFile, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructor.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @param nBufferSize
   *        The size of the internal read buffer in bytes. Must be &gt; 0.
   * @throws IOException
   *         If the file cannot be opened for reading.
   */
  public MarkableFileInputStream (@NonNull final File aFile, final int nBufferSize) throws IOException
  {
    ValueEnforcer.notNull (aFile, "File");
    ValueEnforcer.isGT0 (nBufferSize, "BufferSize");
    m_aChannel = FileChannel.open (aFile.toPath (), StandardOpenOption.READ);
    m_aBuffer = ByteBuffer.allocate (nBufferSize);
    // The buffer starts out empty
    m_aBuffer.limit (0);
  }

  /**
   * @return The offset of the next byte to be delivered by this stream.
   */
  private long _getLogicalPos ()
  {
    return m_nChannelPos - m_aBuffer.remaining ();
  }

  /**
   * Fill the internal buffer from the channel.
   *
   * @return <code>false</code> if EOF was reached.
   */
  private boolean _fill () throws IOException
  {
    m_aBuffer.clear ();
    final int nRead = m_aChannel.read (m_aBuffer);
    m_aBuffer.flip ();
    if (nRead <= 0)
      return false;
    m_nChannelPos += nRead;
    return true;
  }

  /**
   * Re-position the channel and discard all buffered bytes.
   */
  private void _seek (final long nPos) throws IOException
  {
    m_aChannel.position (nPos);
    m_nChannelPos = nPos;
    m_aBuffer.position (0).limit (0);
  }

  @Override
  public int read () throws IOException
  {
    if (!m_aBuffer.hasRemaining () && !_fill ())
      return -1;
    return m_aBuffer.get () & 0xff;
  }

  @Override
  public int read (final byte @NonNull [] aBuf, final int nOfs, final int nLen) throws IOException
  {
    Objects.checkFromIndexSize (nOfs, nLen, aBuf.length);
    if (nLen == 0)
      return 0;

    if (!m_aBuffer.hasRemaining ())
    {
      // Read large requests directly from the channel, bypassing the buffer
      if (nLen >= m_aBuffer.capacity ())
      {
        final int nRead = m_aChannel.read (ByteBuffer.wrap (aBuf, nOfs, nLen));
        if (nRead > 0)
          m_nChannelPos += nRead;
        return nRead;
      }
      if (!_fill ())
        return -1;
    }
    final int nRead = Math.min (m_aBuffer.remaining (), nLen);
    m_aBuffer.get (aBuf, nOfs, nRead);
    return nRead;
  }

  @Override
  public long skip (final long nBytes) throws IOException
  {
    if (nBytes <= 0)
      return 0;
    final long nCurPos = _getLogicalPos ();
    final long nSize = m_aChannel.size ();
    if (nCurPos >= nSize)
      return 0;
    // Never skip beyond EOF
    final long nSkipped = Math.min (nBytes, nSize - nCurPos);
    _seek (nCurPos + nSkipped);
    return nSkipped;
  }

  @Override
  public int available () throws IOException
  {
    final long nRemaining = m_aBuffer.remaining () + Math.max (0, m_aChannel.size () - m_nChannelPos);
    return nRemaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) nRemaining;
  }

  @Override
  public boolean markSupported ()
  {
    return true;
  }

  /**
   * {@inheritDoc}<br>
   * The read limit parameter is ignored: as {@link #reset()} only re-positions the underlying
   * {@link FileChannel}, the mark can never be invalidated, no matter how many bytes are read.
   */
  @Override
  public void mark (final int nReadLimit)
  {
    m_nMarkPos = _getLogicalPos ();
  }

  @Override
  public void reset () throws IOException
  {
    _seek (m_nMarkPos);
  }

  @Override
  public void close () throws IOException
  {
    m_aChannel.close ();
  }

  /**
   * Factory method that logs an error and returns <code>null</code> if the file cannot be opened -
   * same semantics as <code>FileHelper.getBufferedInputStream</code>.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @return <code>null</code> if the file cannot be opened for reading.
   */
  @Nullable
  public static MarkableFileInputStream create (@NonNull final File aFile)
  {
    try
    {
      return new MarkableFileInputStream (aFile);
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("Failed to open file '" + aFile.getAbsolutePath () + "' for reading", ex);
      return null;
    }
  }
}
