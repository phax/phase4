/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile.peppol;

import javax.annotation.Nonnull;

import com.helger.httpclient.HttpClientSettings;
import com.helger.peppol.utils.CRLDownloader;

/**
 * The Peppol specific CRL downloader using the {@link HttpClientUrlDownloader}
 * internally.
 *
 * @author Philip Helger
 */
public class PeppolCRLDownloader extends CRLDownloader
{
  /**
   * Default constructor using {@link Phase4PeppolHttpClientSettings}.
   */
  public PeppolCRLDownloader ()
  {
    this (new Phase4PeppolHttpClientSettings ());
  }

  /**
   * Constructor using specific settings
   *
   * @param aHCS
   *        The {@link HttpClientSettings} to use. May not be <code>null</code>.
   */
  public PeppolCRLDownloader (@Nonnull final HttpClientSettings aHCS)
  {
    super (new HttpClientUrlDownloader (aHCS));
  }
}
