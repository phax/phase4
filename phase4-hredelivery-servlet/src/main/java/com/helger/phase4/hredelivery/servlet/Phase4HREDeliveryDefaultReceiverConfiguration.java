/*
 * Copyright (C) 2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.hredelivery.servlet;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.hredelivery.commons.sbdh.HREDeliverySBDHDataReader;
import com.helger.hredelivery.commons.security.HREDeliveryTrustedCA;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.CAS4;
import com.helger.phase4.hredelivery.servlet.Phase4HREDeliveryReceiverConfiguration.Phase4HREDeliveryReceiverConfigurationBuilder;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;

/**
 * This class contains the references values against which incoming values are compared. These are
 * the static default values that can be overridden in
 * {@link Phase4HREDeliveryServletMessageProcessorSPI}. Please note that this class is not thread
 * safe, as the default values are not meant to be modified during runtime.<br>
 * See {@link Phase4HREDeliveryReceiverConfiguration} for the "per-request" version of this class.
 *
 * @author Philip Helger
 * @since 4.0.2
 */
@NotThreadSafe
public final class Phase4HREDeliveryDefaultReceiverConfiguration
{
  public static final IIdentifierFactory DEFAULT_SBDH_IDENTIFIER_FACTORY = SimpleIdentifierFactory.INSTANCE;
  public static final boolean DEFAULT_RECEIVER_CHECK_ENABLED = true;
  public static final boolean DEFAULT_CHECK_SIGNING_CERTIFICATE_REVOCATION = true;
  public static final TrustedCAChecker DEFAULT_HREDELIVERY_AP_CA_CHECKER = HREDeliveryTrustedCA.hrEdeliveryFinaAll ();

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4HREDeliveryDefaultReceiverConfiguration.class);

  private static boolean s_bReceiverCheckEnabled = DEFAULT_RECEIVER_CHECK_ENABLED;
  private static ISMPExtendedServiceMetadataProvider s_aSMPClient;
  private static String s_sAS4EndpointURL;
  private static X509Certificate s_aAPCertificate;
  private static IIdentifierFactory s_aSBDHIdentifierFactory = DEFAULT_SBDH_IDENTIFIER_FACTORY;
  private static boolean s_bPerformSBDHValueChecks = HREDeliverySBDHDataReader.DEFAULT_PERFORM_VALUE_CHECKS;
  private static boolean s_bCheckSigningCertificateRevocation = DEFAULT_CHECK_SIGNING_CERTIFICATE_REVOCATION;
  private static TrustedCAChecker s_aAPCAChecker = DEFAULT_HREDELIVERY_AP_CA_CHECKER;

  private Phase4HREDeliveryDefaultReceiverConfiguration ()
  {}

  /**
   * @return <code>true</code> if the checks for endpoint URL and endpoint certificate are enabled,
   *         <code>false</code> otherwise. By default the checks are enabled.
   */
  public static boolean isReceiverCheckEnabled ()
  {
    return s_bReceiverCheckEnabled;
  }

  /**
   * Enable or disable the overall receiver checks (check if a receiver is actually registered in
   * the configured SMP with the configured AP URL). If the check is enabled, than all values MUST
   * be set.
   *
   * @param bReceiverCheckEnabled
   *        <code>true</code> to enable the checks, <code>false</code> to disable them.
   * @see #setSMPClient(ISMPExtendedServiceMetadataProvider)
   * @see #setAS4EndpointURL(String)
   * @see #setAPCertificate(X509Certificate)
   */
  public static void setReceiverCheckEnabled (final boolean bReceiverCheckEnabled)
  {
    s_bReceiverCheckEnabled = bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is customizable
   *         because it depends either on the SML or a direct URL to the SMP may be provided. May be
   *         <code>null</code> if not yet configured.
   */
  @Nullable
  public static ISMPExtendedServiceMetadataProvider getSMPClient ()
  {
    return s_aSMPClient;
  }

  /**
   * Set the SMP client to use for reverse checking if the participant is registered or not.
   *
   * @param aSMPClient
   *        The SMP metadata provider to be used. May be <code>null</code>.
   */
  public static void setSMPClient (@Nullable final ISMPExtendedServiceMetadataProvider aSMPClient)
  {
    s_aSMPClient = aSMPClient;
  }

  /**
   * @return The URL of this (my) AP to compare to against the SMP lookup result upon retrieval. Is
   *         <code>null</code> by default.
   */
  @Nullable
  public static String getAS4EndpointURL ()
  {
    return s_sAS4EndpointURL;
  }

  /**
   * Set the expected endpoint URL to be used for comparing against the SMP lookup result.
   *
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May be <code>null</code>.
   */
  public static void setAS4EndpointURL (@Nullable final String sAS4EndpointURL)
  {
    s_sAS4EndpointURL = sAS4EndpointURL;
  }

  /**
   * @return The certificate of this (my) AP to compare to against the SMP lookup result upon
   *         retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static X509Certificate getAPCertificate ()
  {
    return s_aAPCertificate;
  }

  /**
   * Set the HR eDelivery AP certificate to be used for comparing against the SMP lookup result.
   *
   * @param aAPCertificate
   *        The AP certificate to be used for compatibility. May be <code>null</code>.
   */
  public static void setAPCertificate (@Nullable final X509Certificate aAPCertificate)
  {
    s_aAPCertificate = aAPCertificate;
  }

  /**
   * @return The default identifier factory used to parse SBDH data. Never <code>null</code>.
   */
  @NonNull
  public static IIdentifierFactory getSBDHIdentifierFactory ()
  {
    return s_aSBDHIdentifierFactory;
  }

  /**
   * Set the default identifier factory used to parse SBDH data.
   *
   * @param a
   *        The identifier factory to use. May not be <code>null</code>.
   */
  public static void setSBDHIdentifierFactory (@NonNull final IIdentifierFactory a)
  {
    ValueEnforcer.notNull (a, "SBDHIdentifierFactory");
    s_aSBDHIdentifierFactory = a;
  }

  /**
   * @return <code>true</code> if SBDH value checks are enabled, <code>false</code> if they are
   *         disabled.
   */
  public static boolean isPerformSBDHValueChecks ()
  {
    return s_bPerformSBDHValueChecks;
  }

  /**
   * Enable or disable the SBDH value checks. By default checks are enabled.
   *
   * @param b
   *        <code>true</code> to enable the checks, <code>false</code> to disable them
   */
  public static void setPerformSBDHValueChecks (final boolean b)
  {
    final boolean bChange = b != s_bPerformSBDHValueChecks;
    s_bPerformSBDHValueChecks = b;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME + " HR eDelivery SBDH value checks are now " + (b ? "enabled" : "disabled"));
    }
  }

  /**
   * @return <code>true</code> if the signing certificate should be checked for revocation,
   *         <code>false</code> if not.
   */
  public static boolean isCheckSigningCertificateRevocation ()
  {
    return s_bCheckSigningCertificateRevocation;
  }

  /**
   * Set whether the signing certificate should be checked for revocation or not.
   *
   * @param b
   *        <code>true</code> to check, <code>false</code> to disable the check (not recommended).
   */
  public static void setCheckSigningCertificateRevocation (final boolean b)
  {
    final boolean bChange = b != s_bCheckSigningCertificateRevocation;
    s_bCheckSigningCertificateRevocation = b;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME +
                   " HR eDelivery signing certificate revocation check is now " +
                   (b ? "enabled" : "disabled"));
    }
  }

  /**
   * @return The HR eDelivery AP CA checker to be used. May be <code>null</code>.
   */
  @Nullable
  public static TrustedCAChecker getAPCAChecker ()
  {
    return s_aAPCAChecker;
  }

  /**
   * Set the HR eDelivery CA checker to be used.
   *
   * @param a
   *        The HR eDelivery CA checker to be used. May be <code>null</code>.
   */
  public static void setAPCAChecker (@Nullable final TrustedCAChecker a)
  {
    final boolean bChange = a != s_aAPCAChecker;
    s_aAPCAChecker = a;
    if (bChange)
    {
      LOGGER.info (CAS4.LIB_NAME + " HR eDelivery AP CA Checker is set to " + a);
    }
  }

  /**
   * Get the statically configured data as a {@link Phase4HREDeliveryReceiverConfigurationBuilder}
   * instance. This allows for modification before building the final object.
   *
   * @return Completely filled builder. Never <code>null</code>.
   */
  @NonNull
  public static Phase4HREDeliveryReceiverConfigurationBuilder getAsReceiverCheckDataBuilder ()
  {
    final ISMPExtendedServiceMetadataProvider aSMPClient = getSMPClient ();
    final String sAS4EndpointURL = getAS4EndpointURL ();
    final X509Certificate aAPCertificate = getAPCertificate ();

    final boolean bReceiverCheckEnabled;
    if (aSMPClient == null || StringHelper.isEmpty (sAS4EndpointURL) || aAPCertificate == null)
      bReceiverCheckEnabled = false;
    else
      bReceiverCheckEnabled = isReceiverCheckEnabled ();

    return Phase4HREDeliveryReceiverConfiguration.builder ()
                                                 .receiverCheckEnabled (bReceiverCheckEnabled)
                                                 .serviceMetadataProvider (aSMPClient)
                                                 .as4EndpointUrl (sAS4EndpointURL)
                                                 .apCertificate (aAPCertificate)
                                                 .sbdhIdentifierFactory (getSBDHIdentifierFactory ())
                                                 .performSBDHValueChecks (isPerformSBDHValueChecks ())
                                                 .checkSigningCertificateRevocation (isCheckSigningCertificateRevocation ())
                                                 .apCAChecker (getAPCAChecker ());
  }

  /**
   * Get the statically configured data as a {@link Phase4HREDeliveryReceiverConfiguration}
   * instance.
   *
   * @return The instance data and never <code>null</code>.
   */
  @NonNull
  public static Phase4HREDeliveryReceiverConfiguration getAsReceiverCheckData ()
  {
    return getAsReceiverCheckDataBuilder ().build ();
  }
}
