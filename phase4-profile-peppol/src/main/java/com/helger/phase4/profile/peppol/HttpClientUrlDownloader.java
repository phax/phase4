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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.httpclient.HttpClientFactory;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpClientUrlDownloader.class);

  private final HttpClientFactory m_aHCF;

  /**
   * Constructor with {@link HttpClientSettings}
   *
   * @param aHCS
   *        The {@link HttpClientSettings} to use. Must not be
   *        <code>null</code>.
   */
  public HttpClientUrlDownloader (@Nonnull final HttpClientSettings aHCS)
  {
    this (new HttpClientFactory (aHCS));
  }

  /**
   * Constructor with {@link HttpClientFactory}
   *
   * @param aHCF
   *        The {@link HttpClientFactory} to use. Must not be <code>null</code>.
   */
  public HttpClientUrlDownloader (@Nonnull final HttpClientFactory aHCF)
  {
    ValueEnforcer.notNull (aHCF, "HttpClientFactory");
    m_aHCF = aHCF;
  }

  @Nullable
  public byte [] downloadURL (@Nonnull @Nonempty final String sURL) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to download CRL via HttpClient from '" + sURL + "'");

    try (final HttpClientManager aHCM = new HttpClientManager (m_aHCF))
    {
      final HttpGet aGet = new HttpGet (sURL);
      return aHCM.execute (aGet, new ResponseHandlerByteArray ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to download CRL from '" +
                    sURL +
                    "': " +
                    ex.getClass ().getName () +
                    " - " +
                    ex.getMessage ());
      throw ex;
    }
    finally
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Finished downloading CRL via HttpClient from '" + sURL + "'");
    }
  }
}
