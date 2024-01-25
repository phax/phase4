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
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpGet;

import com.helger.commons.ValueEnforcer;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.utils.IUrlDownloader;

/**
 * Special implementation of {@link IUrlDownloader} to download CRL data using
 * Apache HttpClient via GET and the provided {@link HttpClientSettings}.
 *
 * @author Philip Helger
 * @since 2.7.4
 */
public class HttpClientUrlDownloader implements IUrlDownloader
{
  private final HttpClientSettings m_aHCS;

  /**
   * Constructor
   *
   * @param aHCS
   *        The {@link HttpClientSettings} to use. Must not be
   *        <code>null</code>.
   */
  public HttpClientUrlDownloader (@Nonnull final HttpClientSettings aHCS)
  {
    ValueEnforcer.notNull (aHCS, "HttpClientSettings");
    m_aHCS = aHCS;
  }

  @Nullable
  public byte [] downloadURL (final String sURL) throws Exception
  {
    try (final HttpClientManager aHCF = HttpClientManager.create (m_aHCS))
    {
      final HttpGet aGet = new HttpGet (sURL);
      return aHCF.execute (aGet, new ResponseHandlerByteArray ());
    }
  }
}
