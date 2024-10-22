package com.helger.phase4.crypto;

import java.security.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * Interface describing the parameters needed to reference a trust store.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IAS4TrustStoreDescriptor
{
  /**
   * @return The type of the trust store. May not be <code>null</code>.
   */
  @Nonnull
  IKeyStoreType getTrustStoreType ();

  /**
   * @return The path to the trust store. May neither be <code>null</code> nor
   *         empty. The interpretation of the path is implementation dependent.
   */
  @Nonnull
  @Nonempty
  String getTrustStorePath ();

  /**
   * @return The password required to open the trust store. May not be
   *         <code>null</code> but may be empty.
   */
  @Nonnull
  char [] getTrustStorePassword ();

  /**
   * @return The Java security provider for loading the trust store. May be
   *         <code>null</code>.
   */
  @Nullable
  Provider getProvider ();

  /**
   * @return The loaded trust store based on the parameters in this descriptor.
   *         Never <code>null</code>.
   */
  @Nonnull
  LoadedKeyStore loadTrustStore ();
}
