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
package com.helger.phase4.peppol.servlet;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.misc.ChangeNextMajorRelease;
import com.helger.base.builder.IBuilder;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.CertificateRevocationCheckerDefaults;
import com.helger.security.revocation.ERevocationCheckMode;
import com.helger.smpclient.peppol.ISMPExtendedServiceMetadataProvider;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

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
  private final ISMLInfo m_aSMLInfo;
  private final ISMPURLProvider m_aSMPURLProvider;
  private final ERevocationCheckMode m_eSMPRevocationCheckMode;
  private final boolean m_bSMPRevocationSoftFail;
  private final String m_sAS4EndpointURL;
  private final X509Certificate m_aAPCertificate;
  private final IIdentifierFactory m_aSBDHIdentifierFactory;
  private final boolean m_bPerformSBDHValueChecks;
  private final boolean m_bCheckSBDHForMandatoryCountryC1;
  @ChangeNextMajorRelease ("Changed from implicit boolean to m_eAPRevocationCheckMode")
  private final boolean m_bCheckAPSigningCertificateRevocation;
  private final TrustedCAChecker m_aAPCAChecker;
  private final boolean m_bAPRevocationSoftFail;
  private final ETriState m_eAPCacheRevocationCheckResult;
  private final ERevocationCheckMode m_eAPRevocationCheckMode;

  /**
   * Constructor
   *
   * @param bReceiverCheckEnabled
   *        <code>true</code> if the receiver checks are enabled, <code>false</code> otherwise
   * @param aSMPClient
   *        The SMP metadata provider to be used. May be <code>null</code> if {@code aSMLInfo} is
   *        provided instead.
   * @param aSMLInfo
   *        The SML information for dynamic SMP client resolution per participant ID. May be
   *        <code>null</code> if {@code aSMPClient} is provided instead.
   * @param aSMPURLProvider
   *        The SMP URL provider to be used for dynamic SMP client resolution. May be
   *        <code>null</code> to use the default ({@link PeppolNaptrURLProvider#INSTANCE}).
   * @param eSMPRevocationCheckMode
   *        The revocation check mode to apply when verifying SMP response certificates.
   *        <code>null</code> means "use the JVM-wide default from
   *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}". Only
   *        applied to SMP clients created internally via {@link #getOrCreateSMPClientForRecipient}.
   *        Pre-built SMP clients passed via {@code aSMPClient} must be configured by the caller.
   * @param bSMPRevocationSoftFail
   *        <code>true</code> to accept an indeterminable revocation status of an SMP response
   *        certificate (soft-fail), <code>false</code> to reject. Defaults to
   *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults#isAllowSoftFail()}.
   *        Only applied to SMP clients created internally via
   *        {@link #getOrCreateSMPClientForRecipient}.
   * @param sAS4EndpointURL
   *        The endpoint URL to check against. May neither be <code>null</code> nor empty if
   *        receiver checks are enabled.
   * @param aAPCertificate
   *        The AP certificate to be used for compatibility. May not be <code>null</code> if
   *        receiver checks are enabled.
   * @param aSBDHIdentifierFactory
   *        The identifier factory to be used for SBDH parsing. May not be <code>null</code>.
   * @param bPerformSBDHValueChecks
   *        <code>true</code> if SBDH value checks should be performed.
   * @param bCheckSBDHForMandatoryCountryC1
   *        <code>true</code> if SBDH value checks should be performed for mandatory C1 country
   *        code.
   * @param bCheckAPSigningCertificateRevocation
   *        <code>true</code> if signing certificate revocation checks should be performed.
   * @param aAPCAChecker
   *        The Peppol AP CA checker. May not be <code>null</code>.
   * @param bAPRevocationSoftFail
   *        <code>true</code> to accept
   *        {@link com.helger.security.certificate.ECertificateCheckResult#REVOCATION_STATUS_UNKNOWN}
   *        from the AP CA checker as valid, <code>false</code> to treat it as invalid. Defaults to
   *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults#isAllowSoftFail()}.
   *        Applies to the inbound signing certificate check.
   * @param eAPCacheRevocationCheckResult
   *        Override for the revocation result caching flag of the inbound signing certificate
   *        check. {@link ETriState#UNDEFINED} (the default) means "use the JVM-wide default from
   *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}". Must not
   *        be <code>null</code>.
   * @param eAPRevocationCheckMode
   *        Override for the revocation check mode of the inbound signing certificate check.
   *        <code>null</code> (the default) means "use the JVM-wide default from
   *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}".
   * @since 3.0.3
   */
  public Phase4PeppolReceiverConfiguration (final boolean bReceiverCheckEnabled,
                                            @Nullable final ISMPExtendedServiceMetadataProvider aSMPClient,
                                            @Nullable final ISMLInfo aSMLInfo,
                                            @Nullable final ISMPURLProvider aSMPURLProvider,
                                            @Nullable final ERevocationCheckMode eSMPRevocationCheckMode,
                                            final boolean bSMPRevocationSoftFail,
                                            @Nullable final String sAS4EndpointURL,
                                            @Nullable final X509Certificate aAPCertificate,
                                            @NonNull final IIdentifierFactory aSBDHIdentifierFactory,
                                            final boolean bPerformSBDHValueChecks,
                                            final boolean bCheckSBDHForMandatoryCountryC1,
                                            final boolean bCheckAPSigningCertificateRevocation,
                                            @NonNull final TrustedCAChecker aAPCAChecker,
                                            final boolean bAPRevocationSoftFail,
                                            @NonNull final ETriState eAPCacheRevocationCheckResult,
                                            @Nullable final ERevocationCheckMode eAPRevocationCheckMode)
  {
    if (bReceiverCheckEnabled)
    {
      if (aSMPClient == null && aSMLInfo == null)
        throw new IllegalArgumentException ("Either an SMP Client or SML Info must be provided when receiver checks are enabled");
      ValueEnforcer.notEmpty (sAS4EndpointURL, "AS4EndpointURL");
      ValueEnforcer.notNull (aAPCertificate, "APCertificate");
    }
    ValueEnforcer.notNull (aSBDHIdentifierFactory, "SBDHIdentifierFactory");
    ValueEnforcer.notNull (eAPCacheRevocationCheckResult, "APCacheRevocationCheckResult");
    m_bReceiverCheckEnabled = bReceiverCheckEnabled;
    m_aSMPClient = aSMPClient;
    m_aSMLInfo = aSMLInfo;
    m_aSMPURLProvider = aSMPURLProvider != null ? aSMPURLProvider : PeppolNaptrURLProvider.INSTANCE;
    m_eSMPRevocationCheckMode = eSMPRevocationCheckMode;
    m_bSMPRevocationSoftFail = bSMPRevocationSoftFail;
    m_sAS4EndpointURL = sAS4EndpointURL;
    m_aAPCertificate = aAPCertificate;
    m_aSBDHIdentifierFactory = aSBDHIdentifierFactory;
    m_bPerformSBDHValueChecks = bPerformSBDHValueChecks;
    m_bCheckSBDHForMandatoryCountryC1 = bCheckSBDHForMandatoryCountryC1;
    m_bCheckAPSigningCertificateRevocation = bCheckAPSigningCertificateRevocation;
    m_aAPCAChecker = aAPCAChecker;
    m_bAPRevocationSoftFail = bAPRevocationSoftFail;
    m_eAPCacheRevocationCheckResult = eAPCacheRevocationCheckResult;
    m_eAPRevocationCheckMode = eAPRevocationCheckMode;
  }

  public boolean isReceiverCheckEnabled ()
  {
    return m_bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is customizable
   *         because it depends either on the SML or a direct URL to the SMP may be provided. May be
   *         <code>null</code> if SML info is configured for dynamic resolution instead.
   * @see #isReceiverCheckEnabled()
   * @see #getSMLInfo()
   * @see #getOrCreateSMPClientForRecipient(IParticipantIdentifier)
   */
  @Nullable
  public ISMPExtendedServiceMetadataProvider getSMPClient ()
  {
    return m_aSMPClient;
  }

  /**
   * @return The SML information for dynamic SMP client resolution. May be <code>null</code> if a
   *         fixed SMP client is configured instead.
   * @see #getSMPClient()
   * @since v4.4.2
   */
  @Nullable
  public ISMLInfo getSMLInfo ()
  {
    return m_aSMLInfo;
  }

  /**
   * @return The SMP URL provider to be used for dynamic SMP client resolution. Never
   *         <code>null</code>. Defaults to {@link PeppolNaptrURLProvider#INSTANCE}.
   * @since v4.4.2
   */
  @NonNull
  public ISMPURLProvider getSMPURLProvider ()
  {
    return m_aSMPURLProvider;
  }

  /**
   * @return The revocation check mode to apply when verifying SMP response certificates.
   *         <code>null</code> means "use the JVM-wide default from
   *         {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}". Only
   *         applied to SMP clients created internally via
   *         {@link #getOrCreateSMPClientForRecipient(IParticipantIdentifier)}.
   * @since 4.5.0
   */
  @Nullable
  public ERevocationCheckMode getSMPRevocationCheckMode ()
  {
    return m_eSMPRevocationCheckMode;
  }

  /**
   * @return <code>true</code> to accept an indeterminable revocation status of an SMP response
   *         certificate (soft-fail), <code>false</code> to reject. Defaults to
   *         {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults#isAllowSoftFail()}.
   *         Only applied to SMP clients created internally via
   *         {@link #getOrCreateSMPClientForRecipient(IParticipantIdentifier)}.
   * @since 4.5.0
   */
  public boolean isSMPRevocationSoftFail ()
  {
    return m_bSMPRevocationSoftFail;
  }

  /**
   * Get the existing SMP client or create a new one dynamically for the provided recipient
   * participant ID using the configured SML info and URL provider.
   *
   * @param aRecipientID
   *        The recipient participant identifier. May not be <code>null</code>.
   * @return The SMP client. May be <code>null</code> if neither a fixed SMP client nor SML info is
   *         configured.
   * @throws SMPDNSResolutionException
   *         If DNS resolution of the SMP address fails.
   * @since v4.4.2
   */
  @Nullable
  public ISMPExtendedServiceMetadataProvider getOrCreateSMPClientForRecipient (@NonNull final IParticipantIdentifier aRecipientID) throws SMPDNSResolutionException
  {
    if (m_aSMPClient != null)
    {
      // Constant SMP
      return m_aSMPClient;
    }

    if (m_aSMLInfo != null)
    {
      // SMP with dynamic discovery
      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (m_aSMPURLProvider, aRecipientID, m_aSMLInfo);
      aSMPClient.setRevocationCheckMode (m_eSMPRevocationCheckMode);
      aSMPClient.setAllowRevocationSoftFail (m_bSMPRevocationSoftFail);
      return aSMPClient;
    }
    return null;
  }

  /**
   * @return The URL of this AP to compare to against the SMP lookup result upon retrieval. Neither
   *         <code>null</code> nor empty if receiver checks are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @Nullable
  public String getAS4EndpointURL ()
  {
    return m_sAS4EndpointURL;
  }

  /**
   * @return The certificate of this AP to compare to against the SMP lookup result upon retrieval.
   *         Never <code>null</code> if receiver checks are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @NonNull
  public X509Certificate getAPCertificate ()
  {
    return m_aAPCertificate;
  }

  /**
   * @return The identifier factory to be used for SBDH parsing.
   * @since 3.0.1
   */
  @NonNull
  public IIdentifierFactory getSBDHIdentifierFactory ()
  {
    return m_aSBDHIdentifierFactory;
  }

  public boolean isPerformSBDHValueChecks ()
  {
    return m_bPerformSBDHValueChecks;
  }

  @Deprecated (forRemoval = true, since = "4.2.4")
  public boolean isCheckSBDHForMandatoryCountryC1 ()
  {
    return m_bCheckSBDHForMandatoryCountryC1;
  }

  /**
   * @return <code>true</code> if the AP signing certificate should be used.
   * @deprecated Use {@link #isCheckAPSigningCertificateRevocation()} instead
   */
  @Deprecated (forRemoval = true, since = "4.5.0")
  public boolean isCheckSigningCertificateRevocation ()
  {
    return isCheckAPSigningCertificateRevocation ();
  }

  /**
   * @return <code>true</code> if the AP signing certificate should be used.
   */
  public boolean isCheckAPSigningCertificateRevocation ()
  {
    return m_bCheckAPSigningCertificateRevocation;
  }

  /**
   * @return The Peppol CA checker to be used. Must not be <code>null</code>.
   * @since 3.0.3
   */
  @NonNull
  public TrustedCAChecker getAPCAChecker ()
  {
    return m_aAPCAChecker;
  }

  /**
   * @return <code>true</code> to accept
   *         {@link com.helger.security.certificate.ECertificateCheckResult#REVOCATION_STATUS_UNKNOWN}
   *         from the AP CA checker as valid (soft-fail), <code>false</code> to treat it as invalid.
   *         Applies to the inbound signing certificate check.
   * @since 4.5.0
   */
  public boolean isAPRevocationSoftFail ()
  {
    return m_bAPRevocationSoftFail;
  }

  /**
   * @return The revocation result caching override applied during the inbound signing certificate
   *         check. {@link ETriState#UNDEFINED} means "use the JVM-wide default from
   *         {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}". Never
   *         <code>null</code>.
   * @since 4.5.0
   */
  @NonNull
  public ETriState getAPCacheRevocationCheckResult ()
  {
    return m_eAPCacheRevocationCheckResult;
  }

  /**
   * @return The revocation check mode override applied during the inbound signing certificate
   *         check. <code>null</code> means "use the JVM-wide default from
   *         {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}".
   * @since 4.5.0
   */
  @Nullable
  public ERevocationCheckMode getAPRevocationCheckMode ()
  {
    return m_eAPRevocationCheckMode;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ReceiverCheckEnabled", m_bReceiverCheckEnabled)
                                       .append ("SMPClient", m_aSMPClient)
                                       .append ("SMLInfo", m_aSMLInfo)
                                       .append ("SMPURLProvider", m_aSMPURLProvider)
                                       .append ("SMPRevocationCheckMode", m_eSMPRevocationCheckMode)
                                       .append ("SMPRevocationSoftFail", m_bSMPRevocationSoftFail)
                                       .append ("AS4EndpointURL", m_sAS4EndpointURL)
                                       .append ("APCertificate", m_aAPCertificate)
                                       .append ("SBDHIdentifierFactory", m_aSBDHIdentifierFactory)
                                       .append ("PerformSBDHValueChecks", m_bPerformSBDHValueChecks)
                                       .append ("CheckSBDHForMandatoryCountryC1", m_bCheckSBDHForMandatoryCountryC1)
                                       .append ("CheckSigningCertificateRevocation",
                                                m_bCheckAPSigningCertificateRevocation)
                                       .append ("APCAChecker", m_aAPCAChecker)
                                       .append ("APRevocationSoftFail", m_bAPRevocationSoftFail)
                                       .append ("APCacheRevocationCheckResult", m_eAPCacheRevocationCheckResult)
                                       .append ("APRevocationCheckMode", m_eAPRevocationCheckMode)
                                       .getToString ();
  }

  /**
   * @return An empty builder instance. Never <code>null</code>.
   */
  @NonNull
  public static Phase4PeppolReceiverConfigurationBuilder builder ()
  {
    return new Phase4PeppolReceiverConfigurationBuilder ();
  }

  /**
   * Create a builder instance with the data of the provided object already filled in.
   *
   * @param aSrc
   *        The source {@link Phase4PeppolReceiverConfiguration} to take the data from. May not be
   *        <code>null</code>.
   * @return A non-<code>null</code> filled builder instance.
   */
  @NonNull
  public static Phase4PeppolReceiverConfigurationBuilder builder (@NonNull final Phase4PeppolReceiverConfiguration aSrc)
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
    private ISMLInfo m_aSMLInfo;
    private ISMPURLProvider m_aSMPURLProvider;
    private ERevocationCheckMode m_eSMPRevocationCheckMode;
    private boolean m_bSMPRevocationSoftFail = CertificateRevocationCheckerDefaults.isAllowSoftFail ();
    private String m_sAS4EndpointURL;
    private X509Certificate m_aAPCertificate;
    private IIdentifierFactory m_aSBDHIdentifierFactory;
    private boolean m_bPerformSBDHValueChecks;
    private boolean m_bCheckSBDHForMandatoryCountryC1;
    private boolean m_bCheckSigningCertificateRevocation;
    private TrustedCAChecker m_aAPCAChecker;
    private boolean m_bAPRevocationSoftFail;
    private ETriState m_eAPCacheRevocationCheckResult = ETriState.UNDEFINED;
    private ERevocationCheckMode m_eAPRevocationCheckMode;

    public Phase4PeppolReceiverConfigurationBuilder ()
    {}

    public Phase4PeppolReceiverConfigurationBuilder (@NonNull final Phase4PeppolReceiverConfiguration aSrc)
    {
      ValueEnforcer.notNull (aSrc, "Src");
      receiverCheckEnabled (aSrc.isReceiverCheckEnabled ()).serviceMetadataProvider (aSrc.getSMPClient ())
                                                           .smlInfo (aSrc.getSMLInfo ())
                                                           .smpURLProvider (aSrc.getSMPURLProvider ())
                                                           .smpRevocationCheckMode (aSrc.getSMPRevocationCheckMode ())
                                                           .smpRevocationSoftFail (aSrc.isSMPRevocationSoftFail ())
                                                           .as4EndpointUrl (aSrc.getAS4EndpointURL ())
                                                           .apCertificate (aSrc.getAPCertificate ())
                                                           .sbdhIdentifierFactory (aSrc.getSBDHIdentifierFactory ())
                                                           .performSBDHValueChecks (aSrc.isPerformSBDHValueChecks ())
                                                           .checkSBDHForMandatoryCountryC1 (aSrc.isCheckSBDHForMandatoryCountryC1 ())
                                                           .checkAPSigningCertificateRevocation (aSrc.isCheckAPSigningCertificateRevocation ())
                                                           .apCAChecker (aSrc.getAPCAChecker ())
                                                           .apRevocationSoftFail (aSrc.isAPRevocationSoftFail ())
                                                           .apCacheRevocationCheckResult (aSrc.getAPCacheRevocationCheckResult ())
                                                           .apRevocationCheckMode (aSrc.getAPRevocationCheckMode ());
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder receiverCheckEnabled (final boolean b)
    {
      m_bReceiverCheckEnabled = b;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder serviceMetadataProvider (@Nullable final ISMPExtendedServiceMetadataProvider a)
    {
      m_aSMPClient = a;
      return this;
    }

    /**
     * Set the SML information for dynamic per-participant SMP client resolution. This is an
     * alternative to setting a fixed SMP client via
     * {@link #serviceMetadataProvider(ISMPExtendedServiceMetadataProvider)}.
     *
     * @param a
     *        The SML info to use. May be <code>null</code>.
     * @return this for chaining
     * @since v4.4.2
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder smlInfo (@Nullable final ISMLInfo a)
    {
      m_aSMLInfo = a;
      return this;
    }

    /**
     * Set the SMP URL provider to be used for dynamic SMP client resolution. Only relevant if
     * {@link #smlInfo(ISMLInfo)} is set. Defaults to {@link PeppolNaptrURLProvider#INSTANCE}.
     *
     * @param a
     *        The SMP URL provider to use. May be <code>null</code> to use the default.
     * @return this for chaining
     * @since v4.4.2
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder smpURLProvider (@Nullable final ISMPURLProvider a)
    {
      m_aSMPURLProvider = a;
      return this;
    }

    /**
     * Set the revocation check mode to apply when verifying SMP response certificates. Only applied
     * to SMP clients created internally via
     * {@link Phase4PeppolReceiverConfiguration#getOrCreateSMPClientForRecipient(IParticipantIdentifier)}.
     *
     * @param e
     *        The revocation check mode to use. <code>null</code> means "use the JVM-wide default
     *        from {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}".
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder smpRevocationCheckMode (@Nullable final ERevocationCheckMode e)
    {
      m_eSMPRevocationCheckMode = e;
      return this;
    }

    /**
     * Set whether an indeterminable revocation status of an SMP response certificate is accepted
     * (soft-fail) or causes the certificate to be rejected. Only applied to SMP clients created
     * internally via
     * {@link Phase4PeppolReceiverConfiguration#getOrCreateSMPClientForRecipient(IParticipantIdentifier)}.
     *
     * @param b
     *        <code>true</code> to accept on unknown revocation status (soft-fail),
     *        <code>false</code> to reject. Defaults to
     *        {@link CertificateRevocationCheckerDefaults#isAllowSoftFail()}.
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder smpRevocationSoftFail (final boolean b)
    {
      m_bSMPRevocationSoftFail = b;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder as4EndpointUrl (@Nullable final String s)
    {
      m_sAS4EndpointURL = s;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder apCertificate (@Nullable final X509Certificate a)
    {
      m_aAPCertificate = a;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder sbdhIdentifierFactorySimple ()
    {
      return sbdhIdentifierFactory (SimpleIdentifierFactory.INSTANCE);
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder sbdhIdentifierFactoryPeppol ()
    {
      return sbdhIdentifierFactory (PeppolIdentifierFactory.INSTANCE);
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder sbdhIdentifierFactory (@Nullable final IIdentifierFactory a)
    {
      m_aSBDHIdentifierFactory = a;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder performSBDHValueChecks (final boolean b)
    {
      m_bPerformSBDHValueChecks = b;
      return this;
    }

    /**
     * @param b
     *        <code>true</code> to check for mandatory country C1
     * @return this for chaining
     * @deprecated This is deprecated, because the feature is required for years, so there is no
     *             need anymore to disable this feature
     */
    @NonNull
    @Deprecated (forRemoval = true, since = "4.2.4")
    public Phase4PeppolReceiverConfigurationBuilder checkSBDHForMandatoryCountryC1 (final boolean b)
    {
      m_bCheckSBDHForMandatoryCountryC1 = b;
      return this;
    }

    /**
     * @param b
     *        <code>true</code> if signing certificate revocation checks should be enabled,
     *        <code>false</code> if not.
     * @return this for chaining
     * @deprecated Use {@link #checkAPSigningCertificateRevocation(boolean)} instead
     */
    @Deprecated
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder checkSigningCertificateRevocation (final boolean b)
    {
      return checkAPSigningCertificateRevocation (b);
    }

    /**
     * @param b
     *        <code>true</code> if signing certificate revocation checks should be enabled,
     *        <code>false</code> if not.
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder checkAPSigningCertificateRevocation (final boolean b)
    {
      m_bCheckSigningCertificateRevocation = b;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder apCAChecker (@Nullable final TrustedCAChecker a)
    {
      m_aAPCAChecker = a;
      return this;
    }

    /**
     * Enable or disable revocation soft-fail for the inbound signing certificate check. When
     * enabled, an undeterminable revocation status (e.g. unreachable CRL distribution point with no
     * working OCSP fallback) is logged at WARN level and the message is accepted. All other invalid
     * states (revoked, expired, untrusted issuer, ...) still cause the message to be rejected.
     * <p>
     * <strong>Security note:</strong> Peppol mandates revocation checks. Enabling soft-fail allows
     * an inbound message with a potentially-revoked AP signing certificate to be accepted during a
     * CRL/OCSP outage. Use only as a deliberate operational-continuity measure. Defaults to
     * <code>false</code>.
     *
     * @param b
     *        <code>true</code> to accept
     *        {@link com.helger.security.certificate.ECertificateCheckResult#REVOCATION_STATUS_UNKNOWN}
     *        as valid, <code>false</code> (default) to treat it as invalid.
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder apRevocationSoftFail (final boolean b)
    {
      m_bAPRevocationSoftFail = b;
      return this;
    }

    /**
     * Override the revocation result caching flag for the inbound signing certificate check on a
     * per-receive basis.
     *
     * @param e
     *        {@link ETriState#TRUE} to use the global revocation cache, {@link ETriState#FALSE} to
     *        bypass it, {@link ETriState#UNDEFINED} (the default) to use the JVM-wide default from
     *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}. May not
     *        be <code>null</code>.
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder apCacheRevocationCheckResult (@NonNull final ETriState e)
    {
      ValueEnforcer.notNull (e, "APCacheRevocationCheckResult");
      m_eAPCacheRevocationCheckResult = e;
      return this;
    }

    /**
     * Override the revocation check mode for the inbound signing certificate check on a per-receive
     * basis.
     *
     * @param e
     *        The revocation check mode to use. <code>null</code> (the default) means "use the
     *        JVM-wide default from
     *        {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults}".
     * @return this for chaining
     * @since 4.5.0
     */
    @NonNull
    public Phase4PeppolReceiverConfigurationBuilder apRevocationCheckMode (@Nullable final ERevocationCheckMode e)
    {
      m_eAPRevocationCheckMode = e;
      return this;
    }

    @NonNull
    public Phase4PeppolReceiverConfiguration build ()
    {
      if (m_bReceiverCheckEnabled)
      {
        if (m_aSMPClient == null && m_aSMLInfo == null)
          throw new IllegalStateException ("Either an SMP Client or SML Info must be provided");
        if (StringHelper.isEmpty (m_sAS4EndpointURL))
          throw new IllegalStateException ("Our AS4 Endpoint URL must be provided");
        if (m_aAPCertificate == null)
          throw new IllegalStateException ("Our AS4 AP certificate must be provided");
      }
      if (m_aSBDHIdentifierFactory == null)
        throw new IllegalStateException ("The SBDH Identifier Factory must be provided");
      if (m_aAPCAChecker == null)
        throw new IllegalStateException ("The Peppol AP CA checker must be provided");

      return new Phase4PeppolReceiverConfiguration (m_bReceiverCheckEnabled,
                                                    m_aSMPClient,
                                                    m_aSMLInfo,
                                                    m_aSMPURLProvider,
                                                    m_eSMPRevocationCheckMode,
                                                    m_bSMPRevocationSoftFail,
                                                    m_sAS4EndpointURL,
                                                    m_aAPCertificate,
                                                    m_aSBDHIdentifierFactory,
                                                    m_bPerformSBDHValueChecks,
                                                    m_bCheckSBDHForMandatoryCountryC1,
                                                    m_bCheckSigningCertificateRevocation,
                                                    m_aAPCAChecker,
                                                    m_bAPRevocationSoftFail,
                                                    m_eAPCacheRevocationCheckResult,
                                                    m_eAPRevocationCheckMode);
    }
  }
}
