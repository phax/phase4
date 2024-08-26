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
package com.helger.phase4.server.spi;

import java.util.concurrent.atomic.AtomicInteger;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessingStatusSPI;

/**
 * Test implementation of {@link IAS4IncomingMessageProcessingStatusSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class MockAS4IncomingMessageProcessingStatusSPI implements IAS4IncomingMessageProcessingStatusSPI
{
  private static final AtomicInteger STARTED = new AtomicInteger (0);
  private static final AtomicInteger ENDED = new AtomicInteger (0);

  public void onMessageProcessingStarted (final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    STARTED.incrementAndGet ();
  }

  public void onMessageProcessingEnded (final IAS4IncomingMessageMetadata aMessageMetadata,
                                        final Exception aCaughtException)
  {
    ENDED.incrementAndGet ();
  }

  public static int getStarted ()
  {
    return STARTED.get ();
  }

  public static int getEnded ()
  {
    return ENDED.get ();
  }
}
