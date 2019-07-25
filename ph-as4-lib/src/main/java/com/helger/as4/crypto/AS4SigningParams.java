package com.helger.as4.crypto;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.ToStringGenerator;

/**
 * AS4 signing parameters
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS4SigningParams implements Serializable, ICloneable <AS4SigningParams>
{
  private ECryptoAlgorithmSign m_eAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eAlgorithmSignDigest;

  public AS4SigningParams ()
  {}

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

  public boolean isSigningEnabled ()
  {
    return m_eAlgorithmSign != null && m_eAlgorithmSignDigest != null;
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

  @Nonnull
  @ReturnsMutableObject
  public static AS4SigningParams createFromPMode (@Nullable final PModeLegSecurity aSecurity)
  {
    if (aSecurity == null)
      return new AS4SigningParams ();
    return new AS4SigningParams ().setAlgorithmSign (aSecurity.getX509SignatureAlgorithm ())
                                  .setAlgorithmSignDigest (aSecurity.getX509SignatureHashFunction ());
  }
}
