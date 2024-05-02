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

import java.security.Provider;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.wss4j.common.WSS4JConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
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
 * @author Gregor Scholtysik
 * @since 0.9.0
 */
@NotThreadSafe
public class AS4CryptParams implements ICloneable <AS4CryptParams>
{
  public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
  public static final ECryptoKeyEncryptionAlgorithm DEFAULT_KEY_ENCRYPTION_ALGORITHM = ECryptoKeyEncryptionAlgorithm.RSA_OAEP_XENC11;
  public static final String DEFAULT_MGF_ALGORITHM = WSS4JConstants.MGF_SHA256;
  public static final String DEFAULT_DIGEST_ALGORITHM = WSS4JConstants.SHA256;
  public static final ICryptoSessionKeyProvider DEFAULT_SESSION_KEY_PROVIDER = ICryptoSessionKeyProvider.INSTANCE_RANDOM_AES_128;
  public static final boolean DEFAULT_ENCRYPT_SYMMETRIC_SESSION_KEY = true;

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4CryptParams.class);

  // The key identifier type to use
  private ECryptoKeyIdentifierType m_eKeyIdentifierType = DEFAULT_KEY_IDENTIFIER_TYPE;
  // The algorithm to use
  private ECryptoAlgorithmCrypt m_eAlgorithmCrypt;
  // The key encryption algorithm
  private ECryptoKeyEncryptionAlgorithm m_eKeyEncAlgorithm = DEFAULT_KEY_ENCRYPTION_ALGORITHM;
  // The MGF algorithm to use with the RSA-OAEP key transport algorithm
  private String m_sMGFAlgorithm = DEFAULT_MGF_ALGORITHM;
  // The digest algorithm to use with the RSA-OAEP key transport algorithm
  private String m_sDigestAlgorithm = DEFAULT_DIGEST_ALGORITHM;
  // The explicit certificate to use - has precedence over the alias
  private X509Certificate m_aCert;
  // The alias into the WSS4J crypto config
  private String m_sAlias;
  // The session key provider
  private ICryptoSessionKeyProvider m_aSessionKeyProvider = DEFAULT_SESSION_KEY_PROVIDER;
  private Provider m_aSecurityProviderEncrypt;
  private Provider m_aSecurityProviderDecrypt;
  private boolean m_bEncryptSymmetricSessionKey = DEFAULT_ENCRYPT_SYMMETRIC_SESSION_KEY;
  private IWSSecEncryptCustomizer m_aWSSecEncryptCustomizer;

  /**
   * Default constructor using default
   * {@link #setKeyIdentifierType(ECryptoKeyIdentifierType)},
   * {@link #setKeyEncAlgorithm(ECryptoKeyEncryptionAlgorithm)},
   * {@link #setMGFAlgorithm(String)} and {@link #setDigestAlgorithm(String)}
   */
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
   * @return The key identifier type. May not be <code>null</code>.
   * @since 0.11.0
   */
  @Nonnull
  public final ECryptoKeyIdentifierType getKeyIdentifierType ()
  {
    return m_eKeyIdentifierType;
  }

  /**
   * Set the key identifier type to use. That defines how the information about
   * the signing certificate is transmitted.
   *
   * @param eKeyIdentifierType
   *        The key identifier type to use. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.11.0
   */
  @Nonnull
  public final AS4CryptParams setKeyIdentifierType (@Nonnull final ECryptoKeyIdentifierType eKeyIdentifierType)
  {
    ValueEnforcer.notNull (eKeyIdentifierType, "KeyIdentifierType");
    m_eKeyIdentifierType = eKeyIdentifierType;
    return this;
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
  public final ECryptoKeyEncryptionAlgorithm getKeyEncAlgorithm ()
  {
    return m_eKeyEncAlgorithm;
  }

  @Nonnull
  public final AS4CryptParams setKeyEncAlgorithm (@Nonnull final ECryptoKeyEncryptionAlgorithm eKeyEncAlgorithm)
  {
    m_eKeyEncAlgorithm = eKeyEncAlgorithm;
    return this;
  }

  /**
   * @return The mask generation function (MGF) algorithm to use with the
   *         RSA-OAEP key transport algorithm. The default is
   *         {@link #DEFAULT_MGF_ALGORITHM}
   */
  @Nonnull
  @Nonempty
  public final String getMGFAlgorithm ()
  {
    return m_sMGFAlgorithm;
  }

  /**
   * Set the mask generation function (MGF) algorithm to use with the RSA-OAEP
   * key transport algorithm.
   *
   * @param sMGFAlgorithm
   *        The MFG algorithm to use. May neither be <code>null</code> nor
   *        empty.
   * @return this for chaining
   */
  @Nonnull
  public final AS4CryptParams setMGFAlgorithm (@Nonnull @Nonempty final String sMGFAlgorithm)
  {
    ValueEnforcer.notEmpty (sMGFAlgorithm, "MGFAlgorithm");
    m_sMGFAlgorithm = sMGFAlgorithm;
    return this;
  }

  /**
   * @return The digest algorithm to use with the RSA-OAEP key transport
   *         algorithm. The default is {@link #DEFAULT_DIGEST_ALGORITHM}
   */
  @Nonnull
  @Nonempty
  public final String getDigestAlgorithm ()
  {
    return m_sDigestAlgorithm;
  }

  /**
   * Set the digest algorithm to use with the RSA-OAEP key transport algorithm.
   *
   * @param sDigestAlgorithm
   *        The digest algorithm to use. May neither be <code>null</code> nor
   *        empty.
   * @return this for chaining
   */
  @Nonnull
  public final AS4CryptParams setDigestAlgorithm (@Nonnull @Nonempty final String sDigestAlgorithm)
  {
    ValueEnforcer.notEmpty (sDigestAlgorithm, "DigestAlgorithm");
    m_sDigestAlgorithm = sDigestAlgorithm;
    return this;
  }

  /**
   * @return The currently set X509 certificate. May be <code>null</code>.
   */
  @Nullable
  public final X509Certificate getCertificate ()
  {
    return m_aCert;
  }

  /**
   * @return <code>true</code> if an X509 certificate is present,
   *         <code>false</code> if not.
   */
  public final boolean hasCertificate ()
  {
    return m_aCert != null;
  }

  /**
   * Set the X509 certificate be used. The provided certificate is not checked
   * for validity. If it is expired only a warning is logged but the certificate
   * will still be used.
   *
   * @param aCert
   *        The certificate to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final AS4CryptParams setCertificate (@Nullable final X509Certificate aCert)
  {
    m_aCert = aCert;
    if (aCert != null)
    {
      // Note: this is informational only. If the certificate is expired and you
      // don't want that, you need check that before
      try
      {
        aCert.checkValidity ();
      }
      catch (final CertificateExpiredException ex)
      {
        LOGGER.warn ("The provided certificate is already expired. Please use a different one: " + ex.getMessage ());
      }
      catch (final CertificateNotYetValidException ex)
      {
        LOGGER.warn ("The provided certificate is not yet valid. Please use a different one: " + ex.getMessage ());
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

  /**
   * @return The session key provider to be used. Never <code>null</code>.
   * @since 2.1.2
   */
  @Nonnull
  public final ICryptoSessionKeyProvider getSessionKeyProvider ()
  {
    return m_aSessionKeyProvider;
  }

  /**
   * Set the session key provider to be used for encryption. The provided
   * provider must never return a <code>null</code> key.
   *
   * @param aSessionKeyProvider
   *        The session key provider to be used. May not be <code>null</code>.
   * @return this for chaining
   * @since 2.1.2
   */
  @Nonnull
  public final AS4CryptParams setSessionKeyProvider (@Nonnull final ICryptoSessionKeyProvider aSessionKeyProvider)
  {
    ValueEnforcer.notNull (aSessionKeyProvider, "SessionKeyProvider");
    m_aSessionKeyProvider = aSessionKeyProvider;
    return this;
  }

  /**
   * Note: this is currently not used by WSS4J
   *
   * @return The security provider to be used for encryption (not for
   *         decryption). May be <code>null</code>.
   * @since 2.4.0
   */
  @Nullable
  public final Provider getSecurityProviderEncrypt ()
  {
    return m_aSecurityProviderEncrypt;
  }

  /**
   * Set the security provider to be used for encryption (not for
   * decryption).<br>
   * Note: this is currently not used by WSS4J
   *
   * @param aSecurityProviderEncrypt
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.4.0
   */
  @Nonnull
  public final AS4CryptParams setSecurityProviderEncrypt (@Nullable final Provider aSecurityProviderEncrypt)
  {
    m_aSecurityProviderEncrypt = aSecurityProviderEncrypt;
    return this;
  }

  /**
   * Note: this is currently not used by WSS4J
   *
   * @return The security provider to be used for decryption (not for
   *         encryption). May be <code>null</code>.
   * @since 2.4.0
   */
  @Nullable
  public final Provider getSecurityProviderDecrypt ()
  {
    return m_aSecurityProviderDecrypt;
  }

  /**
   * Set the security provider to be used for decryption (not for
   * encryption).<br>
   * Note: this is currently not used by WSS4J
   *
   * @param aSecurityProviderDecrypt
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.4.0
   */
  @Nonnull
  public final AS4CryptParams setSecurityProviderDecrypt (@Nullable final Provider aSecurityProviderDecrypt)
  {
    m_aSecurityProviderDecrypt = aSecurityProviderDecrypt;
    return this;
  }

  /**
   * Set the security provider to be used for encryption and decryption.
   *
   * @param aSecurityProvider
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.1.4
   */
  @Nonnull
  public final AS4CryptParams setSecurityProvider (@Nullable final Provider aSecurityProvider)
  {
    return setSecurityProviderEncrypt (aSecurityProvider).setSecurityProviderDecrypt (aSecurityProvider);
  }

  /**
   * @return <code>true</code> if the symmetric session key should be part of
   *         the transmission or <code>false</code> if not. Default is
   *         {@link #DEFAULT_ENCRYPT_SYMMETRIC_SESSION_KEY}
   * @since 2.1.4
   */
  public final boolean isEncryptSymmetricSessionKey ()
  {
    return m_bEncryptSymmetricSessionKey;
  }

  /**
   * Enable or disable the inclusion of the symmetric session key into the
   * transmission or not.
   *
   * @param b
   *        <code>true</code> to enabled, <code>false</code> to disable it.
   * @return this for chaining
   * @since 2.1.4
   */
  @Nonnull
  public final AS4CryptParams setEncryptSymmetricSessionKey (final boolean b)
  {
    m_bEncryptSymmetricSessionKey = b;
    return this;
  }

  @Nullable
  public final IWSSecEncryptCustomizer getWSSecEncryptCustomizer ()
  {
    return m_aWSSecEncryptCustomizer;
  }

  public final boolean hasWSSecEncryptCustomizer ()
  {
    return m_aWSSecEncryptCustomizer != null;
  }

  @Nonnull
  public final AS4CryptParams setWSSecEncryptCustomizer (@Nullable final IWSSecEncryptCustomizer a)
  {
    m_aWSSecEncryptCustomizer = a;
    return this;
  }

  /**
   * This method calls {@link #setAlgorithmCrypt(ECryptoAlgorithmCrypt)} based
   * on the PMode parameters. If the PMode parameter is <code>null</code> the
   * value will be set to <code>null</code>.
   *
   * @param aSecurity
   *        The PMode security stuff to use. May be <code>null</code>.
   * @return this for chaining
   * @see #setAlgorithmCrypt(ECryptoAlgorithmCrypt)
   */
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

  @OverridingMethodsMustInvokeSuper
  public void cloneTo (@Nonnull final AS4CryptParams aTarget)
  {
    ValueEnforcer.notNull (aTarget, "Target");
    aTarget.setKeyIdentifierType (m_eKeyIdentifierType)
           .setAlgorithmCrypt (m_eAlgorithmCrypt)
           .setKeyEncAlgorithm (m_eKeyEncAlgorithm)
           .setMGFAlgorithm (m_sMGFAlgorithm)
           .setDigestAlgorithm (m_sDigestAlgorithm)
           .setCertificate (m_aCert)
           .setAlias (m_sAlias)
           .setSessionKeyProvider (m_aSessionKeyProvider)
           .setSecurityProviderEncrypt (m_aSecurityProviderEncrypt)
           .setSecurityProviderDecrypt (m_aSecurityProviderDecrypt)
           .setEncryptSymmetricSessionKey (m_bEncryptSymmetricSessionKey)
           .setWSSecEncryptCustomizer (m_aWSSecEncryptCustomizer);
  }

  @Nonnull
  @ReturnsMutableCopy
  public AS4CryptParams getClone ()
  {
    final AS4CryptParams ret = new AS4CryptParams ();
    cloneTo (ret);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("KeyIdentifierType", m_eKeyIdentifierType)
                                       .append ("AlgorithmCrypt", m_eAlgorithmCrypt)
                                       .append ("KeyEncAlgorithm", m_eKeyEncAlgorithm)
                                       .append ("MGFAlgorithm", m_sMGFAlgorithm)
                                       .append ("DigestAlgorithm", m_sDigestAlgorithm)
                                       .append ("Certificate", m_aCert)
                                       .append ("Alias", m_sAlias)
                                       .append ("SessionKeyProvider", m_aSessionKeyProvider)
                                       .append ("SecurityProviderEncrypt", m_aSecurityProviderEncrypt)
                                       .append ("SecurityProviderDecrypt", m_aSecurityProviderDecrypt)
                                       .append ("EncryptSymmetricSessionKey", m_bEncryptSymmetricSessionKey)
                                       .append ("WSSecEncryptCustomizer", m_aWSSecEncryptCustomizer)
                                       .getToString ();
  }

  /**
   * @return A non-<code>null</code> default instance.
   * @see #setAlgorithmCrypt(ECryptoAlgorithmCrypt)
   */
  @Nonnull
  @ReturnsMutableObject
  public static AS4CryptParams createDefault ()
  {
    return new AS4CryptParams ().setAlgorithmCrypt (ECryptoAlgorithmCrypt.ENCRPYTION_ALGORITHM_DEFAULT);
  }
}
