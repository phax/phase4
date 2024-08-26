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

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * A simple {@link IAS4IncomingDumper} that can be used for a single
 * transmission and dumps it to the {@link OutputStream} provided in the
 * constructor. As there are no retries for incoming messages, the
 * implementation is pretty straight forward.
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public class AS4IncomingDumperSingleUse extends AbstractAS4IncomingDumperWithHeaders <AS4IncomingDumperSingleUse>
{
  private final AtomicBoolean m_aUsedOS = new AtomicBoolean (false);
  private final OutputStream m_aOS;

  public AS4IncomingDumperSingleUse (@Nonnull @WillNotClose final OutputStream aOS)
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
  protected OutputStream openOutputStream (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                           @Nonnull final HttpHeaderMap aHttpHeaderMap) throws IOException
  {
    if (!m_aUsedOS.compareAndSet (false, true))
      throw new IllegalStateException ("This single-use dumper was already used.");
    return m_aOS;
  }
}
