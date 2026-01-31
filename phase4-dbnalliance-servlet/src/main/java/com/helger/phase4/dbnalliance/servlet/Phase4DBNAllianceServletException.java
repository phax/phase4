/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.servlet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phase4.incoming.soap.CAS4Soap;
import com.helger.phase4.util.Phase4IncomingException;

/**
 * Generic exception to be thrown from the phase4 DBNAlliance servlet.
 *
 * @author Philip Helger
 */
public class Phase4DBNAllianceServletException extends Phase4IncomingException
{
  /**
   * @param sMessage
   *        Error message
   */
  public Phase4DBNAllianceServletException (@NonNull final String sMessage)
  {
    super (sMessage);
    setHttpStatusCode (CAS4Soap.HTTP_STATUS_CODE_RECEIVER);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public Phase4DBNAllianceServletException (@NonNull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
    setHttpStatusCode (CAS4Soap.HTTP_STATUS_CODE_RECEIVER);
  }
}
