/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.server.servlet;

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
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.as4.CAS4;
import com.helger.as4.client.AS4ClientUserMessage;
import com.helger.as4.client.AbstractAS4Client.SentMessage;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.watchdir.EWatchDirAction;
import com.helger.commons.io.watchdir.IWatchDirCallback;
import com.helger.commons.io.watchdir.WatchDir;
import com.helger.commons.timing.StopWatch;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClient;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.sbdh.builder.SBDHReader;
import com.helger.sbdh.builder.SBDHWriter;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.settings.ISettings;

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

  @Nonnull
  private static String _getCN (final String sPrincipal) throws InvalidNameException
  {
    for (final Rdn aRdn : new LdapName (sPrincipal).getRdns ())
      if (aRdn.getType ().equalsIgnoreCase ("CN"))
        return (String) aRdn.getValue ();
    throw new IllegalStateException ("Failed to get CN from '" + sPrincipal + "'");
  }

  private static void _send (final Path aSendFile, final Path aIncomingDir)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    boolean bSuccess = false;
    LOGGER.info ("Trying to send " + aSendFile.toString ());
    try
    {
      // Read generic SBD
      final StandardBusinessDocument aSBD = SBDHReader.standardBusinessDocument ()
                                                      .read (Files.newInputStream (aSendFile));
      if (aSBD == null)
      {
        LOGGER.error ("Failed to read " + aSendFile.toString () + " as SBDH document!");
      }
      else
      {
        // Extract PEPPOL specific data
        final PeppolSBDHDocument aSBDH = new PeppolSBDHDocumentReader (IF).extractData (aSBD);
        if (aSBDH == null)
        {
          LOGGER.error ("Failed to read " + aSendFile.toString () + " as PEPPOL SBDH document!");
        }
        else
        {
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
            final CryptoProperties aCP = AS4ServerSettings.getAS4CryptoFactory ().getCryptoProperties ();
            final KeyStore aOurKS = KeyStoreHelper.loadKeyStore (aCP.getKeyStoreType (),
                                                                 aCP.getKeyStorePath (),
                                                                 aCP.getKeyStorePassword ())
                                                  .getKeyStore ();
            final KeyStore.PrivateKeyEntry aOurCert = KeyStoreHelper.loadPrivateKey (aOurKS,
                                                                                     aCP.getKeyStorePath (),
                                                                                     aCP.getKeyAlias (),
                                                                                     aCP.getKeyPassword ()
                                                                                        .toCharArray ())
                                                                    .getKeyEntry ();
            final X509Certificate aTheirCert = CertificateHelper.convertStringToCertficate (aEndpoint.getCertificate ());

            final AS4ClientUserMessage aClient = new AS4ClientUserMessage ();
            aClient.setSOAPVersion (ESOAPVersion.SOAP_12);

            // Keystore data
            IReadableResource aRes = new ClassPathResource (aCP.getKeyStorePath ());
            if (!aRes.exists ())
              aRes = new FileSystemResource (aCP.getKeyStorePath ());
            aClient.setKeyStoreResource (aRes);
            aClient.setKeyStorePassword (aCP.getKeyStorePassword ());
            aClient.setKeyStoreType (aCP.getKeyStoreType ());
            aClient.setKeyStoreAlias (aCP.getKeyAlias ());
            aClient.setKeyStoreKeyPassword (aCP.getKeyPassword ());

            aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_512);
            aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);

            // XXX
            // to send the message too
            aClient.setAction ("xxx");
            aClient.setServiceType ("xxx");
            aClient.setServiceValue ("xxx");
            aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
            aClient.setAgreementRefValue ("xxx");

            aClient.setFromRole (CAS4.DEFAULT_ROLE);
            aClient.setFromPartyID (_getCN (((X509Certificate) aOurCert.getCertificate ()).getSubjectX500Principal ()
                                                                                          .getName ()));
            aClient.setToRole (CAS4.DEFAULT_ROLE);
            aClient.setToPartyID (_getCN (aTheirCert.getSubjectX500Principal ().getName ()));
            aClient.setEbms3Properties (new CommonsArrayList <> (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER,
                                                                                                           aSBDH.getSenderValue ()),
                                                                 MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT,
                                                                                                           aSBDH.getReceiverValue ())));
            aClient.setPayload (SBDHWriter.standardBusinessDocument ().getAsDocument (aSBD));

            final SentMessage <byte []> aResponseEntity = aClient.sendMessage (W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()),
                                                                               new ResponseHandlerByteArray ());
            LOGGER.info ("Successfully transmitted document with message ID '" +
                            aResponseEntity.getMessageID () +
                            "' for '" +
                            aSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                            "' to '" +
                            W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()) +
                            "' in " +
                            aSW.stopAndGetMillis () +
                            " ms");

            if (aResponseEntity.hasResponse ())
            {
              final String sMessageID = aResponseEntity.getMessageID ();
              final String sFilename = FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) + "-response.xml";
              final File aResponseFile = aIncomingDir.resolve (sFilename).toFile ();
              if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
                LOGGER.info ("Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
              else
                LOGGER.error ("Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
            }

            bSuccess = true;
          }
        }
      }
    }
    catch (final Throwable t)
    {
      LOGGER.error ("Error sending " + aSendFile.toString (), t);
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

  public static void init ()
  {
    if (s_aWatch != null)
      throw new IllegalStateException ("Already inited!");

    final ISettings aSettings = AS4ServerConfiguration.getSettings ();
    final Path aOutgoingDir = Paths.get (aSettings.getAsString ("server.directory.outgoing", "out"));
    final Path aIncomingDir = Paths.get (aSettings.getAsString ("server.directory.incoming", "in"));

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
            aCurFile.getFileName ().toString ().endsWith (".xml"))
        {
          _send (aCurFile, aIncomingDir);
        }
      };
      s_aWatch = WatchDir.createAsyncRunningWatchDir (aOutgoingDir, false, aCB);

      // Send initially for all existing files
      try (final DirectoryStream <Path> aStream = Files.newDirectoryStream (aOutgoingDir,
                                                                            x -> x.toFile ().isFile () &&
                                                                                 x.getFileName ()
                                                                                  .toString ()
                                                                                  .endsWith (".xml")))
      {
        for (final Path aCur : aStream)
          _send (aCur, aIncomingDir);
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
