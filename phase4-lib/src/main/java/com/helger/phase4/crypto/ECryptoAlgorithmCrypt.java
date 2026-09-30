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
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.lang.EnumHelper;

/**
 * Enumeration with all message encryption algorithms supported.
 * <p>
 * Direct use of this enum, except for the deprecated {@link #getOID()} method, does not require
 * Bouncy Castle. Reflection and AOT tools that eagerly resolve all method descriptors still require
 * it for binary compatibility with that method.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmCrypt implements ICryptoAlgorithmCrypt
{
  CRYPT_3DES ("3des", "1.2.840.113549.3.7", WSS4JConstants.TRIPLE_DES, 192),
  AES_128_CBC ("aes128-cbc", "2.16.840.1.101.3.4.1.2", WSS4JConstants.AES_128, 128),
  AES_128_GCM ("aes128-gcm", "2.16.840.1.101.3.4.1.6", WSS4JConstants.AES_128_GCM, 128),
  AES_192_CBC ("aes192-cbc", "2.16.840.1.101.3.4.1.22", WSS4JConstants.AES_192, 192),
  AES_192_GCM ("aes192-gcm", "2.16.840.1.101.3.4.1.26", WSS4JConstants.AES_192_GCM, 192),
  AES_256_CBC ("aes256-cbc", "2.16.840.1.101.3.4.1.42", WSS4JConstants.AES_256, 256),
  AES_256_GCM ("aes256-gcm", "2.16.840.1.101.3.4.1.46", WSS4JConstants.AES_256_GCM, 256);

  /** Default encrypt algorithm */
  public static final ECryptoAlgorithmCrypt ENCRYPTION_ALGORITHM_DEFAULT = AES_128_GCM;

  /** @deprecated Use {@link #ENCRYPTION_ALGORITHM_DEFAULT} instead - typo in name */
  @Deprecated (since = "4.4.0", forRemoval = true)
  public static final ECryptoAlgorithmCrypt ENCRPYTION_ALGORITHM_DEFAULT = ENCRYPTION_ALGORITHM_DEFAULT;

  private final String m_sID;
  private final String m_sOID;
  private final String m_sAlgorithmURI;
  private final int m_nKeySizeBits;
  private volatile ASN1ObjectIdentifier m_aOID;

  ECryptoAlgorithmCrypt (@NonNull @Nonempty final String sID,
                         @NonNull @Nonempty final String sOID,
                         @NonNull @Nonempty final String sAlgorithmURI,
                         @Nonnegative final int nKeySizeBits)
  {
    m_sID = sID;
    m_sOID = sOID;
    m_sAlgorithmURI = sAlgorithmURI;
    m_nKeySizeBits = nKeySizeBits;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getOIDString ()
  {
    return m_sOID;
  }

  @NonNull
  @Deprecated (since = "4.6.1")
  public ASN1ObjectIdentifier getOID ()
  {
    ASN1ObjectIdentifier ret = m_aOID;
    if (ret == null)
      synchronized (this)
      {
        ret = m_aOID;
        if (ret == null)
          m_aOID = ret = new ASN1ObjectIdentifier (m_sOID).intern ();
      }
    return ret;
  }

  /**
   * @return The algorithm ID for XMLDsig base encryption
   */
  @NonNull
  @Nonempty
  public String getAlgorithmURI ()
  {
    return m_sAlgorithmURI;
  }

  /**
   * @return The size of the symmetric key this algorithm requires, in bits. E.g. 256 for
   *         AES-256-GCM.
   * @since 4.7.1
   */
  @Nonnegative
  public int getKeySizeBits ()
  {
    return m_nKeySizeBits;
  }

  @Nullable
  public static ECryptoAlgorithmCrypt getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithmCrypt.class, sID);
  }

  @NonNull
  public static ECryptoAlgorithmCrypt getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (ECryptoAlgorithmCrypt.class, sID);
  }

  @Nullable
  public static ECryptoAlgorithmCrypt getFromIDOrDefault (@Nullable final String sID,
                                                          @Nullable final ECryptoAlgorithmCrypt eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoAlgorithmCrypt.class, sID, eDefault);
  }
}
