/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSAlgorithm;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Enumeration with all message encryption algorithms supported.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmCrypt implements IHasID <String>
{
  CRYPT_3DES ("3des", PKCSObjectIdentifiers.des_EDE3_CBC),
  CRYPT_CAST5 ("cast5", CMSAlgorithm.CAST5_CBC),
  CRYPT_IDEA ("idea", CMSAlgorithm.IDEA_CBC),
  CRYPT_RC2 ("rc2", PKCSObjectIdentifiers.RC2_CBC),
  AES_128_CBC ("aes128-cbc", CMSAlgorithm.AES128_CBC),
  AES_128_CCM ("aes128-ccm", CMSAlgorithm.AES128_CCM),
  AES_128_GCM ("aes128-gcm", CMSAlgorithm.AES128_GCM),
  AES_192_CBC ("aes192-cbc", CMSAlgorithm.AES192_CBC),
  AES_192_CCM ("aes192-ccm", CMSAlgorithm.AES192_CCM),
  AES_192_GCM ("aes192-gcm", CMSAlgorithm.AES192_GCM),
  AES_256_CBC ("aes256-cbc", CMSAlgorithm.AES256_CBC),
  AES_256_CCM ("aes256-ccm", CMSAlgorithm.AES256_CCM),
  AES_256_GCM ("aes256-gcm", CMSAlgorithm.AES256_GCM);

  public static final ECryptoAlgorithmCrypt SIGN_DIGEST_ALGORITHM_DEFAULT = ECryptoAlgorithmCrypt.AES_128_GCM;

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;

  private ECryptoAlgorithmCrypt (@Nonnull @Nonempty final String sID, @Nonnull final ASN1ObjectIdentifier aOID)
  {
    m_sID = sID;
    m_aOID = aOID;
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
