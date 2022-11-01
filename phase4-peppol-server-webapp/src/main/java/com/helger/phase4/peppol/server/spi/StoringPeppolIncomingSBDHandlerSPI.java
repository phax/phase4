/*
 * Copyright (C) 2020-2022 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.spi;

import java.io.File;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.peppol.server.storage.StorageHelper;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.servlet.IAS4MessageState;

/**
 * Logging implementation of {@link IPhase4PeppolIncomingSBDHandlerSPI}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class StoringPeppolIncomingSBDHandlerSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (StoringPeppolIncomingSBDHandlerSPI.class);

  public void handleIncomingSBD (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final HttpHeaderMap aHeaders,
                                 @Nonnull final Ebms3UserMessage aUserMessage,
                                 @Nonnull final byte [] aSBDBytes,
                                 @Nonnull final StandardBusinessDocument aSBD,
                                 @Nonnull final PeppolSBDHDocument aPeppolSBD,
                                 @Nonnull final IAS4MessageState aState) throws Exception
  {
    final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".sbd");
    LOGGER.info ("Now writing SBD to '" + aFile.getAbsolutePath () + "' (" + aSBDBytes.length + " bytes)");

    if (SimpleFileIO.writeFile (aFile, aSBDBytes).isFailure ())
      throw new IllegalStateException ("Failed to write SBD to '" +
                                       aFile.getAbsolutePath () +
                                       "' (" +
                                       aSBDBytes.length +
                                       " bytes)");

    LOGGER.info ("Successfully wrote SBD to '" + aFile.getAbsolutePath () + "'");
  }
}
