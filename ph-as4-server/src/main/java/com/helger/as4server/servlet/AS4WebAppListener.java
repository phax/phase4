/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.CryptoType.TYPE;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.as4lib.util.StringMap;
import com.helger.as4server.mgr.AS4ServerConfiguration;
import com.helger.as4server.mgr.AS4ServerSettings;
import com.helger.as4server.mgr.MetaManager;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4server.receive.soap.SOAPHeaderElementProcessorWSS4J;
import com.helger.commons.collection.ArrayHelper;
import com.helger.photon.core.requesttrack.RequestTracker;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.UserManager;
import com.helger.security.certificate.CertificateHelper;
import com.helger.servlet.mock.OfflineHttpServletRequest;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.impl.RequestWebScopeNoMultipart;
import com.helger.web.scope.mgr.DefaultWebScopeFactory;
import com.helger.web.scope.mgr.WebScopeFactoryProvider;

public final class AS4WebAppListener extends WebAppListener
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4WebAppListener.class);

  @Override
  @Nullable
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isGlobalDebug ());
  }

  @Override
  @Nullable
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isGlobalProduction ());
  }

  @Override
  @Nullable
  protected String getInitParameterNoStartupInfo (@Nonnull final ServletContext aSC)
  {
    return Boolean.toString (AS4ServerConfiguration.isNoStartupInfo ());
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AS4ServerConfiguration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return false;
  }

  private static void _createDefaultResponder (@Nonnull final String sDefaultPartnerID)
  {
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    if (!aPartnerMgr.containsWithID (sDefaultPartnerID))
    {
      final StringMap aStringMap = new StringMap ();
      aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, sDefaultPartnerID);
      try
      {
        final CryptoType aCT = new CryptoType (TYPE.ALIAS);
        aCT.setAlias (AS4CryptoFactory.getKeyAlias ());
        final X509Certificate [] aCertList = AS4CryptoFactory.getCrypto ().getX509Certificates (aCT);
        if (ArrayHelper.isEmpty (aCertList))
          throw new IllegalStateException ("Failed to find default partner certificate from alias '" +
                                           aCT.getAlias () +
                                           "'");
        aStringMap.setAttribute (Partner.ATTR_CERT, CertificateHelper.getPEMEncodedCertificate (aCertList[0]));
        aPartnerMgr.createOrUpdatePartner (sDefaultPartnerID, aStringMap);
      }
      catch (final WSSecurityException ex)
      {
        throw new IllegalStateException ("Error retrieving certificate", ex);
      }
    }
  }

  @Override
  protected void afterContextInitialized (@Nonnull final ServletContext aSC)
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry aReg = SOAPHeaderElementProcessorRegistry.getInstance ();
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
                                                    "Messaging"),
                                         new SOAPHeaderElementProcessorExtractEbms3Messaging ());
    // WSS4J must be after Ebms3Messaging handler!
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                                                    "Security"),
                                         new SOAPHeaderElementProcessorWSS4J ());

    WebScopeFactoryProvider.setWebScopeFactory (new DefaultWebScopeFactory ()
    {
      @Override
      @Nonnull
      public IRequestWebScope createRequestScope (@Nonnull final HttpServletRequest aHttpRequest,
                                                  @Nonnull final HttpServletResponse aHttpResponse)
      {
        // TODO hard coded AS4 servlet path
        if (!(aHttpRequest instanceof OfflineHttpServletRequest) && aHttpRequest.getServletPath ().equals ("/as4"))
        {
          // Ensure to create request scopes not using Multipart handling so
          // that the MIME parsing can happen in the Servlet
          return new RequestWebScopeNoMultipart (aHttpRequest, aHttpResponse);
        }
        return super.createRequestScope (aHttpRequest, aHttpResponse);
      }
    });

    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();
    MetaManager.getInstance ();
    _createDefaultResponder (AS4ServerSettings.getDefaultResponderID ());
    RequestTracker.getInstance ().getRequestTrackingMgr ().setLongRunningCheckEnabled (false);

    // Ensure user exists
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    if (!aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
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

    s_aLogger.info ("AS4 server started");
  }

  @Override
  protected void afterContextDestroyed (@Nonnull final ServletContext aSC)
  {
    s_aLogger.info ("AS4 server destroyed");
  }
}
