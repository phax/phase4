/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.servlet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.watchdir.EWatchDirAction;
import com.helger.commons.io.watchdir.IWatchDirCallback;
import com.helger.commons.io.watchdir.WatchDir;
import com.helger.commons.timing.StopWatch;
import com.helger.config.IConfig;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.smpclient.url.IPeppolURLProvider;
import com.helger.smpclient.url.PeppolURLProvider;
import com.helger.xsds.peppol.smp1.EndpointType;

public final class DropFolderUserMessage
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider UP = PeppolURLProvider.INSTANCE;
  private static final String PATH_DONE = "done";
  private static final String PATH_ERROR = "error";
  private static final Logger LOGGER = LoggerFactory.getLogger (DropFolderUserMessage.class);
  private static WatchDir s_aWatch;

  private DropFolderUserMessage ()
  {}

  private static void _send (@Nonnull final IAS4CryptoFactory aCF, final Path aSendFile, final Path aIncomingDir)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    boolean bSuccess = false;
    LOGGER.info ("Trying to send " + aSendFile.toString ());
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // Read generic SBD
      final StandardBusinessDocument aSBD = new SBDMarshaller ().read (Files.newInputStream (aSendFile));
      if (aSBD == null)
      {
        LOGGER.error ("Failed to read " + aSendFile.toString () + " as SBDH document!");
      }
      else
      {
        // Extract Peppol specific data
        final PeppolSBDHData aSBDH = new PeppolSBDHDocumentReader (IF).extractData (aSBD);
        final SMPClient aSMPClient = new SMPClient (UP, aSBDH.getReceiverAsIdentifier (), ESML.DIGIT_TEST);
        final EndpointType aEndpoint = aSMPClient.getEndpoint (aSBDH.getReceiverAsIdentifier (),
                                                               aSBDH.getDocumentTypeAsIdentifier (),
                                                               aSBDH.getProcessAsIdentifier (),
                                                               ESMPTransportProfile.TRANSPORT_PROFILE_BDXR_AS4);
        if (aEndpoint == null)
        {
          LOGGER.error ("Found no endpoint for:\n  Receiver ID: " +
                        aSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                        "\n  Document type ID: " +
                        aSBDH.getDocumentTypeAsIdentifier ().getURIEncoded () +
                        "\n  Process ID: " +
                        aSBDH.getProcessAsIdentifier ().getURIEncoded ());
        }
        else
        {
          final KeyStore.PrivateKeyEntry aOurCert = aCF.getPrivateKeyEntry ();
          final X509Certificate aTheirCert = CertificateHelper.convertStringToCertficate (aEndpoint.getCertificate ());

          final AS4ClientUserMessage aClient = new AS4ClientUserMessage (aResHelper);
          aClient.setSoapVersion (ESoapVersion.SOAP_12);

          // Keystore data
          aClient.setCryptoFactory (aCF);

          aClient.signingParams ().setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_512);
          aClient.signingParams ().setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);

          // TODO Action, Service etc. need to be provided by you
          aClient.setAction ("xxx");
          aClient.setServiceType ("xxx");
          aClient.setServiceValue ("xxx");
          aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
          aClient.setAgreementRefValue ("xxx");

          aClient.setFromRole (CAS4.DEFAULT_ROLE);
          aClient.setFromPartyID (PeppolCertificateHelper.getSubjectCN ((X509Certificate) aOurCert.getCertificate ()));
          aClient.setToRole (CAS4.DEFAULT_ROLE);
          aClient.setToPartyID (PeppolCertificateHelper.getSubjectCN (aTheirCert));
          aClient.ebms3Properties ()
                 .setAll (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER,
                                                                    aSBDH.getSenderScheme (),
                                                                    aSBDH.getSenderValue ()),
                          MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT,
                                                                    aSBDH.getReceiverScheme (),
                                                                    aSBDH.getReceiverValue ()));
          aClient.setPayload (new SBDMarshaller ().getAsElement (aSBD));

          final IAS4ClientBuildMessageCallback aCallback = null;
          final IAS4OutgoingDumper aOutgoingDumper = null;
          final IAS4RetryCallback aRetryCallback = null;
          final AS4ClientSentMessage <byte []> aClientSentMessage = aClient.sendMessageWithRetries (W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()),
                                                                                                    new ResponseHandlerByteArray (),
                                                                                                    aCallback,
                                                                                                    aOutgoingDumper,
                                                                                                    aRetryCallback);
          LOGGER.info ("Successfully transmitted document with message ID '" +
                       aClientSentMessage.getMessageID () +
                       "' for '" +
                       aSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                       "' to '" +
                       W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()) +
                       "' in " +
                       aSW.stopAndGetMillis () +
                       " ms");

          if (aClientSentMessage.hasResponseContent ())
          {
            final String sMessageID = aClientSentMessage.getMessageID ();
            final String sFilename = FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) + "-response.xml";
            final File aResponseFile = aIncomingDir.resolve (sFilename).toFile ();
            if (SimpleFileIO.writeFile (aResponseFile, aClientSentMessage.getResponseContent ()).isSuccess ())
              LOGGER.info ("Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
            else
              LOGGER.error ("Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
          }
          bSuccess = true;
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending " + aSendFile.toString (), ex);
    }
    // After the exception handler!
    {
      // Move to done or error directory?
      final Path aDest = aSendFile.resolveSibling (bSuccess ? PATH_DONE : PATH_ERROR)
                                  .resolve (aSendFile.getFileName ());
      try
      {
        Files.move (aSendFile, aDest);
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Error moving from '" + aSendFile.toString () + "' to '" + aDest + "'", ex);
      }
    }
  }

  public static void init (@Nonnull final IAS4CryptoFactory aCryptoFactory)
  {
    if (s_aWatch != null)
      throw new IllegalStateException ("Already inited!");

    final IConfig aConfig = AS4Configuration.getConfig ();
    final Path aOutgoingDir = Paths.get (aConfig.getAsString ("server.directory.outgoing", "out"));
    final Path aIncomingDir = Paths.get (aConfig.getAsString ("server.directory.incoming", "in"));

    try
    {
      // Ensure directories are present
      Files.createDirectories (aOutgoingDir.resolve (PATH_DONE));
      Files.createDirectories (aOutgoingDir.resolve (PATH_ERROR));
      Files.createDirectories (aIncomingDir);

      // Start watching directory for changes
      final IWatchDirCallback aCB = (eAction, aCurFile) -> {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("WatchEvent " + eAction + " - " + aCurFile);
        if (!eAction.equals (EWatchDirAction.DELETE) &&
            aCurFile.toFile ().isFile () &&
            aCurFile.getFileName () != null &&
            aCurFile.getFileName ().toString ().endsWith (".xml"))
        {
          _send (aCryptoFactory, aCurFile, aIncomingDir);
        }
      };
      s_aWatch = WatchDir.createAsyncRunningWatchDir (aOutgoingDir, false, aCB);

      // Send initially for all existing files
      try (final DirectoryStream <Path> aStream = Files.newDirectoryStream (aOutgoingDir,
                                                                            x -> x.toFile ().isFile () &&
                                                                                 x.getFileName () != null &&
                                                                                 x.getFileName ()
                                                                                  .toString ()
                                                                                  .endsWith (".xml")))
      {
        for (final Path aCur : aStream)
          _send (aCryptoFactory, aCur, aIncomingDir);
      }
    }
    catch (final IOException ex)
    {
      // Checked to unchecked conversion
      throw new UncheckedIOException (ex);
    }
  }

  public static void destroy ()
  {
    if (s_aWatch != null)
    {
      StreamHelper.close (s_aWatch);
      s_aWatch = null;
      LOGGER.info ("Successfully shutdown WatchDir");
    }
  }
}
