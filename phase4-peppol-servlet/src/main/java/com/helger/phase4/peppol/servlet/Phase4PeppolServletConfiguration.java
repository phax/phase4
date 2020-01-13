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
package com.helger.phase4.peppol.servlet;

import java.security.cert.X509Certificate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.peppol.smpclient.SMPClientReadOnly;

/**
 * This class contains the references values against which incoming values are
 * compared.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class Phase4PeppolServletConfiguration
{
  public static final boolean DEFAULT_RECEIVER_CHECK_ENABLED = true;

  private static boolean s_bReceiverCheckEnabled = DEFAULT_RECEIVER_CHECK_ENABLED;
  private static SMPClientReadOnly s_aSMPClient;
  private static String s_sAS4EndpointURL;
  private static X509Certificate s_aAPCertificate;

  private Phase4PeppolServletConfiguration ()
  {}

  /**
   * @return <code>true</code> if the checks for endpoint URL and endpoint
   *         certificate are enabled, <code>false</code> otherwise. By default
   *         the checks are enabled.
   */
  public static boolean isReceiverCheckEnabled ()
  {
    return s_bReceiverCheckEnabled;
  }

  public static void setReceiverCheckEnabled (final boolean bReceiverCheckEnabled)
  {
    s_bReceiverCheckEnabled = bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is
   *         customizable because it depends either on the SML or a direct URL
   *         to the SMP may be provided. May be <code>null</code> if not yet
   *         configured.
   */
  @Nullable
  public static SMPClientReadOnly getSMPClient ()
  {
    return s_aSMPClient;
  }

  public static void setSMPClient (@Nullable final SMPClientReadOnly aSMPClient)
  {
    s_aSMPClient = aSMPClient;
  }

  /**
   * @return The URL of this AP to compare to against the SMP lookup result upon
   *         retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static String getAS4EndpointURL ()
  {
    return s_sAS4EndpointURL;
  }

  public static void setAS4EndpointURL (@Nullable final String sAS4EndpointURL)
  {
    s_sAS4EndpointURL = sAS4EndpointURL;
  }

  /**
   * @return The certificate of this AP to compare to against the SMP lookup
   *         result upon retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static X509Certificate getAPCertificate ()
  {
    return s_aAPCertificate;
  }

  public static void setAPCertificate (@Nullable final X509Certificate aAPCertificate)
  {
    s_aAPCertificate = aAPCertificate;
  }
}
