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
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.messaging.EAS4MessageMode;

/**
 * A simple {@link IAS4OutgoingDumper} that can be used for a single
 * transmission and dumps it to the {@link OutputStream} provided in the
 * constructor. As there are can be retries for outgoing messages, only the
 * first try is logged and consecutive calls don't result in dump.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public class AS4OutgoingDumperSingleUse extends AbstractAS4OutgoingDumperWithHeaders <AS4OutgoingDumperSingleUse>
{
  private final AtomicBoolean m_aUsedOS = new AtomicBoolean (false);
  private final OutputStream m_aOS;

  public AS4OutgoingDumperSingleUse (@Nonnull @WillNotClose final OutputStream aOS)
  {
    ValueEnforcer.notNull (aOS, "OS");
    m_aOS = aOS;
  }

  @Nonnull
  protected final OutputStream getOutputStream ()
  {
    return m_aOS;
  }

  @Override
  protected OutputStream openOutputStream (@Nonnull final EAS4MessageMode eMsgMode,
                                           @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                           @Nullable final IAS4IncomingMessageState aIncomingState,
                                           @Nonnull @Nonempty final String sMessageID,
                                           @Nullable final HttpHeaderMap aCustomHeaders,
                                           @Nonnegative final int nTry) throws IOException
  {
    if (nTry > 0)
    {
      // It's a retry - no logging anyway
      return null;
    }

    if (!m_aUsedOS.compareAndSet (false, true))
      throw new IllegalStateException ("This single-use dumper was already used.");
    return m_aOS;
  }
}
