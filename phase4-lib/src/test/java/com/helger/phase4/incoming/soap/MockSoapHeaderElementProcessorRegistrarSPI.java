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
package com.helger.phase4.incoming.soap;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.AS4IncomingMessageState;

/**
 * Mock implementation of {@link IAS4SoapHeaderElementProcessorRegistrarSPI} to test that external
 * modules can extend the default SOAP header element processor registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MockSoapHeaderElementProcessorRegistrarSPI implements
                                                             IAS4SoapHeaderElementProcessorRegistrarSPI
{
  /** The QName registered by this mock */
  public static final QName QNAME_MOCK = new QName ("urn:phase4:test:mock", "MockHeader");

  @UsedViaReflection
  public MockSoapHeaderElementProcessorRegistrarSPI ()
  {}

  public void registerSoapHeaderElementProcessors (@NonNull final SoapHeaderElementProcessorRegistry aRegistry)
  {
    if (!aRegistry.containsHeaderElementProcessor (QNAME_MOCK))
      aRegistry.registerHeaderElementProcessor (QNAME_MOCK, new ISoapHeaderElementProcessor ()
      {
        @NonNull
        public ESuccess processHeaderElement (@NonNull final Document aSoapDoc,
                                              @NonNull final Element aElement,
                                              @NonNull final ICommonsList <WSS4JAttachment> aAttachments,
                                              @NonNull final AS4IncomingMessageState aIncomingState,
                                              @NonNull final AS4ErrorList aProcessingErrorMessagesTarget)
        {
          // Accept everything
          return ESuccess.SUCCESS;
        }
      });
  }
}
