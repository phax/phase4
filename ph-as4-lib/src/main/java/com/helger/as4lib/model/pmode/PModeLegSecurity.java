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
  // TODO SET DEFAULT VALUES
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
  private String m_sWSSVersion;
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
  private ICommonsList <String> m_aX509Sign;
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
  private String m_sX509SignatureHashFunction;
  /**
   * The value of this parameter identifies the algorithm that is used to
   * compute the value of the digital signature. The definitions for these
   * values are found in the [XMLDSIG] or [XMLENC] specifications.
   */
  private String m_sX509SignatureAlgorithm;
  /**
   * The value of this parameter lists the names of XML elements(inside the SOAP
   * envelope) that should be encrypted, as well as whether or not attachments
   * should also be encrypted. The list is represented in two sublists that
   * extend this parameter: Encrypt.Element[] and Encrypt.Attachment[]. An
   * element within these lists is identified as in Security.X509.Sign lists.
   */
  private ICommonsList <String> m_aX509EncryptionEncrypt;
  /**
   * The value of this parameter identifies the public certificate to use when
   * encrypting data.
   */
  private String m_sX509EncryptionCertificate;
  /**
   * The value of this parameter identifies the encryption algorithm to be used.
   * The definitions for these values are found in the [XMLENC] specification.
   */
  private String m_sX509EncryptionAlgorithm;
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
  private String m_sSendReceiptReplyPattern;

  private ETriState m_eX509Sign = ETriState.UNDEFINED;

  private ETriState m_eX509EncryptionEncrypt = ETriState.UNDEFINED;

  public PModeLegSecurity ()
  {

  }

  public PModeLegSecurity (@Nullable final String m_sWSSVersion,
                           @Nullable final ICommonsList <String> m_aX509Sign,
                           @Nullable final String m_sX509SignatureCertificate,
                           @Nullable final String m_sX509SignatureHashFunction,
                           @Nullable final String m_sX509SignatureAlgorithm,
                           @Nullable final ICommonsList <String> m_aX509EncryptionEncrypt,
                           @Nullable final String m_sX509EncryptionCertificate,
                           @Nullable final String m_sX509EncryptionAlgorithm,
                           @Nullable final Integer m_aX509EncryptionMinimumStrength,
                           @Nullable final String m_sUsernameTokenUsername,
                           @Nullable final String m_sUsernameTokenPassword,
                           @Nonnull final ETriState m_eUsernameTokenDigest,
                           @Nonnull final ETriState m_eUsernameTokenNonce,
                           @Nonnull final ETriState m_eUsernameTokenCreated,
                           @Nonnull final ETriState m_ePModeAuthorize,
                           @Nonnull final ETriState m_eSendReceipt,
                           @Nullable final String m_sSendReceiptReplyPattern,
                           @Nonnull final ETriState m_eX509Sign,
                           @Nonnull final ETriState m_eX509EncryptionEncrypt)
  {
    this.m_aX509EncryptionEncrypt = m_aX509EncryptionEncrypt;
    this.m_aX509EncryptionMinimumStrength = m_aX509EncryptionMinimumStrength;
    this.m_aX509Sign = m_aX509Sign;
    this.m_ePModeAuthorize = m_ePModeAuthorize;
    this.m_eSendReceipt = m_eSendReceipt;
    this.m_eUsernameTokenCreated = m_eUsernameTokenCreated;
    this.m_eUsernameTokenDigest = m_eUsernameTokenDigest;
    this.m_eUsernameTokenNonce = m_eUsernameTokenNonce;
    this.m_sSendReceiptReplyPattern = m_sSendReceiptReplyPattern;
    this.m_sUsernameTokenPassword = m_sUsernameTokenPassword;
    this.m_sUsernameTokenUsername = m_sUsernameTokenUsername;
    this.m_sWSSVersion = m_sWSSVersion;
    this.m_sX509EncryptionAlgorithm = m_sX509EncryptionAlgorithm;
    this.m_sX509EncryptionCertificate = m_sX509EncryptionCertificate;
    this.m_sX509SignatureAlgorithm = m_sX509SignatureAlgorithm;
    this.m_sX509SignatureCertificate = m_sX509SignatureCertificate;
    this.m_sX509SignatureHashFunction = m_sX509SignatureHashFunction;
    this.m_eX509Sign = m_eX509Sign;
    this.m_eX509EncryptionEncrypt = m_eX509EncryptionEncrypt;
  }

  @Nullable
  public String getWSSVersion ()
  {
    return m_sWSSVersion;
  }

  public void setWSSVersion (final String sWSSVersion)
  {
    m_sWSSVersion = sWSSVersion;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509Sign ()
  {
    return new CommonsArrayList<> (m_aX509Sign);
  }

  public void setX509Sign (final ICommonsList <String> aX509Sign)
  {
    m_aX509Sign = aX509Sign;
  }

  @Nullable
  public String getX509SignatureCertificate ()
  {
    return m_sX509SignatureCertificate;
  }

  public void setX509SignatureCertificate (final String sX509SignatureCertificate)
  {
    m_sX509SignatureCertificate = sX509SignatureCertificate;
  }

  @Nullable
  public String getX509SignatureHashFunction ()
  {
    return m_sX509SignatureHashFunction;
  }

  public void setX509SignatureHashFunction (final String sX509SignatureHashFunction)
  {
    m_sX509SignatureHashFunction = sX509SignatureHashFunction;
  }

  @Nullable
  public String getX509SignatureAlgorithm ()
  {
    return m_sX509SignatureAlgorithm;
  }

  public void setX509SignatureAlgorithm (final String sX509SignatureAlgorithm)
  {
    m_sX509SignatureAlgorithm = sX509SignatureAlgorithm;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getX509EncryptionEncrypt ()
  {
    return new CommonsArrayList<> (m_aX509EncryptionEncrypt);
  }

  public void setX509EncryptionEncrypt (final ICommonsList <String> aX509EncryptionEncrypt)
  {
    m_aX509EncryptionEncrypt = aX509EncryptionEncrypt;
  }

  @Nullable
  public String getX509EncryptionCertificate ()
  {
    return m_sX509EncryptionCertificate;
  }

  public void setX509EncryptionCertificate (final String sX509EncryptionCertificate)
  {
    m_sX509EncryptionCertificate = sX509EncryptionCertificate;
  }

  @Nullable
  public String getX509EncryptionAlgorithm ()
  {
    return m_sX509EncryptionAlgorithm;
  }

  public void setX509EncryptionAlgorithm (final String sX509EncryptionAlgorithm)
  {
    m_sX509EncryptionAlgorithm = sX509EncryptionAlgorithm;
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

  public void setUsernameTokenUsername (final String sUsernameTokenUsername)
  {
    m_sUsernameTokenUsername = sUsernameTokenUsername;
  }

  @Nullable
  public String getUsernameTokenPassword ()
  {
    return m_sUsernameTokenPassword;
  }

  public void setUsernameTokenPassword (final String sUsernameTokenPassword)
  {
    m_sUsernameTokenPassword = sUsernameTokenPassword;
  }

  @Nullable
  public String getSendReceiptReplyPattern ()
  {
    return m_sSendReceiptReplyPattern;
  }

  public void setSendReceiptReplyPattern (final String sSendReceiptReplyPattern)
  {
    m_sSendReceiptReplyPattern = sSendReceiptReplyPattern;
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

  public boolean isX509SignDefined ()
  {
    return m_eX509Sign.isDefined ();
  }

  @Nonnull
  public boolean isX509Sign ()
  {
    return m_eX509Sign.getAsBooleanValue (DEFAULT_X509_SIGN);
  }

  public void setX509Sign (final boolean bX509Sign)
  {
    setSendReceipt (ETriState.valueOf (bX509Sign));
  }

  public void setX509Sign (@Nonnull final ETriState eX509Sign)
  {
    ValueEnforcer.notNull (eX509Sign, "X509Sign");
    m_eX509Sign = eX509Sign;
  }

  public boolean isX509EncryptionEncryptDefined ()
  {
    return m_eX509EncryptionEncrypt.isDefined ();
  }

  @Nonnull
  public boolean isX509EncryptionEncrypt ()
  {
    return m_eX509EncryptionEncrypt.getAsBooleanValue (DEFAULT_X509_SIGN);
  }

  public void setX509EncryptionEncrypt (final boolean bX509EncryptionEncrypt)
  {
    setSendReceipt (ETriState.valueOf (bX509EncryptionEncrypt));
  }

  public void setX509EncryptionEncrypt (@Nonnull final ETriState eX509EncryptionEncrypt)
  {
    ValueEnforcer.notNull (eX509EncryptionEncrypt, "X509EncryptionEncrypt");
    m_eX509EncryptionEncrypt = eX509EncryptionEncrypt;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLegSecurity rhs = (PModeLegSecurity) o;
    return m_aX509EncryptionEncrypt.equals (rhs.m_aX509EncryptionEncrypt) &&
           EqualsHelper.equals (m_aX509EncryptionMinimumStrength, rhs.m_aX509EncryptionMinimumStrength) &&
           EqualsHelper.equals (m_aX509Sign, rhs.m_aX509Sign) &&
           EqualsHelper.equals (m_ePModeAuthorize, rhs.m_ePModeAuthorize) &&
           EqualsHelper.equals (m_eSendReceipt, rhs.m_eSendReceipt) &&
           EqualsHelper.equals (m_eUsernameTokenCreated, rhs.m_eUsernameTokenCreated) &&
           EqualsHelper.equals (m_eUsernameTokenDigest, rhs.m_eUsernameTokenDigest) &&
           EqualsHelper.equals (m_eUsernameTokenNonce, rhs.m_eUsernameTokenNonce) &&
           EqualsHelper.equals (m_sSendReceiptReplyPattern, rhs.m_sSendReceiptReplyPattern) &&
           EqualsHelper.equals (m_sUsernameTokenPassword, rhs.m_sUsernameTokenPassword) &&
           EqualsHelper.equals (m_sUsernameTokenUsername, rhs.m_sUsernameTokenUsername) &&
           EqualsHelper.equals (m_sWSSVersion, rhs.m_sWSSVersion) &&
           EqualsHelper.equals (m_sX509EncryptionAlgorithm, rhs.m_sX509EncryptionAlgorithm) &&
           EqualsHelper.equals (m_sX509EncryptionCertificate, rhs.m_sX509EncryptionCertificate) &&
           EqualsHelper.equals (m_sX509SignatureAlgorithm, rhs.m_sX509SignatureAlgorithm) &&
           EqualsHelper.equals (m_sX509SignatureCertificate, rhs.m_sX509SignatureCertificate) &&
           EqualsHelper.equals (m_sX509SignatureHashFunction, rhs.m_sX509SignatureHashFunction) &&
           EqualsHelper.equals (m_eX509Sign, rhs.m_eX509Sign) &&
           EqualsHelper.equals (m_eX509EncryptionEncrypt, rhs.m_eX509EncryptionEncrypt);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aX509EncryptionEncrypt)
                                       .append (m_aX509EncryptionMinimumStrength)
                                       .append (m_aX509Sign)
                                       .append (m_ePModeAuthorize)
                                       .append (m_eSendReceipt)
                                       .append (m_eUsernameTokenCreated)
                                       .append (m_eUsernameTokenDigest)
                                       .append (m_eUsernameTokenNonce)
                                       .append (m_sSendReceiptReplyPattern)
                                       .append (m_sUsernameTokenPassword)
                                       .append (m_sUsernameTokenUsername)
                                       .append (m_sWSSVersion)
                                       .append (m_sX509EncryptionAlgorithm)
                                       .append (m_sX509EncryptionCertificate)
                                       .append (m_sX509SignatureAlgorithm)
                                       .append (m_sX509SignatureCertificate)
                                       .append (m_sX509SignatureHashFunction)
                                       .append (m_eX509Sign)
                                       .append (m_eX509EncryptionEncrypt)
                                       .getHashCode ();
  }
}
