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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.v3.ChangePhase4V3;
import com.helger.smpclient.peppol.ISMPServiceMetadataProvider;
import com.helger.smpclient.peppol.PeppolWildcardSelector;

/**
 * This class contains the "per-request" data of
 * {@link Phase4PeppolServletConfiguration}.
 *
 * @author Philip Helger
 * @since 0.9.13
 */
@NotThreadSafe
@ChangePhase4V3 ("Rename to Phase4PeppolReceiverConfiguration; remove setter")
public class Phase4PeppolReceiverCheckData
{
  private final boolean m_bReceiverCheckEnabled;
  private final ISMPServiceMetadataProvider m_aSMPClient;
  private final PeppolWildcardSelector.EMode m_eWildcardSelectionMode;
  private final String m_sAS4EndpointURL;
  private final X509Certificate m_aAPCertificate;
  private final boolean m_bPerformSBDHValueChecks;
  private final boolean m_bCheckSBDHForMandatoryCountryC1;
  private boolean m_bCheckSigningCertificateRevocation;

  /**
   * Constructor
   *
   * @param aSMPClient
   *        The SMP metadata provider to be used. May not be <code>null</code>.
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May neither be <code>null</code>
   *        nor empty.
   * @param aAPCertificate
   *        The AP certificate to be used for compatibility. May not be
   *        <code>null</code>.
   * @param eWildcardSelectionMode
   *        The wildcard selection mode to use for the SMP. May not be
   *        <code>null</code>. Added in 2.7.3.
   */
  @Deprecated (since = "2.8.1", forRemoval = true)
  public Phase4PeppolReceiverCheckData (@Nonnull final ISMPServiceMetadataProvider aSMPClient,
                                        @Nonnull @Nonempty final String sAS4EndpointURL,
                                        @Nonnull final X509Certificate aAPCertificate,
                                        @Nonnull final PeppolWildcardSelector.EMode eWildcardSelectionMode)
  {
    this (true,
          aSMPClient,
          eWildcardSelectionMode,
          sAS4EndpointURL,
          aAPCertificate,
          Phase4PeppolServletConfiguration.isPerformSBDHValueChecks (),
          Phase4PeppolServletConfiguration.isCheckSBDHForMandatoryCountryC1 (),
          Phase4PeppolServletConfiguration.isCheckSigningCertificateRevocation ());
  }

  /**
   * Constructor
   *
   * @param bReceiverCheckEnabled
   *        <code>true</code> if the receiver checks are enabled,
   *        <code>false</code> otherwise
   * @param aSMPClient
   *        The SMP metadata provider to be used. May not be <code>null</code>
   *        if receiver checks are enabled.
   * @param eWildcardSelectionMode
   *        The wildcard selection mode to use for the SMP. May not be
   *        <code>null</code>
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May neither be <code>null</code>
   *        nor empty if receiver checks are enabled.
   * @param aAPCertificate
   *        The AP certificate to be used for compatibility. May not be
   *        <code>null</code> if receiver checks are enabled.
   * @param bPerformSBDHValueChecks
   *        <code>true</code> if SBDH value checks should be performed.
   * @param bCheckSBDHForMandatoryCountryC1
   *        <code>true</code> if SBDH value checks should be performed for
   *        mandatory C1 country code.
   * @param bCheckSigningCertificateRevocation
   *        <code>true</code> if signing certificate revocation checks should be
   *        performed.
   * @since 2.8.1
   */
  public Phase4PeppolReceiverCheckData (final boolean bReceiverCheckEnabled,
                                        @Nullable final ISMPServiceMetadataProvider aSMPClient,
                                        @Nonnull final PeppolWildcardSelector.EMode eWildcardSelectionMode,
                                        @Nullable final String sAS4EndpointURL,
                                        @Nullable final X509Certificate aAPCertificate,
                                        final boolean bPerformSBDHValueChecks,
                                        final boolean bCheckSBDHForMandatoryCountryC1,
                                        final boolean bCheckSigningCertificateRevocation)
  {
    if (bReceiverCheckEnabled)
      ValueEnforcer.notNull (aSMPClient, "SMPClient");
    ValueEnforcer.notNull (eWildcardSelectionMode, "WildcardSelectionMode");
    if (bReceiverCheckEnabled)
      ValueEnforcer.notEmpty (sAS4EndpointURL, "AS4EndpointURL");
    if (bReceiverCheckEnabled)
      ValueEnforcer.notNull (aAPCertificate, "APCertificate");
    m_bReceiverCheckEnabled = bReceiverCheckEnabled;
    m_aSMPClient = aSMPClient;
    m_eWildcardSelectionMode = eWildcardSelectionMode;
    m_sAS4EndpointURL = sAS4EndpointURL;
    m_aAPCertificate = aAPCertificate;
    m_bPerformSBDHValueChecks = bPerformSBDHValueChecks;
    m_bCheckSBDHForMandatoryCountryC1 = bCheckSBDHForMandatoryCountryC1;
    m_bCheckSigningCertificateRevocation = bCheckSigningCertificateRevocation;
  }

  public boolean isReceiverCheckEnabled ()
  {
    return m_bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is
   *         customizable because it depends either on the SML or a direct URL
   *         to the SMP may be provided. Never <code>null</code> if receiver
   *         checks are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @Nullable
  public ISMPServiceMetadataProvider getSMPClient ()
  {
    return m_aSMPClient;
  }

  /**
   * @return The transport profile to be used. Never <code>null</code>.
   * @since 2.7.3
   */
  @Nonnull
  public final PeppolWildcardSelector.EMode getWildcardSelectionMode ()
  {
    return m_eWildcardSelectionMode;
  }

  /**
   * @return The URL of this AP to compare to against the SMP lookup result upon
   *         retrieval. Neither <code>null</code> nor empty if receiver checks
   *         are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @Nullable
  public String getAS4EndpointURL ()
  {
    return m_sAS4EndpointURL;
  }

  /**
   * @return The certificate of this AP to compare to against the SMP lookup
   *         result upon retrieval. Never <code>null</code> if receiver checks
   *         are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @Nonnull
  public X509Certificate getAPCertificate ()
  {
    return m_aAPCertificate;
  }

  public boolean isPerformSBDHValueChecks ()
  {
    return m_bPerformSBDHValueChecks;
  }

  public boolean isCheckSBDHForMandatoryCountryC1 ()
  {
    return m_bCheckSBDHForMandatoryCountryC1;
  }

  public boolean isCheckSigningCertificateRevocation ()
  {
    return m_bCheckSigningCertificateRevocation;
  }

  // only required temporarily
  @Deprecated (since = "2.8.1", forRemoval = true)
  public void internalSetCheckSigningCertificateRevocation (final boolean b)
  {
    m_bCheckSigningCertificateRevocation = b;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ReceiverCheckEnabled", m_bReceiverCheckEnabled)
                                       .append ("SMPClient", m_aSMPClient)
                                       .append ("WildcardSelectionMode", m_eWildcardSelectionMode)
                                       .append ("AS4EndpointURL", m_sAS4EndpointURL)
                                       .append ("APCertificate", m_aAPCertificate)
                                       .append ("PerformSBDHValueChecks", m_bPerformSBDHValueChecks)
                                       .append ("CheckSBDHForMandatoryCountryC1", m_bCheckSBDHForMandatoryCountryC1)
                                       .append ("CheckSigningCertificateRevocation",
                                                m_bCheckSigningCertificateRevocation)
                                       .getToString ();
  }
}
