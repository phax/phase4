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
package com.helger.phase4.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;

/**
 * Special phase4 incoming exception for inbound errors. It adds an HTTP response code to the return
 * values.
 *
 * @author Philip Helger
 * @since 4.1.2
 */
public class Phase4IncomingException extends Phase4Exception
{
  public static final int DEFAULT_HTTP_STATUS_CODE = -1;

  private int m_nHttpStatusCode = DEFAULT_HTTP_STATUS_CODE;

  /**
   * @param sMessage
   *        Error message
   */
  public Phase4IncomingException (@NonNull final String sMessage)
  {
    super (sMessage);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public Phase4IncomingException (@NonNull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
  }

  /**
   * @return The HTTP status code to be returned. All values &le; 0 means: undefined.
   */
  @CheckForSigned
  public final int getHttpStatusCode ()
  {
    return m_nHttpStatusCode;
  }

  /**
   * @return <code>true</code> if a defined HTTP status code is present, <code>false</code>
   *         otherwise.
   */
  public final boolean hasHttpStatusCode ()
  {
    return m_nHttpStatusCode > 0;
  }

  /**
   * Set the HTTP Status code to be used.
   *
   * @param n
   *        The HTTP status code to be used. Any value &le; 0 means to use the default.
   * @return this for chaining
   */
  @NonNull
  public final Phase4IncomingException setHttpStatusCode (final int n)
  {
    m_nHttpStatusCode = n;
    return this;
  }
}
