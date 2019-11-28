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
package com.helger.phase4.crypto;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.commons.string.ToStringGenerator;
import com.helger.security.keystore.EKeyStoreType;

/**
 * Wrapper around the crypto properties file.
 *
 * @author bayerlma
 * @author Philip Helger
 */
@Immutable
public class AS4CryptoProperties implements Serializable
{
  /**
   * The class implementing the CryptoProvider - default values is
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

  private NonBlockingProperties m_aProps;

  /**
   * Constructor
   *
   * @param aProps
   *        key value pair map. May be <code>null</code>.
   */
  public AS4CryptoProperties (@Nullable final Map <String, String> aProps)
  {
    m_aProps = new NonBlockingProperties ();
    if (aProps != null)
      m_aProps.putAll (aProps);
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
      catch (final Throwable t)
      {
        throw new InitializationException ("Failed to init CryptoProperties from resource " + aRes + "!", t);
      }
  }

  /**
   * @return <code>true</code> if properties are available. This is always
   *         <code>true</code> when a map was used to initialize it. Only if a
   *         resource was used, this may become <code>false</code>.
   */
  public boolean isRead ()
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
    if (m_aProps == null)
      return null;
    return m_aProps.getProperty (sName);
  }

  @Nullable
  public EKeyStoreType getKeyStoreType ()
  {
    final String sProp = _getProperty (KEYSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sProp, EKeyStoreType.JKS);
  }

  @Nullable
  public String getKeyStorePath ()
  {
    return _getProperty (KEYSTORE_FILE);
  }

  @Nullable
  public String getKeyStorePassword ()
  {
    return _getProperty (KEYSTORE_PASSWORD);
  }

  @Nullable
  public String getKeyAlias ()
  {
    return _getProperty (KEY_ALIAS);
  }

  @Nullable
  public String getKeyPassword ()
  {
    return _getProperty (KEY_PASSWORD);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Props", m_aProps).getToString ();
  }
}
