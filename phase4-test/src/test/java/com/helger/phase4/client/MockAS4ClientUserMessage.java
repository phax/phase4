package com.helger.phase4.client;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.WillNotClose;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.messaging.http.GenericAS4HttpResponseHandler;
import com.helger.phase4.server.spi.MockAS4IncomingMessageProcessingStatusSPI;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

import jakarta.mail.MessagingException;

final class MockAS4ClientUserMessage extends AS4ClientUserMessage
{
  public MockAS4ClientUserMessage (@NonNull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (aResHelper);
  }

  @Nullable
  @VisibleForTesting
  public IMicroDocument sendMessageAndGetMicroDocument (@NonNull final String sURL) throws WSSecurityException,
                                                                                    IOException,
                                                                                    MessagingException
  {
    final int nOldStarted = MockAS4IncomingMessageProcessingStatusSPI.getStarted ();
    final int nOldEnded = MockAS4IncomingMessageProcessingStatusSPI.getEnded ();

    final IAS4ClientBuildMessageCallback aCallback = null;
    final IAS4OutgoingDumper aOutgoingDumper = null;
    final IAS4RetryCallback aRetryCallback = null;
    final IMicroDocument ret = sendMessageWithRetries (sURL,
                                                       GenericAS4HttpResponseHandler.getHandlerMicroDom (),
                                                       aCallback,
                                                       aOutgoingDumper,
                                                       aRetryCallback).getResponseContent ();
    AS4HttpDebug.debug ( () -> "SEND-RESPONSE received: " +
                               MicroWriter.getNodeAsString (ret, AS4HttpDebug.getDebugXMLWriterSettings ()));

    final int nNewStarted = MockAS4IncomingMessageProcessingStatusSPI.getStarted ();
    final int nNewEnded = MockAS4IncomingMessageProcessingStatusSPI.getEnded ();
    assertTrue (nNewStarted > nOldStarted);
    assertTrue (nNewEnded > nOldEnded);

    return ret;
  }
}