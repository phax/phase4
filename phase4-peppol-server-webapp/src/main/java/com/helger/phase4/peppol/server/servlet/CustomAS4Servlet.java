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
package com.helger.phase4.peppol.server.servlet;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.servlet.Phase4PeppolAS4Servlet;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

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
  &lt;servlet-class&gt;com.helger.phase4.servlet.AS4Servlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Philip Helger
 */
public class CustomAS4Servlet extends Phase4PeppolAS4Servlet
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (CustomAS4Servlet.class);

  public CustomAS4Servlet ()
  {
    super (new Phase4PeppolServletRequestHandlerCustomizer ()
    {
      @Override
      public void customizeBeforeHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                           @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                           @NonNull final AS4RequestHandler aRequestHandler)
      {
        // Parent always first
        super.customizeBeforeHandling (aRequestScope, aUnifiedResponse, aRequestHandler);

        // In case you want a custom Incoming Unique ID
        if (false)
          ((AS4IncomingMessageMetadata) aRequestHandler.getMessageMetadata ()).setIncomingUniqueID ("bla bla bla");

        // Just to show how it works - thread specific prefix
        Phase4LogCustomizer.setThreadLocalLogPrefix ("@" +
                                                     aRequestHandler.getMessageMetadata ().getIncomingUniqueID () +
                                                     " - ");

        // And this shows how to access AS4 Error Messages returned
        aRequestHandler.setErrorConsumer ( (aIncomingState, aEbmsErrors, aAS4ErrorMsg) -> {
          LOGGER.error ("!!! An AS4 error occured: " + aAS4ErrorMsg);
        });
      }

      @Override
      public void customizeAfterHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                          @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                          @NonNull final AS4RequestHandler aRequestHandler)
      {
        Phase4LogCustomizer.clearThreadLocals ();

        // Parent always last
        super.customizeAfterHandling (aRequestScope, aUnifiedResponse, aRequestHandler);
      }
    });
  }
}
