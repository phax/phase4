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
package com.helger.phase4.dbnalliance.servlet;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.builder.IBuilder;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.utils.PeppolCAChecker;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.smpclient.bdxr2.IBDXR2ServiceMetadataProvider;

/**
 * This class contains the "per-request" data of
 * {@link Phase4DBNAllianceDefaultReceiverConfiguration}.
 *
 * @author Philip Helger
 */
@Immutable
public final class Phase4DBNAllianceReceiverConfiguration
{
  private final boolean m_bReceiverCheckEnabled;
  private final IBDXR2ServiceMetadataProvider m_aSMPClient;
  private final String m_sAS4EndpointURL;
  private final X509Certificate m_aAPCertificate;
  private final IIdentifierFactory m_aSBDHIdentifierFactory;
  private final boolean m_bPerformSBDHValueChecks;
  private final boolean m_bCheckSigningCertificateRevocation;
  private final PeppolCAChecker m_aAPCAChecker;

  /**
   * Constructor
   *
   * @param bReceiverCheckEnabled
   *        <code>true</code> if the receiver checks are enabled, <code>false</code> otherwise
   * @param aSMPClient
   *        The SMP metadata provider to be used. May not be <code>null</code> if receiver checks
   *        are enabled.
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
   * @param bCheckSigningCertificateRevocation
   *        <code>true</code> if signing certificate revocation checks should be performed.
   * @param aAPCAChecker
   *        The DBNAlliance AP CA checker. May not be <code>null</code>.
   */
  public Phase4DBNAllianceReceiverConfiguration (final boolean bReceiverCheckEnabled,
                                                 @Nullable final IBDXR2ServiceMetadataProvider aSMPClient,
                                                 @Nullable final String sAS4EndpointURL,
                                                 @Nullable final X509Certificate aAPCertificate,
                                                 @Nonnull final IIdentifierFactory aSBDHIdentifierFactory,
                                                 final boolean bPerformSBDHValueChecks,
                                                 final boolean bCheckSigningCertificateRevocation,
                                                 @Nonnull final PeppolCAChecker aAPCAChecker)
  {
    if (bReceiverCheckEnabled)
      ValueEnforcer.notNull (aSMPClient, "SMPClient");
    if (bReceiverCheckEnabled)
      ValueEnforcer.notEmpty (sAS4EndpointURL, "AS4EndpointURL");
    if (bReceiverCheckEnabled)
      ValueEnforcer.notNull (aAPCertificate, "APCertificate");
    ValueEnforcer.notNull (aSBDHIdentifierFactory, "SBDHIdentifierFactory");
    m_bReceiverCheckEnabled = bReceiverCheckEnabled;
    m_aSMPClient = aSMPClient;
    m_sAS4EndpointURL = sAS4EndpointURL;
    m_aAPCertificate = aAPCertificate;
    m_aSBDHIdentifierFactory = aSBDHIdentifierFactory;
    m_bPerformSBDHValueChecks = bPerformSBDHValueChecks;
    m_bCheckSigningCertificateRevocation = bCheckSigningCertificateRevocation;
    m_aAPCAChecker = aAPCAChecker;
  }

  public boolean isReceiverCheckEnabled ()
  {
    return m_bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is customizable
   *         because it depends either on the SML or a direct URL to the SMP may be provided. Never
   *         <code>null</code> if receiver checks are enabled.
   * @see #isReceiverCheckEnabled()
   */
  @Nullable
  public IBDXR2ServiceMetadataProvider getSMPClient ()
  {
    return m_aSMPClient;
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
  @Nonnull
  public X509Certificate getAPCertificate ()
  {
    return m_aAPCertificate;
  }

  /**
   * @return The identifier factory to be used for SBDH parsing.
   */
  @Nonnull
  public IIdentifierFactory getXHEIdentifierFactory ()
  {
    return m_aSBDHIdentifierFactory;
  }

  public boolean isPerformXHEValueChecks ()
  {
    return m_bPerformSBDHValueChecks;
  }

  public boolean isCheckSigningCertificateRevocation ()
  {
    return m_bCheckSigningCertificateRevocation;
  }

  /**
   * @return The DBNAlliance CA checker to be used. Must not be <code>null</code>.
   */
  @Nonnull
  public PeppolCAChecker getAPCAChecker ()
  {
    return m_aAPCAChecker;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ReceiverCheckEnabled", m_bReceiverCheckEnabled)
                                       .append ("SMPClient", m_aSMPClient)
                                       .append ("AS4EndpointURL", m_sAS4EndpointURL)
                                       .append ("APCertificate", m_aAPCertificate)
                                       .append ("SBDHIdentifierFactory", m_aSBDHIdentifierFactory)
                                       .append ("PerformSBDHValueChecks", m_bPerformSBDHValueChecks)
                                       .append ("CheckSigningCertificateRevocation",
                                                m_bCheckSigningCertificateRevocation)
                                       .append ("APCAChecker", m_aAPCAChecker)
                                       .getToString ();
  }

  /**
   * @return An empty builder instance. Never <code>null</code>.
   */
  @Nonnull
  public static Phase4DBNAllianceReceiverConfigurationBuilder builder ()
  {
    return new Phase4DBNAllianceReceiverConfigurationBuilder ();
  }

  /**
   * Create a builder instance with the data of the provided object already filled in.
   *
   * @param aSrc
   *        The source {@link Phase4DBNAllianceReceiverConfiguration} to take the data from. May not
   *        be <code>null</code>.
   * @return A non-<code>null</code> filled builder instance.
   */
  @Nonnull
  public static Phase4DBNAllianceReceiverConfigurationBuilder builder (@Nonnull final Phase4DBNAllianceReceiverConfiguration aSrc)
  {
    return new Phase4DBNAllianceReceiverConfigurationBuilder (aSrc);
  }

  /**
   * A builder for class {@link Phase4DBNAllianceReceiverConfiguration}.
   *
   * @author Philip Helger
   */
  public static class Phase4DBNAllianceReceiverConfigurationBuilder implements
                                                                    IBuilder <Phase4DBNAllianceReceiverConfiguration>
  {
    private boolean m_bReceiverCheckEnabled;
    private IBDXR2ServiceMetadataProvider m_aSMPClient;
    private String m_sAS4EndpointURL;
    private X509Certificate m_aAPCertificate;
    private IIdentifierFactory m_aSBDHIdentifierFactory;
    private boolean m_bPerformSBDHValueChecks;
    private boolean m_bCheckSigningCertificateRevocation;
    private PeppolCAChecker m_aAPCAChecker;

    public Phase4DBNAllianceReceiverConfigurationBuilder ()
    {}

    public Phase4DBNAllianceReceiverConfigurationBuilder (@Nonnull final Phase4DBNAllianceReceiverConfiguration aSrc)
    {
      ValueEnforcer.notNull (aSrc, "Src");
      receiverCheckEnabled (aSrc.isReceiverCheckEnabled ()).serviceMetadataProvider (aSrc.getSMPClient ())
                                                           .as4EndpointUrl (aSrc.getAS4EndpointURL ())
                                                           .apCertificate (aSrc.getAPCertificate ())
                                                           .xheIdentifierFactory (aSrc.getXHEIdentifierFactory ())
                                                           .performSBDHValueChecks (aSrc.isPerformXHEValueChecks ())
                                                           .checkSigningCertificateRevocation (aSrc.isCheckSigningCertificateRevocation ())
                                                           .apCAChecker (aSrc.getAPCAChecker ());
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder receiverCheckEnabled (final boolean b)
    {
      m_bReceiverCheckEnabled = b;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder serviceMetadataProvider (@Nullable final IBDXR2ServiceMetadataProvider a)
    {
      m_aSMPClient = a;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder as4EndpointUrl (@Nullable final String s)
    {
      m_sAS4EndpointURL = s;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder apCertificate (@Nullable final X509Certificate a)
    {
      m_aAPCertificate = a;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder xheIdentifierFactorySimple ()
    {
      return xheIdentifierFactory (SimpleIdentifierFactory.INSTANCE);
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder xheIdentifierFactory (@Nullable final IIdentifierFactory a)
    {
      m_aSBDHIdentifierFactory = a;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder performSBDHValueChecks (final boolean b)
    {
      m_bPerformSBDHValueChecks = b;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder checkSigningCertificateRevocation (final boolean b)
    {
      m_bCheckSigningCertificateRevocation = b;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfigurationBuilder apCAChecker (@Nullable final PeppolCAChecker a)
    {
      m_aAPCAChecker = a;
      return this;
    }

    @Nonnull
    public Phase4DBNAllianceReceiverConfiguration build ()
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
      if (m_aSBDHIdentifierFactory == null)
        throw new IllegalStateException ("The SBDH Identifier Factory must be provided");
      if (m_aAPCAChecker == null)
        throw new IllegalStateException ("The DBNAlliance AP CA checker must be provided");

      return new Phase4DBNAllianceReceiverConfiguration (m_bReceiverCheckEnabled,
                                                         m_aSMPClient,
                                                         m_sAS4EndpointURL,
                                                         m_aAPCertificate,
                                                         m_aSBDHIdentifierFactory,
                                                         m_bPerformSBDHValueChecks,
                                                         m_bCheckSigningCertificateRevocation,
                                                         m_aAPCAChecker);
    }
  }
}
