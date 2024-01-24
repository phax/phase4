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

import javax.annotation.Nonnull;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;

/**
 * Base interface for an encryption algorithm.
 *
 * @author Philip Helger
 * @since v1.4.4
 */
public interface ICryptoAlgorithmCrypt extends IHasID <String>
{
  /**
   * This is the internal algorithm ID for resolution.
   */
  @Nonnull
  @Nonempty
  String getID ();

  /**
   * @return The OID of the algorithm to be used by the Security Provider.
   */
  @Nonnull
  ASN1ObjectIdentifier getOID ();

  /**
   * @return The unique XMLDsig algorithm URI for this algorithm (as in
   *         <code>http://www.w3.org/2001/04/xmldsig-more#rsa-sha256</code>)
   */
  @Nonnull
  @Nonempty
  String getAlgorithmURI ();
}
