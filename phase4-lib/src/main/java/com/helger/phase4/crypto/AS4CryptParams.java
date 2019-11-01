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
package com.helger.phase4.crypto;

import java.io.Serializable;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.wss4j.common.WSS4JConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;

/**
 * AS4 encrypt/decrypt parameters
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@NotThreadSafe
public class AS4CryptParams implements Serializable, ICloneable <AS4CryptParams>
{
  public static final String DEFAULT_KEY_ENC_ALGORITHM = WSS4JConstants.KEYTRANSPORT_RSAOAEP_XENC11;
  public static final String DEFAULT_MGF_ALGORITHM = WSS4JConstants.MGF_SHA256;
  public static final String DEFAULT_DIGEST_ALGORITHM = WSS4JConstants.SHA256;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4CryptParams.class);

  // The algorithm to use
  private ECryptoAlgorithmCrypt m_eAlgorithmCrypt;
  // The key encryption algorithm
  private String m_sKeyEncAlgorithm = DEFAULT_KEY_ENC_ALGORITHM;
  // The MGF algorithm to use with the RSA-OAEP key transport algorithm
  private String m_sMGFAlgorithm = DEFAULT_MGF_ALGORITHM;
  // The digest algorithm to use with the RSA-OAEP key transport algorithm
  private String m_sDigestAlgorithm = DEFAULT_DIGEST_ALGORITHM;
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

  @Nonnull
  @Nonempty
  public final String getKeyEncAlgorithm ()
  {
    return m_sKeyEncAlgorithm;
  }

  @Nonnull
  public final AS4CryptParams setKeyEncAlgorithm (@Nonnull @Nonempty final String sKeyEncAlgorithm)
  {
    m_sKeyEncAlgorithm = sKeyEncAlgorithm;
    return this;
  }

  /**
   * @return the MGF algorithm to use with the RSA-OAEP key transport algorithm.
   *         The default is {@link #DEFAULT_MGF_ALGORITHM}
   */
  @Nonnull
  @Nonempty
  public final String getMGFAlgorithm ()
  {
    return m_sMGFAlgorithm;
  }

  @Nonnull
  public final AS4CryptParams setMGFAlgorithm (@Nonnull @Nonempty final String sMGFAlgorithm)
  {
    m_sMGFAlgorithm = sMGFAlgorithm;
    return this;
  }

  /**
   * @return the digest algorithm to use with the RSA-OAEP key transport
   *         algorithm. The default is {@link #DEFAULT_DIGEST_ALGORITHM}
   */
  @Nonnull
  @Nonempty
  public final String getDigestAlgorithm ()
  {
    return m_sDigestAlgorithm;
  }

  @Nonnull
  public final AS4CryptParams setDigestAlgorithm (@Nonnull @Nonempty final String sDigestAlgorithm)
  {
    m_sDigestAlgorithm = sDigestAlgorithm;
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
    if (aCert != null)
    {
      try
      {
        aCert.checkValidity ();
      }
      catch (final CertificateExpiredException ex)
      {
        LOGGER.warn ("The provided certificate is already expired. Please use a different one.");
      }
      catch (final CertificateNotYetValidException ex)
      {
        LOGGER.warn ("The provided certificate is not yet valid. Please use a different one.");
      }
    }
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
    return new AS4CryptParams ().setAlgorithmCrypt (m_eAlgorithmCrypt)
                                .setKeyEncAlgorithm (m_sKeyEncAlgorithm)
                                .setMGFAlgorithm (m_sMGFAlgorithm)
                                .setDigestAlgorithm (m_sDigestAlgorithm)
                                .setCertificate (m_aCert)
                                .setAlias (m_sAlias);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AlgorithmCrypt", m_eAlgorithmCrypt)
                                       .append ("KeyEncAlgorithm", m_sKeyEncAlgorithm)
                                       .append ("MGFAlgorithm", m_sMGFAlgorithm)
                                       .append ("DigestAlgorithm", m_sDigestAlgorithm)
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
