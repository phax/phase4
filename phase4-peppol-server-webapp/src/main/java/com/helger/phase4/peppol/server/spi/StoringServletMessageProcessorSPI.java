/*
 * Copyright (C) 2020-2024 Philip Helger (www.helger.com)
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
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mutable.MutableLong;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.incoming.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.marshaller.Ebms3NamespaceHandler;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.peppol.server.storage.StorageHelper;
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
public class StoringServletMessageProcessorSPI implements IAS4IncomingMessageProcessorSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (StoringServletMessageProcessorSPI.class);

  private static void _dumpSoap (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final IAS4IncomingMessageState aState)
  {
    // Write formatted SOAP
    {
      final Document aSoapDoc = aState.getEffectiveDecryptedSoapDocument ();
      if (aSoapDoc == null)
        throw new IllegalStateException ("No SOAP Document present");

      final byte [] aBytes = XMLWriter.getNodeAsBytes (aSoapDoc,
                                                       new XMLWriterSettings ().setNamespaceContext (Ebms3NamespaceHandler.getInstance ())
                                                                               .setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
      if (aBytes == null)
        throw new IllegalStateException ("Failed to serialize XML");

      final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".soap");
      if (SimpleFileIO.writeFile (aFile, aBytes).isFailure ())
      {
        LOGGER.error ("Failed to write SOAP to '" + aFile.getAbsolutePath () + "' (" + aBytes.length + " bytes)");
      }
      else
      {
        LOGGER.info ("Wrote SOAP to '" + aFile.getAbsolutePath () + "' (" + aBytes.length + " bytes)");
      }
    }

    if (aState.hasUsedCertificate ())
    {
      // Dump the senders certificate as PEM file
      // That can usually extracted from the Binary Security Token of the SOAP
      final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".pem");
      final X509Certificate aUsedCert = aState.getUsedCertificate ();
      final String sPEM = CertificateHelper.getPEMEncodedCertificate (aUsedCert);
      final byte [] aBytes = sPEM.getBytes (StandardCharsets.US_ASCII);
      if (SimpleFileIO.writeFile (aFile, aBytes).isFailure ())
      {
        LOGGER.error ("Failed to write certificate to '" +
                      aFile.getAbsolutePath () +
                      "' (" +
                      aBytes.length +
                      " bytes)");
      }
      else
      {
        LOGGER.info ("Wrote certificate to '" + aFile.getAbsolutePath () + "' (" + aBytes.length + " bytes)");
      }
    }
  }

  private static void _dumpIncomingAttachment (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                               @Nonnull final WSS4JAttachment aIncomingAttachment,
                                               final int nIndex)
  {
    final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".attachment" + nIndex);
    LOGGER.info ("Start writing incoming Attachment " + nIndex + " to '" + aFile.getAbsolutePath () + "'");

    final MutableLong aCopyCount = new MutableLong (0);
    if (StreamHelper.copyByteStream ()
                    .from (aIncomingAttachment.getInputStreamProvider ().getInputStream ())
                    .closeFrom (true)
                    .to (FileHelper.getOutputStream (aFile))
                    .closeTo (true)
                    .copyByteCount (aCopyCount)
                    .build ()
                    .isFailure ())
    {
      LOGGER.error ("Failed to write Incoming Attachment " +
                    nIndex +
                    " to '" +
                    aFile.getAbsolutePath () +
                    "' (" +
                    aCopyCount.longValue () +
                    " bytes)");
    }
    else
    {
      LOGGER.info ("Wrote Incoming Attachment " +
                   nIndex +
                   " to '" +
                   aFile.getAbsolutePath () +
                   "' (" +
                   aCopyCount.longValue () +
                   " bytes)");
    }
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4IncomingMessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.info ("Received AS4 UserMessage");

    _dumpSoap (aMessageMetadata, aState);

    // Dump all incoming attachments (but only if they are repeatable)
    if (aIncomingAttachments != null)
    {
      int nAttachmentIndex = 0;
      for (final WSS4JAttachment aIncomingAttachment : aIncomingAttachments)
      {
        if (aIncomingAttachment.isRepeatable ())
          _dumpIncomingAttachment (aMessageMetadata, aIncomingAttachment, nAttachmentIndex);
        nAttachmentIndex++;
      }
    }

    LOGGER.info ("Finished receiving AS4 user message");
    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPMode,
                                                                  @Nonnull final IAS4IncomingMessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages)
  {
    LOGGER.info ("Received AS4 SignalMessage");

    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      LOGGER.info ("Received AS4 Receipt");
      _dumpSoap (aMessageMetadata, aState);
      LOGGER.info ("Finished receiving AS4 Receipt");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      LOGGER.info ("Received AS4 Error");
      _dumpSoap (aMessageMetadata, aState);
      LOGGER.info ("Finished receiving AS4 Error");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (aSignalMessage.getPullRequest () != null)
    {
      // Must be a pull-request
      LOGGER.info ("Received AS4 PullRequest");
      _dumpSoap (aMessageMetadata, aState);
      LOGGER.info ("Finished receiving AS4 PullRequest");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    LOGGER.warn ("Received an unexpected AS4 SignalMessage - see file");
    _dumpSoap (aMessageMetadata, aState);
    return AS4SignalMessageProcessorResult.createSuccess ();
  }

  public void processAS4ResponseMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                         @Nonnull final IAS4IncomingMessageState aState,
                                         @Nonnull @Nonempty final String sResponseMessageID,
                                         @Nullable final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable)
  {
    LOGGER.info ("Processing AS4 response message");

    if (aResponseBytes != null)
    {
      final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".response");
      if (SimpleFileIO.writeFile (aFile, aResponseBytes).isFailure ())
      {
        LOGGER.error ("Failed to write response to '" +
                      aFile.getAbsolutePath () +
                      "' (" +
                      aResponseBytes.length +
                      " bytes)");
      }
      else
        LOGGER.info ("Wrote response to '" + aFile.getAbsolutePath () + "' (" + aResponseBytes.length + " bytes)");
    }
    else
      LOGGER.warn ("No response bytes are available for writing");
  }
}
