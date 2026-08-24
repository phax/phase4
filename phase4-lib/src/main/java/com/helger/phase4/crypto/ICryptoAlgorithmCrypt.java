/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;

/**
 * Base interface for an encryption algorithm.
 * <p>
 * Implementations that override {@link #getOIDString()} can support direct use without Bouncy
 * Castle. This interface nevertheless retains the deprecated {@link #getOID()} method for binary
 * compatibility. Reflection and AOT tools that eagerly resolve every method descriptor therefore
 * still require Bouncy Castle until that method can be removed in a future major release.
 *
 * @author Philip Helger
 * @since v1.4.4
 */
public interface ICryptoAlgorithmCrypt extends IHasID <String>
{
  /**
   * This is the internal algorithm ID for resolution.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The OID of the algorithm to be used by the Security Provider.
   * @deprecated Use {@link #getOIDString()} instead. This compatibility method requires Bouncy
   *             Castle to be present at runtime.
   */
  @NonNull
  @Deprecated (since = "4.6.1", forRemoval = true)
  ASN1ObjectIdentifier getOID ();

  /**
   * Implementations should override this method to make direct invocation independent of Bouncy
   * Castle. The default implementation delegates to {@link #getOID()} for binary compatibility with
   * existing implementations.
   *
   * @return The OID of the algorithm in dot-decimal notation.
   * @since 4.6.1
   */
  @NonNull
  @Nonempty
  default String getOIDString ()
  {
    return getOID ().getId ();
  }

  /**
   * @return The unique XMLDsig algorithm URI for this algorithm (as in
   *         <code>http://www.w3.org/2001/04/xmldsig-more#rsa-sha256</code>)
   */
  @NonNull
  @Nonempty
  String getAlgorithmURI ();
}
