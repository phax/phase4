/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.servlet;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.lang.GenericReflection;
import com.helger.http.EHttpVersion;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Main handler for the {@link AS4Servlet}
 *
 * @author Philip Helger
 */
public class AS4XServletHandler implements IXServletSimpleHandler
{
  @FunctionalInterface
  public static interface IHandlerCustomizer extends Serializable
  {
    void customize (@Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                    @Nonnull AS4UnifiedResponse aUnifiedResponse,
                    @Nonnull AS4Handler aHandler);
  }

  private final AS4ResourceManager m_aResMgr;
  private final AS4CryptoFactory m_aCryptoFactory;
  private final IIncomingAttachmentFactory m_aIAF;
  private IHandlerCustomizer m_aHandlerCustomizer;

  public AS4XServletHandler (@Nonnull final AS4ResourceManager aResMgr,
                             @Nonnull final AS4CryptoFactory aCryptoFactory,
                             @Nonnull final IIncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aResMgr, "ResMgr");
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aIAF, "IAF");
    m_aResMgr = aResMgr;
    m_aCryptoFactory = aCryptoFactory;
    m_aIAF = aIAF;
  }

  @Nullable
  public final IHandlerCustomizer getHandlerCustomizer ()
  {
    return m_aHandlerCustomizer;
  }

  @Nonnull
  public final AS4XServletHandler setHandlerCustomizer (@Nullable final IHandlerCustomizer aHandlerCustomizer)
  {
    m_aHandlerCustomizer = aHandlerCustomizer;
    return this;
  }

  @Nonnull
  @Override
  public AS4UnifiedResponse createUnifiedResponse (@Nonnull final EHttpVersion eHTTPVersion,
                                                   @Nonnull final EHttpMethod eHTTPMethod,
                                                   @Nonnull final HttpServletRequest aHttpRequest,
                                                   @Nonnull final IRequestWebScope aRequestScope)
  {
    return new AS4UnifiedResponse (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Created above in #createUnifiedResponse
    final AS4UnifiedResponse aHttpResponse = GenericReflection.uncheckedCast (aUnifiedResponse);

    try (final AS4Handler aHandler = new AS4Handler (m_aResMgr, m_aCryptoFactory, m_aIAF))
    {
      // Customize before handling
      if (m_aHandlerCustomizer != null)
        m_aHandlerCustomizer.customize (aRequestScope, aHttpResponse, aHandler);

      // Main handling
      aHandler.handleRequest (aRequestScope, aHttpResponse);
    }
    catch (final BadRequestException ex)
    {
      // Logged inside
      aHttpResponse.setResponseError (HttpServletResponse.SC_BAD_REQUEST,
                                      "Bad Request: " + ex.getMessage (),
                                      ex.getCause ());
    }
    catch (final Throwable t)
    {
      // Logged inside
      aHttpResponse.setResponseError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                      "Internal error processing AS4 request",
                                      t);
    }
  }
}
