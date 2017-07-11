package com.helger.as4.server.servlet;

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
import org.w3c.dom.Document;

import com.helger.as4.CAS4;
import com.helger.as4.client.AS4ClientUserMessage;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.server.watchdir.WatchDir;
import com.helger.as4.server.watchdir.WatchDir.EWatchDirAction;
import com.helger.as4.server.watchdir.WatchDir.IWatchDirCallback;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.httpclient.response.ResponseHandlerXml;
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
import com.helger.xml.serialize.write.XMLWriter;

public final class WatchDirSBDH
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider UP = PeppolURLProvider.INSTANCE;
  private static final String PATH_DONE = "done";
  private static final String PATH_ERROR = "error";
  private static final Logger s_aLogger = LoggerFactory.getLogger (WatchDirSBDH.class);
  private static WatchDir s_aWatch;

  private WatchDirSBDH ()
  {}

  @Nonnull
  private static String _getCN (final String sPrincipal) throws InvalidNameException
  {
    for (final Rdn aRdn : new LdapName (sPrincipal).getRdns ())
      if (aRdn.getType ().equalsIgnoreCase ("CN"))
        return (String) aRdn.getValue ();
    throw new IllegalStateException ("Failed to get CN from '" + sPrincipal + "'");
  }

  private static void _send (final Path aPath)
  {
    boolean bSuccess = false;
    s_aLogger.info ("Trying to send " + aPath.toString ());
    try
    {
      // Read generic SBD
      final StandardBusinessDocument aSBD = SBDHReader.standardBusinessDocument ().read (Files.newInputStream (aPath));
      if (aSBD == null)
      {
        s_aLogger.error ("Failed to read " + aPath.toString () + " as SBDH document!");
      }
      else
      {
        // Extract PEPPOL specific data
        final PeppolSBDHDocument aSBDH = new PeppolSBDHDocumentReader (IF).extractData (aSBD);
        if (aSBDH == null)
        {
          s_aLogger.error ("Failed to read " + aPath.toString () + " as PEPPOL SBDH document!");
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
            s_aLogger.error ("Found no endpoint for:\n  Receiver ID: " +
                             aSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                             "\n  Document type ID: " +
                             aSBDH.getDocumentTypeAsIdentifier ().getURIEncoded () +
                             "\n  Process ID: " +
                             aSBDH.getProcessAsIdentifier ().getURIEncoded ());
          }
          else
          {
            final CryptoProperties aCP = AS4ServerSettings.getAS4CryptoFactory ().getCryptoProperties ();
            final KeyStore aOurKS = KeyStoreHelper.loadKeyStore (aCP.getKeyStorePath (), aCP.getKeyStorePassword ())
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
            // XXX
            // to send the message too
            aClient.setAction ("xxx");
            aClient.setServiceType ("xxx");
            aClient.setServiceValue ("xxx");
            aClient.setConversationID ("xxx");
            aClient.setAgreementRefValue ("xxx");
            aClient.setFromRole (CAS4.DEFAULT_ROLE);
            aClient.setFromPartyID (_getCN (((X509Certificate) aOurCert.getCertificate ()).getSubjectX500Principal ()
                                                                                          .getName ()));
            aClient.setToRole (CAS4.DEFAULT_ROLE);
            aClient.setToPartyID (_getCN (aTheirCert.getSubjectX500Principal ().getName ()));

            final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
            aEbms3Properties.add (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER, "C1-test"));
            aEbms3Properties.add (MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT, "C4-test"));

            aClient.setEbms3Properties (aEbms3Properties);
            aClient.setPayload (SBDHWriter.standardBusinessDocument ().getAsDocument (aSBD));

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

            final Document aResponseDoc = aClient.sendGenericMessage (W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()),
                                                                      aClient.buildMessage (),
                                                                      new ResponseHandlerXml ());
            s_aLogger.info ("Successfully transmitted document for " +
                            aSBDH.getReceiverAsIdentifier ().getURIEncoded () +
                            " to " +
                            W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()));
            s_aLogger.info ("Response received:\n" + XMLWriter.getNodeAsString (aResponseDoc));
            bSuccess = true;
          }
        }
      }
    }
    catch (final Throwable t)
    {
      s_aLogger.error ("Error sending " + aPath.toString (), t);
    }

    {
      // Move to error directory
      final Path aDest = aPath.resolveSibling (bSuccess ? PATH_DONE : PATH_ERROR).resolve (aPath.getFileName ());
      try
      {
        Files.move (aPath, aDest);
      }
      catch (final IOException e)
      {
        // TODO Auto-generated catch block
        s_aLogger.error ("Error moving from '" + aPath.toString () + "' to '" + aDest + "'");
      }
    }
  }

  public static void init ()
  {
    if (s_aWatch != null)
      throw new IllegalStateException ("Already inited!");

    // Starting WatchDir
    final IWatchDirCallback aCB = (eAction, aPath) -> {
      s_aLogger.info ("WatchEvent " + eAction + " - " + aPath);
      if (!eAction.equals (EWatchDirAction.DELETE) &&
          aPath.toFile ().exists () &&
          aPath.getFileName ().toString ().endsWith (".xml"))
      {
        _send (aPath);
      }
    };

    final Path aDataDir = Paths.get (AS4ServerConfiguration.getSettings ().getAsString ("server.directory.outgoing",
                                                                                        "out"));
    try
    {
      Files.createDirectories (aDataDir.resolve (PATH_DONE));
      Files.createDirectories (aDataDir.resolve (PATH_ERROR));
      s_aWatch = WatchDir.createAsyncRunningWatchDir (aDataDir, false, aCB);

      // Do initially for all existing files
      try (final DirectoryStream <Path> aStream = Files.newDirectoryStream (aDataDir,
                                                                            x -> x.getFileName ()
                                                                                  .toString ()
                                                                                  .endsWith (".xml")))
      {
        for (final Path aCur : aStream)
          _send (aCur);
      }
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException (ex);
    }
  }

  public static void destroy ()
  {
    if (s_aWatch != null)
    {
      StreamHelper.close (s_aWatch);
      s_aWatch = null;
      s_aLogger.info ("Successfully shutdown WatchDir");
    }
  }
}
