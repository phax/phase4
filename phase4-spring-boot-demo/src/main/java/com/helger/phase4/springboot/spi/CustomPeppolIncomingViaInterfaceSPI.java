/*
 * Copyright (C) 2021-2023 Philip Helger (www.helger.com)
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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.servlet.IAS4MessageState;

/**
 * This is one way of handling incoming messages: have an interface
 * {@link IPeppolIncomingHandler} that mimics the parameters of the
 * {@link IPhase4PeppolIncomingSBDHandlerSPI} handling method. Use a static
 * member of this class to set it. Each invocation of the SPI triggers a call to
 * the registered handler.<br>
 * Based on https://github.com/phax/phase4/discussions/115
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class CustomPeppolIncomingViaInterfaceSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  public interface IPeppolIncomingHandler
  {
    /**
     * Handle the provided incoming StandardBusinessDocument
     *
     * @param aMessageMetadata
     *        Message metadata. Includes data when and from whom it was
     *        received. Never <code>null</code>.
     * @param aHeaders
     *        The (HTTP) headers of the incoming request. Never
     *        <code>null</code>.
     * @param aUserMessage
     *        The received EBMS user message. Never <code>null</code>.
     * @param aSBDBytes
     *        The raw SBD bytes. Never <code>null</code>.
     * @param aSBD
     *        The incoming parsed Standard Business Document that is never
     *        <code>null</code>. This is the pre-parsed SBD bytes.
     * @param aPeppolSBD
     *        The pre-parsed Peppol Standard Business Document. Never
     *        <code>null</code>.
     * @param aState
     *        The message state. Can e.g. be used to retrieve information about
     *        the certificate found in the message. Never <code>null</code>.
     * @param aProcessingErrorMessages
     *        The list of error messages to be filled by the custom handler.
     *        Never <code>null</code>. Since v2.6.0.
     * @throws Exception
     *         In case it cannot be processed.
     */
    void handleIncomingSBD (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                            @Nonnull HttpHeaderMap aHeaders,
                            @Nonnull Ebms3UserMessage aUserMessage,
                            @Nonnull byte [] aSBDBytes,
                            @Nonnull StandardBusinessDocument aSBD,
                            @Nonnull PeppolSBDHDocument aPeppolSBD,
                            @Nonnull IAS4MessageState aState,
                            @Nonnull ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (CustomPeppolIncomingViaInterfaceSPI.class);
  private static IPeppolIncomingHandler s_aHandler = null;

  /**
   * @return The statically defined incoming handler. May be <code>null</code>.
   *         Is <code>null</code> by default.
   */
  @Nullable
  public static IPeppolIncomingHandler getIncomingHandler ()
  {
    return s_aHandler;
  }

  /**
   * Call this method once on application startup. Should only be called once.
   *
   * @param aHandler
   *        The handler to be used. May be <code>null</code> but makes no sense.
   */
  public static void setIncomingHandler (@Nullable final IPeppolIncomingHandler aHandler)
  {
    if (s_aHandler != null && aHandler != null)
      LOGGER.warn ("Overwriting static handler for incoming Peppol messages");
    s_aHandler = aHandler;
  }

  public void handleIncomingSBD (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final HttpHeaderMap aHeaders,
                                 @Nonnull final Ebms3UserMessage aUserMessage,
                                 @Nonnull final byte [] aSBDBytes,
                                 @Nonnull final StandardBusinessDocument aSBD,
                                 @Nonnull final PeppolSBDHDocument aPeppolSBD,
                                 @Nonnull final IAS4MessageState aState,
                                 @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception
  {
    if (s_aHandler != null)
    {
      LOGGER.info ("Invoking the registered handler");
      try
      {
        s_aHandler.handleIncomingSBD (aMessageMetadata,
                                      aHeaders,
                                      aUserMessage,
                                      aSBDBytes,
                                      aSBD,
                                      aPeppolSBD,
                                      aState,
                                      aProcessingErrorMessages);
      }
      finally
      {
        // Also called in case of Exceptions
        LOGGER.info ("Finished invoking the registered handler");
      }
    }
    else
      LOGGER.error ("No handler is registered. Make sure to call 'setIncomingHandler' of this class on application startup");
  }

  @Override
  public boolean exceptionTranslatesToAS4Error ()
  {
    // If we have an Exception, tell the sender so
    return true;
  }
}
