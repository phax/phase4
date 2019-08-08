/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.engine.WSSConfig;

import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.security.keystore.KeyStoreHelper;

@Immutable
public class AS4CryptoFactory implements Serializable
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

  private final AS4CryptoProperties m_aCryptoProps;
  // Lazy initialized
  private transient Crypto m_aCrypto;
  private transient KeyStore m_aKeyStore;
  private transient KeyStore.PrivateKeyEntry m_aPK;

  @Nonnull
  private static AS4CryptoProperties _createPropsFromFile (@Nullable final String sCryptoPropertiesPath)
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
      aCryptoProps = new AS4CryptoProperties (new ClassPathResource (sCryptoPropertiesPath));
    }
    return aCryptoProps;
  }

  /**
   * Should be used if you want to use a non-default crypto properties to create
   * your Crypto-Instance. This constructor reads the properties from a file.
   *
   * @param sCryptoPropertiesPath
   *        when this parameter is <code>null</code>, the default values will
   *        get used. Else it will try to invoke the given properties and read
   *        them throws an exception if it does not work.
   */
  public AS4CryptoFactory (@Nullable final String sCryptoPropertiesPath)
  {
    this (_createPropsFromFile (sCryptoPropertiesPath));
    if (!m_aCryptoProps.isRead ())
      throw new InitializationException ("Failed to locate crypto properties in '" + sCryptoPropertiesPath + "'");
  }

  /**
   * This constructor takes the crypto properties directly from a map. No file
   * access is performed. See the
   * {@link com.helger.as4.client.AbstractAS4Client} for a usage example.
   *
   * @param aProps
   *        The properties to be used. May be <code>null</code>.
   */
  public AS4CryptoFactory (@Nullable final Map <String, String> aProps)
  {
    this (new AS4CryptoProperties (aProps));
  }

  /**
   * This constructor takes the crypto properties directly. See the
   * {@link com.helger.as4.client.AbstractAS4Client} for a usage example.
   *
   * @param aCryptoProps
   *        The properties to be used. May not be <code>null</code>.
   */
  protected AS4CryptoFactory (@Nonnull final AS4CryptoProperties aCryptoProps)
  {
    ValueEnforcer.notNull (aCryptoProps, "CryptoProps");
    m_aCryptoProps = aCryptoProps;
  }

  /**
   * @return The crypto properties as created in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4CryptoProperties getCryptoProperties ()
  {
    return m_aCryptoProps;
  }

  @Nonnull
  public static Crypto createCrypto (@Nonnull final AS4CryptoProperties aCryptoProps)
  {
    ValueEnforcer.notNull (aCryptoProps, "CryptoProps");
    try
    {
      return CryptoFactory.getInstance (aCryptoProps.getAsProperties ());
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init crypto properties!", ex);
    }
  }

  /**
   * Lazily create a {@link Crypto} instance using the properties from
   * {@link #getCryptoProperties()}.
   *
   * @return A {@link Crypto} instance and never <code>null</code>.
   */
  @Nonnull
  public final Crypto getCrypto ()
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
                                                       m_aCryptoProps.getKeyStorePassword ())
                                        .getKeyStore ();
    }
    return ret;
  }

  @Nullable
  public final KeyStore.PrivateKeyEntry getPrivateKeyEntry ()
  {
    KeyStore.PrivateKeyEntry ret = m_aPK;
    if (ret == null)
    {
      final KeyStore aKeyStore = getKeyStore ();
      if (aKeyStore != null)
      {
        ret = m_aPK = KeyStoreHelper.loadPrivateKey (aKeyStore,
                                                     m_aCryptoProps.getKeyStorePath (),
                                                     m_aCryptoProps.getKeyAlias (),
                                                     m_aCryptoProps.getKeyPassword ().toCharArray ())
                                    .getKeyEntry ();
      }
    }
    return ret;
  }

  @Nullable
  public final X509Certificate getCertificate ()
  {
    final KeyStore.PrivateKeyEntry aPK = getPrivateKeyEntry ();
    return aPK == null ? null : (X509Certificate) aPK.getCertificate ();
  }

  @Nonnull
  public WSSConfig createWSSConfig ()
  {
    final WSSConfig ret = WSSConfig.getNewInstance ();
    ret.setIdAllocator (new WsuIdAllocator ()
    {
      public String createId (@Nullable final String sPrefix, final Object o)
      {
        return createSecureId (sPrefix, o);
      }

      public String createSecureId (final String sPrefix, final Object o)
      {
        return StringHelper.getConcatenatedOnDemand (sPrefix, "-", MessageHelperMethods.createRandomWSUID ());
      }
    });
    return ret;
  }
}
