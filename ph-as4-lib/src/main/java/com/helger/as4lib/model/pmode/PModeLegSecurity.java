/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.wss.EWSSVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.ETriState;

/**
 * Security part.
 *
 * @author Philip Helger
 */
public class PModeLegSecurity
{
  public static final boolean DEFAULT_USERNAME_TOKEN_DIGEST = false;
  public static final boolean DEFAULT_USERNAME_TOKEN_NONCE = false;
  public static final boolean DEFAULT_USERNAME_TOKEN_CREATED = false;
  public static final boolean DEFAULT_PMODE_AUTHORIZE = false;
  public static final boolean DEFAULT_SEND_RECEIPT = false;
  public static final boolean DEFAULT_X509_SIGN = false;
  public static final boolean DEFAULT_X509_ENCRYPTION_ENCRYPT = false;

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
  private ICommonsList <String> m_aX509SignElement;
  private ICommonsList <String> m_aX509SignAttachment;
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
  private ICommonsList <String> m_aX509EncryptionEncryptElement;
  private ICommonsList <String> m_aX509EncryptionEncryptAttachment;

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
                           @Nullable final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern)
  {
    setX509EncryptionEncryptElement (aX509EncryptionEncryptElement);
    setX509EncryptionEncryptAttachment (aX509EncryptionEncryptAttachment);
    setX509EncryptionMinimumStrength (aX509EncryptionMinimumStrength);
    setX509SignElement (aX509SignElement);
    setX509SignAttachment (aX509SignAttachment);
    setPModeAuthorize (ePModeAuthorize);
    setSendReceipt (eSendReceipt);
    setUsernameTokenCreated (eUsernameTokenCreated);
    setUsernameTokenDigest (eUsernameTokenDigest);
    setUsernameTokenNonce (eUsernameTokenNonce);
    setSendReceiptReplyPattern (eSendReceiptReplyPattern);
    setUsernameTokenPassword (sUsernameTokenPassword);
    setUsernameTokenUsername (sUsernameTokenUsername);
    setWSSVersion (eWSSVersion);
    setX509EncryptionAlgorithm (sX509EncryptionAlgorithm);
    setX509EncryptionCertificate (sX509EncryptionCertificate);
    setX509SignatureAlgorithm (sX509SignatureAlgorithm);
    setX509SignatureCertificate (sX509SignatureCertificate);
    setX509SignatureHashFunction (eX509SignatureHashFunction);
  }

  @Nullable
  public EWSSVersion getWSSVersion ()
  {
    return m_eWSSVersion;
  }

  @Nullable
  public String getWSSVersionAsString ()
  {
    return m_eWSSVersion == null ? null : m_eWSSVersion.getVersion ();
  }

  public void setWSSVersion (@Nullable final EWSSVersion eWSSVersion)
  {
    m_eWSSVersion = eWSSVersion;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509SignElement ()
  {
    return new CommonsArrayList<> (m_aX509SignElement);
  }

  public void setX509SignElement (@Nullable final ICommonsList <String> aX509SignElement)
  {
    m_aX509SignElement = aX509SignElement;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509SignAttachment ()
  {
    return new CommonsArrayList<> (m_aX509SignAttachment);
  }

  public void setX509SignAttachment (@Nullable final ICommonsList <String> aX509SignAttachment)
  {
    m_aX509SignAttachment = aX509SignAttachment;
  }

  @Nullable
  public String getX509SignatureCertificate ()
  {
    return m_sX509SignatureCertificate;
  }

  public void setX509SignatureCertificate (@Nullable final String sX509SignatureCertificate)
  {
    m_sX509SignatureCertificate = sX509SignatureCertificate;
  }

  @Nullable
  public ECryptoAlgorithmSignDigest getX509SignatureHashFunction ()
  {
    return m_eX509SignatureHashFunction;
  }

  @Nullable
  public String getX509SignatureHashFunctionID ()
  {
    return m_eX509SignatureHashFunction == null ? null : m_eX509SignatureHashFunction.getID ();
  }

  public void setX509SignatureHashFunction (@Nullable final ECryptoAlgorithmSignDigest eX509SignatureHashFunction)
  {
    m_eX509SignatureHashFunction = eX509SignatureHashFunction;
  }

  @Nullable
  public ECryptoAlgorithmSign getX509SignatureAlgorithm ()
  {
    return m_eX509SignatureAlgorithm;
  }

  @Nullable
  public String getX509SignatureAlgorithmID ()
  {
    return m_eX509SignatureAlgorithm == null ? null : m_eX509SignatureAlgorithm.getID ();
  }

  public void setX509SignatureAlgorithm (@Nullable final ECryptoAlgorithmSign eX509SignatureAlgorithm)
  {
    m_eX509SignatureAlgorithm = eX509SignatureAlgorithm;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509EncryptionEncryptElement ()
  {
    return new CommonsArrayList<> (m_aX509EncryptionEncryptElement);
  }

  public void setX509EncryptionEncryptElement (@Nullable final ICommonsList <String> aX509EncryptionEncryptElement)
  {
    m_aX509EncryptionEncryptElement = aX509EncryptionEncryptElement;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509EncryptionEncryptAttachment ()
  {
    return new CommonsArrayList<> (m_aX509EncryptionEncryptAttachment);
  }

  public void setX509EncryptionEncryptAttachment (@Nullable final ICommonsList <String> aX509EncryptionEncryptAttachment)
  {
    m_aX509EncryptionEncryptAttachment = aX509EncryptionEncryptAttachment;
  }

  @Nullable
  public String getX509EncryptionCertificate ()
  {
    return m_sX509EncryptionCertificate;
  }

  public void setX509EncryptionCertificate (@Nullable final String sX509EncryptionCertificate)
  {
    m_sX509EncryptionCertificate = sX509EncryptionCertificate;
  }

  @Nullable
  public ECryptoAlgorithmCrypt getX509EncryptionAlgorithm ()
  {
    return m_eX509EncryptionAlgorithm;
  }

  @Nullable
  public String getX509EncryptionAlgorithmID ()
  {
    return m_eX509EncryptionAlgorithm == null ? null : m_eX509EncryptionAlgorithm.getID ();
  }

  public void setX509EncryptionAlgorithm (@Nullable final ECryptoAlgorithmCrypt eX509EncryptionAlgorithm)
  {
    m_eX509EncryptionAlgorithm = eX509EncryptionAlgorithm;
  }

  public boolean hasX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength != null;
  }

  @Nullable
  public Integer getX509EncryptionMinimumStrength ()
  {
    return m_aX509EncryptionMinimumStrength;
  }

  public void setX509EncryptionMinimumStrength (@Nullable final Integer aX509EncryptionMinimumStrength)
  {
    m_aX509EncryptionMinimumStrength = aX509EncryptionMinimumStrength;
  }

  @Nullable
  public String getUsernameTokenUsername ()
  {
    return m_sUsernameTokenUsername;
  }

  public void setUsernameTokenUsername (@Nullable final String sUsernameTokenUsername)
  {
    m_sUsernameTokenUsername = sUsernameTokenUsername;
  }

  @Nullable
  public String getUsernameTokenPassword ()
  {
    return m_sUsernameTokenPassword;
  }

  public void setUsernameTokenPassword (@Nullable final String sUsernameTokenPassword)
  {
    m_sUsernameTokenPassword = sUsernameTokenPassword;
  }

  @Nullable
  public EPModeSendReceiptReplyPattern getSendReceiptReplyPattern ()
  {
    return m_eSendReceiptReplyPattern;
  }

  @Nullable
  public String getSendReceiptReplyPatternID ()
  {
    return m_eSendReceiptReplyPattern == null ? null : m_eSendReceiptReplyPattern.getID ();
  }

  public void setSendReceiptReplyPattern (@Nullable final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern)
  {
    m_eSendReceiptReplyPattern = eSendReceiptReplyPattern;
  }

  public boolean isUsernameTokenDigestDefined ()
  {
    return m_eUsernameTokenDigest.isDefined ();
  }

  @Nonnull
  public boolean isUsernameTokenDigest ()
  {
    return m_eUsernameTokenDigest.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_DIGEST);
  }

  public void setUsernameTokenDigest (final boolean bUsernameTokenDigest)
  {
    setUsernameTokenDigest (ETriState.valueOf (bUsernameTokenDigest));
  }

  public void setUsernameTokenDigest (@Nonnull final ETriState eUsernameTokenDigest)
  {
    ValueEnforcer.notNull (eUsernameTokenDigest, "UsernameTokenDigest");
    m_eUsernameTokenDigest = eUsernameTokenDigest;
  }

  public boolean isUsernameTokenNonceDefined ()
  {
    return m_eUsernameTokenNonce.isDefined ();
  }

  @Nonnull
  public boolean isUsernameTokenNonce ()
  {
    return m_eUsernameTokenNonce.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_NONCE);
  }

  public void setUsernameTokenNonce (final boolean bUsernameTokenNonce)
  {
    setUsernameTokenNonce (ETriState.valueOf (bUsernameTokenNonce));
  }

  public void setUsernameTokenNonce (@Nonnull final ETriState eUsernameTokenNonce)
  {
    ValueEnforcer.notNull (eUsernameTokenNonce, "UsernameTokenNonce");
    m_eUsernameTokenNonce = eUsernameTokenNonce;
  }

  public boolean isUsernameTokenCreatedDefined ()
  {
    return m_eUsernameTokenCreated.isDefined ();
  }

  @Nonnull
  public boolean isUsernameTokenCreated ()
  {
    return m_eUsernameTokenCreated.getAsBooleanValue (DEFAULT_USERNAME_TOKEN_CREATED);
  }

  public void setUsernameTokenCreated (final boolean bUsernameTokenCreated)
  {
    setUsernameTokenCreated (ETriState.valueOf (bUsernameTokenCreated));
  }

  public void setUsernameTokenCreated (@Nonnull final ETriState eUsernameTokenCreated)
  {
    ValueEnforcer.notNull (eUsernameTokenCreated, "UsernameTokenCreated");
    m_eUsernameTokenCreated = eUsernameTokenCreated;
  }

  public boolean isPModeAuthorizeDefined ()
  {
    return m_ePModeAuthorize.isDefined ();
  }

  @Nonnull
  public boolean isPModeAuthorize ()
  {
    return m_ePModeAuthorize.getAsBooleanValue (DEFAULT_PMODE_AUTHORIZE);
  }

  public void setPModeAuthorize (final boolean bPModeAuthorize)
  {
    setPModeAuthorize (ETriState.valueOf (bPModeAuthorize));
  }

  public void setPModeAuthorize (@Nonnull final ETriState ePModeAuthorize)
  {
    ValueEnforcer.notNull (ePModeAuthorize, "PModeAuthorize");
    m_ePModeAuthorize = ePModeAuthorize;
  }

  public boolean isSendReceiptDefined ()
  {
    return m_eSendReceipt.isDefined ();
  }

  @Nonnull
  public boolean isSendReceipt ()
  {
    return m_eSendReceipt.getAsBooleanValue (DEFAULT_SEND_RECEIPT);
  }

  public void setSendReceipt (final boolean bSendReceipt)
  {
    setSendReceipt (ETriState.valueOf (bSendReceipt));
  }

  public void setSendReceipt (@Nonnull final ETriState eSendReceipt)
  {
    ValueEnforcer.notNull (eSendReceipt, "SendReceipt");
    m_eSendReceipt = eSendReceipt;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLegSecurity rhs = (PModeLegSecurity) o;
    return EqualsHelper.equals (m_aX509EncryptionEncryptElement, rhs.m_aX509EncryptionEncryptElement) &&
           EqualsHelper.equals (m_aX509EncryptionEncryptAttachment, rhs.m_aX509EncryptionEncryptAttachment) &&
           EqualsHelper.equals (m_aX509EncryptionMinimumStrength, rhs.m_aX509EncryptionMinimumStrength) &&
           EqualsHelper.equals (m_aX509SignElement, rhs.m_aX509SignElement) &&
           EqualsHelper.equals (m_aX509SignAttachment, rhs.m_aX509SignAttachment) &&
           EqualsHelper.equals (m_ePModeAuthorize, rhs.m_ePModeAuthorize) &&
           EqualsHelper.equals (m_eSendReceipt, rhs.m_eSendReceipt) &&
           EqualsHelper.equals (m_eUsernameTokenCreated, rhs.m_eUsernameTokenCreated) &&
           EqualsHelper.equals (m_eUsernameTokenDigest, rhs.m_eUsernameTokenDigest) &&
           EqualsHelper.equals (m_eUsernameTokenNonce, rhs.m_eUsernameTokenNonce) &&
           EqualsHelper.equals (m_eSendReceiptReplyPattern, rhs.m_eSendReceiptReplyPattern) &&
           EqualsHelper.equals (m_sUsernameTokenPassword, rhs.m_sUsernameTokenPassword) &&
           EqualsHelper.equals (m_sUsernameTokenUsername, rhs.m_sUsernameTokenUsername) &&
           EqualsHelper.equals (m_eWSSVersion, rhs.m_eWSSVersion) &&
           EqualsHelper.equals (m_eX509EncryptionAlgorithm, rhs.m_eX509EncryptionAlgorithm) &&
           EqualsHelper.equals (m_sX509EncryptionCertificate, rhs.m_sX509EncryptionCertificate) &&
           EqualsHelper.equals (m_eX509SignatureAlgorithm, rhs.m_eX509SignatureAlgorithm) &&
           EqualsHelper.equals (m_sX509SignatureCertificate, rhs.m_sX509SignatureCertificate) &&
           EqualsHelper.equals (m_eX509SignatureHashFunction, rhs.m_eX509SignatureHashFunction);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aX509EncryptionEncryptElement)
                                       .append (m_aX509EncryptionEncryptAttachment)
                                       .append (m_aX509EncryptionMinimumStrength)
                                       .append (m_aX509SignElement)
                                       .append (m_aX509SignAttachment)
                                       .append (m_ePModeAuthorize)
                                       .append (m_eSendReceipt)
                                       .append (m_eUsernameTokenCreated)
                                       .append (m_eUsernameTokenDigest)
                                       .append (m_eUsernameTokenNonce)
                                       .append (m_eSendReceiptReplyPattern)
                                       .append (m_sUsernameTokenPassword)
                                       .append (m_sUsernameTokenUsername)
                                       .append (m_eWSSVersion)
                                       .append (m_eX509EncryptionAlgorithm)
                                       .append (m_sX509EncryptionCertificate)
                                       .append (m_eX509SignatureAlgorithm)
                                       .append (m_sX509SignatureCertificate)
                                       .append (m_eX509SignatureHashFunction)
                                       .getHashCode ();
  }
}
