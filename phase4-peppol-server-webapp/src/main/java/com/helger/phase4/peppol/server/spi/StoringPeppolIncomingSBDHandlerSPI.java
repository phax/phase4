/*
 * Copyright (C) 2020-2025 Philip Helger (www.helger.com)
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
import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.CAS4;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.peppol.server.APConfig;
import com.helger.phase4.peppol.server.storage.StorageHelper;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.security.certificate.CertificateHelper;

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
                                 @Nonnull final PeppolSBDHData aPeppolSBD,
                                 @Nonnull final IAS4IncomingMessageState aIncomingState,
                                 @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception
  {
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

    // Example code snippets how to get data
    LOGGER.info ("Received a new Peppol Message");
    LOGGER.info ("  C1 = " + aPeppolSBD.getSenderAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  C2 = " + CertificateHelper.getSubjectCN (aIncomingState.getSigningCertificate ()));
    LOGGER.info ("  C3 = " + sMyPeppolSeatID);
    LOGGER.info ("  C4 = " + aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  DocType = " + aPeppolSBD.getDocumentTypeAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  Process = " + aPeppolSBD.getProcessAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  CountryC1 = " + aPeppolSBD.getCountryC1 ());

    // Example got that stores the data to disk
    final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".sbd");
    LOGGER.info ("Now writing SBD to '" + aFile.getAbsolutePath () + "' (" + aSBDBytes.length + " bytes)");

    if (SimpleFileIO.writeFile (aFile, aSBDBytes).isFailure ())
      throw new IllegalStateException ("Failed to write SBD to '" +
                                       aFile.getAbsolutePath () +
                                       "' (" +
                                       aSBDBytes.length +
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

    // Last action in this method
    PhotonWorkerPool.getInstance ().run (CAS4.LIB_NAME + " Handle Peppol Reporting for Peppol incoming message", () -> {
      // TODO If you have a way to determine the real end user
      // of the message here, this might be a good opportunity
      // to store the data for Peppol Reporting (do this
      // asynchronously as the last activity)
      // Note: this is a separate thread so that it does not
      // block the sending of the positive receipt message

      // TODO Peppol Reporting - enable if possible to be done
      // in here
      if (false)
        try
        {
          LOGGER.info ("Creating Peppol Reporting Item and storing it");

          // TODO determine correct values for Peppol Reporting for the next three fields
          final String sC3ID = sMyPeppolSeatID;
          final String sC4CountryCode = "AT";
          final String sEndUserID = "EndUserID";

          // Create the reporting item
          final PeppolReportingItem aReportingItem = Phase4PeppolServletMessageProcessorSPI.createPeppolReportingItemForReceivedMessage (aUserMessage,
                                                                                                                                         aPeppolSBD,
                                                                                                                                         aIncomingState,
                                                                                                                                         sC3ID,
                                                                                                                                         sC4CountryCode,
                                                                                                                                         sEndUserID);
          PeppolReportingBackend.withBackendDo (AS4Configuration.getConfig (),
                                                aBackend -> aBackend.storeReportingItem (aReportingItem));
        }
        catch (final PeppolReportingBackendException ex)
        {
          LOGGER.error ("Failed to store Peppol Reporting Item", ex);
          // TODO improve Peppol Reporting error handling
        }
    });
  }
}
