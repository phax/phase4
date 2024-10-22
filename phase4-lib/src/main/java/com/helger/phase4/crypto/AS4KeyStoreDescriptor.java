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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.builder.IBuilder;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * The default implementation of {@link IAS4KeyStoreDescriptor}.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public class AS4KeyStoreDescriptor implements IAS4KeyStoreDescriptor
{
  private final IKeyStoreType m_aType;
  private final String m_sPath;
  private final char [] m_aPassword;
  private final Provider m_aProvider;
  private final String m_sKeyAlias;
  private final char [] m_aKeyPassword;
  // Lazily initialized
  private LoadedKeyStore m_aLKS;
  private LoadedKey <PrivateKeyEntry> m_aLK;

  public AS4KeyStoreDescriptor (@Nonnull final IKeyStoreType aType,
                                @Nonnull @Nonempty final String sPath,
                                @Nonnull final char [] aPassword,
                                @Nullable final Provider aProvider,
                                @Nonnull @Nonempty final String sKeyAlias,
                                @Nonnull final char [] aKeyPassword)
  {
    ValueEnforcer.notNull (aType, "Type");
    ValueEnforcer.notEmpty (sPath, "Path");
    ValueEnforcer.notNull (aPassword, "Password");
    ValueEnforcer.notEmpty (sKeyAlias, "KeyAlias");
    ValueEnforcer.notNull (aKeyPassword, "KeyPassword");
    m_aType = aType;
    m_sPath = sPath;
    m_aPassword = aPassword;
    m_aProvider = aProvider;
    m_sKeyAlias = sKeyAlias;
    m_aKeyPassword = aKeyPassword;
  }

  @Nonnull
  public IKeyStoreType getKeyStoreType ()
  {
    return m_aType;
  }

  @Nonnull
  @Nonempty
  public String getKeyStorePath ()
  {
    return m_sPath;
  }

  @Nonnull
  @ReturnsMutableObject
  public char [] getKeyStorePassword ()
  {
    return m_aPassword;
  }

  @Nullable
  public Provider getProvider ()
  {
    return m_aProvider;
  }

  @Nonnull
  public LoadedKeyStore loadKeyStore ()
  {
    LoadedKeyStore ret = m_aLKS;
    if (ret == null)
      ret = m_aLKS = KeyStoreHelper.loadKeyStore (m_aType, m_sPath, new String (m_aPassword), m_aProvider);
    return ret;
  }

  @Nonnull
  @Nonempty
  public String getKeyAlias ()
  {
    return m_sKeyAlias;
  }

  @Nonnull
  @ReturnsMutableObject
  public char [] getKeyPassword ()
  {
    return m_aKeyPassword;
  }

  @Nonnull
  public LoadedKey <PrivateKeyEntry> loadKey ()
  {
    LoadedKey <PrivateKeyEntry> ret = m_aLK;
    if (ret == null)
    {
      ret = m_aLK = KeyStoreHelper.loadPrivateKey (loadKeyStore ().getKeyStore (),
                                                   m_sPath,
                                                   m_sKeyAlias,
                                                   m_aKeyPassword);
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Type", m_aType)
                                       .append ("Path", m_sPath)
                                       .appendPassword ("Password")
                                       .appendIfNotNull ("Provider", m_aProvider)
                                       .append ("KeyAlias", m_sKeyAlias)
                                       .appendPassword ("KeyPassword")
                                       .getToString ();
  }

  /**
   * Create the key store descriptor from the default configuration item. The
   * following configuration properties are used, relative to the default
   * configuration prefix:
   * <ul>
   * <li><code>keystore.type</code> - the key store type</li>
   * <li><code>keystore.file</code> - the key store path</li>
   * <li><code>keystore.password</code> - the key store password</li>
   * <li><code>keystore.alias</code> - the key store alias</li>
   * <li><code>keystore.private.password</code> - the key store key
   * password</li>
   * </ul>
   *
   * @return A new {@link AS4KeyStoreDescriptor} object and never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4KeyStoreDescriptor createFromConfig ()
  {
    return createFromConfig (AS4Configuration.getConfig (), CAS4Crypto.DEFAULT_CONFIG_PREFIX, null);
  }

  /**
   * Create the key store descriptor from the provided configuration item. The
   * following configuration properties are used, relative to the configuration
   * prefix:
   * <ul>
   * <li><code>keystore.type</code> - the key store type</li>
   * <li><code>keystore.file</code> - the key store path</li>
   * <li><code>keystore.password</code> - the key store password</li>
   * <li><code>keystore.alias</code> - the key store alias</li>
   * <li><code>keystore.private.password</code> - the key store key
   * password</li>
   * </ul>
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   * @param sConfigPrefix
   *        The configuration prefix to be used. May neither be
   *        <code>null</code> nor empty and must end with a dot ('.').
   * @param aProvider
   *        The Java security provider for loading the key store. May be
   *        <code>null</code> to use the default.
   * @return A new {@link AS4KeyStoreDescriptor} object and never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4KeyStoreDescriptor createFromConfig (@Nonnull final IConfigWithFallback aConfig,
                                                        @Nonnull @Nonempty final String sConfigPrefix,
                                                        @Nullable final Provider aProvider)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigPrefix, "ConfigPrefix");
    ValueEnforcer.isTrue ( () -> StringHelper.endsWith (sConfigPrefix, '.'), "ConfigPrefix must end with a dot");

    // Key Store
    final String sType = aConfig.getAsString (sConfigPrefix + "keystore.type");
    final EKeyStoreType aType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType,
                                                                                 CAS4Crypto.DEFAULT_KEY_STORE_TYPE);
    final String sPath = aConfig.getAsString (sConfigPrefix + "keystore.file");
    final char [] aPassword = aConfig.getAsCharArray (sConfigPrefix + "keystore.password");

    // Key Store Key
    final String sKeyAlias = aConfig.getAsString (sConfigPrefix + "keystore.alias");
    final char [] aKeyPassword = aConfig.getAsCharArray (sConfigPrefix + "keystore.private.password");

    return new AS4KeyStoreDescriptor (aType, sPath, aPassword, aProvider, sKeyAlias, aKeyPassword);
  }

  /**
   * @return A new builder for {@link AS4KeyStoreDescriptor} objects. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4KeyStoreDescriptorBuilder builder ()
  {
    return new AS4KeyStoreDescriptorBuilder ();
  }

  /**
   * Create a new builder using the provided descriptor.
   *
   * @param a
   *        The existing descriptor. May not be <code>null</code>.
   * @return A new builder for {@link AS4KeyStoreDescriptor} objects. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4KeyStoreDescriptorBuilder builder (@Nonnull final AS4KeyStoreDescriptor a)
  {
    return new AS4KeyStoreDescriptorBuilder (a);
  }

  /**
   * Builder class for class {@link AS4KeyStoreDescriptor}.
   *
   * @author Philip Helger
   */
  public static class AS4KeyStoreDescriptorBuilder implements IBuilder <AS4KeyStoreDescriptor>
  {
    private IKeyStoreType m_aType;
    private String m_sPath;
    private char [] m_aPassword;
    private Provider m_aProvider;
    private String m_sKeyAlias;
    private char [] m_aKeyPassword;

    public AS4KeyStoreDescriptorBuilder ()
    {}

    public AS4KeyStoreDescriptorBuilder (@Nonnull final AS4KeyStoreDescriptor a)
    {
      type (a.m_aType).path (a.m_sPath)
                      .password (a.m_aPassword)
                      .provider (m_aProvider)
                      .keyAlias (m_sKeyAlias)
                      .keyPassword (m_aKeyPassword);
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder type (@Nullable final IKeyStoreType a)
    {
      m_aType = a;
      return this;
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder path (@Nullable final String s)
    {
      m_sPath = s;
      return this;
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder password (@Nullable final String s)
    {
      return password (s == null ? null : s.toCharArray ());
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder password (@Nullable final char [] a)
    {
      m_aPassword = a;
      return this;
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder provider (@Nullable final Provider a)
    {
      m_aProvider = a;
      return this;
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder keyAlias (@Nullable final String s)
    {
      m_sKeyAlias = s;
      return this;
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder keyPassword (@Nullable final String s)
    {
      return keyPassword (s == null ? null : s.toCharArray ());
    }

    @Nonnull
    public final AS4KeyStoreDescriptorBuilder keyPassword (@Nullable final char [] a)
    {
      m_aKeyPassword = a;
      return this;
    }

    @Nonnull
    public AS4KeyStoreDescriptor build ()
    {
      if (m_aType == null)
        throw new IllegalStateException ("Type is missing");
      if (StringHelper.hasNoText (m_sPath))
        throw new IllegalStateException ("Path is empty");
      if (m_aPassword == null)
        throw new IllegalStateException ("Password is missing");
      // Provider may be null
      if (StringHelper.hasNoText (m_sKeyAlias))
        throw new IllegalStateException ("KeyAlias is empty");
      if (m_aKeyPassword == null)
        throw new IllegalStateException ("KeyPassword is missing");
      return new AS4KeyStoreDescriptor (m_aType, m_sPath, m_aPassword, m_aProvider, m_sKeyAlias, m_aKeyPassword);
    }
  }
}
