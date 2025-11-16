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
package com.helger.phase4.dump;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.http.header.HttpHeaderMap;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.messaging.EAS4MessageMode;

/**
 * A simple {@link IAS4OutgoingDumper} that can be used for a single transmission and dumps it to
 * the {@link OutputStream} provided in the constructor. As there are can be retries for outgoing
 * messages, only the first try is logged and consecutive calls don't result in dump.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public class AS4OutgoingDumperSingleUse extends AbstractAS4OutgoingDumperWithHeaders <AS4OutgoingDumperSingleUse>
{
  private final AtomicBoolean m_aUsedOS = new AtomicBoolean (false);
  private final OutputStream m_aOS;

  public AS4OutgoingDumperSingleUse (@NonNull @WillNotClose final OutputStream aOS)
  {
    ValueEnforcer.notNull (aOS, "OS");
    m_aOS = aOS;
  }

  @NonNull
  protected final OutputStream getOutputStream ()
  {
    return m_aOS;
  }

  @Override
  protected OutputStream openOutputStream (@NonNull final EAS4MessageMode eMsgMode,
                                           @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                           @Nullable final IAS4IncomingMessageState aIncomingState,
                                           @NonNull @Nonempty final String sMessageID,
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
