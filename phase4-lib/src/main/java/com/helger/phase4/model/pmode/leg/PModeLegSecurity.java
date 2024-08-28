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
package com.helger.phase4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.wss.EWSSVersion;

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
  private final ICommonsList <String> m_aX509SignElements = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aX509SignAttachments = new CommonsArrayList <> ();

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
  private final ICommonsList <String> m_aX509EncryptionEncryptElements = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aX509EncryptionEncryptAttachments = new CommonsArrayList <> ();

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

  /**
   * This parameter indicates, that if a Receipt signal is to be sent, whether
   * the Non-Repudiation of receipt information should be included in the
   * Receipt signal or not.
   */
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
    setX509SignElements (aX509SignElement);
    setX509SignAttachments (aX509SignAttachment);
    setX509SignatureCertificate (sX509SignatureCertificate);
    setX509SignatureHashFunction (eX509SignatureHashFunction);
    setX509SignatureAlgorithm (sX509SignatureAlgorithm);
    setX509EncryptionEncryptElements (aX509EncryptionEncryptElement);
    setX509EncryptionEncryptAttachments (aX509EncryptionEncryptAttachment);
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

  /**
   * @return The version of WS Security to use. May be <code>null</code>.
   */
  @Nullable
  public final EWSSVersion getWSSVersion ()
  {
    return m_eWSSVersion;
  }

  /**
   * @return <code>true</code> if a WS Security version is defined,
   *         <code>false</code> if not.
   */
  public final boolean hasWSSVersion ()
  {
    return m_eWSSVersion != null;
  }

  /**
   * @return The WS Security version string to use or <code>null</code> if none
   *         is defined.
   */
  @Nullable
  public final String getWSSVersionAsString ()
  {
    return m_eWSSVersion == null ? null : m_eWSSVersion.getVersion ();
  }

  /**
   * Set the WS Security version to use.
   *
   * @param eWSSVersion
   *        The version to use. May be <code>null</code>.
   * @return {@link EChange}.
   */
  @Nonnull
  public final EChange setWSSVersion (@Nullable final EWSSVersion eWSSVersion)
  {
    if (EqualsHelper.equals (eWSSVersion, m_eWSSVersion))
      return EChange.UNCHANGED;
    m_eWSSVersion = eWSSVersion;
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <String> x509SignElements ()
  {
    return m_aX509SignElements;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <String> getAllX509SignElements ()
  {
    return m_aX509SignElements.getClone ();
  }

  @Nonnull
  public final EChange setX509SignElements (@Nullable final ICommonsList <String> aX509SignElement)
  {
    if (EqualsHelper.equals (aX509SignElement, m_aX509SignElements))
      return EChange.UNCHANGED;
    m_aX509SignElements.setAll (aX509SignElement);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <String> x509SignAttachments ()
  {
    return m_aX509SignAttachments;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <String> getAllX509SignAttachments ()
  {
    return m_aX509SignAttachments.getClone ();
  }

  @Nonnull
  public final EChange setX509SignAttachments (@Nullable final ICommonsList <String> aX509SignAttachment)
  {
    if (EqualsHelper.equals (aX509SignAttachment, m_aX509SignAttachments))
      return EChange.UNCHANGED;
    m_aX509SignAttachments.setAll (aX509SignAttachment);
    return EChange.CHANGED;
  }

  @Nullable
  public final String getX509SignatureCertificate ()
  {
    return m_sX509SignatureCertificate;
  }

  public final boolean hasX509SignatureCertificate ()
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
  public final ECryptoAlgorithmSignDigest getX509SignatureHashFunction ()
  {
    return m_eX509SignatureHashFunction;
  }

  public boolean hasX509SignatureHashFunction ()
  {
    return m_eX509SignatureHashFunction != null;
  }

  @Nullable
  public final String getX509SignatureHashFunctionID ()
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
  public final ECryptoAlgorithmSign getX509SignatureAlgorithm ()
  {
    return m_eX509SignatureAlgorithm;
  }

  public final boolean hasX509SignatureAlgorithm ()
  {
    return m_eX509SignatureAlgorithm != null;
  }

  @Nullable
  public final String getX509SignatureAlgorithmID ()
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
  @ReturnsMutableObject
  public final ICommonsList <String> x509EncryptionEncryptElements ()
  {
    return m_aX509EncryptionEncryptElements;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <String> getAllX509EncryptionEncryptElements ()
  {
    return m_aX509EncryptionEncryptElements.getClone ();
  }

  @Nonnull
  public final EChange setX509EncryptionEncryptElements (@Nullable final ICommonsList <String> aX509EncryptionEncryptElement)
  {
    if (EqualsHelper.equals (aX509EncryptionEncryptElement, m_aX509EncryptionEncryptElements))
      return EChange.UNCHANGED;
    m_aX509EncryptionEncryptElements.setAll (aX509EncryptionEncryptElement);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <String> x509EncryptionEncryptAttachments ()
  {
    return m_aX509EncryptionEncryptAttachments;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <String> getAllX509EncryptionEncryptAttachments ()
  {
    return m_aX509EncryptionEncryptAttachments.getClone ();
  }

  @Nonnull
  public final EChange setX509EncryptionEncryptAttachments (@Nullable final ICommonsList <String> aX509EncryptionEncryptAttachment)
  {
    if (EqualsHelper.equals (aX509EncryptionEncryptAttachment, m_aX509EncryptionEncryptAttachments))
      return EChange.UNCHANGED;
    m_aX509EncryptionEncryptAttachments.setAll (aX509EncryptionEncryptAttachment);
    return EChange.CHANGED;
  }

  @Nullable
  public final String getX509EncryptionCertificate ()
  {
    return m_sX509EncryptionCertificate;
  }

  public final boolean hasX509EncryptionCertificate ()
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
  public final ECryptoAlgorithmCrypt getX509EncryptionAlgorithm ()
  {
    return m_eX509EncryptionAlgorithm;
  }

  public final boolean hasX509EncryptionAlgorithm ()
  {
    return m_eX509EncryptionAlgorithm != null;
  }

  @Nullable
  public final String getX509EncryptionAlgorithmID ()
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
  public final Integer getX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength;
  }

  public final boolean hasX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength != null;
  }

  @Nonnull
  public final EChange setX509EncryptionMinimumStrength (final int nX509EncryptionMinimumStrength)
  {
    return setX509EncryptionMinimumStrength (Integer.valueOf (nX509EncryptionMinimumStrength));
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
  public final String getUsernameTokenUsername ()
  {
    return m_sUsernameTokenUsername;
  }

  public final boolean hasUsernameTokenUsername ()
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
  public final String getUsernameTokenPassword ()
  {
    return m_sUsernameTokenPassword;
  }

  public final boolean hasUsernameTokenPassword ()
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

  public final boolean isUsernameTokenDigestDefined ()
  {
    return m_eUsernameTokenDigest.isDefined ();
  }

  public final boolean isUsernameTokenDigest ()
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

  public final boolean isUsernameTokenNonceDefined ()
  {
    return m_eUsernameTokenNonce.isDefined ();
  }

  public final boolean isUsernameTokenNonce ()
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

  public final boolean isUsernameTokenCreatedDefined ()
  {
    return m_eUsernameTokenCreated.isDefined ();
  }

  public final boolean isUsernameTokenCreated ()
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

  public final boolean isPModeAuthorizeDefined ()
  {
    return m_ePModeAuthorize.isDefined ();
  }

  public final boolean isPModeAuthorize ()
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

  public final boolean isSendReceiptDefined ()
  {
    return m_eSendReceipt.isDefined ();
  }

  public final boolean isSendReceipt ()
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
  public final EPModeSendReceiptReplyPattern getSendReceiptReplyPattern ()
  {
    return m_eSendReceiptReplyPattern;
  }

  @Nullable
  public final String getSendReceiptReplyPatternID ()
  {
    return m_eSendReceiptReplyPattern == null ? null : m_eSendReceiptReplyPattern.getID ();
  }

  public final boolean hasSendReceiptReplyPattern ()
  {
    return m_eSendReceiptReplyPattern != null;
  }

  @Nonnull
  public final EChange setSendReceiptReplyPattern (@Nullable final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern)
  {
    if (EqualsHelper.equals (eSendReceiptReplyPattern, m_eSendReceiptReplyPattern))
      return EChange.UNCHANGED;
    m_eSendReceiptReplyPattern = eSendReceiptReplyPattern;
    return EChange.CHANGED;
  }

  public final boolean isSendReceiptNonRepudiationDefined ()
  {
    return m_eSendReceiptNonRepudiation.isDefined ();
  }

  public final boolean isSendReceiptNonRepudiation ()
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

  /**
   * Set all field that affect signing to "no signing".
   *
   * @see #setX509SignElements(ICommonsList)
   * @see #setX509SignAttachments(ICommonsList)
   * @see #setX509SignatureCertificate(String)
   * @see #setX509SignatureHashFunction(ECryptoAlgorithmSignDigest)
   * @see #setX509SignatureAlgorithm(ECryptoAlgorithmSign)
   */
  public final void disableSigning ()
  {
    setX509SignElements (null);
    setX509SignAttachments (null);
    setX509SignatureCertificate (null);
    setX509SignatureHashFunction (null);
    setX509SignatureAlgorithm (null);
  }

  /**
   * Set all field that affect encryption to "no encryption".
   *
   * @see #setX509EncryptionEncryptElements(ICommonsList)
   * @see #setX509EncryptionEncryptAttachments(ICommonsList)
   * @see #setX509EncryptionCertificate(String)
   * @see #setX509EncryptionAlgorithm(ECryptoAlgorithmCrypt)
   * @see #setX509EncryptionMinimumStrength(Integer)
   */
  public final void disableEncryption ()
  {
    setX509EncryptionEncryptElements (null);
    setX509EncryptionEncryptAttachments (null);
    setX509EncryptionCertificate (null);
    setX509EncryptionAlgorithm (null);
    setX509EncryptionMinimumStrength (null);
  }

  /**
   * Set all field that affect username token to "don't use".
   *
   * @see #setUsernameTokenUsername(String)
   * @see #setUsernameTokenPassword(String)
   * @see #setUsernameTokenDigest(ETriState)
   * @see #setUsernameTokenNonce(ETriState)
   * @see #setUsernameTokenCreated(ETriState)
   * @since 0.13.1
   */
  public final void disableUsernameToken ()
  {
    setUsernameTokenUsername (null);
    setUsernameTokenPassword (null);
    setUsernameTokenDigest (ETriState.UNDEFINED);
    setUsernameTokenNonce (ETriState.UNDEFINED);
    setUsernameTokenCreated (ETriState.UNDEFINED);
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
           EqualsHelper.equals (m_aX509SignElements, rhs.m_aX509SignElements) &&
           EqualsHelper.equals (m_aX509SignAttachments, rhs.m_aX509SignAttachments) &&
           EqualsHelper.equals (m_sX509SignatureCertificate, rhs.m_sX509SignatureCertificate) &&
           EqualsHelper.equals (m_eX509SignatureHashFunction, rhs.m_eX509SignatureHashFunction) &&
           EqualsHelper.equals (m_eX509SignatureAlgorithm, rhs.m_eX509SignatureAlgorithm) &&
           EqualsHelper.equals (m_aX509EncryptionEncryptElements, rhs.m_aX509EncryptionEncryptElements) &&
           EqualsHelper.equals (m_aX509EncryptionEncryptAttachments, rhs.m_aX509EncryptionEncryptAttachments) &&
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
                                       .append (m_aX509SignElements)
                                       .append (m_aX509SignAttachments)
                                       .append (m_sX509SignatureCertificate)
                                       .append (m_eX509SignatureHashFunction)
                                       .append (m_eX509SignatureAlgorithm)
                                       .append (m_aX509EncryptionEncryptElements)
                                       .append (m_aX509EncryptionEncryptAttachments)
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
                                       .append ("X509SignElement", m_aX509SignElements)
                                       .append ("X509SignAttachment", m_aX509SignAttachments)
                                       .append ("X509SignatureCertificate", m_sX509SignatureCertificate)
                                       .append ("X509SignatureHashFunction", m_eX509SignatureHashFunction)
                                       .append ("X509SignatureAlgorithm", m_eX509SignatureAlgorithm)
                                       .append ("X509EncryptionEncryptElement", m_aX509EncryptionEncryptElements)
                                       .append ("X509EncryptionEncryptAttachment", m_aX509EncryptionEncryptAttachments)
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
