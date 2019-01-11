/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.http;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;

/**
 * Turn on/off AS4 HTTP debug logging
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS4HttpDebug
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4HttpDebug.class);
  private static final AtomicBoolean s_aEnabled = new AtomicBoolean (false);

  private AS4HttpDebug ()
  {}

  /**
   * Enable or disable
   *
   * @param bEnabled
   *        <code>true</code> to enabled, <code>false</code> to disable
   */
  public static void setEnabled (final boolean bEnabled)
  {
    s_aEnabled.set (bEnabled);
  }

  /**
   * @return <code>true</code> if enabled, <code>false</code> if not.
   */
  public static boolean isEnabled ()
  {
    return s_aEnabled.get ();
  }

  /**
   * Debug the provided string if {@link #isEnabled()}. Uses the logger to log
   * to the console
   *
   * @param aMsg
   *        The message supplier. May not be <code>null</code>. Invoked only if
   *        {@link #isEnabled()}
   */
  public static void debug (@Nonnull final Supplier <? super String> aMsg)
  {
    if (isEnabled ())
      LOGGER.info ("$$$ AS4 HTTP [" + PDTFactory.getCurrentLocalTime ().toString () + "] " + aMsg.get ());
  }
}
