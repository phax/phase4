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
import java.io.RandomAccessFile;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.CGlobal;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * A buffered {@link InputStream} on a {@link File} that supports {@link #mark(int)} and
 * {@link #reset()} with constant heap usage, by re-positioning the underlying
 * {@link RandomAccessFile} instead of buffering all the bytes read after the mark.<br>
 * This is relevant for the streams handed over to WSS4J: for every signed attachment,
 * <code>AttachmentContentSignatureTransform#processAttachment</code> calls
 * <code>mark (Integer.MAX_VALUE)</code> on the source stream, reads it to the end to calculate the
 * digest and calls <code>reset ()</code> afterwards, so that the attachment stays readable. On a
 * heap buffering stream (like <code>BufferedInputStream</code> or
 * <code>NonBlockingBufferedInputStream</code>) that mark/read/reset sequence keeps the complete
 * attachment on the heap, so the heap usage scales with the attachment size. With this class it
 * stays constant, and the 2 GB limit of heap buffering streams does not apply. See issue #380.<br>
 * The mark position is initially 0, so a {@link #reset()} without a preceding {@link #mark(int)}
 * re-reads from the beginning (like {@link java.io.ByteArrayInputStream}).<br>
 * Note: a {@link RandomAccessFile} is used and not a {@link java.nio.channels.FileChannel}, because
 * every {@link java.nio.channels.FileChannel} - no matter if it was created via
 * <code>FileChannel.open (...)</code> or via <code>RandomAccessFile.getChannel ()</code> - is an
 * {@link java.nio.channels.InterruptibleChannel}: it is closed permanently if the reading thread
 * has its interrupt flag set. That would make this stream fail where the previously used
 * {@link java.io.FileInputStream} based streams work fine.
 *
 * @since 4.6.0
 */
@NotThreadSafe
public class MarkableFileInputStream extends InputStream
{
  /** The default size of the internal read buffer in bytes */
  public static final int DEFAULT_BUFFER_SIZE = 16 * CGlobal.BYTES_PER_KILOBYTE;

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (MarkableFileInputStream.class);

  // Deliberately a RandomAccessFile and not a FileChannel: every FileChannel is
  // an InterruptibleChannel and is closed permanently if the reading thread has
  // its interrupt flag set - which happens in servlet containers (request
  // timeouts) or on pooled threads with a leftover flag. That also applies to
  // RandomAccessFile.getChannel(), so only the plain java.io API avoids it and
  // keeps the behaviour of the FileInputStream based streams used before.
  // FileChannel is ~1-4% faster on a warm page cache, which is not worth it
  private final RandomAccessFile m_aRAF;

  // Read buffer; the bytes from m_nBufPos (inclusive) to m_nBufLen (exclusive)
  // are the unread ones
  private final byte [] m_aBuffer;
  private int m_nBufPos = 0;
  private int m_nBufLen = 0;

  // File offset of the next byte to be read from the file (mirrors
  // RandomAccessFile.getFilePointer)
  private long m_nFilePos = 0;

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

    m_aRAF = new RandomAccessFile (aFile, "r");
    m_aBuffer = new byte [nBufferSize];
  }

  /**
   * @return The offset of the next byte to be delivered by this stream.
   */
  private long _getLogicalPos ()
  {
    return m_nFilePos - (m_nBufLen - m_nBufPos);
  }

  /**
   * Discard all buffered bytes.
   */
  private void _dropBuffer ()
  {
    m_nBufPos = 0;
    m_nBufLen = 0;
  }

  /**
   * Fill the internal buffer from the file.
   *
   * @return <code>false</code> if EOF was reached.
   */
  private boolean _fill () throws IOException
  {
    // Drop the previous content first, so that a failing read cannot leave
    // stale bytes behind that would silently be delivered afterwards
    _dropBuffer ();

    final int nReadBytes = m_aRAF.read (m_aBuffer, 0, m_aBuffer.length);
    if (nReadBytes <= 0)
      return false;

    m_nBufLen = nReadBytes;
    m_nFilePos += nReadBytes;
    return true;
  }

  /**
   * Re-position the file and discard all buffered bytes.
   */
  private void _seek (final long nPos) throws IOException
  {
    // Drop the buffer first - if the seek fails, the logical position is then
    // still identical to the unchanged file pointer
    _dropBuffer ();
    m_aRAF.seek (nPos);
    m_nFilePos = nPos;
  }

  @Override
  public int read () throws IOException
  {
    if (m_nBufPos >= m_nBufLen && !_fill ())
      return -1;

    return m_aBuffer[m_nBufPos++] & 0xff;
  }

  @Override
  public int read (final byte @NonNull [] aBuf, final int nOfs, final int nLen) throws IOException
  {
    Objects.checkFromIndexSize (nOfs, nLen, aBuf.length);
    if (nLen == 0)
      return 0;

    if (m_nBufPos >= m_nBufLen)
    {
      // Read large requests directly from the file, bypassing the buffer -
      // only correct while the buffer is empty, because the file pointer is
      // ahead of the logical position by the number of buffered bytes, so a
      // direct read with bytes still pending would silently skip them
      if (nLen >= m_aBuffer.length)
      {
        final int nRead = m_aRAF.read (aBuf, nOfs, nLen);
        if (nRead > 0)
          m_nFilePos += nRead;
        return nRead;
      }

      if (!_fill ())
        return -1;
    }
    final int nRead = Math.min (m_nBufLen - m_nBufPos, nLen);
    System.arraycopy (m_aBuffer, m_nBufPos, aBuf, nOfs, nRead);
    m_nBufPos += nRead;
    return nRead;
  }

  @Override
  public long skip (final long nBytes) throws IOException
  {
    if (nBytes <= 0)
      return 0;

    final long nCurPos = _getLogicalPos ();
    final long nSize = m_aRAF.length ();
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
    final long nRemaining = (m_nBufLen - m_nBufPos) + Math.max (0, m_aRAF.length () - m_nFilePos);
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
   * {@link RandomAccessFile}, the mark can never be invalidated, no matter how many bytes are read.
   */
  @SuppressWarnings ("sync-override")
  @Override
  public void mark (final int nReadLimit)
  {
    m_nMarkPos = _getLogicalPos ();
  }

  @SuppressWarnings ("sync-override")
  @Override
  public void reset () throws IOException
  {
    _seek (m_nMarkPos);
  }

  @Override
  public void close () throws IOException
  {
    // Drop the buffer, so that reading after close cannot deliver stale bytes
    // but consistently fails like all the other methods
    _dropBuffer ();
    m_aRAF.close ();
  }

  /**
   * Factory method that logs a warning and returns <code>null</code> if the file cannot be opened -
   * the same <code>null</code> semantics as <code>FileHelper.getBufferedInputStream</code>.
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
