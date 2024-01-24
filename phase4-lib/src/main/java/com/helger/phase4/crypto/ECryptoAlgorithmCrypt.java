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
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSS4JConstants;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;

/**
 * Enumeration with all message encryption algorithms supported.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmCrypt implements ICryptoAlgorithmCrypt
{
  CRYPT_3DES ("3des", CMSAlgorithm.DES_EDE3_CBC, WSS4JConstants.TRIPLE_DES),
  AES_128_CBC ("aes128-cbc", CMSAlgorithm.AES128_CBC, WSS4JConstants.AES_128),
  AES_128_GCM ("aes128-gcm", CMSAlgorithm.AES128_GCM, WSS4JConstants.AES_128_GCM),
  AES_192_CBC ("aes192-cbc", CMSAlgorithm.AES192_CBC, WSS4JConstants.AES_192),
  AES_192_GCM ("aes192-gcm", CMSAlgorithm.AES192_GCM, WSS4JConstants.AES_192_GCM),
  AES_256_CBC ("aes256-cbc", CMSAlgorithm.AES256_CBC, WSS4JConstants.AES_256),
  AES_256_GCM ("aes256-gcm", CMSAlgorithm.AES256_GCM, WSS4JConstants.AES_256_GCM);

  /** Default encrypt algorithm */
  public static final ECryptoAlgorithmCrypt ENCRPYTION_ALGORITHM_DEFAULT = AES_128_GCM;

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;
  private final String m_sAlgorithmURI;

  ECryptoAlgorithmCrypt (@Nonnull @Nonempty final String sID,
                         @Nonnull final ASN1ObjectIdentifier aOID,
                         @Nonnull @Nonempty final String sAlgorithmURI)
  {
    m_sID = sID;
    m_aOID = aOID;
    m_sAlgorithmURI = sAlgorithmURI;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ASN1ObjectIdentifier getOID ()
  {
    return m_aOID;
  }

  /**
   * @return The algorithm ID for XMLDsig base encryption
   */
  @Nonnull
  @Nonempty
  public String getAlgorithmURI ()
  {
    return m_sAlgorithmURI;
  }

  @Nullable
  public static ECryptoAlgorithmCrypt getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithmCrypt.class, sID);
  }

  @Nonnull
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
