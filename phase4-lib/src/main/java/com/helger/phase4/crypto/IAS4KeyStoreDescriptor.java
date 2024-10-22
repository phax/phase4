package com.helger.phase4.crypto;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * Interface describing the parameters needed to reference a key store.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IAS4KeyStoreDescriptor
{
  /**
   * @return The type of the key store. May not be <code>null</code>.
   */
  @Nonnull
  IKeyStoreType getKeyStoreType ();

  /**
   * @return The path to the key store. May neither be <code>null</code> nor
   *         empty. The interpretation of the path is implementation dependent.
   */
  @Nonnull
  @Nonempty
  String getKeyStorePath ();

  /**
   * @return The password required to open the key store. May not be
   *         <code>null</code> but may be empty.
   */
  @Nonnull
  char [] getKeyStorePassword ();

  /**
   * @return The Java security provider for loading the key store. May be
   *         <code>null</code>.
   */
  @Nullable
  Provider getProvider ();

  /**
   * @return The loaded key store based on the parameters in this descriptor.
   *         Never <code>null</code>.
   */
  @Nonnull
  LoadedKeyStore loadKeyStore ();

  /**
   * Note: the case sensitivity of the key alias depends on the key store type.
   *
   * @return The alias of the key inside a key store. May neither be
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getKeyAlias ();

  /**
   * @return The password required to access the key inside the key store. May
   *         not be <code>null</code> but may be empty.
   */
  @Nonnull
  char [] getKeyPassword ();

  /**
   * @return The loaded key based on the loaded key store and the parameters in
   *         this descriptor.
   */
  @Nonnull
  LoadedKey <PrivateKeyEntry> loadKey ();
}
