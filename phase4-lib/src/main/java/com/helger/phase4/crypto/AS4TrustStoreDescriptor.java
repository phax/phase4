package com.helger.phase4.crypto;

import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
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
   * @return A new {@link AS4TrustStoreDescriptor} object and never
   *         <code>null</code>.
   */
  @Nonnull
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

    return new AS4TrustStoreDescriptor (aType, sPath, aPassword, aProvider);
  }
}
