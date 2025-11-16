/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.util.Collection;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.clone.ICloneable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;

/**
 * AS4 signing parameters
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@NotThreadSafe
public class AS4SigningParams implements ICloneable <AS4SigningParams>
{
  public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;
  public static final boolean DEFAULT_USE_SINGLE_CERTIFICATE = true;

  // The key identifier type to use
  private ECryptoKeyIdentifierType m_eKeyIdentifierType = DEFAULT_KEY_IDENTIFIER_TYPE;
  private ECryptoAlgorithmSign m_eAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eAlgorithmSignDigest;
  private ECryptoAlgorithmC14N m_eAlgorithmC14N = ECryptoAlgorithmC14N.C14N_ALGORITHM_DEFAULT;
  private Provider m_aSecurityProviderSign;
  private Provider m_aSecurityProviderVerify;
  private boolean m_bUseSingleCertificate = DEFAULT_USE_SINGLE_CERTIFICATE;
  private IWSSecSignatureCustomizer m_aWSSecSignatureCustomizer;
  @CodingStyleguideUnaware
  private Collection <Pattern> m_aSubjectCertConstraints;

  public AS4SigningParams ()
  {}

  /**
   * @return <code>true</code> if signing is enabled, <code>false</code> if not
   */
  public boolean isSigningEnabled ()
  {
    return m_eAlgorithmSign != null && m_eAlgorithmSignDigest != null;
  }

  /**
   * @return The key identifier type. May not be <code>null</code>.
   * @since 0.11.0
   */
  @NonNull
  public final ECryptoKeyIdentifierType getKeyIdentifierType ()
  {
    return m_eKeyIdentifierType;
  }

  /**
   * Set the key identifier type to use. That defines how the information about the signing
   * certificate is transmitted.
   *
   * @param eKeyIdentifierType
   *        The key identifier type to use. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.11.0
   */
  @NonNull
  public final AS4SigningParams setKeyIdentifierType (@NonNull final ECryptoKeyIdentifierType eKeyIdentifierType)
  {
    ValueEnforcer.notNull (eKeyIdentifierType, "KeyIdentifierType");
    m_eKeyIdentifierType = eKeyIdentifierType;
    return this;
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
  @NonNull
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
  @NonNull
  public final AS4SigningParams setAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eAlgorithmSignDigest)
  {
    m_eAlgorithmSignDigest = eAlgorithmSignDigest;
    return this;
  }

  /**
   * @return The canonicalization algorithm to use. Never <code>null</code>.
   * @since 0.10.6
   */
  @NonNull
  public final ECryptoAlgorithmC14N getAlgorithmC14N ()
  {
    return m_eAlgorithmC14N;
  }

  /**
   * Set the canonicalization algorithm to be used. By default "Exclusive without comments" is used
   * as suggested by the WS Security SOAP Message Security Version 1.1.1 spec, chapter 8.1.<br>
   * Source: http://docs.oasis-open.org/wss-m/wss/v1.1.1/wss-SOAPMessageSecurity-v1.1.1.doc
   *
   * @param eAlgorithmC14N
   *        the canonicalization algorithm that should be set. May not be <code>null</code>.
   * @return this for chaining
   * @since 0.10.6
   */
  @NonNull
  public final AS4SigningParams setAlgorithmC14N (@NonNull final ECryptoAlgorithmC14N eAlgorithmC14N)
  {
    ValueEnforcer.notNull (eAlgorithmC14N, "AlgorithmC14N");
    m_eAlgorithmC14N = eAlgorithmC14N;
    return this;
  }

  /**
   * @return The security provider for signing (not for verification) to be used. May be
   *         <code>null</code>.
   * @since 2.4.0
   */
  @Nullable
  public final Provider getSecurityProviderSign ()
  {
    return m_aSecurityProviderSign;
  }

  /**
   * Set the security provider to be used for signing (not for verification).
   *
   * @param aSecurityProviderSign
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.4.0
   */
  @NonNull
  public final AS4SigningParams setSecurityProviderSign (@Nullable final Provider aSecurityProviderSign)
  {
    m_aSecurityProviderSign = aSecurityProviderSign;
    return this;
  }

  /**
   * @return The security provider for verification (not for signing) to be used. May be
   *         <code>null</code>.
   * @since 2.4.0
   */
  @Nullable
  public final Provider getSecurityProviderVerify ()
  {
    return m_aSecurityProviderVerify;
  }

  /**
   * Set the security provider to be used for verification (not for signing).
   *
   * @param aSecurityProviderVerify
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.4.0
   */
  @NonNull
  public final AS4SigningParams setSecurityProviderVerify (@Nullable final Provider aSecurityProviderVerify)
  {
    m_aSecurityProviderVerify = aSecurityProviderVerify;
    return this;
  }

  /**
   * Set the security provider to be used for signing and verification.
   *
   * @param aSecurityProvider
   *        The security provider to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 2.1.3
   */
  @NonNull
  public final AS4SigningParams setSecurityProvider (@Nullable final Provider aSecurityProvider)
  {
    return setSecurityProviderSign (aSecurityProvider).setSecurityProviderVerify (aSecurityProvider);
  }

  /**
   * @return <code>true</code> to use the BST ValueType "#X509v3", <code>false</code> to use the BST
   *         value type "#X509PKIPathv1".
   * @since 2.1.5
   */
  public final boolean isUseSingleCertificate ()
  {
    return m_bUseSingleCertificate;
  }

  /**
   * Set the Binary Security Token value type. The default is
   * {@value #DEFAULT_USE_SINGLE_CERTIFICATE}.
   *
   * @param bUseSingleCertificate
   *        <code>true</code> maps to "#X509v3" (e.g. for Peppol) and <code>false</code> maps to
   *        "#X509PKIPathv1".
   * @return this for chaining
   * @since 2.1.5
   */
  @NonNull
  public final AS4SigningParams setUseSingleCertificate (final boolean bUseSingleCertificate)
  {
    m_bUseSingleCertificate = bUseSingleCertificate;
    return this;
  }

  @Nullable
  public final IWSSecSignatureCustomizer getWSSecSignatureCustomizer ()
  {
    return m_aWSSecSignatureCustomizer;
  }

  public final boolean hasWSSecSignatureCustomizer ()
  {
    return m_aWSSecSignatureCustomizer != null;
  }

  @NonNull
  public final AS4SigningParams setWSSecSignatureCustomizer (@Nullable final IWSSecSignatureCustomizer a)
  {
    m_aWSSecSignatureCustomizer = a;
    return this;
  }

  /**
   * Returns the signature subject certificate constraints as regular expressions
   *
   * @return The signature subject certificate constraints as regular expressions or
   *         <code>null</code> if no checks should be performed.
   * @since 3.0.7
   */
  @Nullable
  @ReturnsMutableObject
  public final Collection <Pattern> getSubjectCertConstraints ()
  {
    return m_aSubjectCertConstraints;
  }

  /**
   * Returns the signature subject certificate constraints as regular expressions
   *
   * @return The signature subject certificate constraints as regular expressions as a copy. Never
   *         <code>null</code>.
   * @since 3.0.7
   */
  @NonNull
  @ReturnsMutableCopy
  public final Collection <Pattern> getAllSubjectCertConstraints ()
  {
    // Create a copy
    return new CommonsArrayList <> (m_aSubjectCertConstraints);
  }

  /**
   * Returns whether signature subject certificate constraints exists
   *
   * @return Returns <code>true</code> if signature subject certificate constraints exists,
   *         <code>false</code> otherwise.
   * @since 3.0.7
   */
  public final boolean hasSubjectCertConstraints ()
  {
    return m_aSubjectCertConstraints != null && !m_aSubjectCertConstraints.isEmpty ();
  }

  /**
   * Sets the signature subject certificate constraints as regular expressions.<br>
   * Please note: {@link Pattern} does not implement equals/hashCode, so using a Set as a parameter
   * is not really helpful. However, please make sure to add each pattern only once.
   *
   * @param aSubjectCertConstraints
   *        The collection of regular expression patterns to check. May be <code>null</code> or
   *        empty.
   * @return this for chaining
   * @since 3.0.7
   */
  @NonNull
  public final AS4SigningParams setSubjectCertConstraints (@Nullable final Collection <Pattern> aSubjectCertConstraints)
  {
    m_aSubjectCertConstraints = aSubjectCertConstraints;
    return this;
  }

  /**
   * Sets the signature subject certificate constraints as regular expressions.
   *
   * @param aSubjectCertConstraints
   *        The array of regular expression patterns to check. May be <code>null</code> or empty.
   * @return this for chaining
   * @since 3.0.7
   */
  @NonNull
  public final AS4SigningParams setSubjectCertConstraints (@Nullable final Pattern... aSubjectCertConstraints)
  {
    return setSubjectCertConstraints (aSubjectCertConstraints == null || aSubjectCertConstraints.length == 0 ? null
                                                                                                             : new CommonsArrayList <> (aSubjectCertConstraints));
  }

  /**
   * This method calls {@link #setAlgorithmSign(ECryptoAlgorithmSign)} and
   * {@link #setAlgorithmSignDigest(ECryptoAlgorithmSignDigest)} based on the PMode parameters. If
   * the PMode parameter is <code>null</code> both values will be set to <code>null</code>.
   *
   * @param aSecurity
   *        The PMode security stuff to use. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public final AS4SigningParams setFromPMode (@Nullable final PModeLegSecurity aSecurity)
  {
    // Note: The canonicalization algorithm is not part of the PMode!
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

  @OverridingMethodsMustInvokeSuper
  public void cloneTo (@NonNull final AS4SigningParams aTarget)
  {
    ValueEnforcer.notNull (aTarget, "Target");
    aTarget.setKeyIdentifierType (m_eKeyIdentifierType)
           .setAlgorithmSign (m_eAlgorithmSign)
           .setAlgorithmSignDigest (m_eAlgorithmSignDigest)
           .setAlgorithmC14N (m_eAlgorithmC14N)
           .setSecurityProviderSign (m_aSecurityProviderSign)
           .setSecurityProviderVerify (m_aSecurityProviderVerify)
           .setUseSingleCertificate (m_bUseSingleCertificate)
           .setWSSecSignatureCustomizer (m_aWSSecSignatureCustomizer)
           .setSubjectCertConstraints (m_aSubjectCertConstraints);
  }

  @NonNull
  @ReturnsMutableCopy
  public AS4SigningParams getClone ()
  {
    final AS4SigningParams ret = new AS4SigningParams ();
    cloneTo (ret);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("KeyIdentifierType", m_eKeyIdentifierType)
                                       .append ("AlgorithmSign", m_eAlgorithmSign)
                                       .append ("AlgorithmSignDigest", m_eAlgorithmSignDigest)
                                       .append ("AlgorithmC14N", m_eAlgorithmC14N)
                                       .append ("SecurityProviderSign", m_aSecurityProviderSign)
                                       .append ("SecurityProviderVerify", m_aSecurityProviderVerify)
                                       .append ("UseSingleCertificate", m_bUseSingleCertificate)
                                       .append ("WSSecSignatureCustomizer", m_aWSSecSignatureCustomizer)
                                       .append ("SubjectCertConstraints", m_aSubjectCertConstraints)
                                       .getToString ();
  }

  /**
   * @return A non-<code>null</code> {@link AS4SigningParams} object with default values assigned.
   * @see #setAlgorithmSign(ECryptoAlgorithmSign)
   * @see #setAlgorithmSignDigest(ECryptoAlgorithmSignDigest)
   * @see #setAlgorithmC14N(ECryptoAlgorithmC14N)
   */
  @NonNull
  @ReturnsMutableObject
  public static AS4SigningParams createDefault ()
  {
    return new AS4SigningParams ().setAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT)
                                  .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT)
                                  .setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_ALGORITHM_DEFAULT);
  }
}
