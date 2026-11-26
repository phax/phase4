/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.sender;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.phase4.CAS4;

/**
 * Captures the raw HTTP outcome of a multi-hop send, so that an empty HTTP 2xx response can be
 * interpreted as "accepted by the I-Cloud" rather than as a missing signal message (D10).
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class MultiHopSendOutcomeHolder
{
  private final Object m_aLock = new Object ();
  private int m_nHttpStatusCode = CAS4.HTTP_STATUS_UNDEFINED;
  private boolean m_bHasResponseContent = false;

  public MultiHopSendOutcomeHolder ()
  {}

  /**
   * @param nHttpStatusCode
   *        The received HTTP status code.
   * @param bHasResponseContent
   *        <code>true</code> if the HTTP response had a non-empty body.
   */
  public void setResponse (final int nHttpStatusCode, final boolean bHasResponseContent)
  {
    synchronized (m_aLock)
    {
      m_nHttpStatusCode = nHttpStatusCode;
      m_bHasResponseContent = bHasResponseContent;
    }
  }

  /**
   * @return The received HTTP status code or {@link CAS4#HTTP_STATUS_UNDEFINED} if none was
   *         received.
   */
  public int getHttpStatusCode ()
  {
    synchronized (m_aLock)
    {
      return m_nHttpStatusCode;
    }
  }

  /**
   * @return <code>true</code> if the HTTP response had a non-empty body.
   */
  public boolean hasResponseContent ()
  {
    synchronized (m_aLock)
    {
      return m_bHasResponseContent;
    }
  }

  /**
   * @return <code>true</code> if an HTTP 2xx with an empty body was received. That is how an edge
   *         intermediary acknowledges a message it will deliver later.
   */
  public boolean isEmpty2xxResponse ()
  {
    synchronized (m_aLock)
    {
      return m_nHttpStatusCode >= 200 && m_nHttpStatusCode <= 299 && !m_bHasResponseContent;
    }
  }

  @Override
  @NonNull
  public String toString ()
  {
    synchronized (m_aLock)
    {
      return "MultiHopSendOutcomeHolder[httpStatusCode=" +
             m_nHttpStatusCode +
             "; hasResponseContent=" +
             m_bHasResponseContent +
             "]";
    }
  }
}
