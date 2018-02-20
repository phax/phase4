/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.util.Map;

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
public final class AS4CryptoFactory implements Serializable
{
  static
  {
    // Init once
    WSSConfig.init ();
  }

  /**
   * Default {@link AS4CryptoFactory} using file 'private-crypto.properties' or
   * 'crypto.properties'
   */
  public static final AS4CryptoFactory DEFAULT_INSTANCE = new AS4CryptoFactory ((String) null);

  private final CryptoProperties m_aCryptoProps;
  private transient Crypto m_aCrypto;

  @Nonnull
  private static CryptoProperties _createPropsFromFile (@Nullable final String sCryptoPropertiesPath)
  {
    CryptoProperties aCryptoProps;
    if (StringHelper.hasNoText (sCryptoPropertiesPath))
    {
      // Uses crypto.properties => needs exact name crypto.properties
      aCryptoProps = new CryptoProperties (new ClassPathResource ("private-crypto.properties"));
      if (!aCryptoProps.isRead ())
        aCryptoProps = new CryptoProperties (new ClassPathResource ("crypto.properties"));
    }
    else
    {
      aCryptoProps = new CryptoProperties (new ClassPathResource (sCryptoPropertiesPath));
    }
    return aCryptoProps;
  }

  /**
   * Should be used if you want to use a non-default crypto properties to create
   * your Crypto-Instance.
   *
   * @param sCryptoPropertiesPath
   *        when this parameter is <code>null</code>, the default values will
   *        get used. Else it will try to invoke the given properties and read
   *        them throws an exception if it does not work.
   */
  public AS4CryptoFactory (@Nullable final String sCryptoPropertiesPath)
  {
    m_aCryptoProps = _createPropsFromFile (sCryptoPropertiesPath);
    if (!m_aCryptoProps.isRead ())
      throw new InitializationException ("Failed to locate crypto properties in '" + sCryptoPropertiesPath + "'");
  }

  public AS4CryptoFactory (@Nullable final Map <String, String> aProps)
  {
    m_aCryptoProps = new CryptoProperties (aProps);
  }

  @Nonnull
  public CryptoProperties getCryptoProperties ()
  {
    return m_aCryptoProps;
  }

  @Nonnull
  public Crypto getCrypto ()
  {
    Crypto ret = m_aCrypto;
    if (ret == null)
    {
      try
      {
        ret = CryptoFactory.getInstance (m_aCryptoProps.getAsProperties ());
      }
      catch (final Exception ex)
      {
        throw new InitializationException ("Failed to init crypto properties!", ex);
      }
      m_aCrypto = ret;
    }
    return ret;
  }
}
