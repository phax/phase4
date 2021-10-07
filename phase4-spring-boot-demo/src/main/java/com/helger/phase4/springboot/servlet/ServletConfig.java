/*
 * Copyright (C) 2021 Philip Helger (www.helger.com)
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

import javax.activation.CommandMap;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.httpclient.HttpDebugger;
import com.helger.peppol.utils.EPeppolCertificateCheckResult;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.peppol.servlet.Phase4PeppolServlet;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletConfiguration;
import com.helger.phase4.profile.peppol.AS4PeppolProfileRegistarSPI;
import com.helger.phase4.servlet.AS4ServerInitializer;
import com.helger.phase4.servlet.mgr.AS4ProfileSelector;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.ServletHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

@Configuration
public class ServletConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServletConfig.class);

  @Bean
  public ServletRegistrationBean <Phase4PeppolServlet> servletRegistrationBean (final ServletContext ctx)
  {
    // Must be called BEFORE the servlet is instantiated
    _init (ctx);
    final ServletRegistrationBean <Phase4PeppolServlet> bean = new ServletRegistrationBean <> (new Phase4PeppolServlet (), true, "/as4");
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
    if (CommandMap.getDefaultCommandMap ().createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) == null)
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
    // Check if crypto properties are okay
    final KeyStore aKS = AS4CryptoFactoryProperties.getDefaultInstance ().getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured Keystore");
    LOGGER.info ("Successfully loaded configured key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = AS4CryptoFactoryProperties.getDefaultInstance ().getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured private key");
    LOGGER.info ("Successfully loaded configured private key from the crypto factory");

    // No OCSP check for performance
    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    // TODO This block SHOULD be uncommented once you have a Peppol certificate
    if (false)
    {
      final EPeppolCertificateCheckResult eCheckResult = PeppolCertificateChecker.checkPeppolAPCertificate (aAPCert,
                                                                                                            MetaAS4Manager.getTimestampMgr ()
                                                                                                                          .getCurrentDateTime (),
                                                                                                            ETriState.FALSE,
                                                                                                            null);
      if (eCheckResult.isInvalid ())
        throw new InitializationException ("The provided certificate is not a Peppol certificate. Check result: " + eCheckResult);
      LOGGER.info ("Sucessfully checked that the provided Peppol AP certificate is valid.");
    }

    final String sSMPURL = AS4Configuration.getConfig ().getAsString ("smp.url");
    final String sAPURL = AS4Configuration.getThisEndpointAddress ();
    if (StringHelper.hasText (sSMPURL) && StringHelper.hasText (sAPURL))
    {
      // To process the message even though the receiver is not registered in
      // our AP
      Phase4PeppolServletConfiguration.setReceiverCheckEnabled (false);
      Phase4PeppolServletConfiguration.setSMPClient (new SMPClientReadOnly (URLHelper.getAsURI (sSMPURL)));
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

  @PreDestroy
  public void destroy ()
  {
    if (WebScopeManager.isGlobalScopePresent ())
    {
      AS4ServerInitializer.shutdownAS4Server ();
      WebScopeManager.onGlobalEnd ();
    }
  }
}
