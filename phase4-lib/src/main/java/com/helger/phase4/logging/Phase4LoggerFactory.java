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
package com.helger.phase4.logging;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.PresentForCodeCoverage;
import com.helger.base.string.StringHelper;

/**
 * Specific logger factory for the phase4 library that allows an easy customization of log messages.
 *
 * @author Philip Helger
 * @since 3.1.0-beta3
 */
public final class Phase4LoggerFactory
{
  @PresentForCodeCoverage
  private static final Phase4LoggerFactory INSTANCE = new Phase4LoggerFactory ();

  @Deprecated (forRemoval = false)
  private Phase4LoggerFactory ()
  {}

  private static final Function <String, String> MSG_CUSTOMIZER = sMsg -> {
    final String sPrefix = Phase4LogCustomizer.getThreadLocalLogPrefix ();
    final String sSuffix = Phase4LogCustomizer.getThreadLocalLogSuffix ();

    final boolean bHasPrefix = StringHelper.isNotEmpty (sPrefix);
    final boolean bHasSuffix = StringHelper.isNotEmpty (sSuffix);

    if (bHasPrefix || bHasSuffix)
    {
      // At least one of prefix or suffix is present
      final StringBuilder aSB = new StringBuilder ();
      if (bHasPrefix)
        aSB.append (sPrefix);
      if (StringHelper.isNotEmpty (sMsg))
        aSB.append (sMsg);
      if (bHasSuffix)
        aSB.append (sSuffix);
      return aSB.toString ();
    }

    // No prefix or suffix present
    return sMsg;
  };

  /**
   * Get a new SLF4J logger using the provided class.
   *
   * @param aClass
   *        The class to use. May not be <code>null</code>.
   * @return The wrapped {@link Phase4DelegatedLogger}. Never <code>null</code>.
   */
  @NonNull
  public static Phase4DelegatedLogger getLogger (@NonNull final Class <?> aClass)
  {
    // This is the only place, where the original SLF4J Logger Factory is invoked
    final Logger aLogger = LoggerFactory.getLogger (aClass);
    return new Phase4DelegatedLogger (aLogger, MSG_CUSTOMIZER);
  }

  /**
   * Get a new SLF4J logger using the provided logger name.
   *
   * @param sLoggerName
   *        The logger name to use. May neither be <code>null</code> nor empty.
   * @return The wrapped {@link Phase4DelegatedLogger}. Never <code>null</code>.
   */
  @NonNull
  public static Phase4DelegatedLogger getLogger (@NonNull @Nonempty final String sLoggerName)
  {
    // This is the only place, where the original SLF4J Logger Factory is invoked
    final Logger aLogger = LoggerFactory.getLogger (sLoggerName);
    return new Phase4DelegatedLogger (aLogger, MSG_CUSTOMIZER);
  }
}
