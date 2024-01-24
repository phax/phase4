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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

/**
 * This enum contains all signing supported crypto algorithms.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmSignDigest implements ICryptoAlgorithmSignDigest
{
  DIGEST_SHA_256 ("sha-256", WSS4JConstants.SHA256),
  DIGEST_SHA_384 ("sha-384", WSS4JConstants.SHA384),
  DIGEST_SHA_512 ("sha-512", WSS4JConstants.SHA512);

  public static final ECryptoAlgorithmSignDigest SIGN_DIGEST_ALGORITHM_DEFAULT = DIGEST_SHA_256;

  private final String m_sID;
  private final String m_sAlgorithmURI;

  ECryptoAlgorithmSignDigest (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sAlgorithmURI)
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

  @Nullable
  public static ECryptoAlgorithmSignDigest getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasNoText (sURI))
      return null;
    return EnumHelper.findFirst (ECryptoAlgorithmSignDigest.class, x -> x.getAlgorithmURI ().equals (sURI));
  }
}
