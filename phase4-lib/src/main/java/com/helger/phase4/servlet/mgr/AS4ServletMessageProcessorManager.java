/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet.mgr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;

/**
 * This class manages all the {@link IAS4ServletMessageProcessorSPI} SPI
 * implementations.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4ServletMessageProcessorManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ServletMessageProcessorManager.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsList <IAS4ServletMessageProcessorSPI> PROCESSORS = new CommonsArrayList <> ();

  private AS4ServletMessageProcessorManager ()
  {}

  /**
   * Reload all SPI implementations of {@link IAS4ServletMessageProcessorSPI}.
   */
  public static void reinitProcessors ()
  {
    final ICommonsList <IAS4ServletMessageProcessorSPI> aProcessorSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4ServletMessageProcessorSPI.class);
    if (aProcessorSPIs.isEmpty ())
      LOGGER.warn ("No AS4 message processor is registered. All incoming messages will be discarded!");
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
   * @return A list of all registered receiver handlers. Never <code>null</code>
   *         but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <IAS4ServletMessageProcessorSPI> getAllProcessors ()
  {
    return RW_LOCK.readLockedGet (PROCESSORS::getClone);
  }
}
