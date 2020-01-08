/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;

/**
 * AS4 signing parameters
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@NotThreadSafe
public class AS4SigningParams implements Serializable, ICloneable <AS4SigningParams>
{
  private ECryptoAlgorithmSign m_eAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eAlgorithmSignDigest;

  public AS4SigningParams ()
  {}

  public boolean isSigningEnabled ()
  {
    return m_eAlgorithmSign != null && m_eAlgorithmSignDigest != null;
  }

  /**
   * @return The signing algorithm to use. May be <code>null</code>.
   */
  @Nullable
  public final ECryptoAlgorithmSign getAlgorithmSign ()
  {
    return m_eAlgorithmSign;
  }

  /**
   * A signing algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see {@link #setAlgorithmSignDigest(ECryptoAlgorithmSignDigest)}
   *
   * @param eAlgorithmSign
   *        the signing algorithm that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4SigningParams setAlgorithmSign (@Nullable final ECryptoAlgorithmSign eAlgorithmSign)
  {
    m_eAlgorithmSign = eAlgorithmSign;
    return this;
  }

  /**
   * @return The signing digest algorithm to use. May be <code>null</code>.
   */
  @Nullable
  public final ECryptoAlgorithmSignDigest getAlgorithmSignDigest ()
  {
    return m_eAlgorithmSignDigest;
  }

  /**
   * A signing digest algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see {@link #setAlgorithmSign(ECryptoAlgorithmSign)}
   *
   * @param eAlgorithmSignDigest
   *        the signing digest algorithm that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4SigningParams setAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eAlgorithmSignDigest)
  {
    m_eAlgorithmSignDigest = eAlgorithmSignDigest;
    return this;
  }

  @Nonnull
  public final AS4SigningParams setFromPMode (@Nullable final PModeLegSecurity aSecurity)
  {
    if (aSecurity == null)
    {
      setAlgorithmSign (null);
      setAlgorithmSignDigest (null);
    }
    else
    {
      setAlgorithmSign (aSecurity.getX509SignatureAlgorithm ());
      setAlgorithmSignDigest (aSecurity.getX509SignatureHashFunction ());
    }
    return this;
  }

  @Nonnull
  @ReturnsMutableCopy
  public AS4SigningParams getClone ()
  {
    return new AS4SigningParams ().setAlgorithmSign (m_eAlgorithmSign).setAlgorithmSignDigest (m_eAlgorithmSignDigest);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AlgorithmSign", m_eAlgorithmSign)
                                       .append ("AlgorithmSignDigest", m_eAlgorithmSignDigest)
                                       .getToString ();
  }

  @Nonnull
  @ReturnsMutableObject
  public static AS4SigningParams createDefault ()
  {
    return new AS4SigningParams ().setAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT)
                                  .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
  }
}
