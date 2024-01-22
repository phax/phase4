/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;
import com.helger.smpclient.peppol.ISMPServiceMetadataProvider;
import com.helger.smpclient.peppol.PeppolWildcardSelector;
import com.helger.smpclient.peppol.PeppolWildcardSelector.EMode;

/**
 * This class contains the "per-request" data of
 * {@link Phase4PeppolServletConfiguration}.
 *
 * @author Philip Helger
 * @since 0.9.13
 */
@Immutable
public class Phase4PeppolReceiverCheckData
{
  private final ISMPServiceMetadataProvider m_aSMPClient;
  private final EMode m_eWildcardSelectionMode;
  private final String m_sAS4EndpointURL;
  private final X509Certificate m_aAPCertificate;

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
  public Phase4PeppolReceiverCheckData (@Nonnull final ISMPServiceMetadataProvider aSMPClient,
                                        @Nonnull @Nonempty final String sAS4EndpointURL,
                                        @Nonnull final X509Certificate aAPCertificate,
                                        @Nonnull final PeppolWildcardSelector.EMode eWildcardSelectionMode)
  {
    ValueEnforcer.notNull (aSMPClient, "SMPClient");
    ValueEnforcer.notNull (eWildcardSelectionMode, "WildcardSelectionMode");
    ValueEnforcer.notEmpty (sAS4EndpointURL, "AS4EndpointURL");
    ValueEnforcer.notNull (aAPCertificate, "APCertificate");
    m_aSMPClient = aSMPClient;
    m_eWildcardSelectionMode = eWildcardSelectionMode;
    m_sAS4EndpointURL = sAS4EndpointURL;
    m_aAPCertificate = aAPCertificate;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is
   *         customizable because it depends either on the SML or a direct URL
   *         to the SMP may be provided. Never <code>null</code>.
   */
  @Nonnull
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
   *         retrieval. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getAS4EndpointURL ()
  {
    return m_sAS4EndpointURL;
  }

  /**
   * @return The certificate of this AP to compare to against the SMP lookup
   *         result upon retrieval. Never <code>null</code>.
   */
  @Nonnull
  public X509Certificate getAPCertificate ()
  {
    return m_aAPCertificate;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("SMPClient", m_aSMPClient)
                                       .append ("WildcardSelectionMode", m_eWildcardSelectionMode)
                                       .append ("AS4EndpointURL", m_sAS4EndpointURL)
                                       .append ("APCertificate", m_aAPCertificate)
                                       .getToString ();
  }
}
