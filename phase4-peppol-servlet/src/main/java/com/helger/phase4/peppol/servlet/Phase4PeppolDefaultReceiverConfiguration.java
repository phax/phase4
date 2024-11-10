/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.phase4.CAS4;
import com.helger.phase4.peppol.servlet.Phase4PeppolReceiverConfiguration.Phase4PeppolReceiverConfigurationBuilder;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.PeppolWildcardSelector;
import com.helger.smpclient.peppol.PeppolWildcardSelector.EMode;
import com.helger.smpclient.peppol.Pfuoi420;

/**
 * This class contains the references values against which incoming values are
 * compared. These are the static default values that can be overridden in
 * {@link Phase4PeppolServletMessageProcessorSPI}. Please note that this class
 * is not thread safe, as the default values are not meant to be modified during
 * runtime.<br>
 * See {@link Phase4PeppolReceiverConfiguration} for the "per-request" version
 * of this class.<br/>
 * Old name before v3: <code>Phase4PeppolServletConfiguration</code>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class Phase4PeppolDefaultReceiverConfiguration
{
  public static final boolean DEFAULT_RECEIVER_CHECK_ENABLED = true;
  @Pfuoi420
  public static final EMode DEFAULT_WILDCARD_SELECTION_MODE = EMode.WILDCARD_ONLY;
  public static final boolean DEFAULT_CHECK_SIGNING_CERTIFICATE_REVOCATION = true;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4PeppolDefaultReceiverConfiguration.class);

  private static boolean s_bReceiverCheckEnabled = DEFAULT_RECEIVER_CHECK_ENABLED;
  private static ISMPExtendedServiceMetadataProvider s_aSMPClient;
  @Pfuoi420
  private static PeppolWildcardSelector.EMode s_eWildcardSelectionMode = DEFAULT_WILDCARD_SELECTION_MODE;
  private static String s_sAS4EndpointURL;
  private static X509Certificate s_aAPCertificate;
  private static boolean s_bPerformSBDHValueChecks = PeppolSBDHDocumentReader.DEFAULT_PERFORM_VALUE_CHECKS;
  private static boolean s_bCheckSBDHForMandatoryCountryC1 = PeppolSBDHDocumentReader.DEFAULT_CHECK_FOR_COUNTRY_C1;
  private static boolean s_bCheckSigningCertificateRevocation = DEFAULT_CHECK_SIGNING_CERTIFICATE_REVOCATION;

  private Phase4PeppolDefaultReceiverConfiguration ()
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
   * @see #setSMPClient(ISMPExtendedServiceMetadataProvider)
   * @see #setWildcardSelectionMode(EMode)
   * @see #setAS4EndpointURL(String)
   * @see #setAPCertificate(X509Certificate)
   */
  @SuppressWarnings ("javadoc")
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
  public static ISMPExtendedServiceMetadataProvider getSMPClient ()
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
  public static void setSMPClient (@Nullable final ISMPExtendedServiceMetadataProvider aSMPClient)
  {
    s_aSMPClient = aSMPClient;
  }

  /**
   * @return The Peppol SMP wildcard selection to be used for document type
   *         resolution, if a wildcard document type identifier is used. Never
   *         <code>null</code>. Defaults to
   *         {@link #DEFAULT_WILDCARD_SELECTION_MODE}.
   * @since 2.7.3
   */
  @Nonnull
  @Pfuoi420
  public static PeppolWildcardSelector.EMode getWildcardSelectionMode ()
  {
    return s_eWildcardSelectionMode;
  }

  /**
   * Change the Peppol SMP wildcard selection to be used for document type
   * resolution, if a wildcard document type identifier is used.
   *
   * @param eWildcardSelectionMode
   *        The wildcard selection mode to be used. May not be
   *        <code>null</code>.
   * @since 2.7.3
   */
  @Pfuoi420
  public static void setWildcardSelectionMode (@Nonnull final PeppolWildcardSelector.EMode eWildcardSelectionMode)
  {
    ValueEnforcer.notNull (eWildcardSelectionMode, "WildcardSlectionMode");
    s_eWildcardSelectionMode = eWildcardSelectionMode;
  }

  /**
   * @return The URL of this (my) AP to compare to against the SMP lookup result
   *         upon retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static String getAS4EndpointURL ()
  {
    return s_sAS4EndpointURL;
  }

  /**
   * Set the expected endpoint URL to be used for comparing against the SMP
   * lookup result.
   *
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May be <code>null</code>.
   */
  public static void setAS4EndpointURL (@Nullable final String sAS4EndpointURL)
  {
    s_sAS4EndpointURL = sAS4EndpointURL;
  }

  /**
   * @return The certificate of this (my) AP to compare to against the SMP
   *         lookup result upon retrieval. Is <code>null</code> by default.
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
   * @param b
   *        <code>true</code> to enable the checks, <code>false</code> to
   *        disable them
   * @since 0.12.1
   */
  public static void setPerformSBDHValueChecks (final boolean b)
  {
    final boolean bChange = b != s_bPerformSBDHValueChecks;
    s_bPerformSBDHValueChecks = b;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME + " Peppol SBDH value checks are now " + (b ? "enabled" : "disabled"));
    }
  }

  /**
   * @return <code>true</code> if the Country C1 element in the SBDH of received
   *         messages is mandatory, and if such messages should be rejected, if
   *         that field is missing. By default it is enabled.
   * @since 2.7.1
   */
  public static boolean isCheckSBDHForMandatoryCountryC1 ()
  {
    return s_bCheckSBDHForMandatoryCountryC1;
  }

  /**
   * Set whether the check for the mandatory Country C1 element in SBDH of
   * received message is mandatory or not. By default it is enabled.
   *
   * @param b
   *        <code>true</code> to check, <code>false</code> to disable the check.
   * @since 2.7.1
   */
  public static void setCheckSBDHForMandatoryCountryC1 (final boolean b)
  {
    final boolean bChange = b != s_bCheckSBDHForMandatoryCountryC1;
    s_bCheckSBDHForMandatoryCountryC1 = b;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME +
                   " Peppol SBDH checking for mandatory C1 Country Code is now " +
                   (b ? "enabled" : "disabled"));
    }
  }

  /**
   * @return <code>true</code> if the signing certificate should be checked for
   *         revocation, <code>false</code> if not.
   * @since 2.7.1
   */
  public static boolean isCheckSigningCertificateRevocation ()
  {
    return s_bCheckSigningCertificateRevocation;
  }

  /**
   * Set whether the signing certificate should be checked for revocation or
   * not.
   *
   * @param b
   *        <code>true</code> to check, <code>false</code> to disable the check
   *        (not recommended).
   * @since 2.7.1
   */
  public static void setCheckSigningCertificateRevocation (final boolean b)
  {
    final boolean bChange = b != s_bCheckSigningCertificateRevocation;
    s_bCheckSigningCertificateRevocation = b;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME +
                   " Peppol signing certificate revocation check is now " +
                   (b ? "enabled" : "disabled"));
    }
  }

  /**
   * Get the statically configured data as a
   * {@link Phase4PeppolReceiverConfigurationBuilder} instance. This allows for
   * modification before building the final object.
   *
   * @return Completely filled builder. Never <code>null</code>.
   * @since 3.0.0 Beta7
   */
  @Nonnull
  public static Phase4PeppolReceiverConfigurationBuilder getAsReceiverCheckDataBuilder ()
  {
    final ISMPExtendedServiceMetadataProvider aSMPClient = getSMPClient ();
    final String sAS4EndpointURL = getAS4EndpointURL ();
    final X509Certificate aAPCertificate = getAPCertificate ();

    final boolean bReceiverCheckEnabled;
    if (aSMPClient == null || StringHelper.hasNoText (sAS4EndpointURL) || aAPCertificate == null)
      bReceiverCheckEnabled = false;
    else
      bReceiverCheckEnabled = isReceiverCheckEnabled ();

    return Phase4PeppolReceiverConfiguration.builder ()
                                            .receiverCheckEnabled (bReceiverCheckEnabled)
                                            .serviceMetadataProvider (aSMPClient)
                                            .wildcardSelectionMode (getWildcardSelectionMode ())
                                            .as4EndpointUrl (sAS4EndpointURL)
                                            .apCertificate (aAPCertificate)
                                            .performSBDHValueChecks (isPerformSBDHValueChecks ())
                                            .checkSBDHForMandatoryCountryC1 (isCheckSBDHForMandatoryCountryC1 ())
                                            .checkSigningCertificateRevocation (isCheckSigningCertificateRevocation ());
  }

  /**
   * Get the statically configured data as a
   * {@link Phase4PeppolReceiverConfiguration} instance.
   *
   * @return The instance data and never <code>null</code>.
   * @since 0.9.13
   */
  @Nonnull
  public static Phase4PeppolReceiverConfiguration getAsReceiverCheckData ()
  {
    return getAsReceiverCheckDataBuilder ().build ();
  }
}
