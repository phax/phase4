/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.lang.GenericReflection;
import com.helger.http.EHttpVersion;
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactory;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
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
  public static interface IHandlerCustomizer extends Serializable
  {
    /**
     * Called before the message is handled. <br>
     * Note: was called "customize" until v0.9.4
     *
     * @param aRequestScope
     *        Request scope. Never <code>null</code>.
     * @param aUnifiedResponse
     *        The response to be filled. Never <code>null</code>.
     * @param aHandler
     *        The main handler doing the hard work. Never <code>null</code>.
     */
    void customizeBeforeHandling (@Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                                  @Nonnull AS4UnifiedResponse aUnifiedResponse,
                                  @Nonnull AS4RequestHandler aHandler);

    /**
     * Called after the message was handled, and no exception was thrown.
     *
     * @param aRequestScope
     *        Request scope. Never <code>null</code>.
     * @param aUnifiedResponse
     *        The response to be filled. Never <code>null</code>.
     * @param aHandler
     *        The main handler doing the hard work. Never <code>null</code>.
     * @since 0.9.5
     */
    default void customizeAfterHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                         @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                         @Nonnull final AS4RequestHandler aHandler)
    {}
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4XServletHandler.class);

  private final IAS4CryptoFactory m_aCryptoFactory;
  private final IPModeResolver m_aPModeResolver;
  private final IIncomingAttachmentFactory m_aIAF;
  private IHandlerCustomizer m_aHandlerCustomizer;

  /**
   * Default constructor.
   *
   * @since 0.9.7
   */
  public AS4XServletHandler ()
  {
    this (AS4CryptoFactory.getDefaultInstance (),
          DefaultPModeResolver.DEFAULT_PMODE_RESOLVER,
          IIncomingAttachmentFactory.DEFAULT_INSTANCE);
  }

  /**
   * Constructor
   *
   * @param aCryptoFactory
   *        Crypto factory. May not be <code>null</code>.
   * @param aPModeResolver
   *        PMode resolved to be used. May not be <code>null</code>.
   * @param aIAF
   *        The attachment factory for incoming attachments.
   */
  public AS4XServletHandler (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                             @Nonnull final IPModeResolver aPModeResolver,
                             @Nonnull final IIncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aPModeResolver, "PModeResolver");
    ValueEnforcer.notNull (aIAF, "IAF");
    m_aCryptoFactory = aCryptoFactory;
    m_aPModeResolver = aPModeResolver;
    m_aIAF = aIAF;
  }

  /**
   * @return The additional customizer. May be <code>null</code>.
   */
  @Nullable
  public final IHandlerCustomizer getHandlerCustomizer ()
  {
    return m_aHandlerCustomizer;
  }

  /**
   * The customizer to be used.
   *
   * @param aHandlerCustomizer
   *        The new customizer. May be <code>null</code>.
   * @return this for chaining
   */
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

    // Start metadata
    final AS4IncomingRequestMetadata aRequestMetadata = new AS4IncomingRequestMetadata ().setIncomingDTNow ()
                                                                                         .setRemoteAddr (aRequestScope.getRemoteAddr ())
                                                                                         .setRemoteHost (aRequestScope.getRemoteHost ())
                                                                                         .setRemotePort (aRequestScope.getRemotePort ());

    try (final AS4RequestHandler aHandler = new AS4RequestHandler (m_aCryptoFactory,
                                                                   m_aPModeResolver,
                                                                   m_aIAF,
                                                                   aRequestMetadata))
    {
      // Customize before handling
      if (m_aHandlerCustomizer != null)
      {
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before customizeBeforeHandling");
        m_aHandlerCustomizer.customizeBeforeHandling (aRequestScope, aHttpResponse, aHandler);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After customizeBeforeHandling");
      }

      // Main handling
      aHandler.handleRequest (aRequestScope, aHttpResponse);

      // Customize after handling
      if (m_aHandlerCustomizer != null)
      {
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before customizeAfterHandling");
        m_aHandlerCustomizer.customizeAfterHandling (aRequestScope, aHttpResponse, aHandler);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After customizeAfterHandling");
      }
    }
    catch (final AS4BadRequestException ex)
    {
      // Logged inside
      aHttpResponse.setResponseError (CHttp.HTTP_BAD_REQUEST, "Bad Request: " + ex.getMessage (), ex.getCause ());
    }
    catch (final Exception ex)
    {
      // Logged inside
      aHttpResponse.setResponseError (CHttp.HTTP_INTERNAL_SERVER_ERROR, "Internal error processing AS4 request", ex);
    }
  }
}
