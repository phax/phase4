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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.io.stream.WrappedInputStream;

/**
 * An {@link InputStream} that remembers the first exception thrown by the wrapped stream. This
 * allows to tell a failure of the underlying stream (e.g. decompression or size limit) apart from a
 * failure of the consumer (e.g. an XML parser that wraps the exception into its own one).
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class AS4ReadErrorRecordingInputStream extends WrappedInputStream
{
  private Exception m_aReadError;

  /**
   * Constructor
   *
   * @param aWrappedIS
   *        The input stream to be wrapped. May not be <code>null</code>.
   */
  public AS4ReadErrorRecordingInputStream (@NonNull final InputStream aWrappedIS)
  {
    super (aWrappedIS);
  }

  private void _remember (@NonNull final Exception ex)
  {
    if (m_aReadError == null)
      m_aReadError = ex;
  }

  @Override
  public int read () throws IOException
  {
    try
    {
      return super.read ();
    }
    catch (final IOException | RuntimeException ex)
    {
      _remember (ex);
      throw ex;
    }
  }

  @Override
  public int read (final byte @NonNull [] aBuf, final int nOfs, final int nLen) throws IOException
  {
    try
    {
      return super.read (aBuf, nOfs, nLen);
    }
    catch (final IOException | RuntimeException ex)
    {
      _remember (ex);
      throw ex;
    }
  }

  @Override
  public long skip (final long n) throws IOException
  {
    try
    {
      return super.skip (n);
    }
    catch (final IOException | RuntimeException ex)
    {
      _remember (ex);
      throw ex;
    }
  }

  @Override
  public int available () throws IOException
  {
    try
    {
      return super.available ();
    }
    catch (final IOException | RuntimeException ex)
    {
      _remember (ex);
      throw ex;
    }
  }

  /**
   * @return The first exception thrown by the wrapped stream, or <code>null</code> if none was
   *         thrown so far.
   */
  @Nullable
  public final Exception getReadError ()
  {
    return m_aReadError;
  }

  /**
   * @return <code>true</code> if the wrapped stream threw an exception.
   */
  public final boolean hasReadError ()
  {
    return m_aReadError != null;
  }
}
