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
