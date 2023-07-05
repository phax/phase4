/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.EHttpMethod;
import com.helger.http.EHttpVersion;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.util.Phase4Exception;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Main handler for the {@link AS4Servlet}
 *
 * @author Philip Helger
 */
public class AS4XServletHandler implements IXServletSimpleHandler
{
  public interface IHandlerCustomizer
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

  private Supplier <? extends IAS4CryptoFactory> m_aCryptoFactorySupplier;
  private Supplier <? extends IAS4CryptoFactory> m_aResponseCryptoFactorySupplier;
  private IPModeResolver m_aPModeResolver;
  private IAS4IncomingAttachmentFactory m_aIAF;
  private IHandlerCustomizer m_aHandlerCustomizer;

  /**
   * Default constructor.
   *
   * @since 0.9.7
   */
  public AS4XServletHandler ()
  {
    this (AS4CryptoFactoryProperties::getDefaultInstance,
          DefaultPModeResolver.DEFAULT_PMODE_RESOLVER,
          IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
  }

  /**
   * Constructor
   *
   * @param aCryptoFactorySupplier
   *        Crypto factory supplier. May not be <code>null</code>.
   * @param aPModeResolver
   *        PMode resolved to be used. May not be <code>null</code>.
   * @param aIAF
   *        The attachment factory for incoming attachments. May not be
   *        <code>null</code>.
   * @since v0.9.8
   */
  public AS4XServletHandler (@Nonnull final Supplier <? extends IAS4CryptoFactory> aCryptoFactorySupplier,
                             @Nonnull final IPModeResolver aPModeResolver,
                             @Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    this(aCryptoFactorySupplier, aCryptoFactorySupplier, aPModeResolver, aIAF);
  }

  /**
   * Constructor
   *
   * @param aCryptoFactorySupplier
   *        Crypto factory supplier. May not be <code>null</code>.
   * @param aResponseCryptoFactorySupplier
   *        Crypto factory supplier for response messages. May not be <code>null</code>.
   * @param aPModeResolver
   *        PMode resolved to be used. May not be <code>null</code>.
   * @param aIAF
   *        The attachment factory for incoming attachments. May not be
   *        <code>null</code>.
   * @since v0.9.8
   */
  public AS4XServletHandler (@Nonnull final Supplier <? extends IAS4CryptoFactory> aCryptoFactorySupplier,
                             @Nonnull final Supplier <? extends IAS4CryptoFactory> aResponseCryptoFactorySupplier,
                             @Nonnull final IPModeResolver aPModeResolver,
                             @Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    setCryptoFactorySupplier (aCryptoFactorySupplier);
    setResponseCryptoFactorySupplier (aResponseCryptoFactorySupplier);
    setPModeResolver (aPModeResolver);
    setIncomingAttachmentFactory (aIAF);
  }

  /**
   * @return The supplier for the {@link IAS4CryptoFactory}. May not be
   *         <code>null</code>.
   * @since 0.9.15
   */
  @Nonnull
  public final Supplier <? extends IAS4CryptoFactory> getCryptoFactorySupplier ()
  {
    return m_aCryptoFactorySupplier;
  }

  /**
   * @param aCryptoFactorySupplier
   *        Crypto factory supplier. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.9.15
   */
  @Nonnull
  public final AS4XServletHandler setCryptoFactorySupplier (@Nonnull final Supplier <? extends IAS4CryptoFactory> aCryptoFactorySupplier)
  {
    ValueEnforcer.notNull (aCryptoFactorySupplier, "CryptoFactorySupplier");
    m_aCryptoFactorySupplier = aCryptoFactorySupplier;
    return this;
  }

  /**
   * @return The supplier for the {@link IAS4CryptoFactory} for response messages. May not be
   *         <code>null</code>.
   * @since x.x.x
   */
  @Nonnull
  public final Supplier <? extends IAS4CryptoFactory> getResponseCryptoFactorySupplier ()
  {
    return m_aResponseCryptoFactorySupplier;
  }

  /**
   * @param aResponseCryptoFactorySupplier
   *        Crypto factory supplier for response messages. May not be <code>null</code>.
   * @return this for chaining
   * @since x.x.x
   */
  @Nonnull
  public final AS4XServletHandler setResponseCryptoFactorySupplier (@Nonnull final Supplier <? extends IAS4CryptoFactory> aResponseCryptoFactorySupplier)
  {
    ValueEnforcer.notNull (aResponseCryptoFactorySupplier, "ResponseCryptoFactorySupplier");
    m_aResponseCryptoFactorySupplier = aResponseCryptoFactorySupplier;
    return this;
  }

  /**
   * @return The {@link IPModeResolver} to be used. Never <code>null</code>.
   * @since 0.9.15
   */
  @Nonnull
  public final IPModeResolver getPModeResolver ()
  {
    return m_aPModeResolver;
  }

  /**
   * @param aPModeResolver
   *        PMode resolved to be used. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.9.15
   */
  @Nonnull
  public final AS4XServletHandler setPModeResolver (@Nonnull final IPModeResolver aPModeResolver)
  {
    ValueEnforcer.notNull (aPModeResolver, "PModeResolver");
    m_aPModeResolver = aPModeResolver;
    return this;
  }

  /**
   * @return The {@link IAS4IncomingAttachmentFactory} to be used. Never
   *         <code>null</code>.
   * @since 0.9.15
   */
  @Nonnull
  public final IAS4IncomingAttachmentFactory getIncomingAttachmentFactory ()
  {
    return m_aIAF;
  }

  /**
   * @param aIAF
   *        The attachment factory for incoming attachments. May not be
   *        <code>null</code>.
   * @return this for chaining
   * @since 0.9.15
   */
  @Nonnull
  public final AS4XServletHandler setIncomingAttachmentFactory (@Nonnull final IAS4IncomingAttachmentFactory aIAF)
  {
    ValueEnforcer.notNull (aIAF, "IAF");
    m_aIAF = aIAF;
    return this;
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
    // Override from base class
    return new AS4UnifiedResponse (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  /**
   * Create the incoming message metadata based on the provided request. This
   * method may be overridden by sub-classes to customize the header generation
   * e.g. when sitting behind a proxy or the like.
   *
   * @param aRequestScope
   *        The request scope to use.
   * @return New {@link AS4IncomingMessageMetadata} and never <code>null</code>.
   * @since 0.12.0
   */
  @Nonnull
  @OverrideOnDemand
  protected AS4IncomingMessageMetadata createIncomingMessageMetadata (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    return AS4IncomingMessageMetadata.createForRequest ()
                                     .setRemoteAddr (aRequestScope.getRemoteAddr ())
                                     .setRemoteHost (aRequestScope.getRemoteHost ())
                                     .setRemotePort (aRequestScope.getRemotePort ())
                                     .setRemoteUser (aRequestScope.getRemoteUser ())
                                     .setCookies (aRequestScope.getCookies ());
  }

  /**
   * Handle an incoming request. Compared to
   * {@link #handleRequest(IRequestWebScopeWithoutResponse, UnifiedResponse)}
   * all the member variables are resolved into parameters to make overriding
   * simpler and also avoid the risk of race conditions on members that use the
   * Supplier pattern.
   *
   * @param aRequestScope
   *        The request scope. May not be <code>null</code>.
   * @param aHttpResponse
   *        The HTTP response to be filled. May not be <code>null</code>.
   * @param aCF
   *        The AS4 crypto factory to be used. May not be <code>null</code>.
   *        Defaults to {@link #getCryptoFactorySupplier()}<code>.get()</code>
   * @param aPModeResolver
   *        The PMode resolver to be used. May not be <code>null</code>.
   *        Defaults to {@link #getPModeResolver()}.
   * @param aIAF
   *        The factory to parse incoming attachments. May not be
   *        <code>null</code>. Defaults to
   *        {@link #getIncomingAttachmentFactory()}.
   * @param aHandlerCustomizer
   *        An optional callback that can be used to modify the internal
   *        {@link AS4RequestHandler} before and after processing. May be
   *        <code>null</code>.
   * @throws Exception
   *         In case of a processing error
   * @since 1.3.1
   */
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final AS4UnifiedResponse aHttpResponse,
                                @Nonnull final IAS4CryptoFactory aCF,
                                @Nonnull final IAS4CryptoFactory aResponseCF,
                                @Nonnull final IPModeResolver aPModeResolver,
                                @Nonnull final IAS4IncomingAttachmentFactory aIAF,
                                @Nullable final IHandlerCustomizer aHandlerCustomizer) throws Exception
  {
    // Start metadata
    final IAS4IncomingMessageMetadata aMessageMetadata = createIncomingMessageMetadata (aRequestScope);

    try (final AS4RequestHandler aHandler = new AS4RequestHandler (aCF, aResponseCF, aPModeResolver, aIAF, aMessageMetadata))
    {
      // Customize before handling
      if (aHandlerCustomizer != null)
      {
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before customizeBeforeHandling");
        aHandlerCustomizer.customizeBeforeHandling (aRequestScope, aHttpResponse, aHandler);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After customizeBeforeHandling");
      }

      // Main handling
      aHandler.handleRequest (aRequestScope, aHttpResponse);

      // Customize after handling
      if (aHandlerCustomizer != null)
      {
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Before customizeAfterHandling");
        aHandlerCustomizer.customizeAfterHandling (aRequestScope, aHttpResponse, aHandler);
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("After customizeAfterHandling");
      }
    }
    catch (final Phase4Exception ex)
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

  // Don't make this final, so that subclasses can call the other handleRequest
  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Resolved once per request
    final IAS4CryptoFactory aCF = m_aCryptoFactorySupplier.get ();
    // Fallback to regular CryptoFactory if no specific CryptoFactory for response messages was provided
    final IAS4CryptoFactory aResponseCF = (m_aResponseCryptoFactorySupplier != null) ? m_aResponseCryptoFactorySupplier.get () : aCF;
    if (aCF == null)
      throw new IllegalStateException ("Failed to get an AS4 CryptoFactory");
    if (aResponseCF == null)
      throw new IllegalStateException ("Failed to get an AS4 response CryptoFactory");

    // Created above in #createUnifiedResponse
    handleRequest (aRequestScope,
                   (AS4UnifiedResponse) aUnifiedResponse,
                   aCF,
                   aResponseCF,
                   m_aPModeResolver,
                   m_aIAF,
                   m_aHandlerCustomizer);
  }
}
