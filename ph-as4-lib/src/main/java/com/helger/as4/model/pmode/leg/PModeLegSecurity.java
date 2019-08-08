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
package com.helger.as4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * PMode security part.
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeLegSecurity implements Serializable
{
  public static final boolean DEFAULT_USERNAME_TOKEN_DIGEST = false;
  public static final boolean DEFAULT_USERNAME_TOKEN_NONCE = false;
  public static final boolean DEFAULT_USERNAME_TOKEN_CREATED = false;
  public static final boolean DEFAULT_PMODE_AUTHORIZE = false;
  public static final boolean DEFAULT_X509_SIGN = false;
  public static final boolean DEFAULT_X509_ENCRYPTION_ENCRYPT = false;
  public static final boolean DEFAULT_SEND_RECEIPT = false;
  public static final boolean DEFAULT_SEND_RECEIPT_NON_REPUDIATION = false;

  /**
   * This parameter has two possible values, 1.0 and 1.1. The value of this
   * parameter represents the version of WS-Security to be used.
   */
  private EWSSVersion m_eWSSVersion;
  /**
   * The value of this parameter is a list of the names of XML elements (inside
   * the SOAP envelope) that should be signed, as well as whether or not
   * attachments should also be signed. The list is represented in two sublists
   * that extend this parameter: Sign.Element[] and Sign.Attachment[]. An
   * element within the Element[] list could be specified either by its XML name
   * or by its qualified name (its XML name and the namespace to which it
   * belongs). An element within the Attachment[] list is identified by the
   * Content-Id.
   */
  private final ICommonsList <String> m_aX509SignElement = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aX509SignAttachment = new CommonsArrayList <> ();
  /**
   * The value of this parameter identifies the public certificate to use when
   * verifying signed data.
   */
  private String m_sX509SignatureCertificate;
  /**
   * The value of this parameter identifies the algorithm that is used to
   * compute the digest of the message being signed. The definitions for these
   * values are in the [XMLDSIG] specification.
   */
  private ECryptoAlgorithmSignDigest m_eX509SignatureHashFunction;
  /**
   * The value of this parameter identifies the algorithm that is used to
   * compute the value of the digital signature. The definitions for these
   * values are found in the [XMLDSIG] or [XMLENC] specifications.
   */
  private ECryptoAlgorithmSign m_eX509SignatureAlgorithm;
  /**
   * The value of this parameter lists the names of XML elements (inside the
   * SOAP envelope) that should be encrypted, as well as whether or not
   * attachments should also be encrypted. The list is represented in two
   * sublists that extend this parameter: Encrypt.Element[] and
   * Encrypt.Attachment[]. An element within these lists is identified as in
   * Security.X509.Sign lists.
   */
  private final ICommonsList <String> m_aX509EncryptionEncryptElement = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aX509EncryptionEncryptAttachment = new CommonsArrayList <> ();

  /**
   * The value of this parameter identifies the public certificate to use when
   * encrypting data.
   */
  private String m_sX509EncryptionCertificate;
  /**
   * The value of this parameter identifies the encryption algorithm to be used.
   * The definitions for these values are found in the [XMLENC] specification.
   */
  private ECryptoAlgorithmCrypt m_eX509EncryptionAlgorithm;
  /**
   * The integer value of this parameter describes the effective strength the
   * encryption algorithm MUST provide in terms of "effective" or random bits.
   * The value is less than the key length in bits when check bits are used in
   * the key. So, for example the 8 check bits of a 64-bit DES key would not be
   * included in the count, and to require a minimum strength the same as
   * supplied by DES would be reported by setting MinimumStrength to 56.
   */
  private Integer m_aX509EncryptionMinimumStrength;
  /**
   * The value of this parameter is the username to include in a WSS Username
   * Token.
   */
  private String m_sUsernameTokenUsername;
  /**
   * The value of this parameter is the password to use inside a WSS Username
   * Token.
   */
  private String m_sUsernameTokenPassword;
  /**
   * The Boolean value of this parameter indicates whether a password digest
   * should be included in the WSS UsernameToken element.
   */
  private ETriState m_eUsernameTokenDigest = ETriState.UNDEFINED;
  /**
   * The Boolean value of this parameter indicates whether the WSS UsernameToken
   * element should contain a Nonce element.
   */
  private ETriState m_eUsernameTokenNonce = ETriState.UNDEFINED;
  /**
   * The Boolean value of this parameter indicates whether the WSS UsernameToken
   * element should have a Created timestamp element.
   */
  private ETriState m_eUsernameTokenCreated = ETriState.UNDEFINED;
  /**
   * The Boolean value of this parameter indicates whether messages on this MEP
   * leg must be authorized for processing under this P-Mode. If the parameter
   * is "true" this implies that either
   * PMode.Responder.Authorization.{username/password}, if the message is sent
   * by Responder, or PMode.Initiator.Authorization if the message is sent by
   * Initiator, must be used for this purpose, as specified in Section 7.10. For
   * example, when set to "true" for a PullRequest message sent by the
   * Initiator, the pulling will only be authorized over the MPC indicated by
   * this Pull signal if (a) the MPC is the same as specified in the P-Mode leg
   * for the pulled message , and (b) the signal contains the right credentials
   * (e.g. username/password).
   */
  private ETriState m_ePModeAuthorize = ETriState.UNDEFINED;
  /**
   * The Boolean value of this parameter indicates whether a signed receipt
   * (Receipt ebMS signal) containing a digest of the message must be sent back.
   */
  private ETriState m_eSendReceipt = ETriState.UNDEFINED;
  /**
   * This parameter indicates whether the Receipt signal is to be sent as a
   * callback (value "callback"), or synchronously in the back-channel response
   * (value "response"). If not present, any pattern may be used.
   */
  private EPModeSendReceiptReplyPattern m_eSendReceiptReplyPattern;

  private ETriState m_eSendReceiptNonRepudiation = ETriState.UNDEFINED;

  public PModeLegSecurity ()
  {}

  public PModeLegSecurity (@Nullable final EWSSVersion eWSSVersion,
                           @Nullable final ICommonsList <String> aX509SignElement,
                           @Nullable final ICommonsList <String> aX509SignAttachment,
                           @Nullable final String sX509SignatureCertificate,
                           @Nullable final ECryptoAlgorithmSignDigest eX509SignatureHashFunction,
                           @Nullable final ECryptoAlgorithmSign sX509SignatureAlgorithm,
                           @Nullable final ICommonsList <String> aX509EncryptionEncryptElement,
                           @Nullable final ICommonsList <String> aX509EncryptionEncryptAttachment,
                           @Nullable final String sX509EncryptionCertificate,
                           @Nullable final ECryptoAlgorithmCrypt sX509EncryptionAlgorithm,
                           @Nullable final Integer aX509EncryptionMinimumStrength,
                           @Nullable final String sUsernameTokenUsername,
                           @Nullable final String sUsernameTokenPassword,
                           @Nonnull final ETriState eUsernameTokenDigest,
                           @Nonnull final ETriState eUsernameTokenNonce,
                           @Nonnull final ETriState eUsernameTokenCreated,
                           @Nonnull final ETriState ePModeAuthorize,
                           @Nonnull final ETriState eSendReceipt,
                           @Nullable final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern,
                           @Nonnull final ETriState eSendReceiptNonRepudiation)
  {
    setWSSVersion (eWSSVersion);
    setX509SignElement (aX509SignElement);
    setX509SignAttachment (aX509SignAttachment);
    setX509SignatureCertificate (sX509SignatureCertificate);
    setX509SignatureHashFunction (eX509SignatureHashFunction);
    setX509SignatureAlgorithm (sX509SignatureAlgorithm);
    setX509EncryptionEncryptElement (aX509EncryptionEncryptElement);
    setX509EncryptionEncryptAttachment (aX509EncryptionEncryptAttachment);
    setX509EncryptionCertificate (sX509EncryptionCertificate);
    setX509EncryptionAlgorithm (sX509EncryptionAlgorithm);
    setX509EncryptionMinimumStrength (aX509EncryptionMinimumStrength);
    setUsernameTokenUsername (sUsernameTokenUsername);
    setUsernameTokenPassword (sUsernameTokenPassword);
    setUsernameTokenDigest (eUsernameTokenDigest);
    setUsernameTokenNonce (eUsernameTokenNonce);
    setUsernameTokenCreated (eUsernameTokenCreated);
    setPModeAuthorize (ePModeAuthorize);
    setSendReceipt (eSendReceipt);
    setSendReceiptReplyPattern (eSendReceiptReplyPattern);
    setSendReceiptNonRepudiation (eSendReceiptNonRepudiation);
  }

  @Nullable
  public EWSSVersion getWSSVersion ()
  {
    return m_eWSSVersion;
  }

  public boolean hasWSSVersion ()
  {
    return m_eWSSVersion != null;
  }

  @Nullable
  public String getWSSVersionAsString ()
  {
    return m_eWSSVersion == null ? null : m_eWSSVersion.getVersion ();
  }

  @Nonnull
  public final EChange setWSSVersion (@Nullable final EWSSVersion eWSSVersion)
  {
    if (EqualsHelper.equals (eWSSVersion, m_eWSSVersion))
      return EChange.UNCHANGED;
    m_eWSSVersion = eWSSVersion;
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509SignElement ()
  {
    return m_aX509SignElement.getClone ();
  }

  @Nonnull
  public final EChange setX509SignElement (@Nullable final ICommonsList <String> aX509SignElement)
  {
    if (EqualsHelper.equals (aX509SignElement, m_aX509SignElement))
      return EChange.UNCHANGED;
    m_aX509SignElement.setAll (aX509SignElement);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509SignAttachment ()
  {
    return m_aX509SignAttachment.getClone ();
  }

  @Nonnull
  public final EChange setX509SignAttachment (@Nullable final ICommonsList <String> aX509SignAttachment)
  {
    if (EqualsHelper.equals (aX509SignAttachment, m_aX509SignAttachment))
      return EChange.UNCHANGED;
    m_aX509SignAttachment.setAll (aX509SignAttachment);
    return EChange.CHANGED;
  }

  @Nullable
  public String getX509SignatureCertificate ()
  {
    return m_sX509SignatureCertificate;
  }

  public boolean hasX509SignatureCertificate ()
  {
    return StringHelper.hasText (m_sX509SignatureCertificate);
  }

  @Nonnull
  public final EChange setX509SignatureCertificate (@Nullable final String sX509SignatureCertificate)
  {
    if (EqualsHelper.equals (sX509SignatureCertificate, m_sX509SignatureCertificate))
      return EChange.UNCHANGED;
    m_sX509SignatureCertificate = sX509SignatureCertificate;
    return EChange.CHANGED;
  }

  @Nullable
  public ECryptoAlgorithmSignDigest getX509SignatureHashFunction ()
  {
    return m_eX509SignatureHashFunction;
  }

  public boolean hasX509SignatureHashFunction ()
  {
    return m_eX509SignatureHashFunction != null;
  }

  @Nullable
  public String getX509SignatureHashFunctionID ()
  {
    return m_eX509SignatureHashFunction == null ? null : m_eX509SignatureHashFunction.getID ();
  }

  @Nonnull
  public final EChange setX509SignatureHashFunction (@Nullable final ECryptoAlgorithmSignDigest eX509SignatureHashFunction)
  {
    if (EqualsHelper.equals (eX509SignatureHashFunction, m_eX509SignatureHashFunction))
      return EChange.UNCHANGED;
    m_eX509SignatureHashFunction = eX509SignatureHashFunction;
    return EChange.CHANGED;
  }

  @Nullable
  public ECryptoAlgorithmSign getX509SignatureAlgorithm ()
  {
    return m_eX509SignatureAlgorithm;
  }

  public boolean hasX509SignatureAlgorithm ()
  {
    return m_eX509SignatureAlgorithm != null;
  }

  @Nullable
  public String getX509SignatureAlgorithmID ()
  {
    return m_eX509SignatureAlgorithm == null ? null : m_eX509SignatureAlgorithm.getID ();
  }

  @Nonnull
  public final EChange setX509SignatureAlgorithm (@Nullable final ECryptoAlgorithmSign eX509SignatureAlgorithm)
  {
    if (EqualsHelper.equals (eX509SignatureAlgorithm, m_eX509SignatureAlgorithm))
      return EChange.UNCHANGED;
    m_eX509SignatureAlgorithm = eX509SignatureAlgorithm;
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509EncryptionEncryptElement ()
  {
    return m_aX509EncryptionEncryptElement.getClone ();
  }

  @Nonnull
  public final EChange setX509EncryptionEncryptElement (@Nullable final ICommonsList <String> aX509EncryptionEncryptElement)
  {
    if (EqualsHelper.equals (aX509EncryptionEncryptElement, m_aX509EncryptionEncryptElement))
      return EChange.UNCHANGED;
    m_aX509EncryptionEncryptElement.setAll (aX509EncryptionEncryptElement);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509EncryptionEncryptAttachment ()
  {
    return m_aX509EncryptionEncryptAttachment.getClone ();
  }

  @Nonnull
  public final EChange setX509EncryptionEncryptAttachment (@Nullable final ICommonsList <String> aX509EncryptionEncryptAttachment)
  {
    if (EqualsHelper.equals (aX509EncryptionEncryptAttachment, m_aX509EncryptionEncryptAttachment))
      return EChange.UNCHANGED;
    m_aX509EncryptionEncryptAttachment.setAll (aX509EncryptionEncryptAttachment);
    return EChange.CHANGED;
  }

  @Nullable
  public String getX509EncryptionCertificate ()
  {
    return m_sX509EncryptionCertificate;
  }

  public boolean hasX509EncryptionCertificate ()
  {
    return StringHelper.hasText (m_sX509EncryptionCertificate);
  }

  @Nonnull
  public final EChange setX509EncryptionCertificate (@Nullable final String sX509EncryptionCertificate)
  {
    if (EqualsHelper.equals (sX509EncryptionCertificate, m_sX509EncryptionCertificate))
      return EChange.UNCHANGED;
    m_sX509EncryptionCertificate = sX509EncryptionCertificate;
    return EChange.CHANGED;
  }

  @Nullable
  public ECryptoAlgorithmCrypt getX509EncryptionAlgorithm ()
  {
    return m_eX509EncryptionAlgorithm;
  }

  public boolean hasX509EncryptionAlgorithm ()
  {
    return m_eX509EncryptionAlgorithm != null;
  }

  @Nullable
  public String getX509EncryptionAlgorithmID ()
  {
    return m_eX509EncryptionAlgorithm == null ? null : m_eX509EncryptionAlgorithm.getID ();
  }

  @Nonnull
  public final EChange setX509EncryptionAlgorithm (@Nullable final ECryptoAlgorithmCrypt eX509EncryptionAlgorithm)
  {
    if (EqualsHelper.equals (eX509EncryptionAlgorithm, m_eX509EncryptionAlgorithm))
      return EChange.UNCHANGED;
    m_eX509EncryptionAlgorithm = eX509EncryptionAlgorithm;
    return EChange.CHANGED;
  }

  @Nullable
  public Integer getX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength;
  }

  public boolean hasX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength != null;
  }

  @Nonnull
  public final EChange setX509EncryptionMinimumStrength (@Nullable final Integer aX509EncryptionMinimumStrength)
  {
    if (EqualsHelper.equals (aX509EncryptionMinimumStrength, m_aX509EncryptionMinimumStrength))
      return EChange.UNCHANGED;
    m_aX509EncryptionMinimumStrength = aX509EncryptionMinimumStrength;
    return EChange.CHANGED;
  }

  @Nullable
  public String getUsernameTokenUsername ()
  {
    return m_sUsernameTokenUsername;
  }

  public boolean hasUsernameTokenUsername ()
  {
    return StringHelper.hasText (m_sUsernameTokenUsername);
  }

  @Nonnull
  public final EChange setUsernameTokenUsername (@Nullable final String sUsernameTokenUsername)
  {
    if (EqualsHelper.equals (sUsernameTokenUsername, m_sUsernameTokenUsername))
      return EChange.UNCHANGED;
    m_sUsernameTokenUsername = sUsernameTokenUsername;
    return EChange.CHANGED;
  }

  @Nullable
  public String getUsernameTokenPassword ()
  {
    return m_sUsernameTokenPassword;
  }

  public boolean hasUsernameTokenPassword ()
  {
    return StringHelper.hasText (m_sUsernameTokenPassword);
  }

  @Nonnull
  public final EChange setUsernameTokenPassword (@Nullable final String sUsernameTokenPassword)
  {
    if (EqualsHelper.equals (sUsernameTokenPassword, m_sUsernameTokenPassword))
      return EChange.UNCHANGED;
    m_sUsernameTokenPassword = sUsernameTokenPassword;
    return EChange.CHANGED;
  }

  public boolean isUsernameTokenDigestDefined ()
  {
    return m_eUsernameTokenDigest.isDefined ();
  }

  public boolean isUsernameTokenDigest ()
  {
    return m_eUsernameTokenDigest.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_DIGEST);
  }

  @Nonnull
  public final EChange setUsernameTokenDigest (final boolean bUsernameTokenDigest)
  {
    return setUsernameTokenDigest (ETriState.valueOf (bUsernameTokenDigest));
  }

  @Nonnull
  public final EChange setUsernameTokenDigest (@Nonnull final ETriState eUsernameTokenDigest)
  {
    ValueEnforcer.notNull (eUsernameTokenDigest, "UsernameTokenDigest");
    if (eUsernameTokenDigest.equals (m_eUsernameTokenDigest))
      return EChange.UNCHANGED;
    m_eUsernameTokenDigest = eUsernameTokenDigest;
    return EChange.CHANGED;
  }

  public boolean isUsernameTokenNonceDefined ()
  {
    return m_eUsernameTokenNonce.isDefined ();
  }

  public boolean isUsernameTokenNonce ()
  {
    return m_eUsernameTokenNonce.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_NONCE);
  }

  @Nonnull
  public final EChange setUsernameTokenNonce (final boolean bUsernameTokenNonce)
  {
    return setUsernameTokenNonce (ETriState.valueOf (bUsernameTokenNonce));
  }

  @Nonnull
  public final EChange setUsernameTokenNonce (@Nonnull final ETriState eUsernameTokenNonce)
  {
    ValueEnforcer.notNull (eUsernameTokenNonce, "UsernameTokenNonce");
    if (eUsernameTokenNonce.equals (m_eUsernameTokenNonce))
      return EChange.UNCHANGED;
    m_eUsernameTokenNonce = eUsernameTokenNonce;
    return EChange.CHANGED;
  }

  public boolean isUsernameTokenCreatedDefined ()
  {
    return m_eUsernameTokenCreated.isDefined ();
  }

  public boolean isUsernameTokenCreated ()
  {
    return m_eUsernameTokenCreated.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_CREATED);
  }

  @Nonnull
  public final EChange setUsernameTokenCreated (final boolean bUsernameTokenCreated)
  {
    return setUsernameTokenCreated (ETriState.valueOf (bUsernameTokenCreated));
  }

  @Nonnull
  public final EChange setUsernameTokenCreated (@Nonnull final ETriState eUsernameTokenCreated)
  {
    ValueEnforcer.notNull (eUsernameTokenCreated, "UsernameTokenCreated");
    if (eUsernameTokenCreated.equals (m_eUsernameTokenCreated))
      return EChange.UNCHANGED;
    m_eUsernameTokenCreated = eUsernameTokenCreated;
    return EChange.CHANGED;
  }

  public boolean isPModeAuthorizeDefined ()
  {
    return m_ePModeAuthorize.isDefined ();
  }

  public boolean isPModeAuthorize ()
  {
    return m_ePModeAuthorize.getAsBooleanValue (DEFAULT_PMODE_AUTHORIZE);
  }

  @Nonnull
  public final EChange setPModeAuthorize (final boolean bPModeAuthorize)
  {
    return setPModeAuthorize (ETriState.valueOf (bPModeAuthorize));
  }

  @Nonnull
  public final EChange setPModeAuthorize (@Nonnull final ETriState ePModeAuthorize)
  {
    ValueEnforcer.notNull (ePModeAuthorize, "PModeAuthorize");
    if (ePModeAuthorize.equals (m_ePModeAuthorize))
      return EChange.UNCHANGED;
    m_ePModeAuthorize = ePModeAuthorize;
    return EChange.CHANGED;
  }

  public boolean isSendReceiptDefined ()
  {
    return m_eSendReceipt.isDefined ();
  }

  public boolean isSendReceipt ()
  {
    return m_eSendReceipt.getAsBooleanValue (DEFAULT_SEND_RECEIPT);
  }

  @Nonnull
  public final EChange setSendReceipt (final boolean bSendReceipt)
  {
    return setSendReceipt (ETriState.valueOf (bSendReceipt));
  }

  @Nonnull
  public final EChange setSendReceipt (@Nonnull final ETriState eSendReceipt)
  {
    ValueEnforcer.notNull (eSendReceipt, "SendReceipt");
    if (eSendReceipt.equals (m_eSendReceipt))
      return EChange.UNCHANGED;
    m_eSendReceipt = eSendReceipt;
    return EChange.CHANGED;
  }

  @Nullable
  public EPModeSendReceiptReplyPattern getSendReceiptReplyPattern ()
  {
    return m_eSendReceiptReplyPattern;
  }

  public String getSendReceiptReplyPatternID ()
  {
    return m_eSendReceiptReplyPattern == null ? null : m_eSendReceiptReplyPattern.getID ();
  }

  @Nonnull
  public final EChange setSendReceiptReplyPattern (@Nullable final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern)
  {
    if (EqualsHelper.equals (eSendReceiptReplyPattern, m_eSendReceiptReplyPattern))
      return EChange.UNCHANGED;
    m_eSendReceiptReplyPattern = eSendReceiptReplyPattern;
    return EChange.CHANGED;
  }

  public boolean isSendReceiptNonRepudiationDefined ()
  {
    return m_eSendReceiptNonRepudiation.isDefined ();
  }

  public boolean isSendReceiptNonRepudiation ()
  {
    return m_eSendReceiptNonRepudiation.getAsBooleanValue (DEFAULT_SEND_RECEIPT);
  }

  @Nonnull
  public final EChange setSendReceiptNonRepudiation (final boolean bSendReceiptNonRepudiation)
  {
    return setSendReceiptNonRepudiation (ETriState.valueOf (bSendReceiptNonRepudiation));
  }

  @Nonnull
  public final EChange setSendReceiptNonRepudiation (@Nonnull final ETriState eSendReceiptNonRepudiation)
  {
    ValueEnforcer.notNull (eSendReceiptNonRepudiation, "SendReceiptNonRepudiation");
    if (eSendReceiptNonRepudiation.equals (m_eSendReceiptNonRepudiation))
      return EChange.UNCHANGED;
    m_eSendReceiptNonRepudiation = eSendReceiptNonRepudiation;
    return EChange.CHANGED;
  }

  public void disableSigning ()
  {
    setX509SignElement (null);
    setX509SignAttachment (null);
    setX509SignatureCertificate (null);
    setX509SignatureHashFunction (null);
    setX509SignatureAlgorithm (null);
  }

  public void disableEncryption ()
  {
    setX509EncryptionEncryptElement (null);
    setX509EncryptionEncryptAttachment (null);
    setX509EncryptionCertificate (null);
    setX509EncryptionAlgorithm (null);
    setX509EncryptionMinimumStrength (null);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLegSecurity rhs = (PModeLegSecurity) o;
    return EqualsHelper.equals (m_eWSSVersion, rhs.m_eWSSVersion) &&
           EqualsHelper.equals (m_aX509SignElement, rhs.m_aX509SignElement) &&
           EqualsHelper.equals (m_aX509SignAttachment, rhs.m_aX509SignAttachment) &&
           EqualsHelper.equals (m_sX509SignatureCertificate, rhs.m_sX509SignatureCertificate) &&
           EqualsHelper.equals (m_eX509SignatureHashFunction, rhs.m_eX509SignatureHashFunction) &&
           EqualsHelper.equals (m_eX509SignatureAlgorithm, rhs.m_eX509SignatureAlgorithm) &&
           EqualsHelper.equals (m_aX509EncryptionEncryptElement, rhs.m_aX509EncryptionEncryptElement) &&
           EqualsHelper.equals (m_aX509EncryptionEncryptAttachment, rhs.m_aX509EncryptionEncryptAttachment) &&
           EqualsHelper.equals (m_sX509EncryptionCertificate, rhs.m_sX509EncryptionCertificate) &&
           EqualsHelper.equals (m_eX509EncryptionAlgorithm, rhs.m_eX509EncryptionAlgorithm) &&
           EqualsHelper.equals (m_aX509EncryptionMinimumStrength, rhs.m_aX509EncryptionMinimumStrength) &&
           EqualsHelper.equals (m_sUsernameTokenUsername, rhs.m_sUsernameTokenUsername) &&
           EqualsHelper.equals (m_sUsernameTokenPassword, rhs.m_sUsernameTokenPassword) &&
           EqualsHelper.equals (m_eUsernameTokenDigest, rhs.m_eUsernameTokenDigest) &&
           EqualsHelper.equals (m_eUsernameTokenNonce, rhs.m_eUsernameTokenNonce) &&
           EqualsHelper.equals (m_eUsernameTokenCreated, rhs.m_eUsernameTokenCreated) &&
           EqualsHelper.equals (m_ePModeAuthorize, rhs.m_ePModeAuthorize) &&
           EqualsHelper.equals (m_eSendReceipt, rhs.m_eSendReceipt) &&
           EqualsHelper.equals (m_eSendReceiptReplyPattern, rhs.m_eSendReceiptReplyPattern) &&
           EqualsHelper.equals (m_eSendReceiptNonRepudiation, rhs.m_eSendReceiptNonRepudiation);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eWSSVersion)
                                       .append (m_aX509SignElement)
                                       .append (m_aX509SignAttachment)
                                       .append (m_sX509SignatureCertificate)
                                       .append (m_eX509SignatureHashFunction)
                                       .append (m_eX509SignatureAlgorithm)
                                       .append (m_aX509EncryptionEncryptElement)
                                       .append (m_aX509EncryptionEncryptAttachment)
                                       .append (m_sX509EncryptionCertificate)
                                       .append (m_eX509EncryptionAlgorithm)
                                       .append (m_aX509EncryptionMinimumStrength)
                                       .append (m_sUsernameTokenUsername)
                                       .append (m_sUsernameTokenPassword)
                                       .append (m_eUsernameTokenDigest)
                                       .append (m_eUsernameTokenNonce)
                                       .append (m_eUsernameTokenCreated)
                                       .append (m_ePModeAuthorize)
                                       .append (m_eSendReceipt)
                                       .append (m_eSendReceiptReplyPattern)
                                       .append (m_eSendReceiptNonRepudiation)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("WSSVersion", m_eWSSVersion)
                                       .append ("X509SignElement", m_aX509SignElement)
                                       .append ("X509SignAttachment", m_aX509SignAttachment)
                                       .append ("X509SignatureCertificate", m_sX509SignatureCertificate)
                                       .append ("X509SignatureHashFunction", m_eX509SignatureHashFunction)
                                       .append ("X509SignatureAlgorithm", m_eX509SignatureAlgorithm)
                                       .append ("X509EncryptionEncryptElement", m_aX509EncryptionEncryptElement)
                                       .append ("X509EncryptionEncryptAttachment", m_aX509EncryptionEncryptAttachment)
                                       .append ("X509EncryptionCertificate", m_sX509EncryptionCertificate)
                                       .append ("X509EncryptionAlgorithm", m_eX509EncryptionAlgorithm)
                                       .append ("X509EncryptionMinimumStrength", m_aX509EncryptionMinimumStrength)
                                       .append ("UsernameTokenUsername", m_sUsernameTokenUsername)
                                       .append ("UsernameTokenPassword", m_sUsernameTokenPassword)
                                       .append ("UsernameTokenDigest", m_eUsernameTokenDigest)
                                       .append ("UsernameTokenNonce", m_eUsernameTokenNonce)
                                       .append ("UsernameTokenCreated", m_eUsernameTokenCreated)
                                       .append ("PModeAuthorize", m_ePModeAuthorize)
                                       .append ("SendReceipt", m_eSendReceipt)
                                       .append ("SendReceiptReplyPattern", m_eSendReceiptReplyPattern)
                                       .append ("SendReceiptNonRepudiation", m_eSendReceiptNonRepudiation)
                                       .getToString ();
  }
}
