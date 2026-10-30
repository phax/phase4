/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.incoming.soap.ISoapHeaderElementProcessor;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.model.MultiHopRoutingInput;
import com.helger.phase4.multihop.model.MultiHopRoutingInputMarshaller;

/**
 * Processes the <code>ebint:RoutingInput</code> SOAP header element of an incoming routed signal
 * message. Without this processor such a header - which carries <code>mustUnderstand</code> -
 * causes the whole message to be rejected (R10).
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class SoapHeaderElementProcessorMultiHopRoutingInput implements ISoapHeaderElementProcessor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (SoapHeaderElementProcessorMultiHopRoutingInput.class);

  public SoapHeaderElementProcessorMultiHopRoutingInput ()
  {}

  @NonNull
  public ESuccess processHeaderElement (@NonNull final Document aSoapDoc,
                                        @NonNull final Element aHeaderElement,
                                        @NonNull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @NonNull final AS4IncomingMessageState aIncomingState,
                                        @NonNull final AS4ErrorList aProcessingErrorMessagesTarget)
  {
    final MultiHopRoutingInput aRoutingInput = new MultiHopRoutingInputMarshaller ().read (aHeaderElement);
    if (aRoutingInput == null)
    {
      final String sDetails = "Failed to parse the multi-hop ebint:RoutingInput header element";
      LOGGER.error (sDetails);
      aProcessingErrorMessagesTarget.add (EEbmsError.EBMS_INVALID_HEADER.errorBuilder (aIncomingState.getLocale ())
                                                                        .errorDetail (sDetails)
                                                                        .build ());
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully parsed the multi-hop ebint:RoutingInput header element");

    aIncomingState.putIn (CAS4MultiHop.STATE_ATTR_ROUTING_INPUT, aRoutingInput);
    return ESuccess.SUCCESS;
  }
}
