/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.soapfault;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;

/**
 * Exception that indicates that a SOAP Fault with a {@link EAS4FaultDisposition#PERMANENT}
 * disposition was received as the synchronous response. It is derived from {@link IOException} so
 * that it passes through the HTTP retry handling, where it explicitly stops all further retries.
 * <br>
 * Note: this exception deliberately does NOT extend {@link com.helger.phase4.util.Phase4Exception},
 * even though that would be the more natural base class. It is thrown from within an Apache
 * HttpClient {@link org.apache.hc.core5.http.io.HttpClientResponseHandler}, whose
 * <code>handleResponse</code> method is declared to only throw <code>HttpException</code> and
 * {@link IOException} - a checked <code>Phase4Exception</code> could not be thrown from there at
 * all. Additionally it must pass through <code>BasicHttpPoster.sendGenericMessageWithRetries</code>
 * and <code>AbstractAS4Client.sendMessageWithRetries</code>, which are both declared to only throw
 * {@link IOException} - extending their <code>throws</code> clauses would be a source-incompatible
 * change for all direct callers. Making this exception a <code>Phase4Exception</code> would
 * therefore require an additional internal {@link IOException} based carrier exception for the HTTP
 * layer, at the price of direct callers of the low-level sending methods no longer seeing this
 * exception type.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public class AS4SoapFaultException extends IOException
{
  private final transient AS4SoapFault m_aSoapFault;
  private String m_sSentMessageID;

  public AS4SoapFaultException (@NonNull final AS4SoapFault aSoapFault)
  {
    super ("Received a SOAP " +
           aSoapFault.getSoapVersion ().getVersion () +
           " Fault with code " +
           (aSoapFault.hasFaultCode () ? "'" + aSoapFault.getFaultCode () + "'" : "<none>") +
           " and reason " +
           (aSoapFault.getFaultReason () != null ? "'" + aSoapFault.getFaultReason () + "'" : "<none>"));
    m_aSoapFault = aSoapFault;
  }

  /**
   * @return The SOAP Fault that was received. Never <code>null</code>.
   */
  @NonNull
  public AS4SoapFault getSoapFault ()
  {
    return m_aSoapFault;
  }

  /**
   * @return The AS4 Message ID of the sent message the fault was received for. May be
   *         <code>null</code> if it was not determined.
   */
  @Nullable
  public String getSentMessageID ()
  {
    return m_sSentMessageID;
  }

  /**
   * @return <code>true</code> if the AS4 Message ID of the sent message the fault was received for
   *         is present, <code>false</code> otherwise.
   */
  public boolean hasSentMessageID ()
  {
    return StringHelper.isNotEmpty (m_sSentMessageID);
  }

  /**
   * Set the AS4 Message ID of the sent message the fault was received for.
   *
   * @param sSentMessageID
   *        The AS4 Message ID. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public AS4SoapFaultException setSentMessageID (@NonNull final String sSentMessageID)
  {
    ValueEnforcer.notNull (sSentMessageID, "SentMessageID");
    m_sSentMessageID = sSentMessageID;
    return this;
  }
}
