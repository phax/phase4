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
package com.helger.phase4.dbnalliance.server.servlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.dbnalliance.commons.security.DBNAllianceTrustStores;
import com.helger.httpclient.HttpDebugger;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.dbnalliance.server.APConfig;
import com.helger.phase4.dbnalliance.server.api.Phase4API;
import com.helger.phase4.dbnalliance.server.storage.StorageHelper;
import com.helger.phase4.dbnalliance.servlet.Phase4DBNAllianceDefaultReceiverConfiguration;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.incoming.AS4IncomingHelper;
import com.helger.phase4.incoming.AS4ServerInitializer;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.profile.dbnalliance.AS4DBNAllianceProfileRegistarSPI;
import com.helger.phase4.profile.dbnalliance.DBNAllianceCRLDownloader;
import com.helger.phase4.profile.dbnalliance.Phase4DBNAllianceHttpClientSettings;
import com.helger.photon.api.IAPIRegistry;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.activation.CommandMap;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;

@WebListener
public final class Phase4DBNAllianceWebAppListener extends WebAppListener
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4DBNAllianceWebAppListener.class);

  @Override
  @Nullable
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isGlobalDebug ());
  }

  @Override
  @Nullable
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isGlobalProduction ());
  }

  @Override
  @Nullable
  protected String getInitParameterNoStartupInfo (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4Configuration.isNoStartupInfo ());
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AS4Configuration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return false;
  }

  @Override
  protected void afterContextInitialized (@Nonnull final ServletContext aSC)
  {
    super.afterContextInitialized (aSC);

    // Show registered servlets
    for (final Map.Entry <String, ? extends ServletRegistration> aEntry : aSC.getServletRegistrations ().entrySet ())
      LOGGER.info ("Servlet '" + aEntry.getKey () + "' is mapped to " + aEntry.getValue ().getMappings ());
  }

  @Override
  protected void initGlobalSettings ()
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    if (GlobalDebug.isDebugMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    HttpDebugger.setEnabled (false);

    // Sanity check
    if (CommandMap.getDefaultCommandMap ().createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) ==
        null)
      throw new IllegalStateException ("No DataContentHandler for MIME Type '" +
                                       CMimeType.MULTIPART_RELATED.getAsString () +
                                       "' is available. There seems to be a problem with the dependencies/packaging");
  }

  @Override
  protected void initSecurity ()
  {
    // Ensure user exists
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    if (!aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
    {
      aUserMgr.createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
                                     CSecurity.USER_ADMINISTRATOR_LOGIN,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_PASSWORD,
                                     "Admin",
                                     "istrator",
                                     null,
                                     Locale.US,
                                     null,
                                     false);
    }
  }

  private static void _initAS4 ()
  {
    // Enforce DBNAlliance profile usage
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4DBNAllianceProfileRegistarSPI.AS4_PROFILE_ID);

    // Start duplicate check
    AS4ServerInitializer.initAS4Server ();

    // Store the incoming file as is
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ( (aMessageMetadata,
                                                                        aHttpHeaderMap) -> StorageHelper.getStorageFile (aMessageMetadata,
                                                                                                                         ".as4in"))
    {
      @Override
      public void onEndRequest (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                @Nullable final Exception aCaughtException)
      {
        // Save the metadata also to a file
        final File aFile = StorageHelper.getStorageFile (aMessageMetadata, ".metadata");
        if (SimpleFileIO.writeFile (aFile,
                                    AS4IncomingHelper.getIncomingMetadataAsJson (aMessageMetadata)
                                                     .getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED),
                                    StandardCharsets.UTF_8).isFailure ())
          LOGGER.error ("Failed to write metadata to '" + aFile.getAbsolutePath () + "'");
        else
          LOGGER.info ("Wrote metadata to '" + aFile.getAbsolutePath () + "'");
      }
    });

    // Store the outgoings file as well
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ( (eMsgMode, sMessageID, nTry) -> StorageHelper
                                                                                                                    .getStorageFile (sMessageID,
                                                                                                                                     nTry,
                                                                                                                                     ".as4out")));
  }

  private static void _initDBNAllianceAS4 ()
  {
    // Our server should check all signing certificates of incoming messages if
    // they are revoked or not
    // (this is the default setting, but added it here for easy modification)
    Phase4DBNAllianceDefaultReceiverConfiguration.setCheckSigningCertificateRevocation (true);

    // Make sure the download of CRL is using Apache HttpClient and that the
    // provided settings are used. If e.g. a proxy is needed to access outbound
    // resources, it can be configured here
    final Phase4DBNAllianceHttpClientSettings aHCS = new Phase4DBNAllianceHttpClientSettings ();
    if (false)
    {
      // TODO enable when you use a proxy
      aHCS.setProxyHost (new HttpHost (APConfig.getHttpProxyHost (), APConfig.getHttpProxyPort ()));
    }
    DBNAllianceCRLDownloader.setAsDefaultCRLCache (aHCS);

    // Throws an exception if configuration parameters are missing
    final AS4CryptoFactoryConfiguration aCF = AS4CryptoFactoryConfiguration.getDefaultInstance ();

    // Check if crypto properties are okay - fail early if something is
    // misconfigured
    LOGGER.info ("Trying to load configured key store (type=" +
                 aCF.getKeyStoreDescriptor ().getKeyStoreType () +
                 ", path=" +
                 aCF.getKeyStoreDescriptor ().getKeyStorePath () +
                 ")");
    final KeyStore aKS = aCF.getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured Keystore");
    LOGGER.info ("  Successfully loaded configured key store from the crypto factory.");
    LOGGER.info ("  Loaded key store type is '" + aKS.getType () + "'");

    // List all the aliases - debug only
    try
    {
      final ICommonsList <String> aAliases = CollectionHelper.newList (aKS.aliases ());
      LOGGER.info ("The keystore contains the following " + aAliases.size () + " alias(es): " + aAliases);
      int nIndex = 1;
      for (final String sAlias : aAliases)
      {
        String sType = "unknown";
        try
        {
          final KeyStore.Entry aEntry = aKS.getEntry (sAlias,
                                                      new KeyStore.PasswordProtection (aCF.getKeyStoreDescriptor ()
                                                                                          .getKeyPassword ()));
          if (aEntry instanceof KeyStore.PrivateKeyEntry)
            sType = "private-key";
          else
            if (aEntry instanceof KeyStore.SecretKeyEntry)
              sType = "secret-key";
            else
              if (aEntry instanceof KeyStore.TrustedCertificateEntry)
                sType = "trusted-certificate";
        }
        catch (final Exception ex)
        {
          // Ignore
        }
        LOGGER.info ("  " +
                     nIndex +
                     ".: alias(" +
                     sAlias +
                     ") type(" +
                     sType +
                     ") date(" +
                     aKS.getCreationDate (sAlias) +
                     ")");
        ++nIndex;
      }
    }
    catch (final GeneralSecurityException ex)
    {
      LOGGER.error ("Failed to list all aliases in the keystore", ex);
    }

    // Check if the key configuration is okay - fail early if something is
    // misconfigured
    LOGGER.info ("Trying to load configured private key (alias=" + aCF.getKeyAlias () + ")");
    final PrivateKeyEntry aPKE = aCF.getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured private key");
    LOGGER.info ("  Successfully loaded configured private key from the crypto factory");

    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    // Try the reverse search in the key store - debug only
    try
    {
      final String sAlias = aKS.getCertificateAlias (aAPCert);
      LOGGER.info ("  The reverse search of the certificate lead to alias '" + sAlias + "'");
    }
    catch (final GeneralSecurityException ex)
    {
      LOGGER.error ("Failed to do a reverse search of the certificate", ex);
    }

    // Separate between pilot, test and production
    // final EDBNAllianceStage eStage = APConfig.getStage ();
    // TODO pick the right CA here
    final TrustedCAChecker aAPCAChecker = DBNAllianceTrustStores.Config2023.PILOT_CA;

    // Check if the certificate is really a DBNAlliance AP certificate - fail early
    // if something is misconfigured
    // * Do not cache result
    // * Use the global checking mode or provide a new one
    final ECertificateCheckResult eCheckResult = aAPCAChecker.checkCertificate (aAPCert,
                                                                                MetaAS4Manager.getTimestampMgr ()
                                                                                              .getCurrentDateTime (),
                                                                                ETriState.FALSE,
                                                                                null);
    if (eCheckResult.isInvalid ())
      throw new InitializationException ("The provided certificate is not a valid DBNAlliance AP certificate. Check result: " +
                                         eCheckResult);
    LOGGER.info ("Successfully checked that the provided certificate is a valid DBNAlliance AP certificate.");

    // Enable or disable, if upon reception, the received should be checked or
    // not
    Phase4DBNAllianceDefaultReceiverConfiguration.setAPCAChecker (aAPCAChecker);

    final String sSMPURL = APConfig.getMySmpUrl ();
    final String sAPURL = AS4Configuration.getThisEndpointAddress ();
    if (StringHelper.hasText (sSMPURL) && StringHelper.hasText (sAPURL))
    {
      Phase4DBNAllianceDefaultReceiverConfiguration.setReceiverCheckEnabled (true);
      Phase4DBNAllianceDefaultReceiverConfiguration.setSMPClient (new BDXR2ClientReadOnly (URLHelper.getAsURI (sSMPURL)));
      Phase4DBNAllianceDefaultReceiverConfiguration.setAS4EndpointURL (sAPURL);
      Phase4DBNAllianceDefaultReceiverConfiguration.setAPCertificate (aAPCert);
      LOGGER.info (CAS4.LIB_NAME +
                   " DBNAlliance receiver checks are enabled on SMP '" +
                   sSMPURL +
                   "' and AP '" +
                   sAPURL +
                   "'");
    }
    else
    {
      Phase4DBNAllianceDefaultReceiverConfiguration.setReceiverCheckEnabled (false);
      LOGGER.warn (CAS4.LIB_NAME + " DBNAlliance receiver checks are disabled");
    }
  }

  @Override
  protected void initManagers ()
  {
    _initAS4 ();
    _initDBNAllianceAS4 ();
  }

  @Override
  protected void initAPI (@Nonnull final IAPIRegistry aAPIRegistry)
  {
    Phase4API.init (aAPIRegistry);
  }

  @Override
  protected void beforeContextDestroyed (@Nonnull final ServletContext aSC)
  {
    AS4ServerInitializer.shutdownAS4Server ();
  }
}
