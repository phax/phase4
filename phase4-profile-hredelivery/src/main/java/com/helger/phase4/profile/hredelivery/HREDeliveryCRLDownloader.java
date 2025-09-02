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
package com.helger.phase4.profile.hredelivery;

import org.slf4j.Logger;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientUrlDownloader;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.security.crl.CRLCache;
import com.helger.security.crl.CRLDownloader;
import com.helger.security.revocation.CertificateRevocationCheckerDefaults;

import jakarta.annotation.Nonnull;

/**
 * The HR eDelivery specific CRL downloader using the {@link HttpClientUrlDownloader} internally.
 *
 * @author Philip Helger
 */
public class HREDeliveryCRLDownloader extends CRLDownloader
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (HREDeliveryCRLDownloader.class);

  /**
   * Default constructor using {@link Phase4HREDeliveryHttpClientSettings}.
   */
  public HREDeliveryCRLDownloader ()
  {
    this (new Phase4HREDeliveryHttpClientSettings ());
  }

  /**
   * Constructor using specific settings
   *
   * @param aHCS
   *        The {@link HttpClientSettings} to use. May not be <code>null</code>.
   */
  public HREDeliveryCRLDownloader (@Nonnull final HttpClientSettings aHCS)
  {
    super (new HttpClientUrlDownloader (aHCS));
  }

  /**
   * Constructor using specific settings
   *
   * @param aHCF
   *        The {@link HttpClientFactory} to use. May not be <code>null</code>.
   */
  public HREDeliveryCRLDownloader (@Nonnull final HttpClientFactory aHCF)
  {
    super (new HttpClientUrlDownloader (aHCF));
  }

  /**
   * Install a global CRLCache using this CRL downloader and the provided
   * {@link HttpClientSettings}.
   *
   * @param aHCS
   *        The {@link HttpClientSettings} to be used. May not be <code>null</code>.
   */
  public static void setAsDefaultCRLCache (@Nonnull final HttpClientSettings aHCS)
  {
    ValueEnforcer.notNull (aHCS, "HttpClientSettings");

    LOGGER.info ("Installing the PeppolCRLDownloader as the default CRL cache using HttpClientSettings " + aHCS);
    CertificateRevocationCheckerDefaults.setDefaultCRLCache (new CRLCache (new HREDeliveryCRLDownloader (aHCS),
                                                                           CRLCache.DEFAULT_CACHING_DURATION));
  }
}
