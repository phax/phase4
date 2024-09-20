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
package com.helger.phase4.mgr;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import javax.annotation.Nonnull;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDateTime;

/**
 * Interface for providing time stamps.<br>
 * The precision of all the methods in this class is milliseconds, so that it
 * stays compatible to XML serialization. Since version 1.1.0 the return types
 * of the methods changed from <code>Local(Date|Time|DateTime)</code> to
 * <code>Offset(Date|Time|DateTime)</code>
 *
 * @author Philip Helger
 * @since 0.10.0
 */
public interface IAS4TimestampManager
{
  /**
   * @return The current date in time in the current time zone. Never
   *         <code>null</code>.
   */
  @Nonnull
  OffsetDateTime getCurrentDateTime ();

  /**
   * @return The current date in time in the current time zone for XML
   *         processing. Never <code>null</code>.
   */
  @Nonnull
  default XMLOffsetDateTime getCurrentXMLDateTime ()
  {
    return XMLOffsetDateTime.of (getCurrentDateTime ());
  }

  /**
   * @return The current date in the current time zone. Never <code>null</code>.
   * @since 0.10.4
   */
  @Nonnull
  default LocalDate getCurrentDate ()
  {
    return getCurrentDateTime ().toLocalDate ();
  }

  /**
   * @return The current time in the current time zone. Never <code>null</code>.
   * @since 0.10.4
   */
  @Nonnull
  default OffsetTime getCurrentTime ()
  {
    return getCurrentDateTime ().toOffsetTime ();
  }

  @Nonnull
  static IAS4TimestampManager createDefaultInstance ()
  {
    // Limited to milliseconds only
    return () -> PDTFactory.getWithMillisOnly (PDTFactory.getCurrentOffsetDateTime ());
  }
}
