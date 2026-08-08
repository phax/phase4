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
package com.helger.phase4.sender;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.soapfault.AS4SoapFault;

/**
 * Default implementation of {@link IAS4SoapFaultConsumer} that logs the received SOAP Fault on
 * error level.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public class LoggingAS4SoapFaultConsumer implements IAS4SoapFaultConsumer
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (LoggingAS4SoapFaultConsumer.class);

  public void handleSoapFault (@Nullable final String sSentMessageID,
                               @NonNull final AS4SoapFault aSoapFault,
                               @Nullable final AS4ClientSentMessage <byte []> aClientSentMessage)
  {
    LOGGER.error ("Received a SOAP " +
                  aSoapFault.getSoapVersion ().getVersion () +
                  " Fault as the response to AS4 message '" +
                  sSentMessageID +
                  "' with code " +
                  (aSoapFault.hasFaultCode () ? "'" + aSoapFault.getFaultCode () + "'" : "<none>") +
                  " and reason " +
                  (aSoapFault.getFaultReason () != null ? "'" + aSoapFault.getFaultReason () + "'" : "<none>") +
                  " (disposition " +
                  aSoapFault.getDisposition () +
                  ")");
  }
}
