/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.mgr;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

  /**
   * @return The current date in the current time zone.
   * @since 0.10.4
   */
  @Nonnull
  default LocalDate getCurrentDate ()
  {
    return getCurrentDateTime ().toLocalDate ();
  }

  /**
   * @return The current time in the current time zone.
   * @since 0.10.4
   */
  @Nonnull
  default LocalTime getCurrentTime ()
  {
    return getCurrentDateTime ().toLocalTime ();
  }

  @Nonnull
  static IAS4TimestampManager createDefaultInstance ()
  {
    return () -> PDTFactory.getCurrentLocalDateTime ();
  }
}
