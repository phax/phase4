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
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.incoming.soap.ISoapHeaderElementProcessor;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Processes the <code>wsa:Action</code> SOAP header element of an incoming routed signal message.
 * <br>
 * The value is only remembered and logged - an unexpected value is never treated as an error.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class SoapHeaderElementProcessorWsaAction implements ISoapHeaderElementProcessor
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (SoapHeaderElementProcessorWsaAction.class);

  public SoapHeaderElementProcessorWsaAction ()
  {}

  @NonNull
  public ESuccess processHeaderElement (@NonNull final Document aSoapDoc,
                                        @NonNull final Element aHeaderElement,
                                        @NonNull final ICommonsList <WSS4JAttachment> aAttachments,
                                        @NonNull final AS4IncomingMessageState aIncomingState,
                                        @NonNull final AS4ErrorList aProcessingErrorMessagesTarget)
  {
    final String sValue = StringHelper.getNotNull (aHeaderElement.getTextContent ()).trim ();

    if (!CAS4MultiHop.WSA_ACTION_ONEWAY_RECEIPT.equals (sValue) &&
        !CAS4MultiHop.WSA_ACTION_ONEWAY_ERROR.equals (sValue))
      LOGGER.warn ("The wsa:Action header of the incoming message is the unexpected value '" + sValue + "'");

    aIncomingState.putIn (CAS4MultiHop.STATE_ATTR_WSA_ACTION, sValue);
    return ESuccess.SUCCESS;
  }
}
