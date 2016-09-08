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
package com.helger.as4lib.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSS4JConstants;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * This enum contains all signing supported crypto algorithms.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmSignDigest implements IHasID <String>
{
  DIGEST_SHA_256 ("sha-256", NISTObjectIdentifiers.id_sha256, "SHA256WITHRSA", WSS4JConstants.SHA256),
  DIGEST_SHA_384 ("sha-384", NISTObjectIdentifiers.id_sha384, "SHA384WITHRSA", WSS4JConstants.SHA384),
  DIGEST_SHA_512 ("sha-512", NISTObjectIdentifiers.id_sha512, "SHA512WITHRSA", WSS4JConstants.SHA512);

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;
  private final String m_sBCAlgorithmName;
  private final String m_sAlgorithmURI;

  private ECryptoAlgorithmSignDigest (@Nonnull @Nonempty final String sID,
                                      @Nonnull final ASN1ObjectIdentifier aOID,
                                      @Nonnull @Nonempty final String sBCAlgorithmName,
                                      @Nonnull @Nonempty final String sAlgorithmURI)
  {
    m_sID = sID;
    m_aOID = aOID;
    m_sBCAlgorithmName = sBCAlgorithmName;
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
   * @return The algorithm name to be used for BouncyCastle to do the SMIME
   *         packaging.
   */
  @Nonnull
  @Nonempty
  public String getSignAlgorithmName ()
  {
    return m_sBCAlgorithmName;
  }

  @Nonnull
  @Nonempty
  public String getAlgorithmURI ()
  {
    return m_sAlgorithmURI;
  }

  @Nullable
  public static ECryptoAlgorithmSignDigest getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithmSignDigest.class, sID);
  }

  @Nonnull
  public static ECryptoAlgorithmSignDigest getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (ECryptoAlgorithmSignDigest.class, sID);
  }

  @Nullable
  public static ECryptoAlgorithmSignDigest getFromIDOrDefault (@Nullable final String sID,
                                                               @Nullable final ECryptoAlgorithmSignDigest eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoAlgorithmSignDigest.class, sID, eDefault);
  }
}
