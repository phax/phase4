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
package com.helger.phase4.incoming.mgr;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.incoming.spi.IAS4IncomingSignalMessagePModeProviderSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * This class manages all the {@link IAS4IncomingSignalMessagePModeProviderSPI} SPI implementations.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public final class AS4IncomingSignalMessagePModeProviderManager
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4IncomingSignalMessagePModeProviderManager.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsList <IAS4IncomingSignalMessagePModeProviderSPI> PROVIDERS = new CommonsArrayList <> ();

  private AS4IncomingSignalMessagePModeProviderManager ()
  {}

  /**
   * Reload all SPI implementations of {@link IAS4IncomingSignalMessagePModeProviderSPI}.
   */
  public static void reinitProviders ()
  {
    final List <IAS4IncomingSignalMessagePModeProviderSPI> aProviderSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4IncomingSignalMessagePModeProviderSPI.class);
    if (aProviderSPIs.isEmpty ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("No AS4 incoming signal message PMode provider is registered");
    }
    else
      LOGGER.info ("Found " + aProviderSPIs.size () + " AS4 incoming signal message PMode providers");

    RW_LOCK.writeLocked ( () -> PROVIDERS.setAll (aProviderSPIs));
  }

  static
  {
    // Init once at the beginning
    reinitProviders ();
  }

  /**
   * @return A list of all registered providers. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAS4IncomingSignalMessagePModeProviderSPI> getAllProviders ()
  {
    return RW_LOCK.readLockedGet (PROVIDERS::getClone);
  }
}
