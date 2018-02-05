/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.server.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.servlet.IAS4MessageState;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Example implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class ExampleReceiveMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ExampleReceiveMessageProcessorSPI.class);

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState)
  {
    s_aLogger.info ("Received AS4 user message");

    return AS4MessageProcessorResult.createSuccess (aIncomingAttachments, null);
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4MessageState aState)
  {
    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      s_aLogger.info ("Received AS4 Receipt");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      s_aLogger.info ("Received AS4 Error");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    // Must be a pull-request
    s_aLogger.info ("Received AS4 Pull-Request");
    return AS4SignalMessageProcessorResult.createFailure ("Pull-Request is not supported by this example SPI");
  }
}
