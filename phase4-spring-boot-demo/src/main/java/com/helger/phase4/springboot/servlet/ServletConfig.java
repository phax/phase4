/*
 * Copyright (C) 2021-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.springboot.servlet;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.httpclient.HttpDebugger;
import com.helger.peppol.utils.EPeppolCertificateCheckResult;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.peppol.servlet.Phase4PeppolReceiverCheckData;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletConfiguration;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.phase4.profile.peppol.AS4PeppolProfileRegistarSPI;
import com.helger.phase4.profile.peppol.PeppolCRLDownloader;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.servlet.AS4IncomingProfileSelectorFromGlobal;
import com.helger.phase4.servlet.AS4RequestHandler;
import com.helger.phase4.servlet.AS4ServerInitializer;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.phase4.servlet.AS4XServletHandler.IHandlerCustomizer;
import com.helger.phase4.servlet.mgr.AS4ProfileSelector;
import com.helger.photon.io.WebFileIO;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.ServletHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.AbstractXServlet;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.activation.CommandMap;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletContext;

@Configuration
public class ServletConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServletConfig.class);

  /**
   * This method is a placeholder for retrieving a custom
   * {@link IAS4CryptoFactory}.
   *
   * @return the {@link IAS4CryptoFactory} to use. May not be <code>null</code>.
   */
  @Nonnull
  public static IAS4CryptoFactory getCryptoFactoryToUse ()
  {
    // If you have a custom crypto factory, build/return it here
    return AS4CryptoFactoryProperties.getDefaultInstance ();
  }

  public static class MyAS4Servlet extends AbstractXServlet
  {
    public MyAS4Servlet ()
    {
      // Multipart is handled specifically inside
      settings ().setMultipartEnabled (false);
      final AS4XServletHandler hdl = new AS4XServletHandler ();
      // This method refers to the outer static method
      hdl.setCryptoFactorySupplier (ServletConfig::getCryptoFactoryToUse);

      if (false)
      {
        // Example code to show all possibilities of the handler customizer
        hdl.setHandlerCustomizer (new IHandlerCustomizer ()
        {
          public void customizeBeforeHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                               @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                               @Nonnull final AS4RequestHandler aHandler)
          {
            // Set a different crypto factory based on the request
            hdl.setCryptoFactory (null);
            // TODO
          }

          public void customizeAfterHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                              @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                              @Nonnull final AS4RequestHandler aHandler)
          {
            // empty
          }
        });
      }

      if (false)
      {
        // Example code to disable PMode validation
        hdl.setHandlerCustomizer ( (aRequestScope, aUnifiedResponse, aHandler) -> aHandler.setIncomingProfileSelector (
                                                                                                                       new AS4IncomingProfileSelectorFromGlobal ()
                                                                                                                       {
                                                                                                                         public boolean validateAgainstProfile ()
                                                                                                                         {
                                                                                                                           // override;
                                                                                                                           return false;
                                                                                                                         }
                                                                                                                       }));
      }

      // Example for changing the receiver data based on the source URL
      if (false)
      {
        final IHandlerCustomizer aHandlerCustomizer = (aRequestScope, aUnifiedResponse, aHandler) -> {
          final String sUrl = aRequestScope.getURLDecoded ();
          // The receiver check data you want to set
          final Phase4PeppolReceiverCheckData aReceiverCheckData;
          if (sUrl != null && sUrl.startsWith ("https://ap-prod.example.org/as4"))
          {
            aReceiverCheckData = new Phase4PeppolReceiverCheckData (true,
                                                                    new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-prod.example.org")),
                                                                    Phase4PeppolServletConfiguration.DEFAULT_WILDCARD_SELECTION_MODE,
                                                                    "https://ap-prod.example.org/as4",
                                                                    CertificateHelper.convertStringToCertficateOrNull ("....Public Prod AP Cert...."),
                                                                    Phase4PeppolServletConfiguration.isPerformSBDHValueChecks (),
                                                                    Phase4PeppolServletConfiguration.isCheckSBDHForMandatoryCountryC1 (),
                                                                    Phase4PeppolServletConfiguration.isCheckSigningCertificateRevocation ());
          }
          else
          {
            aReceiverCheckData = new Phase4PeppolReceiverCheckData (true,
                                                                    new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-test.example.org")),
                                                                    Phase4PeppolServletConfiguration.DEFAULT_WILDCARD_SELECTION_MODE,
                                                                    "https://ap-test.example.org/as4",
                                                                    CertificateHelper.convertStringToCertficateOrNull ("....Public Test AP Cert...."),
                                                                    Phase4PeppolServletConfiguration.isPerformSBDHValueChecks (),
                                                                    Phase4PeppolServletConfiguration.isCheckSBDHForMandatoryCountryC1 (),
                                                                    Phase4PeppolServletConfiguration.isCheckSigningCertificateRevocation ());
          }

          // Find the right SPI handler
          aHandler.getProcessorOfType (Phase4PeppolServletMessageProcessorSPI.class)
                  .setReceiverCheckData (aReceiverCheckData);
        };
        hdl.setHandlerCustomizer (aHandlerCustomizer);
      }

      // HTTP POST only
      handlerRegistry ().registerHandler (EHttpMethod.POST, hdl);
    }
  }

  @Bean
  public ServletRegistrationBean <MyAS4Servlet> servletRegistrationBean (final ServletContext ctx)
  {
    // Must be called BEFORE the servlet is instantiated
    _init (ctx);
    final ServletRegistrationBean <MyAS4Servlet> bean = new ServletRegistrationBean <> (new MyAS4Servlet (),
                                                                                        true,
                                                                                        "/as4");
    bean.setLoadOnStartup (1);
    return bean;
  }

  private void _init (@Nonnull final ServletContext aSC)
  {
    // Do it only once
    if (!WebScopeManager.isGlobalScopePresent ())
    {
      WebScopeManager.onGlobalBegin (aSC);
      _initGlobalSettings (aSC);
      _initAS4 ();
      _initPeppolAS4 ();
    }
  }

  private static void _initGlobalSettings (@Nonnull final ServletContext aSC)
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

    // Enforce Peppol profile usage
    AS4ProfileSelector.setCustomAS4ProfileID (AS4PeppolProfileRegistarSPI.AS4_PROFILE_ID);

    // Init the data path
    {
      // Get the ServletContext base path
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aSC);
      // Get the data path
      final String sDataPath = AS4Configuration.getDataPath ();
      if (StringHelper.hasNoText (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final boolean bFileAccessCheck = false;
      // Init the IO layer
      WebFileIO.initPaths (new File (sDataPath).getAbsoluteFile (), sServletContextPath, bFileAccessCheck);
    }
  }

  private static void _initAS4 ()
  {
    AS4ServerInitializer.initAS4Server ();
  }

  private static void _initPeppolAS4 ()
  {
    // Our server expects all SBDH to contain the COUNTRY_C1 element in SBDH
    // (this is the default setting, but added it here for easy modification)
    Phase4PeppolServletConfiguration.setCheckSBDHForMandatoryCountryC1 (true);

    // Our server should check all signing certificates of incoming messages
    // if
    // they are revoked or not
    // (this is the default setting, but added it here for easy modification)
    Phase4PeppolServletConfiguration.setCheckSigningCertificateRevocation (true);

    // Make sure the download of CRL is using Apache HttpClient and that the
    // provided settings are used. If e.g. a proxy is needed to access
    // outbound
    // resources, it can be configured here
    PeppolCRLDownloader.setAsDefaultCRLCache (new Phase4PeppolHttpClientSettings ());

    // Check if crypto properties are okay
    final KeyStore aKS = AS4CryptoFactoryProperties.getDefaultInstance ().getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured AS4 Key store - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = AS4CryptoFactoryProperties.getDefaultInstance ().getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured AS4 private key - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 private key from the crypto factory");

    // No OCSP check for performance
    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    // TODO This block SHOULD be uncommented once you have a Peppol
    // certificate
    if (false)
    {
      // Check that your Peppol AP certificate is valid
      // * No caching
      // * Use global certificate check mode
      final EPeppolCertificateCheckResult eCheckResult = PeppolCertificateChecker.peppolAllAP ()
                                                                                 .checkCertificate (aAPCert,
                                                                                                    MetaAS4Manager.getTimestampMgr ()
                                                                                                                  .getCurrentDateTime (),
                                                                                                    ETriState.FALSE,
                                                                                                    null);
      if (eCheckResult.isInvalid ())
        throw new InitializationException ("The provided certificate is not a Peppol certificate. Check result: " +
                                           eCheckResult);
      LOGGER.info ("Sucessfully checked that the provided Peppol AP certificate is valid.");
    }

    final String sSMPURL = AS4Configuration.getConfig ().getAsString ("smp.url");
    final String sAPURL = AS4Configuration.getThisEndpointAddress ();
    if (StringHelper.hasText (sSMPURL) && StringHelper.hasText (sAPURL))
    {
      // To process the message even though the receiver is not registered in
      // our AP
      Phase4PeppolServletConfiguration.setReceiverCheckEnabled (true);
      Phase4PeppolServletConfiguration.setSMPClient (new SMPClientReadOnly (URLHelper.getAsURI (sSMPURL)));
      Phase4PeppolServletConfiguration.setWildcardSelectionMode (Phase4PeppolServletConfiguration.DEFAULT_WILDCARD_SELECTION_MODE);
      Phase4PeppolServletConfiguration.setAS4EndpointURL (sAPURL);
      Phase4PeppolServletConfiguration.setAPCertificate (aAPCert);
      LOGGER.info ("phase4 Peppol receiver checks are enabled");
    }
    else
    {
      Phase4PeppolServletConfiguration.setReceiverCheckEnabled (false);
      LOGGER.warn ("phase4 Peppol receiver checks are disabled");
    }
  }

  private static final class Destroyer
  {
    @PreDestroy
    public void destroy ()
    {
      if (WebScopeManager.isGlobalScopePresent ())
      {
        AS4ServerInitializer.shutdownAS4Server ();
        WebFileIO.resetPaths ();
        WebScopeManager.onGlobalEnd ();
      }
    }
  }

  @Bean
  public Destroyer destroyer ()
  {
    return new Destroyer ();
  }
}
