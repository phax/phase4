/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.smpclient.peppol.ISMPServiceMetadataProvider;

/**
 * This class contains the references values against which incoming values are
 * compared. These are the static default values that can be overridden in
 * {@link Phase4PeppolServletMessageProcessorSPI}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class Phase4PeppolServletConfiguration
{
  public static final boolean DEFAULT_RECEIVER_CHECK_ENABLED = true;
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolServletConfiguration.class);

  private static boolean s_bReceiverCheckEnabled = DEFAULT_RECEIVER_CHECK_ENABLED;
  private static ISMPServiceMetadataProvider s_aSMPClient;
  private static String s_sAS4EndpointURL;
  private static X509Certificate s_aAPCertificate;
  private static boolean s_bPerformSBDHValueChecks = PeppolSBDHDocumentReader.DEFAULT_PERFORM_VALUE_CHECKS;

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

  /**
   * Enable or disable the overall receiver checks. If the check is enabled,
   * than all values MUST be set.
   *
   * @param bReceiverCheckEnabled
   *        <code>true</code> to enable the checks, <code>false</code> to
   *        disable them.
   * @see #setAS4EndpointURL(String)
   * @see #setSMPClient(ISMPServiceMetadataProvider)
   * @see #setAPCertificate(X509Certificate)
   */
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
  public static ISMPServiceMetadataProvider getSMPClient ()
  {
    return s_aSMPClient;
  }

  /**
   * Set the SMP client to use for reverse checking if the participant is
   * registered or not.
   *
   * @param aSMPClient
   *        The SMP metadata provider to be used. May be <code>null</code>.
   */
  public static void setSMPClient (@Nullable final ISMPServiceMetadataProvider aSMPClient)
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

  /**
   * Set the expected endpoint URL to be used.
   *
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May be <code>null</code>.
   */
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

  /**
   * Set the Peppol AP certificate to be used for comparing against the SMP
   * lookup result.
   *
   * @param aAPCertificate
   *        The AP certificate to be used for compatibility. May be
   *        <code>null</code>.
   */
  public static void setAPCertificate (@Nullable final X509Certificate aAPCertificate)
  {
    s_aAPCertificate = aAPCertificate;
  }

  /**
   * Get the statically configured data as a
   * {@link Phase4PeppolReceiverCheckData} instance. Returns <code>null</code>
   * if the checks are disabled, or if at least one mandatory field is not set.
   *
   * @return The instance data or <code>null</code>.
   * @since 0.9.13
   */
  @Nullable
  public static Phase4PeppolReceiverCheckData getAsReceiverCheckData ()
  {
    if (!isReceiverCheckEnabled ())
      return null;

    final ISMPServiceMetadataProvider aSMPClient = getSMPClient ();
    final String sAS4EndpointURL = getAS4EndpointURL ();
    final X509Certificate aAPCertificate = getAPCertificate ();
    if (aSMPClient == null || StringHelper.hasNoText (sAS4EndpointURL) || aAPCertificate == null)
      return null;

    return new Phase4PeppolReceiverCheckData (aSMPClient, sAS4EndpointURL, aAPCertificate);
  }

  /**
   * @return <code>true</code> if SBDH value checks are enabled,
   *         <code>false</code> if they are disabled.
   * @since 0.12.1
   */
  public static boolean isPerformSBDHValueChecks ()
  {
    return s_bPerformSBDHValueChecks;
  }

  /**
   * Enable or disable the SBDH value checks. By default checks are enabled.
   *
   * @param bPerformSBDHValueChecks
   *        <code>true</code> to enable the checks, <code>false</code> to
   *        disable them
   * @since 0.12.1
   */
  public static void setPerformSBDHValueChecks (final boolean bPerformSBDHValueChecks)
  {
    final boolean bChange = bPerformSBDHValueChecks != s_bPerformSBDHValueChecks;
    s_bPerformSBDHValueChecks = bPerformSBDHValueChecks;
    if (bChange)
    {
      if (bPerformSBDHValueChecks)
        LOGGER.info ("phase4 Peppol SBDH value checks are now enabled");
      else
        LOGGER.warn ("phase4 Peppol SBDH value checks are now disabled");
    }
  }
}
