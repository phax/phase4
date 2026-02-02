/*
 * Copyright (C) 2020-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.servlet;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.array.ArrayHelper;
import com.helger.http.EHttpMethod;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.phase4.servlet.IAS4ServletRequestHandlerCustomizer;
import com.helger.phase4.util.Phase4Exception;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;

/**
 * AS4 receiving servlet.<br>
 * This servlet works only, if a single AS4 profile is present. If multiple AS4 profiles should be
 * served, it is recommended to provide two different servlets and customize the
 * {@link AS4XServletHandler} accordingly. See
 * https://github.com/phax/phase4/wiki/Multi-Profile-Handling for a more detailed description.<br>
 * Use a configuration like the following in your <code>WEB-INF/web.xm</code> file:
 *
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.helger.phase4.peppol.servlet.Phase4PeppolAS4Servlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Philip Helger
 * @since 4.2.6
 */
public class Phase4PeppolAS4Servlet extends AbstractXServlet
{
  public static class Phase4PeppolServletRequestHandlerCustomizer implements IAS4ServletRequestHandlerCustomizer
  {
    private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4PeppolAS4Servlet.class);

    @OverridingMethodsMustInvokeSuper
    public void customizeBeforeHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                         @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                         @NonNull final AS4RequestHandler aRequestHandler)
    {
      // Make sure, Peppol only accepts BinarySecurityToken
      final AS4IncomingSecurityConfiguration aIncomingSecCfg = AS4IncomingSecurityConfiguration.createDefaultInstance ();
      aIncomingSecCfg.setDecryptParameterModifier (new IAS4DecryptParameterModifier ()
      {
        public void modifyWSSConfig (@NonNull final WSSConfig aWSSConfig)
        {
          aWSSConfig.setValidator (WSConstants.SIGNATURE, new SignatureTrustValidator ()
          {
            @Override
            public Credential validate (@NonNull final Credential aCredential, @NonNull final RequestData aReqData)
                                                                                                                    throws WSSecurityException
            {
              // Check that we have the full certificate available
              if (ArrayHelper.isEmpty (aCredential.getCertificates ()))
              {
                // No BST used -> reject
                throw new WSSecurityException (WSSecurityException.ErrorCode.FAILURE,
                                               new Phase4Exception ("Only BinarySecurityToken-based keys allowed for signature verification"));
              }

              if (LOGGER.isDebugEnabled ())
                LOGGER.debug ("Verified that inbound message uses a BinarySecurityToken for signature verification");

              return super.validate (aCredential, aReqData);
            }
          });
        }
      });
      aRequestHandler.setIncomingSecurityConfiguration (aIncomingSecCfg);
    }

    @OverridingMethodsMustInvokeSuper
    public void customizeAfterHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                        @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                        @NonNull final AS4RequestHandler aRequestHandler)
    {
      // Nada yet
    }
  }

  public Phase4PeppolAS4Servlet ()
  {
    // Use the default customizer
    this (new Phase4PeppolServletRequestHandlerCustomizer ());
  }

  public Phase4PeppolAS4Servlet (@Nullable final IAS4ServletRequestHandlerCustomizer aCustomizer)
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);

    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST,
                                        new AS4XServletHandler ().setRequestHandlerCustomizer (aCustomizer));
  }
}
