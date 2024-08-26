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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.phase4.wss.EWSSVersion;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * XML converter for objects of class {@link PModeLegSecurity}.
 *
 * @author Philip Helger
 */
public class PModeLegSecurityMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLegSecurity>
{
  private static final IMicroQName ATTR_WSS_VERSION = new MicroQName ("WSSVersion");
  private static final String ELEMENT_X509_SIGN_ELEMENT = "X509SignElement";
  private static final String ELEMENT_X509_SIGN_ATTACHMENT = "X509SignAttachment";
  private static final String ELEMENT_X509_SIGNATURE_CERTIFICATE = "X509SignatureCertificate";
  private static final IMicroQName ATTR_X509_SIGNATURE_HASH_FUNCTION = new MicroQName ("X509SignatureHashFunction");
  private static final IMicroQName ATTR_X509_SIGNATURE_ALGORITHM = new MicroQName ("X509SignatureAlgorithm");
  private static final String ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT = "X509EncryptionEncryptElement";
  private static final String ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT = "X509EncryptionEncryptAttachment";
  private static final String ELEMENT_X509_ENCRYPTION_CERTIFICATE = "X509EncryptionCertificate";
  private static final IMicroQName ATTR_X509_ENCRYPTION_ALGORITHM = new MicroQName ("X509EncryptionAlgorithm");
  private static final IMicroQName ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH = new MicroQName ("X509EncryptionMinimumStrength");
  private static final IMicroQName ATTR_USERNAME_TOKEN_USERNAME = new MicroQName ("UsernameTokenUsername");
  private static final IMicroQName ATTR_USERNAME_TOKEN_PASSWORD = new MicroQName ("UsernameTokenPassword");
  private static final IMicroQName ATTR_USERNAME_TOKEN_DIGEST = new MicroQName ("UsernameTokenDigest");
  private static final IMicroQName ATTR_USERNAME_TOKEN_NONCE = new MicroQName ("UsernameTokenNonce");
  private static final IMicroQName ATTR_USERNAME_TOKEN_CREATED = new MicroQName ("UsernameTokenCreated");
  private static final IMicroQName ATTR_PMODE_AUTHORIZE = new MicroQName ("PModeAuthorize");
  private static final IMicroQName ATTR_SEND_RECEIPT = new MicroQName ("SendReceipt");
  private static final IMicroQName ATTR_SEND_RECEIPT_REPLY_PATTERN = new MicroQName ("SendReceiptReplyPattern");
  private static final IMicroQName ATTR_SEND_RECEIPT_NON_REPUDIATION = new MicroQName ("SendReceiptNonRepudiation");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeLegSecurity aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    ret.setAttribute (ATTR_WSS_VERSION, aValue.getWSSVersionAsString ());
    for (final String sSign : aValue.getAllX509SignElements ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_SIGN_ELEMENT).appendText (sSign);
    }
    for (final String sSign : aValue.getAllX509SignAttachments ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_SIGN_ATTACHMENT).appendText (sSign);
    }
    if (aValue.hasX509SignatureCertificate ())
      ret.appendElement (sNamespaceURI, ELEMENT_X509_SIGNATURE_CERTIFICATE)
         .appendText (aValue.getX509SignatureCertificate ());
    ret.setAttribute (ATTR_X509_SIGNATURE_HASH_FUNCTION, aValue.getX509SignatureHashFunctionID ());
    ret.setAttribute (ATTR_X509_SIGNATURE_ALGORITHM, aValue.getX509SignatureAlgorithmID ());
    for (final String sEncrypt : aValue.getAllX509EncryptionEncryptElements ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT).appendText (sEncrypt);
    }
    for (final String sEncrypt : aValue.getAllX509EncryptionEncryptAttachments ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT).appendText (sEncrypt);
    }
    if (aValue.hasX509EncryptionCertificate ())
      ret.appendElement (sNamespaceURI, ELEMENT_X509_ENCRYPTION_CERTIFICATE)
         .appendText (aValue.getX509EncryptionCertificate ());
    ret.setAttribute (ATTR_X509_ENCRYPTION_ALGORITHM, aValue.getX509EncryptionAlgorithmID ());
    if (aValue.hasX509EncryptionMinimumStrength ())
      ret.setAttribute (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH, aValue.getX509EncryptionMinimumStrength ().intValue ());
    ret.setAttribute (ATTR_USERNAME_TOKEN_USERNAME, aValue.getUsernameTokenUsername ());
    ret.setAttribute (ATTR_USERNAME_TOKEN_PASSWORD, aValue.getUsernameTokenPassword ());
    if (aValue.isUsernameTokenDigestDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_DIGEST, aValue.isUsernameTokenDigest ());
    if (aValue.isUsernameTokenNonceDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_NONCE, aValue.isUsernameTokenNonce ());
    if (aValue.isUsernameTokenCreatedDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_CREATED, aValue.isUsernameTokenCreated ());
    if (aValue.isPModeAuthorizeDefined ())
      ret.setAttribute (ATTR_PMODE_AUTHORIZE, aValue.isPModeAuthorize ());
    if (aValue.isSendReceiptDefined ())
      ret.setAttribute (ATTR_SEND_RECEIPT, aValue.isSendReceipt ());
    ret.setAttribute (ATTR_SEND_RECEIPT_REPLY_PATTERN, aValue.getSendReceiptReplyPatternID ());
    if (aValue.isSendReceiptNonRepudiationDefined ())
      ret.setAttribute (ATTR_SEND_RECEIPT_NON_REPUDIATION, aValue.isSendReceiptNonRepudiation ());
    return ret;
  }

  @Nonnull
  public PModeLegSecurity convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sWSSVersion = aElement.getAttributeValue (ATTR_WSS_VERSION);
    final EWSSVersion eWSSVersion = EWSSVersion.getFromVersionOrNull (sWSSVersion);
    if (eWSSVersion == null && sWSSVersion != null)
    {
      throw new IllegalStateException ("Invalid WSS version '" + sWSSVersion + "'");
    }

    final ICommonsList <String> aX509SignElement = new CommonsArrayList <> ();
    for (final IMicroElement aSignElement : aElement.getAllChildElements (ELEMENT_X509_SIGN_ELEMENT))
    {
      aX509SignElement.add (aSignElement.getTextContentTrimmed ());
    }
    final ICommonsList <String> aX509SignAttachment = new CommonsArrayList <> ();
    for (final IMicroElement aSignElement : aElement.getAllChildElements (ELEMENT_X509_SIGN_ATTACHMENT))
    {
      aX509SignAttachment.add (aSignElement.getTextContentTrimmed ());
    }
    final String sX509SignatureCertificate = MicroHelper.getChildTextContentTrimmed (aElement,
                                                                                     ELEMENT_X509_SIGNATURE_CERTIFICATE);
    final String sX509SignatureHashFunction = aElement.getAttributeValue (ATTR_X509_SIGNATURE_HASH_FUNCTION);
    final ECryptoAlgorithmSignDigest eX509SignatureHashFunction = ECryptoAlgorithmSignDigest.getFromIDOrNull (sX509SignatureHashFunction);
    if (eX509SignatureHashFunction == null && sX509SignatureHashFunction != null)
    {
      throw new IllegalStateException ("Invalid signature hash function '" + sX509SignatureHashFunction + "'");
    }

    final String sX509SignatureAlgorithm = aElement.getAttributeValue (ATTR_X509_SIGNATURE_ALGORITHM);
    final ECryptoAlgorithmSign eX509SignatureAlgorithm = ECryptoAlgorithmSign.getFromIDOrNull (sX509SignatureAlgorithm);
    if (eX509SignatureAlgorithm == null && sX509SignatureAlgorithm != null)
    {
      throw new IllegalStateException ("Invalid signature algorithm '" + sX509SignatureAlgorithm + "'");
    }

    final ICommonsList <String> aX509EncryptionEncryptElement = new CommonsArrayList <> ();
    for (final IMicroElement aEncryptElement : aElement.getAllChildElements (ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT))
    {
      aX509EncryptionEncryptElement.add (aEncryptElement.getTextContentTrimmed ());
    }
    final ICommonsList <String> aX509EncryptionEncryptAttachment = new CommonsArrayList <> ();
    for (final IMicroElement aEncryptElement : aElement.getAllChildElements (ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT))
    {
      aX509EncryptionEncryptAttachment.add (aEncryptElement.getTextContentTrimmed ());
    }
    final String sX509EncryptionCertificate = MicroHelper.getChildTextContentTrimmed (aElement,
                                                                                      ELEMENT_X509_ENCRYPTION_CERTIFICATE);
    final String sX509EncryptionAlgorithm = aElement.getAttributeValue (ATTR_X509_ENCRYPTION_ALGORITHM);
    final ECryptoAlgorithmCrypt eX509EncryptionAlgorithm = ECryptoAlgorithmCrypt.getFromIDOrNull (sX509EncryptionAlgorithm);
    if (eX509EncryptionAlgorithm == null && sX509EncryptionAlgorithm != null)
    {
      throw new IllegalStateException ("Invalid encrypt algorithm '" + sX509EncryptionAlgorithm + "'");
    }

    final Integer aX509EncryptionMinimumStrength = aElement.getAttributeValueWithConversion (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH,
                                                                                             Integer.class);
    final String sUsernameTokenUsername = aElement.getAttributeValue (ATTR_USERNAME_TOKEN_USERNAME);
    final String sUsernameTokenPassword = aElement.getAttributeValue (ATTR_USERNAME_TOKEN_PASSWORD);
    final ETriState eUsernameTokenDigest = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_DIGEST),
                                                        PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    final ETriState eUsernameTokenNonce = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_NONCE),
                                                       PModeLegSecurity.DEFAULT_USERNAME_TOKEN_NONCE);
    final ETriState eUsernameTokenCreated = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_CREATED),
                                                         PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState ePModeAuthorize = getTriState (aElement.getAttributeValue (ATTR_PMODE_AUTHORIZE),
                                                   PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);
    final ETriState eSendReceipt = getTriState (aElement.getAttributeValue (ATTR_SEND_RECEIPT),
                                                PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    final String sSendReceiptReplyPattern = aElement.getAttributeValue (ATTR_SEND_RECEIPT_REPLY_PATTERN);
    final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern = EPModeSendReceiptReplyPattern.getFromIDOrNull (sSendReceiptReplyPattern);
    if (eSendReceiptReplyPattern == null && sSendReceiptReplyPattern != null)
    {
      throw new IllegalStateException ("Invalid SendReceipt ReplyPattern version '" + sSendReceiptReplyPattern + "'");
    }

    final ETriState eSendReceiptNonRepudiation = getTriState (aElement.getAttributeValue (ATTR_SEND_RECEIPT_NON_REPUDIATION),
                                                              PModeLegSecurity.DEFAULT_SEND_RECEIPT_NON_REPUDIATION);

    return new PModeLegSecurity (eWSSVersion,
                                 aX509SignElement,
                                 aX509SignAttachment,
                                 sX509SignatureCertificate,
                                 eX509SignatureHashFunction,
                                 eX509SignatureAlgorithm,
                                 aX509EncryptionEncryptElement,
                                 aX509EncryptionEncryptAttachment,
                                 sX509EncryptionCertificate,
                                 eX509EncryptionAlgorithm,
                                 aX509EncryptionMinimumStrength,
                                 sUsernameTokenUsername,
                                 sUsernameTokenPassword,
                                 eUsernameTokenDigest,
                                 eUsernameTokenNonce,
                                 eUsernameTokenCreated,
                                 ePModeAuthorize,
                                 eSendReceipt,
                                 eSendReceiptReplyPattern,
                                 eSendReceiptNonRepudiation);
  }

}
