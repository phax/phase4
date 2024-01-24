/*
 * Copyright (C) 2021-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.springboot.spi;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.springboot.enumeration.ESBDHHandlerServiceSelector;
import com.helger.phase4.springboot.service.ISBDHandlerService;
import com.helger.phase4.springboot.service.SDBHandlerServiceLocator;

/**
 * This is one way of handling incoming messages: creating a domain object and
 * passing it to a Spring Service object
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class CustomPeppolIncomingSBDHandlerSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CustomPeppolIncomingSBDHandlerSPI.class);

  public void handleIncomingSBD (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final HttpHeaderMap aHeaders,
                                 @Nonnull final Ebms3UserMessage aUserMessage,
                                 @Nonnull final byte [] aSBDBytes,
                                 @Nonnull final StandardBusinessDocument aSBD,
                                 @Nonnull final PeppolSBDHData aPeppolSBD,
                                 @Nonnull final IAS4MessageState aState,
                                 @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception
  {
    // Get the service from the locator
    LOGGER.info ("Retrieving the handler service.");
    final ISBDHandlerService service = SDBHandlerServiceLocator.getService (ESBDHHandlerServiceSelector.CUSTOM_PEPPOL_INCOMING);
    LOGGER.info ("Successfully retrieved the handler service.");

    // Inject the parameters into the service
    LOGGER.info ("Injecting parameters into the handler service.");
    service.setMessageMetadata (aMessageMetadata);
    service.setHttpHeaders (aHeaders);
    service.setUserMessage (aUserMessage);
    service.setStandardBusinessDocumentBytes (aSBDBytes);
    service.setStandardBusinessDocument (aSBD);
    service.setPeppolStandardBusinessDocumentHeader (aPeppolSBD);
    service.setMessageState (aState);

    // Handle the request, might raise an exception that needs to be dealt
    // carefully
    LOGGER.info ("Handling request.");
    service.handle ();
    LOGGER.info ("Request handled with success.");
  }

  @Override
  public boolean exceptionTranslatesToAS4Error ()
  {
    // If we have an Exception, tell the sender so
    return true;
  }
}
