/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.http.EHttpMethod;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.phase4.servlet.IAS4ServletRequestHandlerCustomizer;
import com.helger.phase4.wss.AS4BinarySecurityTokenOnlySignatureTrustValidator;
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
  /**
   * Default {@link IAS4ServletRequestHandlerCustomizer} implementation
   *
   * @author Philip Helger
   * @since 4.2.6
   */
  public static class Phase4PeppolServletRequestHandlerCustomizer implements IAS4ServletRequestHandlerCustomizer
  {
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
          aWSSConfig.setValidator (WSConstants.SIGNATURE, new AS4BinarySecurityTokenOnlySignatureTrustValidator ());
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

  /**
   * Default constructor using {@link Phase4PeppolServletRequestHandlerCustomizer} as the
   * customizer.
   */
  public Phase4PeppolAS4Servlet ()
  {
    // Use the default customizer
    this (new Phase4PeppolServletRequestHandlerCustomizer ());
  }

  /**
   * Custom constructor providing a custom customizer. This is primarily intended to subclass this
   * class.
   *
   * @param aCustomizer
   *        Request handler customizer to be used. Must not be <code>null</code>
   */
  protected Phase4PeppolAS4Servlet (@NonNull final Phase4PeppolServletRequestHandlerCustomizer aCustomizer)
  {
    ValueEnforcer.notNull (aCustomizer, "Customizer");

    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);

    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST,
                                        new AS4XServletHandler ().setRequestHandlerCustomizer (aCustomizer));
  }
}
