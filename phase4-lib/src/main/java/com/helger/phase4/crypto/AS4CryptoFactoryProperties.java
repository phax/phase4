/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.crypto;

import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * phase4 crypto factory settings based on {@link AS4CryptoProperties}
 *
 * @author Philip Helger
 * @since 0.11.0
 */
@Immutable
public class AS4CryptoFactoryProperties extends AbstractAS4CryptoFactory
{
  private static final AS4CryptoFactoryProperties DEFAULT_INSTANCE = new AS4CryptoFactoryProperties (AS4CryptoProperties.createFromConfig ());

  /**
   * @return The default instance, created by reading the properties from the
   *         configuration sources.
   * @since 0.11.0
   */
  @Nonnull
  public static AS4CryptoFactoryProperties getDefaultInstance ()
  {
    return DEFAULT_INSTANCE;
  }

  private final AS4CryptoProperties m_aCryptoProps;
  // Lazy initialized
  private Crypto m_aCrypto;
  private KeyStore m_aKeyStore;
  private KeyStore m_aTrustStore;

  /**
   * This constructor takes the crypto properties directly. See the
   * {@link com.helger.phase4.client.AbstractAS4Client} for a usage example.
   *
   * @param aCryptoProps
   *        The properties to be used. May not be <code>null</code>. Note: the
   *        object is cloned internally to avoid outside modification.
   */
  public AS4CryptoFactoryProperties (@Nonnull final AS4CryptoProperties aCryptoProps)
  {
    ValueEnforcer.notNull (aCryptoProps, "CryptoProps");
    m_aCryptoProps = aCryptoProps.getClone ();
  }

  /**
   * @return The crypto properties as created in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final AS4CryptoProperties cryptoProperties ()
  {
    return m_aCryptoProps;
  }

  /**
   * Helper method to create a WSS4J {@link Crypto} instance based on the
   * provided crypto properties.
   *
   * @param aCryptoProps
   *        The crypto properties to use. May not be <code>null</code>.
   * @return A new {@link Crypto} object.
   * @throws IllegalStateException
   *         if creation failed
   */
  @Nonnull
  public static Crypto createCrypto (@Nonnull final AS4CryptoProperties aCryptoProps)
  {
    ValueEnforcer.notNull (aCryptoProps, "CryptoProps");
    try
    {
      return CryptoFactory.getInstance (aCryptoProps.getAsProperties ());
    }
    catch (final WSSecurityException ex)
    {
      throw new IllegalStateException ("Failed to create Crypto instance", ex);
    }
  }

  /**
   * Lazily create a {@link Crypto} instance using the properties from
   * {@link #cryptoProperties()}.
   */
  @Nonnull
  public final Crypto getCrypto (@Nonnull final ECryptoMode eCryptoMode)
  {
    Crypto ret = m_aCrypto;
    if (ret == null)
      ret = m_aCrypto = createCrypto (m_aCryptoProps);
    return ret;
  }

  @Nullable
  public final KeyStore getKeyStore ()
  {
    KeyStore ret = m_aKeyStore;
    if (ret == null)
    {
      ret = m_aKeyStore = KeyStoreHelper.loadKeyStore (m_aCryptoProps.getKeyStoreType (),
                                                       m_aCryptoProps.getKeyStorePath (),
                                                       m_aCryptoProps.getKeyStorePassword ()).getKeyStore ();
    }
    return ret;
  }

  @Nullable
  public final String getKeyAlias ()
  {
    return m_aCryptoProps.getKeyAlias ();
  }

  @Nullable
  public String getKeyPasswordPerAlias (@Nullable final String sSearchKeyAlias)
  {
    final String sKeyAlias = getKeyAlias ();

    // Use case insensitive compare, depends on the keystore type
    if (sKeyAlias != null && sSearchKeyAlias != null && sKeyAlias.equalsIgnoreCase (sSearchKeyAlias))
      return m_aCryptoProps.getKeyPassword ();

    return null;
  }

  @Nullable
  public final KeyStore getTrustStore ()
  {
    KeyStore ret = m_aTrustStore;
    if (ret == null)
    {
      ret = m_aTrustStore = KeyStoreHelper.loadKeyStore (m_aCryptoProps.getTrustStoreType (),
                                                         m_aCryptoProps.getTrustStorePath (),
                                                         m_aCryptoProps.getTrustStorePassword ()).getKeyStore ();
    }
    return ret;
  }

  /**
   * Read crypto properties from the specified file path.
   *
   * @param sCryptoPropertiesPath
   *        The class path to read the properties file from. It is
   *        <code>null</code> or empty, than the default file
   *        "crypto.properties" is read.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static AS4CryptoProperties readCryptoPropertiesFromFile (@Nullable final String sCryptoPropertiesPath)
  {
    AS4CryptoProperties aCryptoProps;
    if (StringHelper.hasNoText (sCryptoPropertiesPath))
    {
      // Uses crypto.properties => needs exact name crypto.properties
      aCryptoProps = new AS4CryptoProperties (new ClassPathResource ("private-crypto.properties"));
      if (!aCryptoProps.isRead ())
        aCryptoProps = new AS4CryptoProperties (new ClassPathResource ("crypto.properties"));
    }
    else
    {
      // Use provided filename
      aCryptoProps = new AS4CryptoProperties (new ClassPathResource (sCryptoPropertiesPath));
    }
    return aCryptoProps;
  }
}
