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
package com.helger.phase4.peppol.server.reporting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.IPeppolReportingItemReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * Holds the single Peppol Reporting backend of this application. It is created once on startup and
 * closed on shutdown.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AppReportingBackend
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AppReportingBackend.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IPeppolReportingBackend s_aBackend;

  private AppReportingBackend ()
  {}

  /**
   * Create the Peppol Reporting backend. If no backend provider is on the classpath, no backend is
   * created.
   *
   * @param aConfig
   *        The configuration to use. May not be <code>null</code>.
   * @throws InitializationException
   *         If the backend could not be created
   * @throws IllegalStateException
   *         If the backend was already created
   */
  public static void initBackend (@NonNull final IConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");

    // Check if any SPI is present
    if (PeppolReportingBackend.getAllBackendProviders ().isEmpty ())
    {
      LOGGER.warn ("No Peppol Reporting backend provider was found on the classpath - Peppol Reporting is not available");
      return;
    }

    RW_LOCK.writeLocked (() -> {
      if (s_aBackend != null)
        throw new IllegalStateException ("The Peppol Reporting backend was already created");

      try
      {
        s_aBackend = PeppolReportingBackend.createBackend (aConfig);
      }
      catch (final PeppolReportingBackendException ex)
      {
        throw new InitializationException ("Failed to create the Peppol Reporting backend", ex);
      }
    });
  }

  /**
   * @return The Peppol Reporting backend or <code>null</code> if none was created.
   */
  @Nullable
  public static IPeppolReportingBackend getBackend ()
  {
    return RW_LOCK.readLockedGet (() -> s_aBackend);
  }

  /**
   * @return The Peppol Reporting backend as an item reader or <code>null</code> if no backend was
   *         created or if the backend cannot read the stored reporting items.
   */
  @Nullable
  public static IPeppolReportingItemReader getItemReader ()
  {
    return getBackend () instanceof final IPeppolReportingItemReader aReader ? aReader : null;
  }

  /**
   * Close the Peppol Reporting backend, if it was created.
   */
  public static void shutdownBackend ()
  {
    @SuppressWarnings ("resource")
    final IPeppolReportingBackend aBackend = RW_LOCK.writeLockedGet (() -> {
      final IPeppolReportingBackend ret = s_aBackend;
      s_aBackend = null;
      return ret;
    });
    if (aBackend != null)
      aBackend.close ();
  }
}
