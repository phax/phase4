/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.xml.security.signature.XMLSignature;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

/**
 * This enum contains all signing supported crypto algorithms.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmSign implements IHasID <String>
{
  RSA_SHA_256 ("rsa-sha-256", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256),
  RSA_SHA_384 ("rsa-sha-384", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384),
  RSA_SHA_512 ("rsa-sha-512", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512);

  public static final ECryptoAlgorithmSign SIGN_ALGORITHM_DEFAULT = RSA_SHA_256;

  private final String m_sID;
  private final String m_sAlgorithmURI;

  private ECryptoAlgorithmSign (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sAlgorithmURI)
  {
    m_sID = sID;
    m_sAlgorithmURI = sAlgorithmURI;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getAlgorithmURI ()
  {
    return m_sAlgorithmURI;
  }

  @Nullable
  public static ECryptoAlgorithmSign getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithmSign.class, sID);
  }

  @Nonnull
  public static ECryptoAlgorithmSign getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (ECryptoAlgorithmSign.class, sID);
  }

  @Nullable
  public static ECryptoAlgorithmSign getFromIDOrDefault (@Nullable final String sID,
                                                         @Nullable final ECryptoAlgorithmSign eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoAlgorithmSign.class, sID, eDefault);
  }

  @Nullable
  public static ECryptoAlgorithmSign getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasNoText (sURI))
      return null;
    return EnumHelper.findFirst (ECryptoAlgorithmSign.class, x -> x.getAlgorithmURI ().equals (sURI));
  }
}
