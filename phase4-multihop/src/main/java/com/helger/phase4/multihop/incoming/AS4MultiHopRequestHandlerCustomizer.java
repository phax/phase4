/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.incoming;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.model.pmode.resolve.IAS4PModeResolver;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.IAS4ServletRequestHandlerCustomizer;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The {@link IAS4ServletRequestHandlerCustomizer} that activates the multi-hop endpoint support
 * for an {@code AS4Servlet}.
 * <p>
 * Install it once per servlet:
 * </p>
 *
 * <pre>
 * final AS4XServletHandler aHandler = new AS4XServletHandler ();
 * aHandler.setRequestHandlerCustomizer (new AS4MultiHopRequestHandlerCustomizer ());
 * </pre>
 * <p>
 * A fresh {@code AS4RequestHandler} is created per request and pre-seeded with the defaults before
 * this customizer runs, so overriding the PMode resolver here is safe.
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class AS4MultiHopRequestHandlerCustomizer implements IAS4ServletRequestHandlerCustomizer
{
  private final IAS4ServletRequestHandlerCustomizer m_aDelegate;
  private final AS4MultiHopConfig m_aConfig;

  /**
   * Constructor using the default configuration and no delegate.
   */
  public AS4MultiHopRequestHandlerCustomizer ()
  {
    this (null, AS4MultiHopConfig.getDefaultInstance ());
  }

  /**
   * Constructor.
   *
   * @param aDelegate
   *        An optional existing customizer that is always invoked first. May be <code>null</code>.
   * @param aConfig
   *        The configuration to be used. May not be <code>null</code>.
   */
  public AS4MultiHopRequestHandlerCustomizer (@Nullable final IAS4ServletRequestHandlerCustomizer aDelegate,
                                              @NonNull final AS4MultiHopConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    m_aDelegate = aDelegate;
    m_aConfig = aConfig;
  }

  /**
   * @return The optional delegate customizer. May be <code>null</code>.
   */
  @Nullable
  public final IAS4ServletRequestHandlerCustomizer getDelegate ()
  {
    return m_aDelegate;
  }

  /**
   * @return The used configuration. Never <code>null</code>.
   */
  @NonNull
  public final AS4MultiHopConfig getConfig ()
  {
    return m_aConfig;
  }

  public void customizeBeforeHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                       @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                       @NonNull final AS4RequestHandler aRequestHandler)
  {
    if (m_aDelegate != null)
      m_aDelegate.customizeBeforeHandling (aRequestScope, aUnifiedResponse, aRequestHandler);

    // Add the routing headers to outgoing Receipts and Errors (R4)
    aRequestHandler.setResponseSignalCustomizer (new MultiHopResponseSignalCustomizer (m_aConfig));

    // Resolve PMode IDs that lost their .init / .resp suffix (D7)
    final IAS4PModeResolver aExistingResolver = aRequestHandler.getPModeResolver ();
    if (aExistingResolver != null && !(aExistingResolver instanceof MultiHopPModeResolver))
      aRequestHandler.setPModeResolver (new MultiHopPModeResolver (aExistingResolver));
  }

  public void customizeAfterHandling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                      @NonNull final AS4UnifiedResponse aUnifiedResponse,
                                      @NonNull final AS4RequestHandler aRequestHandler)
  {
    if (m_aDelegate != null)
      m_aDelegate.customizeAfterHandling (aRequestScope, aUnifiedResponse, aRequestHandler);
  }
}
