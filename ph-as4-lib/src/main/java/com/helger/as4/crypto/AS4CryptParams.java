/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * AS4 encrypt/decrypt parameters
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@NotThreadSafe
public class AS4CryptParams implements Serializable, ICloneable <AS4CryptParams>
{
  // The algorithm to use
  private ECryptoAlgorithmCrypt m_eAlgorithmCrypt;
  // The explicit certificate to use - has precedence over the alias
  private X509Certificate m_aCert;
  // The alias into the WSS4J crypto config
  private String m_sAlias;

  public AS4CryptParams ()
  {}

  public boolean isCryptEnabled (@Nullable final Consumer <String> aWarningConsumer)
  {
    if (m_eAlgorithmCrypt == null)
      return false;

    // One of certificate or alias must be present
    if (!hasCertificate () && !hasAlias ())
    {
      if (aWarningConsumer != null)
        aWarningConsumer.accept ("Crypt parameters have an algorithm defined but neither an alias nor a certificate was provided. Therefore encryption is not enabled.");
      return false;
    }

    return true;
  }

  /**
   * @return The encryption algorithm to use. May be <code>null</code>.
   */
  @Nullable
  public final ECryptoAlgorithmCrypt getAlgorithmCrypt ()
  {
    return m_eAlgorithmCrypt;
  }

  /**
   * A encryption algorithm can be set. <br>
   * MANDATORY if you want to use encryption.
   *
   * @param eAlgorithmCrypt
   *        the encryption algorithm that should be set
   * @return this for chaining
   */
  @Nonnull
  public final AS4CryptParams setAlgorithmCrypt (@Nullable final ECryptoAlgorithmCrypt eAlgorithmCrypt)
  {
    m_eAlgorithmCrypt = eAlgorithmCrypt;
    return this;
  }

  @Nullable
  public final X509Certificate getCertificate ()
  {
    return m_aCert;
  }

  public final boolean hasCertificate ()
  {
    return m_aCert != null;
  }

  @Nonnull
  public final AS4CryptParams setCertificate (@Nullable final X509Certificate aCert)
  {
    m_aCert = aCert;
    return this;
  }

  @Nullable
  public final String getAlias ()
  {
    return m_sAlias;
  }

  public final boolean hasAlias ()
  {
    return StringHelper.hasText (m_sAlias);
  }

  @Nonnull
  public final AS4CryptParams setAlias (@Nullable final String sAlias)
  {
    m_sAlias = sAlias;
    return this;
  }

  @Nonnull
  public final AS4CryptParams setFromPMode (@Nullable final PModeLegSecurity aSecurity)
  {
    if (aSecurity == null)
    {
      setAlgorithmCrypt (null);
    }
    else
    {
      setAlgorithmCrypt (aSecurity.getX509EncryptionAlgorithm ());
    }
    return this;
  }

  @Nonnull
  @ReturnsMutableCopy
  public AS4CryptParams getClone ()
  {
    return new AS4CryptParams ().setAlgorithmCrypt (m_eAlgorithmCrypt).setCertificate (m_aCert).setAlias (m_sAlias);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AlgorithmCrypt", m_eAlgorithmCrypt)
                                       .append ("Certificate", m_aCert)
                                       .append ("Alias", m_sAlias)
                                       .getToString ();
  }

  @Nonnull
  @ReturnsMutableObject
  public static AS4CryptParams createDefault ()
  {
    return new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
  }
}
