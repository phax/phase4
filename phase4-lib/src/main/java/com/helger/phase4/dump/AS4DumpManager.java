/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.dump;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.WrappedInputStream;
import com.helger.commons.wrapper.Wrapper;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * This class holds the global stream dumpers.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@ThreadSafe
public final class AS4DumpManager
{
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IAS4IncomingDumper s_aIncomingDumper;
  @GuardedBy ("RW_LOCK")
  private static IAS4OutgoingDumper s_aOutgoingDumper;

  private AS4DumpManager ()
  {}

  /**
   * @return The incoming dumper. May be <code>null</code>.
   */
  @Nullable
  public static IAS4IncomingDumper getIncomingDumper ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aIncomingDumper);
  }

  /**
   * Set the incoming dumper to be globally used.
   *
   * @param aIncomingDumper
   *        The new dumper. May be <code>null</code>.
   */
  public static void setIncomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
  {
    RW_LOCK.writeLocked ( () -> s_aIncomingDumper = aIncomingDumper);
  }

  /**
   * @return The outgoing dumper. May be <code>null</code>.
   */
  @Nullable
  public static IAS4OutgoingDumper getOutgoingDumper ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aOutgoingDumper);
  }

  /**
   * Set the outgoing dumper to be globally used.
   *
   * @param aOutgoingDumper
   *        The new dumper. May be <code>null</code>.
   */
  public static void setOutgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
  {
    RW_LOCK.writeLocked ( () -> s_aOutgoingDumper = aOutgoingDumper);
  }

  /**
   * @param aIncomingDumper
   *        The incoming AS4 dumper. May be <code>null</code>. The caller is
   *        responsible to invoke {@link AS4DumpManager#getIncomingDumper()}
   *        outside.
   * @param aRequestInputStream
   *        The InputStream to read the request payload from. Will not be closed
   *        internally. Never <code>null</code>.
   * @param aMessageMetadata
   *        Request metadata. Never <code>null</code>.
   * @param aHttpHeaders
   *        the HTTP headers of the current request. Never <code>null</code>.
   * @param aDumpOSHolder
   *        A wrapper that holds the debug output stream. This can be used to
   *        determine if the message should be dumped or not. Parameter was
   *        added in v1.3.0. Do not close the received OutputStream, that is
   *        done internally automatically.
   * @return the InputStream to be used. The caller is responsible for closing
   *         the stream. Never <code>null</code>.
   * @throws IOException
   *         In case of IO error
   */
  @Nonnull
  public static InputStream getIncomingDumpAwareInputStream (@Nullable final IAS4IncomingDumper aIncomingDumper,
                                                             @Nonnull @WillNotClose final InputStream aRequestInputStream,
                                                             @Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                             @Nonnull final HttpHeaderMap aHttpHeaders,
                                                             @Nonnull final Wrapper <OutputStream> aDumpOSHolder) throws IOException
  {
    if (aIncomingDumper == null)
    {
      // No wrapping needed
      return aRequestInputStream;
    }

    // Dump worthy?
    final OutputStream aDumpOS = aIncomingDumper.onNewRequest (aMessageMetadata, aHttpHeaders);
    if (aDumpOS == null)
    {
      // No wrapping needed
      return aRequestInputStream;
    }

    // Remember where we dump to
    aDumpOSHolder.set (aDumpOS);

    // Read and write at once
    return new WrappedInputStream (aRequestInputStream)
    {
      private boolean m_bClosed = false;

      @Override
      public int read () throws IOException
      {
        final int ret = super.read ();
        if (ret != -1)
        {
          // Dump byte
          aDumpOS.write (ret & 0xff);
        }
        return ret;
      }

      @Override
      public int read (final byte [] b, final int nOffset, final int nLength) throws IOException
      {
        final int ret = super.read (b, nOffset, nLength);
        if (ret != -1)
        {
          // Dump bytes
          aDumpOS.write (b, nOffset, ret);
        }
        return ret;
      }

      @Override
      public void close () throws IOException
      {
        // Make sure to close only once (see #120)
        if (!m_bClosed)
        {
          try
          {
            // Flush and close output stream as well
            StreamHelper.flush (aDumpOS);
            StreamHelper.close (aDumpOS);
            m_bClosed = true;
          }
          finally
          {
            super.close ();
          }
        }
      }
    };
  }
}
