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
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.AS4ServerInitializer;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.peppol.servlet.Phase4PeppolDefaultReceiverConfiguration;
import com.helger.phase4.peppol.servlet.Phase4PeppolReceiverConfiguration;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.phase4.profile.peppol.AS4PeppolProfileRegistarSPI;
import com.helger.phase4.profile.peppol.PeppolCRLDownloader;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.phase4.servlet.IAS4ServletRequestHandlerCustomizer;
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
    return AS4CryptoFactoryConfiguration.getDefaultInstance ();
  }

  public static class MyAS4Servlet extends AbstractXServlet
  {
    public MyAS4Servlet ()
    {
      // Multipart is handled specifically inside
      settings ().setMultipartEnabled (false);
      final AS4XServletHandler hdl = new AS4XServletHandler ();
      hdl.setRequestHandlerCustomizer (new IAS4ServletRequestHandlerCustomizer ()
      {
        public void customizeBeforeHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                             @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                             @Nonnull final AS4RequestHandler aRequestHandler)
        {
          // This method refers to the outer static method
          aRequestHandler.setCryptoFactory (ServletConfig.getCryptoFactoryToUse ());

          // Example code to disable PMode validation
          if (false)
          {
            // Note: API is for 3.0.0 only
            aRequestHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (AS4PeppolProfileRegistarSPI.AS4_PROFILE_ID,
                                                                                                false));
          }

          // Example for changing the Peppol receiver data based on the source
          // URL
          if (false)
          {
            final String sUrl = aRequestScope.getURLDecoded ();
            // The receiver check data you want to set
            final Phase4PeppolReceiverConfiguration aReceiverCheckData;
            if (sUrl != null && sUrl.startsWith ("https://ap-prod.example.org/as4"))
            {
              aReceiverCheckData = new Phase4PeppolReceiverConfiguration (true,
                                                                          new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-prod.example.org")),
                                                                          Phase4PeppolDefaultReceiverConfiguration.DEFAULT_WILDCARD_SELECTION_MODE,
                                                                          "https://ap-prod.example.org/as4",
                                                                          CertificateHelper.convertStringToCertficateOrNull ("....Public Prod AP Cert...."),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isPerformSBDHValueChecks (),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isCheckSBDHForMandatoryCountryC1 (),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isCheckSigningCertificateRevocation ());
            }
            else
            {
              aReceiverCheckData = new Phase4PeppolReceiverConfiguration (true,
                                                                          new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-test.example.org")),
                                                                          Phase4PeppolDefaultReceiverConfiguration.DEFAULT_WILDCARD_SELECTION_MODE,
                                                                          "https://ap-test.example.org/as4",
                                                                          CertificateHelper.convertStringToCertficateOrNull ("....Public Test AP Cert...."),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isPerformSBDHValueChecks (),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isCheckSBDHForMandatoryCountryC1 (),
                                                                          Phase4PeppolDefaultReceiverConfiguration.isCheckSigningCertificateRevocation ());
            }

            // Find the right SPI handler
            aRequestHandler.getProcessorOfType (Phase4PeppolServletMessageProcessorSPI.class)
                           .setReceiverCheckData (aReceiverCheckData);
          }
        }

        public void customizeAfterHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                            @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                            @Nonnull final AS4RequestHandler aRequestHandler)
        {
          // empty
        }
      });

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
    if (CommandMap.getDefaultCommandMap ()
                  .createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) == null)
      throw new IllegalStateException ("No DataContentHandler for MIME Type '" +
                                       CMimeType.MULTIPART_RELATED.getAsString () +
                                       "' is available. There seems to be a problem with the dependencies/packaging");

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
    // Enforce Peppol profile usage
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4PeppolProfileRegistarSPI.AS4_PROFILE_ID);

    AS4ServerInitializer.initAS4Server ();
  }

  private static void _initPeppolAS4 ()
  {
    // Our server expects all SBDH to contain the COUNTRY_C1 element in SBDH
    // (this is the default setting, but added it here for easy modification)
    Phase4PeppolDefaultReceiverConfiguration.setCheckSBDHForMandatoryCountryC1 (true);

    // Our server should check all signing certificates of incoming messages
    // if
    // they are revoked or not
    // (this is the default setting, but added it here for easy modification)
    Phase4PeppolDefaultReceiverConfiguration.setCheckSigningCertificateRevocation (true);

    // Make sure the download of CRL is using Apache HttpClient and that the
    // provided settings are used. If e.g. a proxy is needed to access
    // outbound
    // resources, it can be configured here
    PeppolCRLDownloader.setAsDefaultCRLCache (new Phase4PeppolHttpClientSettings ());

    // Check if crypto properties are okay
    final KeyStore aKS = AS4CryptoFactoryConfiguration.getDefaultInstance ().getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured AS4 Key store - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = AS4CryptoFactoryConfiguration.getDefaultInstance ().getPrivateKeyEntry ();
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
      final EPeppolCertificateCheckResult eCheckResult = PeppolCertificateChecker.checkPeppolAPCertificate (aAPCert,
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
      Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (true);
      Phase4PeppolDefaultReceiverConfiguration.setSMPClient (new SMPClientReadOnly (URLHelper.getAsURI (sSMPURL)));
      Phase4PeppolDefaultReceiverConfiguration.setWildcardSelectionMode (Phase4PeppolDefaultReceiverConfiguration.DEFAULT_WILDCARD_SELECTION_MODE);
      Phase4PeppolDefaultReceiverConfiguration.setAS4EndpointURL (sAPURL);
      Phase4PeppolDefaultReceiverConfiguration.setAPCertificate (aAPCert);
      LOGGER.info ("phase4 Peppol receiver checks are enabled");
    }
    else
    {
      Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (false);
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
