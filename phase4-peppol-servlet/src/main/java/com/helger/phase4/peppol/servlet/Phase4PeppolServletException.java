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
package com.helger.phase4.peppol.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.util.Phase4Exception;

/**
 * Generic exception to be thrown from the phase4 Peppol servlet.
 *
 * @author Philip Helger
 */
public class Phase4PeppolServletException extends Phase4Exception
{
  /**
   * @param sMessage
   *        Error message
   */
  public Phase4PeppolServletException (@Nonnull final String sMessage)
  {
    super (sMessage);
  }

  /**
   * @param aCause
   *        Optional causing exception
   * @since 0.13.0
   */
  @Deprecated (forRemoval = true, since = "3.2.0")
  public Phase4PeppolServletException (@Nullable final Throwable aCause)
  {
    super (aCause);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public Phase4PeppolServletException (@Nonnull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
  }
}
