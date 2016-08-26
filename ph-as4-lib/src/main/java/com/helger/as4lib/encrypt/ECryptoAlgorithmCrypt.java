/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as4lib.encrypt;

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
