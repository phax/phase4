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