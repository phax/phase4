/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.http.CHttp;
import com.helger.http.EHttpMethod;
import com.helger.http.EHttpVersion;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.IMimeType;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.messaging.http.AS4HttpDebug;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.util.Phase4Exception;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Main handler for the {@link AS4Servlet}
 *
 * @author Philip Helger
 */
public class AS4XServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4XServletHandler.class);

  private IAS4ServletRequestHandlerCustomizer m_aRequestHandlerCustomizer;

  /**
   * Default constructor.
   *
   * @since 0.9.7
   */
  public AS4XServletHandler ()
  {}

  /**
   * @return The additional customizer. May be <code>null</code>.
   */
  @Nullable
  public final IAS4ServletRequestHandlerCustomizer getRequestHandlerCustomizer ()
  {
    return m_aRequestHandlerCustomizer;
  }

  /**
   * The customizer to be used.
   *
   * @param aHandlerCustomizer
   *        The new customizer. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public final AS4XServletHandler setRequestHandlerCustomizer (@Nullable final IAS4ServletRequestHandlerCustomizer aHandlerCustomizer)
  {
    m_aRequestHandlerCustomizer = aHandlerCustomizer;
    return this;
  }

  @NonNull
  @Override
  public AS4UnifiedResponse createUnifiedResponse (@NonNull final EHttpVersion eHTTPVersion,
                                                   @NonNull final EHttpMethod eHTTPMethod,
                                                   @NonNull final HttpServletRequest aHttpRequest,
                                                   @NonNull final IRequestWebScope aRequestScope)
  {
    // Override from base class
    return new AS4UnifiedResponse (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  /**
   * Create the incoming message metadata based on the provided request. This method may be
   * overridden by sub-classes to customize the header generation e.g. when sitting behind a proxy
   * or the like.
   *
   * @param aRequestScope
   *        The request scope to use.
   * @return New {@link AS4IncomingMessageMetadata} and never <code>null</code>.
   * @since 0.12.0
   */
  @NonNull
  @OverrideOnDemand
  protected AS4IncomingMessageMetadata createIncomingMessageMetadata (@NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    X509Certificate [] aClientTlsCerts = null;
    try
    {
      // No constant available
      aClientTlsCerts = (X509Certificate []) aRequestScope.getRequest ()
                                                          .getAttribute ("jakarta.servlet.request.X509Certificate");
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("No client TLS certificate provided: " + ex.getMessage ());
    }

    return AS4IncomingMessageMetadata.createForRequest ()
                                     .setRemoteAddr (aRequestScope.getRemoteAddr ())
                                     .setRemoteHost (aRequestScope.getRemoteHost ())
                                     .setRemotePort (aRequestScope.getRemotePort ())
                                     .setRemoteUser (aRequestScope.getRemoteUser ())
                                     .setCookies (aRequestScope.getCookies ())
                                     .setHttpHeaders (aRequestScope.headers ())
                                     .setRemoteTlsCerts (aClientTlsCerts);
  }

  /**
   * Create the {@link IAS4ResponseAbstraction} for use with {@link AS4UnifiedResponse}.
   *
   * @param aHttpResponse
   *        The unified response to be wrapped. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static IAS4ResponseAbstraction createResponseAbstraction (@NonNull final AS4UnifiedResponse aHttpResponse)
  {
    return new IAS4ResponseAbstraction ()
    {
      public void setContent (final byte @NonNull [] aBytes, @NonNull final Charset aCharset)
      {
        aHttpResponse.setContent (aBytes);
        aHttpResponse.setCharset (aCharset);
      }

      public void setContent (@NonNull final HttpHeaderMap aHeaderMap, @NonNull final IHasInputStream aHasIS)
      {
        aHttpResponse.addCustomResponseHeaders (aHeaderMap);
        aHttpResponse.setContent (aHasIS);
      }

      public void setMimeType (@NonNull final IMimeType aMimeType)
      {
        aHttpResponse.setMimeType (aMimeType);
      }

      public void setStatus (final int nStatusCode)
      {
        aHttpResponse.setStatus (nStatusCode);
      }
    };
  }

  /**
   * Handle an incoming request. Compared to
   * {@link #handleRequest(IRequestWebScopeWithoutResponse, UnifiedResponse)} all the member
   * variables are resolved into parameters to make overriding simpler.
   *
   * @param aRequestScope
   *        The request scope. May not be <code>null</code>.
   * @param aHttpResponse
   *        The HTTP response to be filled. May not be <code>null</code>.
   * @param aHandlerCustomizer
   *        An optional callback that can be used to modify the internal {@link AS4RequestHandler}
   *        before and after processing. May be <code>null</code>.
   * @throws Exception
   *         In case of a processing error
   * @since 1.3.1
   */
  protected void handleRequest (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                @NonNull final AS4UnifiedResponse aHttpResponse,
                                @Nullable final IAS4ServletRequestHandlerCustomizer aHandlerCustomizer) throws Exception
  {
    // Start metadata
    final IAS4IncomingMessageMetadata aMessageMetadata = createIncomingMessageMetadata (aRequestScope);

    try (final AS4RequestHandler aHandler = new AS4RequestHandler (aMessageMetadata))
    {
      // No specific AS4 profile is available here - choose the default one
      final String sAS4ProfileID = AS4ProfileSelector.getDefaultAS4ProfileID ();

      // Set default values in handler
      final IAS4CryptoFactory aCF = AS4CryptoFactoryConfiguration.getDefaultInstanceOrNull ();
      if (aCF != null)
        aHandler.setCryptoFactory (aCF);
      aHandler.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
      aHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID, true));
      aHandler.setIncomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
      aHandler.setIncomingSecurityConfiguration (AS4IncomingSecurityConfiguration.createDefaultInstance ());
      aHandler.setIncomingReceiverConfiguration (new AS4IncomingReceiverConfiguration ());

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
      AS4HttpDebug.debug ( () -> "RECEIVE-START at " + aRequestScope.getFullContextAndServletPath ());

      final ServletInputStream aServletRequestIS = aRequestScope.getRequest ().getInputStream ();
      final HttpHeaderMap aHttpHeaders = aRequestScope.headers ().getClone ();
      final IAS4ResponseAbstraction aResponse = createResponseAbstraction (aHttpResponse);

      aHandler.handleRequest (aServletRequestIS, aHttpHeaders, aResponse);

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

  public final void handleRequest (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                   @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Created above in #createUnifiedResponse
    final AS4UnifiedResponse aRealUnifiedResponse = (AS4UnifiedResponse) aUnifiedResponse;
    handleRequest (aRequestScope, aRealUnifiedResponse, m_aRequestHandlerCustomizer);
  }
}
