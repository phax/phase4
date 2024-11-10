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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.builder.IBuilder;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.PeppolWildcardSelector;
import com.helger.smpclient.peppol.Pfuoi420;

/**
 * This class contains the "per-request" data of
 * {@link Phase4PeppolDefaultReceiverConfiguration}.<br/>
 * Old name before v3: <code>Phase4PeppolReceiverCheckData</code>
 *
 * @author Philip Helger
 * @since 0.9.13
 */
@Immutable
public final class Phase4PeppolReceiverConfiguration
{
  private final boolean m_bReceiverCheckEnabled;
  private final ISMPExtendedServiceMetadataProvider m_aSMPClient;
  @Pfuoi420
  private final PeppolWildcardSelector.EMode m_eWildcardSelectionMode;
  private final String m_sAS4EndpointURL;
  private final X509Certificate m_aAPCertificate;
  private final boolean m_bPerformSBDHValueChecks;
  private final boolean m_bCheckSBDHForMandatoryCountryC1;
  private final boolean m_bCheckSigningCertificateRevocation;

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
  public Phase4PeppolReceiverConfiguration (final boolean bReceiverCheckEnabled,
                                            @Nullable final ISMPExtendedServiceMetadataProvider aSMPClient,
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
  public ISMPExtendedServiceMetadataProvider getSMPClient ()
  {
    return m_aSMPClient;
  }

  /**
   * @return The transport profile to be used. Never <code>null</code>.
   * @since 2.7.3
   */
  @Nonnull
  @Pfuoi420
  public PeppolWildcardSelector.EMode getWildcardSelectionMode ()
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

  /**
   * @return An empty builder instance. Never <code>null</code>.
   */
  @Nonnull
  public static Phase4PeppolReceiverConfigurationBuilder builder ()
  {
    return new Phase4PeppolReceiverConfigurationBuilder ();
  }

  /**
   * Create a builder instance with the data of the provided object already
   * filled in.
   *
   * @param aSrc
   *        The source {@link Phase4PeppolReceiverConfiguration} to take the
   *        data from. May not be <code>null</code>.
   * @return A non-<code>null</code> filled builder instance.
   */
  @Nonnull
  public static Phase4PeppolReceiverConfigurationBuilder builder (@Nonnull final Phase4PeppolReceiverConfiguration aSrc)
  {
    return new Phase4PeppolReceiverConfigurationBuilder (aSrc);
  }

  /**
   * A builder for class {@link Phase4PeppolReceiverConfiguration}.
   *
   * @author Philip Helger
   * @since 3.0.0 Beta7
   */
  public static class Phase4PeppolReceiverConfigurationBuilder implements IBuilder <Phase4PeppolReceiverConfiguration>
  {
    private boolean m_bReceiverCheckEnabled;
    private ISMPExtendedServiceMetadataProvider m_aSMPClient;
    @Pfuoi420
    private PeppolWildcardSelector.EMode m_eWildcardSelectionMode;
    private String m_sAS4EndpointURL;
    private X509Certificate m_aAPCertificate;
    private boolean m_bPerformSBDHValueChecks;
    private boolean m_bCheckSBDHForMandatoryCountryC1;
    private boolean m_bCheckSigningCertificateRevocation;

    public Phase4PeppolReceiverConfigurationBuilder ()
    {}

    public Phase4PeppolReceiverConfigurationBuilder (@Nonnull final Phase4PeppolReceiverConfiguration aSrc)
    {
      ValueEnforcer.notNull (aSrc, "Src");
      receiverCheckEnabled (aSrc.isReceiverCheckEnabled ()).serviceMetadataProvider (aSrc.getSMPClient ())
                                                           .wildcardSelectionMode (aSrc.getWildcardSelectionMode ())
                                                           .as4EndpointUrl (aSrc.getAS4EndpointURL ())
                                                           .apCertificate (aSrc.getAPCertificate ())
                                                           .performSBDHValueChecks (aSrc.isPerformSBDHValueChecks ())
                                                           .checkSBDHForMandatoryCountryC1 (aSrc.isCheckSBDHForMandatoryCountryC1 ())
                                                           .checkSigningCertificateRevocation (aSrc.isCheckSigningCertificateRevocation ());
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder receiverCheckEnabled (final boolean b)
    {
      m_bReceiverCheckEnabled = b;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder serviceMetadataProvider (@Nullable final ISMPExtendedServiceMetadataProvider a)
    {
      m_aSMPClient = a;
      return this;
    }

    @Nonnull
    @Pfuoi420
    public Phase4PeppolReceiverConfigurationBuilder wildcardSelectionMode (@Nullable final PeppolWildcardSelector.EMode e)
    {
      m_eWildcardSelectionMode = e;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder as4EndpointUrl (@Nullable final String s)
    {
      m_sAS4EndpointURL = s;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder apCertificate (@Nullable final X509Certificate a)
    {
      m_aAPCertificate = a;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder performSBDHValueChecks (final boolean b)
    {
      m_bPerformSBDHValueChecks = b;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder checkSBDHForMandatoryCountryC1 (final boolean b)
    {
      m_bCheckSBDHForMandatoryCountryC1 = b;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfigurationBuilder checkSigningCertificateRevocation (final boolean b)
    {
      m_bCheckSigningCertificateRevocation = b;
      return this;
    }

    @Nonnull
    public Phase4PeppolReceiverConfiguration build ()
    {
      if (m_bReceiverCheckEnabled)
      {
        if (m_aSMPClient == null)
          throw new IllegalStateException ("The SMP Client must be provided");
        if (StringHelper.hasNoText (m_sAS4EndpointURL))
          throw new IllegalStateException ("Our AS4 Endpoint URL must be provided");
        if (m_aAPCertificate == null)
          throw new IllegalStateException ("Our AS4 AP certificate must be provided");
      }
      if (m_eWildcardSelectionMode == null)
        throw new IllegalStateException ("The Wildcard Selection Mode must be provided");

      return new Phase4PeppolReceiverConfiguration (m_bReceiverCheckEnabled,
                                                    m_aSMPClient,
                                                    m_eWildcardSelectionMode,
                                                    m_sAS4EndpointURL,
                                                    m_aAPCertificate,
                                                    m_bPerformSBDHValueChecks,
                                                    m_bCheckSBDHForMandatoryCountryC1,
                                                    m_bCheckSigningCertificateRevocation);
    }
  }
}
