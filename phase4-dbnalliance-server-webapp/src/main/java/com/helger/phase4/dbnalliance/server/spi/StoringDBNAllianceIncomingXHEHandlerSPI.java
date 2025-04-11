/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.server.spi;

import java.io.File;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppol.xhe.DBNAllianceXHEData;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.dbnalliance.server.APConfig;
import com.helger.phase4.dbnalliance.server.storage.StorageHelper;
import com.helger.phase4.dbnalliance.servlet.IPhase4DBNAllianceIncomingXHEHandlerSPI;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.xhe.v10.XHE10XHEType;

/**
 * Logging implementation of {@link IPhase4DBNAllianceIncomingXHEHandlerSPI}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class StoringDBNAllianceIncomingXHEHandlerSPI implements IPhase4DBNAllianceIncomingXHEHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (StoringDBNAllianceIncomingXHEHandlerSPI.class);

  public void handleIncomingXHE (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final HttpHeaderMap aHeaders,
                                 @Nonnull final Ebms3UserMessage aUserMessage,
                                 @Nonnull final byte [] aXHEBytes,
                                 @Nonnull final XHE10XHEType aXHE,
                                 @Nonnull final DBNAllianceXHEData aDBNAllianceXHE,
                                 @Nonnull final IAS4IncomingMessageState aIncomingState,
                                 @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception
  {
    final String sMyPeppolSeatID = APConfig.getMySeatID ();
    final IIdentifierFactory aIdentifierFactory = SimpleIdentifierFactory.INSTANCE;

    // Example code snippets how to get data
    LOGGER.info ("Received a new DBNAlliance Message");
    LOGGER.info ("  C1 = " + aDBNAllianceXHE.getFromPartyAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  C2 = " + PeppolCertificateHelper.getSubjectCN (aIncomingState.getSigningCertificate ()));
    LOGGER.info ("  C3 = " + sMyPeppolSeatID);
    LOGGER.info ("  C4 = " + aDBNAllianceXHE.getToPartyAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  DocType = " +
                 aIdentifierFactory.parseDocumentTypeIdentifier (aUserMessage.getCollaborationInfo ().getAction ()));
    LOGGER.info ("  Process = " +
                 aIdentifierFactory.createProcessIdentifier (aUserMessage.getCollaborationInfo ()
                                                                         .getService ()
                                                                         .getType (),
                                                             aUserMessage.getCollaborationInfo ()
                                                                         .getService ()
                                                                         .getValue ()).getURIEncoded ());

    // Example got that stores the data to disk
    final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".xhe");
    LOGGER.info ("Now writing SBD to '" + aFile.getAbsolutePath () + "' (" + aXHEBytes.length + " bytes)");

    if (SimpleFileIO.writeFile (aFile, aXHEBytes).isFailure ())
      throw new IllegalStateException ("Failed to write SBD to '" +
                                       aFile.getAbsolutePath () +
                                       "' (" +
                                       aXHEBytes.length +
                                       " bytes)");
    LOGGER.info ("Successfully wrote SBD to '" + aFile.getAbsolutePath () + "'");

    // TODO This is only demo code to force an error
    // Check if any "MessageProperty" with name "MockAction" is contained
    final Ebms3Property aMockAction = CollectionHelper.findFirst (aUserMessage.getMessageProperties ().getProperty (),
                                                                  x -> "MockAction".equals (x.getName ()));
    if (aMockAction != null)
    {
      // Explicitly return an Error - for testing errors
      LOGGER.info ("Found MockAction to return error with value '" + aMockAction.getValue () + "'");
      aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (Locale.US)
                                                         .errorDetail ("Mock error: " + aMockAction.getValue ())
                                                         .build ());
    }
  }
}
