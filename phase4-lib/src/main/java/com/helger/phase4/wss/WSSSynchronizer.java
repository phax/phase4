package com.helger.phase4.wss;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.functional.IThrowingSupplier;
import com.helger.phase4.config.AS4Configuration;

/**
 * A helper class to run all WSS stuff in a lock. {@link WSSConfig#init()} and
 * {@link WSSConfig#cleanUp()} is called for every invocation.<br>
 * Note: this class may only be invoked if
 * {@link AS4Configuration#isWSS4JSynchronizedSecurity()} returns
 * <code>true</code>.
 *
 * @author Philip Helger
 * @since 0.11.0
 */
@ThreadSafe
public final class WSSSynchronizer
{
  private static final Lock s_aLock = new ReentrantLock ();

  private WSSSynchronizer ()
  {}

  public static void run (@Nonnull final Runnable aRunnable)
  {
    call ( () -> {
      aRunnable.run ();
      return null;
    });
  }

  @Nullable
  public static <T, EX extends Exception> T call (@Nonnull final IThrowingSupplier <T, EX> aSupplier) throws EX
  {
    // Lock
    s_aLock.lock ();
    try
    {
      // Register
      WSSConfig.init ();
      try
      {
        return aSupplier.get ();
      }
      finally
      {
        // Unregister
        WSSConfig.cleanUp ();
      }
    }
    finally
    {
      // Unlock
      s_aLock.unlock ();
    }
  }
}
