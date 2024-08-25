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
package com.helger.phase4.incoming.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;

/**
 * Interface to configure the security configuration for incoming messages.
 *
 * @author Philip Helger
 * @since 2.1.3
 */
public interface IAS4IncomingSecurityConfiguration
{
  /**
   * @return The signing parameters to be used for incoming messages. May be
   *         <code>null</code>.
   * @since 2.3.0
   */
  @Nullable
  AS4SigningParams getSigningParams ();

  /**
   * @return A clone of the existing signing parameters or a new object. Never
   *         <code>null</code>.
   * @since 2.3.0
   */
  @Nonnull
  default AS4SigningParams getSigningParamsCloneOrNew ()
  {
    final AS4SigningParams a = getSigningParams ();
    return a == null ? new AS4SigningParams () : a.getClone ();
  }

  /**
   * @return The crypt parameters to be used for incoming messages. May be
   *         <code>null</code>.
   * @since 2.3.0
   */
  @Nullable
  AS4CryptParams getCryptParams ();

  /**
   * @return A clone of the existing crypt parameters or a new object. Never
   *         <code>null</code>.
   * @since 2.3.0
   */
  @Nonnull
  default AS4CryptParams getCryptParamsCloneOrNew ()
  {
    final AS4CryptParams a = getCryptParams ();
    return a == null ? new AS4CryptParams () : a.getClone ();
  }

  /**
   * @return An optional modifier to customize WSS4J
   *         {@link org.apache.wss4j.dom.handler.RequestData} objects for
   *         decrypting. This may e.g. be used to allow RSA 1.5 algorithms.
   * @since 2.2.0
   */
  @Nullable
  IAS4DecryptParameterModifier getDecryptParameterModifier ();
}
