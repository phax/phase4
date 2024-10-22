package com.helger.phase4.crypto;

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
import com.helger.security.keystore.LoadedKeyStore;

/**
 * The default implementation of {@link IAS4TrustStoreDescriptor}.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public class AS4TrustStoreDescriptor implements IAS4TrustStoreDescriptor
{
  private final IKeyStoreType m_aType;
  private final String m_sPath;
  private final char [] m_aPassword;
  private final Provider m_aProvider;
  // Lazily initialized
  private LoadedKeyStore m_aLTS;

  public AS4TrustStoreDescriptor (@Nonnull final IKeyStoreType aType,
                                  @Nonnull @Nonempty final String sPath,
                                  @Nonnull final char [] aPassword,
                                  @Nullable final Provider aProvider)
  {
    ValueEnforcer.notNull (aType, "Type");
    ValueEnforcer.notEmpty (sPath, "Path");
    ValueEnforcer.notNull (aPassword, "Password");
    m_aType = aType;
    m_sPath = sPath;
    m_aPassword = aPassword;
    m_aProvider = aProvider;
  }

  @Nonnull
  public IKeyStoreType getTrustStoreType ()
  {
    return m_aType;
  }

  @Nonnull
  @Nonempty
  public String getTrustStorePath ()
  {
    return m_sPath;
  }

  @Nonnull
  @ReturnsMutableObject
  public char [] getTrustStorePassword ()
  {
    return m_aPassword;
  }

  @Nullable
  public Provider getProvider ()
  {
    return m_aProvider;
  }

  @Nonnull
  public LoadedKeyStore loadTrustStore ()
  {
    LoadedKeyStore ret = m_aLTS;
    if (ret == null)
      ret = m_aLTS = KeyStoreHelper.loadKeyStore (m_aType, m_sPath, new String (m_aPassword), m_aProvider);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Type", m_aType)
                                       .append ("Path", m_sPath)
                                       .appendPassword ("Password")
                                       .appendIfNotNull ("Provider", m_aProvider)
                                       .getToString ();
  }

  /**
   * Create the trust store descriptor from the default configuration item. The
   * following configuration properties are used, relative to the default
   * configuration prefix:
   * <ul>
   * <li><code>truststore.type</code> - the trust store type</li>
   * <li><code>truststore.file</code> - the trust store path</li>
   * <li><code>truststore.password</code> - the trust store password</li>
   * password</li>
   * </ul>
   *
   * @return A new {@link AS4TrustStoreDescriptor} object or <code>null</code>
   *         if path or password are not present.
   */
  @Nullable
  public static AS4TrustStoreDescriptor createFromConfig ()
  {
    return createFromConfig (AS4Configuration.getConfig (), CAS4Crypto.DEFAULT_CONFIG_PREFIX, null);
  }

  /**
   * Create the trust store descriptor from the provided configuration item. The
   * following configuration properties are used, relative to the configuration
   * prefix:
   * <ul>
   * <li><code>truststore.type</code> - the trust store type</li>
   * <li><code>truststore.file</code> - the trust store path</li>
   * <li><code>truststore.password</code> - the trust store password</li>
   * password</li>
   * </ul>
   *
   * @param aConfig
   *        The configuration object to be used. May not be <code>null</code>.
   * @param sConfigPrefix
   *        The configuration prefix to be used. May neither be
   *        <code>null</code> nor empty and must end with a dot ('.').
   * @param aProvider
   *        The Java security provider for loading the trust store. May be
   *        <code>null</code> to use the default.
   * @return A new {@link AS4TrustStoreDescriptor} object or <code>null</code>
   *         if path or password are not present.
   */
  @Nullable
  public static AS4TrustStoreDescriptor createFromConfig (@Nonnull final IConfigWithFallback aConfig,
                                                          @Nonnull @Nonempty final String sConfigPrefix,
                                                          @Nullable final Provider aProvider)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigPrefix, "ConfigPrefix");
    ValueEnforcer.isTrue ( () -> StringHelper.endsWith (sConfigPrefix, '.'), "ConfigPrefix must end with a dot");

    // Trust Store
    final String sType = aConfig.getAsString (sConfigPrefix + "truststore.type");
    final EKeyStoreType aType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType,
                                                                                 CAS4Crypto.DEFAULT_TRUST_STORE_TYPE);
    final String sPath = aConfig.getAsString (sConfigPrefix + "truststore.file");
    final char [] aPassword = aConfig.getAsCharArray (sConfigPrefix + "truststore.password");

    // No trust store configured
    if (StringHelper.hasNoText (sPath) || aPassword == null)
      return null;

    return new AS4TrustStoreDescriptor (aType, sPath, aPassword, aProvider);
  }

  /**
   * @return A new builder for {@link AS4TrustStoreDescriptor} objects. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4TrustStoreDescriptorBuilder builder ()
  {
    return new AS4TrustStoreDescriptorBuilder ();
  }

  /**
   * Create a new builder using the provided descriptor.
   *
   * @param a
   *        The existing descriptor. May not be <code>null</code>.
   * @return A new builder for {@link AS4TrustStoreDescriptor} objects. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static AS4TrustStoreDescriptorBuilder builder (@Nonnull final AS4TrustStoreDescriptor a)
  {
    return new AS4TrustStoreDescriptorBuilder (a);
  }

  /**
   * Builder class for class {@link AS4TrustStoreDescriptor}.
   *
   * @author Philip Helger
   */
  public static class AS4TrustStoreDescriptorBuilder implements IBuilder <AS4TrustStoreDescriptor>
  {
    private IKeyStoreType m_aType;
    private String m_sPath;
    private char [] m_aPassword;
    private Provider m_aProvider;

    public AS4TrustStoreDescriptorBuilder ()
    {}

    public AS4TrustStoreDescriptorBuilder (@Nonnull final AS4TrustStoreDescriptor a)
    {
      type (a.m_aType).path (a.m_sPath).password (a.m_aPassword).provider (m_aProvider);
    }

    @Nonnull
    public final AS4TrustStoreDescriptorBuilder type (@Nullable final IKeyStoreType a)
    {
      m_aType = a;
      return this;
    }

    @Nonnull
    public final AS4TrustStoreDescriptorBuilder path (@Nullable final String s)
    {
      m_sPath = s;
      return this;
    }

    @Nonnull
    public final AS4TrustStoreDescriptorBuilder password (@Nullable final String s)
    {
      return password (s == null ? null : s.toCharArray ());
    }

    @Nonnull
    public final AS4TrustStoreDescriptorBuilder password (@Nullable final char [] a)
    {
      m_aPassword = a;
      return this;
    }

    @Nonnull
    public final AS4TrustStoreDescriptorBuilder provider (@Nullable final Provider a)
    {
      m_aProvider = a;
      return this;
    }

    @Nonnull
    public AS4TrustStoreDescriptor build ()
    {
      if (m_aType == null)
        throw new IllegalStateException ("Type is missing");
      if (StringHelper.hasNoText (m_sPath))
        throw new IllegalStateException ("Path is empty");
      if (m_aPassword == null)
        throw new IllegalStateException ("Password is missing");
      // Provider may be null
      return new AS4TrustStoreDescriptor (m_aType, m_sPath, m_aPassword, m_aProvider);
    }
  }
}
