package com.helger.as4server.spi;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.lang.ServiceLoaderHelper;

/**
 * This class manages all the {@link IAS4ServletMessageProcessorSPI} SPI
 * implementations.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4ServletMessageProcessorManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4ServletMessageProcessorManager.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static final ICommonsList <IAS4ServletMessageProcessorSPI> s_aProcessors = new CommonsArrayList<> ();

  private AS4ServletMessageProcessorManager ()
  {}

  /**
   * Reload all SPI implementations of {@link IAS4ServletMessageProcessorSPI}.
   */
  public static void reinitProcessors ()
  {
    final ICommonsList <IAS4ServletMessageProcessorSPI> aProcessorSPIs = ServiceLoaderHelper.getAllSPIImplementations (IAS4ServletMessageProcessorSPI.class);
    if (aProcessorSPIs.isEmpty ())
      s_aLogger.warn ("No AS4 message processor is registered. All incoming messages will be discarded!");
    else
      s_aLogger.info ("Found " + aProcessorSPIs.size () + " AS4 message processors");

    s_aRWLock.writeLocked ( () -> s_aProcessors.setAll (aProcessorSPIs));
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
    return s_aRWLock.readLocked ( () -> s_aProcessors.getClone ());
  }
}
