/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.sender;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.commons.lang.TimeValue;
import com.helger.commons.state.ESuccess;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class with the requirements for all message types.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
public abstract class AbstractAS4MessageBuilder <IMPLTYPE extends AbstractAS4MessageBuilder <IMPLTYPE>> implements
                                                IGenericImplTrait <IMPLTYPE>
{
  public static final Locale DEFAULT_LOCALE = Locale.US;

  protected HttpClientFactory m_aHttpClientFactory;
  protected IAS4CryptoFactory m_aCryptoFactory;
  protected String m_sMessageID;
  protected LocalDateTime m_aSendingDateTime;
  protected ESoapVersion m_eSoapVersion;
  protected int m_nMaxRetries = -1;
  protected long m_nRetryIntervalMS = -1;
  protected Locale m_aLocale = DEFAULT_LOCALE;

  /**
   * Create a new builder, with the following fields already set:<br>
   * {@link #httpClientFactory(HttpClientFactory)}<br>
   * {@link #cryptoFactory(IAS4CryptoFactory)}<br>
   * {@link #soapVersion(ESoapVersion)}
   */
  public AbstractAS4MessageBuilder ()
  {
    // Set default values
    try
    {
      httpClientFactory (new HttpClientFactory ());
      cryptoFactory (AS4CryptoFactoryProperties.getDefaultInstance ());
      soapVersion (ESoapVersion.SOAP_12);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AbstractAS4MessageBuilder", ex);
    }
  }

  /**
   * @return The currently set {@link HttpClientFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final HttpClientFactory httpClientFactory ()
  {
    return m_aHttpClientFactory;
  }

  /**
   * Set the HTTP client factory to be used. If the passed settings are
   * provided, a new {@link HttpClientFactory} is created with them.
   *
   * @param aHttpClientSettings
   *        The new HTTP client settings to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientSettings aHttpClientSettings)
  {
    return httpClientFactory (aHttpClientSettings == null ? null : new HttpClientFactory (aHttpClientSettings));
  }

  /**
   * Set the HTTP client factory to be used. By default an instance of
   * {@link HttpClientFactory} is used and there is no need to invoke this
   * method.
   *
   * @param aHttpClientFactory
   *        The new HTTP client factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientFactory aHttpClientFactory)
  {
    m_aHttpClientFactory = aHttpClientFactory;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IAS4CryptoFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final IAS4CryptoFactory cryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  /**
   * Set the crypto factory to be used. The default crypto factory uses the
   * properties from the file "crypto.properties".
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE cryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    m_aCryptoFactory = aCryptoFactory;
    return thisAsT ();
  }

  /**
   * Set the optional AS4 message ID. If this field is not set, a random message
   * ID is created.
   *
   * @param sMessageID
   *        The optional AS4 message ID to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE messageID (@Nullable final String sMessageID)
  {
    m_sMessageID = sMessageID;
    return thisAsT ();
  }

  /**
   * Set the optional sending date time. If no time is specified, the current
   * date time is used.
   *
   * @param aSendingDateTime
   *        The sending date time to set. May be <code>null</code>.
   * @return this for chaining
   * @since 0.12.0
   */
  @Nonnull
  public final IMPLTYPE sendingDateTime (@Nullable final LocalDateTime aSendingDateTime)
  {
    m_aSendingDateTime = aSendingDateTime;
    return thisAsT ();
  }

  /**
   * @return The SOAP version to be used. May be <code>null</code>.
   */
  @Nullable
  public final ESoapVersion soapVersion ()
  {
    return m_eSoapVersion;
  }

  /**
   * Set the SOAP version to be used. Default is SOAP 1.2
   *
   * @param eSoapVersion
   *        The SOAP version to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE soapVersion (@Nullable final ESoapVersion eSoapVersion)
  {
    m_eSoapVersion = eSoapVersion;
    return thisAsT ();
  }

  /**
   * Set the maximum number of retries.
   *
   * @param n
   *        The maximum number of retries.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE maxRetries (final int n)
  {
    m_nMaxRetries = n;
    return thisAsT ();
  }

  /**
   * Set the retry interval for retries. This is only relevant if at least 1
   * retry happens.
   *
   * @param a
   *        The retry interval
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE retryInterval (@Nullable final TimeValue a)
  {
    return retryIntervalMilliseconds (a == null ? null : a.getAsMillis ());
  }

  /**
   * Set the retry interval for retries. This is only relevant if at least 1
   * retry happens.
   *
   * @param a
   *        The retry interval
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE retryInterval (@Nullable final Duration a)
  {
    return retryIntervalMilliseconds (a == null ? null : a.toMillis ());
  }

  /**
   * Set the retry interval for retries. This is only relevant if at least 1
   * retry happens.
   *
   * @param n
   *        The retry interval in milliseconds
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE retryIntervalMilliseconds (final long n)
  {
    m_nRetryIntervalMS = n;
    return thisAsT ();
  }

  /**
   * Set the locale to use. The main purpose is to use the correct language for
   * processing error message in response messages. This field must be set. The
   * default value is {@link #DEFAULT_LOCALE}.
   *
   * @param a
   *        The locale to use. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE locale (@Nullable final Locale a)
  {
    m_aLocale = a;
    return thisAsT ();
  }

  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (m_aHttpClientFactory == null)
      return false;
    // m_aCryptoFactory may be null
    // m_sMessageID is optional
    // m_aSendingDateTime may be null
    if (m_eSoapVersion == null)
      return false;

    // m_nMaxRetries doesn't matter
    // m_nRetryIntervalMS doesn't matter
    if (m_aLocale == null)
      return false;

    // All valid
    return true;
  }

  /**
   * Synchronously send the AS4 message. Before sending,
   * {@link #isEveryRequiredFieldSet()} is called to check that the mandatory
   * elements are set.
   *
   * @return {@link ESuccess#FAILURE} if not all mandatory parameters are set or
   *         if sending failed, {@link ESuccess#SUCCESS} upon success. Never
   *         <code>null</code>.
   * @throws Phase4Exception
   *         In case of any error
   * @see #isEveryRequiredFieldSet()
   */
  @Nonnull
  public abstract ESuccess sendMessage () throws Phase4Exception;
}
