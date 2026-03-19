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

import org.apache.wss4j.common.WSS4JConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Enumeration of key agreement methods for XML Encryption. Key agreement is an alternative to key
 * transport (e.g. RSA-OAEP) where both parties contribute to deriving a shared secret.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public enum ECryptoKeyAgreementMethod implements IHasID <String>
{
  /** ECDH-ES key agreement (generic, for EC keys on standard curves like secp256r1) */
  ECDH_ES (WSS4JConstants.AGREEMENT_METHOD_ECDH_ES),
  /** X25519 key agreement (Curve25519, eDelivery AS4 2.0 Common Usage Profile) */
  X25519 (WSS4JConstants.AGREEMENT_METHOD_X25519),
  /** X448 key agreement (Curve448) */
  X448 (WSS4JConstants.AGREEMENT_METHOD_X448);

  private final String m_sID;

  ECryptoKeyAgreementMethod (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ECryptoKeyAgreementMethod getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoKeyAgreementMethod.class, sID);
  }

  @Nullable
  public static ECryptoKeyAgreementMethod getFromIDOrDefault (@Nullable final String sID,
                                                              @Nullable final ECryptoKeyAgreementMethod eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoKeyAgreementMethod.class, sID, eDefault);
  }
}
