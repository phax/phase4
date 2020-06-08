package com.helger.phase4.mgr;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.datetime.PDTFactory;

/**
 * Interface for providing time stamps.
 *
 * @author Philip Helger
 * @since 0.10.0
 */
public interface IAS4TimestampManager
{
  /**
   * @return The current date in time in the current time zone.
   */
  @Nonnull
  LocalDateTime getCurrentDateTime ();

  @Nonnull
  static IAS4TimestampManager createDefaultInstance ()
  {
    return () -> PDTFactory.getCurrentLocalDateTime ();
  }
}
