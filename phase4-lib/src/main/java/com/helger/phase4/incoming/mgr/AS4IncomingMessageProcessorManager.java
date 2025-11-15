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
import com.helger.phase4.incoming.spi.IAS4IncomingMessageProcessorSPI;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * This class manages all the {@link IAS4IncomingMessageProcessorSPI} SPI implementations.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4IncomingMessageProcessorManager
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AS4IncomingMessageProcessorManager.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsList <IAS4IncomingMessageProcessorSPI> PROCESSORS = new CommonsArrayList <> ();

  private AS4IncomingMessageProcessorManager ()
  {}

  /**
   * Reload all SPI implementations of {@link IAS4IncomingMessageProcessorSPI}.
   */
  public static void reinitProcessors ()
  {
    final List <IAS4IncomingMessageProcessorSPI> aProcessorSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4IncomingMessageProcessorSPI.class);
    if (aProcessorSPIs.isEmpty ())
      LOGGER.warn ("No AS4 message processor is registered. All incoming messages will be positively responded and discarded on the receiver side!");
    else
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Found " + aProcessorSPIs.size () + " AS4 message processors");

    RW_LOCK.writeLocked ( () -> PROCESSORS.setAll (aProcessorSPIs));
  }

  static
  {
    // Init once at the beginning
    reinitProcessors ();
  }

  /**
   * @return A list of all registered receiver handlers. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAS4IncomingMessageProcessorSPI> getAllProcessors ()
  {
    return RW_LOCK.readLockedGet (PROCESSORS::getClone);
  }
}
