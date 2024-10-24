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

import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringParser;
import com.helger.commons.string.ToStringGenerator;
import com.helger.config.IConfig;
import com.helger.phase4.config.AS4Configuration;
import com.helger.security.keystore.EKeyStoreType;

/**
 * Wrapper around the crypto properties file.<br>
 * Deprecated since v3. Please use {@link AS4KeyStoreDescriptor} and
 * {@link AS4TrustStoreDescriptor} in combination with
 * {@link AS4CryptoFactoryInMemoryKeyStore} instead.
 *
 * @author bayerlma
 * @author Philip Helger
 * @deprecated Use {@link AS4CryptoFactoryConfiguration} instead. This class
 *             will be removed in the next major release.
 */
@Immutable
@Deprecated (forRemoval = true, since = "3.0.0")
public class AS4CryptoProperties implements Serializable, ICloneable <AS4CryptoProperties>
{
  /**
   * The class name implementing the CryptoProvider - default value is
   * "org.apache.wss4j.common.crypto.Merlin"
   */
  public static final String CRYPTO_PROVIDER = "org.apache.wss4j.crypto.provider";
  /** Keystore type: JKS or PKCS12 */
  public static final String KEYSTORE_TYPE = "org.apache.wss4j.crypto.merlin.keystore.type";
  /** Keystore filename/path */
  public static final String KEYSTORE_FILE = "org.apache.wss4j.crypto.merlin.keystore.file";
  /** Keystore password */
  public static final String KEYSTORE_PASSWORD = "org.apache.wss4j.crypto.merlin.keystore.password";
  /** Keystore key alias */
  public static final String KEY_ALIAS = "org.apache.wss4j.crypto.merlin.keystore.alias";
  /** Keystore key password */
  public static final String KEY_PASSWORD = "org.apache.wss4j.crypto.merlin.keystore.private.password";

  /** Type boolean */
  public static final String LOAD_CACERTS = "org.apache.wss4j.crypto.merlin.load.cacerts";
  /** Truststore provider - must usually not be set */
  public static final String TRUSTSTORE_PROVIDER = "org.apache.wss4j.crypto.merlin.truststore.provider";
  /** Truststore type - JKS or PKCS12? */
  public static final String TRUSTSTORE_TYPE = "org.apache.wss4j.crypto.merlin.truststore.type";
  /** Truststore filename/path */
  public static final String TRUSTSTORE_FILE = "org.apache.wss4j.crypto.merlin.truststore.file";
  /** Truststore password */
  public static final String TRUSTSTORE_PASSWORD = "org.apache.wss4j.crypto.merlin.truststore.password";

  public static final EKeyStoreType DEFAULT_KEYSTORE_TYPE = CAS4Crypto.DEFAULT_KEY_STORE_TYPE;
  public static final EKeyStoreType DEFAULT_TRUSTSTORE_TYPE = CAS4Crypto.DEFAULT_TRUST_STORE_TYPE;

  private NonBlockingProperties m_aProps;

  /**
   * Default constructor having only the crypto provider property set to
   * default.
   */
  public AS4CryptoProperties ()
  {
    m_aProps = new NonBlockingProperties ();
    setCryptoProviderDefault ();
  }

  /**
   * Constructor reading the properties from a resource using the Properties
   * file syntax
   *
   * @param aRes
   *        The resource to read. May not be <code>null</code>.
   */
  public AS4CryptoProperties (@Nonnull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Resource");
    if (aRes.exists ())
      try
      {
        m_aProps = new NonBlockingProperties ();
        try (final InputStream aIS = aRes.getInputStream ())
        {
          m_aProps.load (aIS);
        }
      }
      catch (final Exception ex)
      {
        throw new InitializationException ("Failed to init CryptoProperties from resource " + aRes + "!", ex);
      }
  }

  /**
   * @return <code>true</code> if properties are available. This is always
   *         <code>true</code> when a map was used to initialize it. Only if a
   *         resource was used, this may become <code>false</code>.
   */
  public final boolean isRead ()
  {
    return m_aProps != null;
  }

  /**
   * @return A representation of the contained properties as {@link Properties}.
   *         May be <code>null</code>.
   */
  @Nullable
  public Properties getAsProperties ()
  {
    if (m_aProps == null)
      return null;

    final Properties ret = new Properties ();
    ret.putAll (m_aProps);
    return ret;
  }

  @Nullable
  private String _getProperty (@Nonnull final String sName)
  {
    final NonBlockingProperties aProps = m_aProps;
    return aProps == null ? null : aProps.getProperty (sName);
  }

  private void _setProperty (@Nonnull final String sName, @Nullable final String sValue)
  {
    ValueEnforcer.notNull (sName, "Name");
    NonBlockingProperties aProps = m_aProps;
    if (sValue == null)
    {
      // Remove property
      if (aProps != null)
        aProps.remove (sName);
    }
    else
    {
      // Set property
      if (aProps == null)
        aProps = m_aProps = new NonBlockingProperties ();
      aProps.put (sName, sValue);
    }
  }

  @Nullable
  public String getCryptoProvider ()
  {
    return _getProperty (CRYPTO_PROVIDER);
  }

  @Nonnull
  public final AS4CryptoProperties setCryptoProvider (@Nullable final String sCryptoProvider)
  {
    _setProperty (CRYPTO_PROVIDER, sCryptoProvider);
    return this;
  }

  @Nonnull
  public final AS4CryptoProperties setCryptoProviderDefault ()
  {
    return setCryptoProvider (org.apache.wss4j.common.crypto.Merlin.class.getName ());
  }

  @Nonnull
  public EKeyStoreType getKeyStoreType ()
  {
    final String sProp = _getProperty (KEYSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sProp, DEFAULT_KEYSTORE_TYPE);
  }

  @Nonnull
  public final AS4CryptoProperties setKeyStoreType (@Nullable final EKeyStoreType eType)
  {
    _setProperty (KEYSTORE_TYPE, eType == null ? null : eType.getID ());
    return this;
  }

  @Nullable
  public String getKeyStorePath ()
  {
    return _getProperty (KEYSTORE_FILE);
  }

  @Nonnull
  public final AS4CryptoProperties setKeyStorePath (@Nullable final String sKeyStorePath)
  {
    _setProperty (KEYSTORE_FILE, sKeyStorePath);
    return this;
  }

  @Nullable
  public String getKeyStorePassword ()
  {
    return _getProperty (KEYSTORE_PASSWORD);
  }

  @Nonnull
  public final AS4CryptoProperties setKeyStorePassword (@Nullable final String sKeyStorePassword)
  {
    _setProperty (KEYSTORE_PASSWORD, sKeyStorePassword);
    return this;
  }

  @Nullable
  public String getKeyAlias ()
  {
    return _getProperty (KEY_ALIAS);
  }

  @Nonnull
  public final AS4CryptoProperties setKeyAlias (@Nullable final String sKeyAlias)
  {
    _setProperty (KEY_ALIAS, sKeyAlias);
    return this;
  }

  @Nullable
  public String getKeyPassword ()
  {
    return _getProperty (KEY_PASSWORD);
  }

  @Nullable
  public char [] getKeyPasswordCharArray ()
  {
    final String s = getKeyPassword ();
    return s == null ? null : s.toCharArray ();
  }

  @Nonnull
  public final AS4CryptoProperties setKeyPassword (@Nullable final String sKeyPassword)
  {
    _setProperty (KEY_PASSWORD, sKeyPassword);
    return this;
  }

  @Nonnull
  public ETriState getLoadCACerts ()
  {
    final String sProp = _getProperty (LOAD_CACERTS);
    return sProp == null ? ETriState.UNDEFINED : ETriState.valueOf (StringParser.parseBool (sProp));
  }

  @Nonnull
  public final AS4CryptoProperties setLoadCACerts (final boolean bLoadCACerts)
  {
    _setProperty (LOAD_CACERTS, Boolean.toString (bLoadCACerts));
    return this;
  }

  @Nullable
  public String getTrustStoreProvider ()
  {
    return _getProperty (TRUSTSTORE_PROVIDER);
  }

  @Nonnull
  public final AS4CryptoProperties setTrustStoreProvider (@Nullable final String sTrustStoreProvider)
  {
    _setProperty (TRUSTSTORE_PROVIDER, sTrustStoreProvider);
    return this;
  }

  @Nonnull
  public EKeyStoreType getTrustStoreType ()
  {
    final String sProp = _getProperty (TRUSTSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sProp, DEFAULT_TRUSTSTORE_TYPE);
  }

  @Nonnull
  public final AS4CryptoProperties setTrustStoreType (@Nullable final EKeyStoreType eType)
  {
    _setProperty (TRUSTSTORE_TYPE, eType == null ? null : eType.getID ());
    return this;
  }

  @Nullable
  public String getTrustStorePath ()
  {
    return _getProperty (TRUSTSTORE_FILE);
  }

  @Nonnull
  public final AS4CryptoProperties setTrustStorePath (@Nullable final String sTrustStorePath)
  {
    _setProperty (TRUSTSTORE_FILE, sTrustStorePath);
    return this;
  }

  @Nullable
  public String getTrustStorePassword ()
  {
    return _getProperty (TRUSTSTORE_PASSWORD);
  }

  @Nonnull
  public final AS4CryptoProperties setTrustStorePassword (@Nullable final String sTrustStorePassword)
  {
    _setProperty (TRUSTSTORE_PASSWORD, sTrustStorePassword);
    return this;
  }

  @Nonnull
  @ReturnsMutableCopy
  public AS4CryptoProperties getClone ()
  {
    final AS4CryptoProperties ret = new AS4CryptoProperties ();
    ret.m_aProps.setAll (m_aProps);
    return ret;
  }

  @Override
  public String toString ()
  {
    // May contain a password property
    return new ToStringGenerator (this).append ("Props", m_aProps).getToString ();
  }

  /**
   * @return A new {@link AS4CryptoProperties} object filled with all values
   *         from the global configuration file. Values not present in the
   *         configuration are not set and stay with their default values.
   * @since 0.11.0
   */
  @Nonnull
  public static AS4CryptoProperties createFromConfig ()
  {
    final IConfig aConfig = AS4Configuration.getConfig ();
    final AS4CryptoProperties ret = new AS4CryptoProperties ();
    for (final String sKey : new String [] { CRYPTO_PROVIDER,
                                             KEYSTORE_TYPE,
                                             KEYSTORE_FILE,
                                             KEYSTORE_PASSWORD,
                                             KEY_ALIAS,
                                             KEY_PASSWORD,
                                             LOAD_CACERTS,
                                             TRUSTSTORE_PROVIDER,
                                             TRUSTSTORE_TYPE,
                                             TRUSTSTORE_FILE,
                                             TRUSTSTORE_PASSWORD })
    {
      final String sConfigValue = aConfig.getAsString (sKey);
      if (sConfigValue != null)
        ret.m_aProps.put (sKey, sConfigValue);
    }
    return ret;
  }
}
