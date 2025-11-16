/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.spi;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.io.file.FileHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.marshaller.Ebms3NamespaceHandler;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.server.storage.StorageHelper;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Example implementation of {@link IAS4IncomingMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class ExampleReceiveMessageProcessorSPI implements IAS4IncomingMessageProcessorSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (ExampleReceiveMessageProcessorSPI.class);

  private static void _dumpSoap (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final IAS4IncomingMessageState aState)
  {
    // Write formatted SOAP
    {
      final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".soap");
      final Document aSoapDoc = aState.getEffectiveDecryptedSoapDocument ();
      final byte [] aBytes = XMLWriter.getNodeAsBytes (aSoapDoc,
                                                       new XMLWriterSettings ().setNamespaceContext (Ebms3NamespaceHandler.getInstance ())
                                                                               .setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
      if (SimpleFileIO.writeFile (aFile, aBytes).isFailure ())
        LOGGER.error ("Failed to write SOAP to '" + aFile.getAbsolutePath () + "'");
      else
        LOGGER.info ("Wrote SOAP to '" + aFile.getAbsolutePath () + "'");
    }

    if (aState.hasSigningCertificate ())
    {
      // Dump the senders certificate as PEM file
      // That can usually extracted from the Binary Security Token of the SOAP
      final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".pem");
      final X509Certificate aSigningCert = aState.getSigningCertificate ();
      final String sPEM = CertificateHelper.getPEMEncodedCertificate (aSigningCert);
      final byte [] aBytes = sPEM.getBytes (StandardCharsets.US_ASCII);
      if (SimpleFileIO.writeFile (aFile, aBytes).isFailure ())
        LOGGER.error ("Failed to write certificate to '" + aFile.getAbsolutePath () + "'");
      else
        LOGGER.info ("Wrote certificate to '" + aFile.getAbsolutePath () + "'");
    }
  }

  @NonNull
  public AS4MessageProcessorResult processAS4UserMessage (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @NonNull final HttpHeaderMap aHttpHeaders,
                                                          @NonNull final Ebms3UserMessage aUserMessage,
                                                          @NonNull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @NonNull final IAS4IncomingMessageState aState,
                                                          @NonNull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.info ("Received AS4 user message");
    _dumpSoap (aMessageMetadata, aState);

    if (aIncomingAttachments != null)
    {
      int nIndex = 1;
      for (final WSS4JAttachment aIncomingAttachment : aIncomingAttachments)
      {
        final File aFile = StorageHelper.getStorageFile (aMessageMetadata, "-" + nIndex + ".payload");
        if (StreamHelper.copyInputStreamToOutputStream (aIncomingAttachment.getSourceStream (),
                                                        FileHelper.getOutputStream (aFile)).isFailure ())
        {
          LOGGER.error ("Failed to write incoming attachment [" + nIndex + "] to '" + aFile.getAbsolutePath () + "'");
        }
        else
        {
          LOGGER.info ("Wrote incoming attachment [" + nIndex + "] to '" + aFile.getAbsolutePath () + "'");
        }
        ++nIndex;
      }
    }

    return AS4MessageProcessorResult.createSuccess ();
  }

  @NonNull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @NonNull final HttpHeaderMap aHttpHeaders,
                                                                  @NonNull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @NonNull final IAS4IncomingMessageState aState,
                                                                  @NonNull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      LOGGER.info ("Received AS4 Receipt");
      _dumpSoap (aMessageMetadata, aState);
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      LOGGER.info ("Received AS4 Error");
      _dumpSoap (aMessageMetadata, aState);
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (aSignalMessage.getPullRequest () != null)
    {
      // Must be a pull-request
      LOGGER.info ("Received AS4 Pull-Request");
      _dumpSoap (aMessageMetadata, aState);
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    LOGGER.warn ("Received an unexpected signal message - see file");
    _dumpSoap (aMessageMetadata, aState);
    return AS4SignalMessageProcessorResult.createSuccess ();
  }

  public void processAS4ResponseMessage (final IAS4IncomingMessageMetadata aMessageMetadata,
                                         final IAS4IncomingMessageState aState,
                                         final String sResponseMessageID,
                                         final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable)
  {
    LOGGER.info ("Sending AS4 response with ID '" + sResponseMessageID + "'");
  }
}
