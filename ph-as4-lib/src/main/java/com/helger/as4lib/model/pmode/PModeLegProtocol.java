/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nullable;

/**
 * PMode leg protocol parameters.
 *
 * @author Philip Helger
 */
public class PModeLegProtocol
{
  public static final String SOAP_VERSION_11 = "1.1";
  public static final String SOAP_VERSION_12 = "1.2";

  /**
   * the value of this parameter represents the address (endpoint URL) of the
   * Receiver MSH (or Receiver Party) to which Messages under this P-Mode leg
   * are to be sent. Note that a URL generally determines the transport protocol
   * (for example, if the endpoint is an email address, then the transport
   * protocol must be SMTP; if the address scheme is "http", then the transport
   * protocol must be HTTP).
   */
  private String m_sAddress;

  /**
   * this parameter indicates the SOAP version to be used (<code>1.1</code> or
   * <code>1.2</code>). In some implementations, this parameter may be
   * constrained by the implementation, and not set by users.
   *
   * @see #PROTOCOL_SOAP_VERSION_11
   * @see #PROTOCOL_SOAP_VERSION_12
   */
  private String m_sSOAPVersion;

  public PModeLegProtocol ()
  {}

  @Nullable
  public String getAddress ()
  {
    return m_sAddress;
  }

  public void setAddress (@Nullable final String sAddress)
  {
    m_sAddress = sAddress;
  }

  @Nullable
  public String getSOAPVersion ()
  {
    return m_sSOAPVersion;
  }

  public void setSOAPVersion (@Nullable final String sSOAPVersion)
  {
    m_sSOAPVersion = sSOAPVersion;
  }
}
