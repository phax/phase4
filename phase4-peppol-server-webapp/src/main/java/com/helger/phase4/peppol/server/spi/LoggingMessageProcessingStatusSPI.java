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
