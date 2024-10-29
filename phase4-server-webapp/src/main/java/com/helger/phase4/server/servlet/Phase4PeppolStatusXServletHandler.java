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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phase4.CAS4Version;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Create the demo application status information
 *
 * @author Philip Helger
 */
public class Phase4PeppolStatusXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentOffsetDateTimeUTC ()));
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());
    aStatusData.add ("java.version", SystemProperties.getJavaVersion ());
    aStatusData.add ("phase4.version", CAS4Version.BUILD_VERSION);
    aStatusData.add ("phase4.build-timestamp", CAS4Version.BUILD_TIMESTAMP);

    final IAS4CryptoFactory aCF = AS4CryptoFactoryConfiguration.getDefaultInstanceOrNull ();
    if (aCF != null)
    {
      final KeyStore aKS = aCF.getKeyStore ();
      aStatusData.add ("phase4.keystore.loaded", aKS != null);
      if (aKS != null)
      {
        aStatusData.add ("phase4.keystore.key.alias", aCF.getKeyAlias ());
        final KeyStore.PrivateKeyEntry aPKE = aCF.getPrivateKeyEntry ();
        aStatusData.add ("phase4.keystore.key.loaded", aPKE != null);
        if (aPKE != null)
        {
          final X509Certificate aCert = (X509Certificate) aPKE.getCertificate ();
          aStatusData.add ("phase4.keystore.key.issuer", aCert.getIssuerX500Principal ().getName ());
          aStatusData.add ("phase4.keystore.key.subject", aCert.getSubjectX500Principal ().getName ());
        }
      }
    }
    return aStatusData;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Status information requested");

    // Build data to provide
    final IJsonObject aStatusData = getDefaultStatusData ();

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (aStatusData.getAsJsonString (), CHARSET);
  }
}
