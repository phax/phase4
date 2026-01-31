/*
 * Copyright (C) 2020-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.spi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessingStatusSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * Logging implementation of {@link IAS4IncomingMessageProcessingStatusSPI}.
 *
 * @author Philip Helger
 */
public class LoggingMessageProcessingStatusSPI implements IAS4IncomingMessageProcessingStatusSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (LoggingMessageProcessingStatusSPI.class);

  public void onMessageProcessingStarted (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    LOGGER.info ("[ProcessingStatusSPI] Start processing " + aMessageMetadata.getIncomingUniqueID ());
  }

  public void onMessageProcessingEnded (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                        @Nullable final Exception aCaughtException)
  {
    LOGGER.info ("[ProcessingStatusSPI] Finished processing " + aMessageMetadata.getIncomingUniqueID (),
                 aCaughtException);
  }
}
