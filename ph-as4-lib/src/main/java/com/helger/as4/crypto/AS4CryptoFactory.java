/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;

@Immutable
public final class AS4CryptoFactory
{
  static
  {
    // Init once
    WSSConfig.init ();
  }

  private Crypto m_aCrypto;
  private CryptoProperties m_aCryptoProps;

  /**
   * If this constructor is used the default properties get used.
   */
  public AS4CryptoFactory ()
  {
    this (null);
  }

  /**
   * Should be used if you want to use a non-default crypto properties to create
   * your Crypto-Instance.
   *
   * @param sCryptoProperties
   *        when this parameter is <code>null</code>, the default values will
   *        get used. Else it will try to invoke the given properties and read
   *        them throws an exception if it does not work.
   */
  public AS4CryptoFactory (@Nullable final String sCryptoProperties)
  {
    if (StringHelper.hasNoText (sCryptoProperties))
    {
      // Uses crypto.properties => needs exact name crypto.properties
      m_aCryptoProps = new CryptoProperties (new ClassPathResource ("private-crypto.properties"));
      if (!m_aCryptoProps.isRead ())
        m_aCryptoProps = new CryptoProperties (new ClassPathResource ("crypto.properties"));
    }
    else
    {
      m_aCryptoProps = new CryptoProperties (new ClassPathResource (sCryptoProperties));
    }

    if (!m_aCryptoProps.isRead ())
      throw new InitializationException ("Failed to locate crypto properties");

    try
    {
      m_aCrypto = CryptoFactory.getInstance (m_aCryptoProps.getProperties ());
    }
    catch (final Throwable t)
    {
      throw new InitializationException ("Failed to init crypto properties!", t);
    }
  }

  @Nonnull
  public Crypto getCrypto ()
  {
    return m_aCrypto;
  }

  @Nonnull
  public CryptoProperties getCryptoProperties ()
  {
    return m_aCryptoProps;
  }
}
