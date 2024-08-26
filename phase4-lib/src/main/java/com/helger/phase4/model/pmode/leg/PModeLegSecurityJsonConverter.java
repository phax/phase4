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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.IJsonValue;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.phase4.wss.EWSSVersion;

/**
 * JSON converter for objects of class {@link PModeLegSecurity}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegSecurityJsonConverter
{
  private static final String ATTR_WSS_VERSION = "WSSVersion";
  private static final String ELEMENT_X509_SIGN_ELEMENT = "X509SignElement";
  private static final String ELEMENT_X509_SIGN_ATTACHMENT = "X509SignAttachment";
  private static final String ELEMENT_X509_SIGNATURE_CERTIFICATE = "X509SignatureCertificate";
  private static final String ATTR_X509_SIGNATURE_HASH_FUNCTION = "X509SignatureHashFunction";
  private static final String ATTR_X509_SIGNATURE_ALGORITHM = "X509SignatureAlgorithm";
  private static final String ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT = "X509EncryptionEncryptElement";
  private static final String ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT = "X509EncryptionEncryptAttachment";
  private static final String ELEMENT_X509_ENCRYPTION_CERTIFICATE = "X509EncryptionCertificate";
  private static final String ATTR_X509_ENCRYPTION_ALGORITHM = "X509EncryptionAlgorithm";
  private static final String ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH = "X509EncryptionMinimumStrength";
  private static final String ATTR_USERNAME_TOKEN_USERNAME = "UsernameTokenUsername";
  private static final String ATTR_USERNAME_TOKEN_PASSWORD = "UsernameTokenPassword";
  private static final String ATTR_USERNAME_TOKEN_DIGEST = "UsernameTokenDigest";
  private static final String ATTR_USERNAME_TOKEN_NONCE = "UsernameTokenNonce";
  private static final String ATTR_USERNAME_TOKEN_CREATED = "UsernameTokenCreated";
  private static final String ATTR_PMODE_AUTHORIZE = "PModeAuthorize";
  private static final String ATTR_SEND_RECEIPT = "SendReceipt";
  private static final String ATTR_SEND_RECEIPT_REPLY_PATTERN = "SendReceiptReplyPattern";
  private static final String ATTR_SEND_RECEIPT_NON_REPUDIATION = "SendReceiptNonRepudiation";

  private PModeLegSecurityJsonConverter ()
  {}

  /**
   * Convert the provided {@link PModeLegSecurity} object to a JSON object. The
   * conversion from JSON Object back to a domain object happens via
   * {@link #convertToNative(IJsonObject)}.
   *
   * @param aValue
   *        The value to be converted. May not be <code>null</code>.
   * @return The non-<code>null</code> JSON object filled with the necessary
   *         values.
   */
  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLegSecurity aValue)
  {
    final IJsonObject ret = new JsonObject ();

    if (aValue.hasWSSVersion ())
      ret.add (ATTR_WSS_VERSION, aValue.getWSSVersionAsString ());

    if (aValue.x509SignElements ().isNotEmpty ())
      ret.addJson (ELEMENT_X509_SIGN_ELEMENT, new JsonArray ().addAll (aValue.x509SignElements ()));
    if (aValue.x509SignAttachments ().isNotEmpty ())
      ret.addJson (ELEMENT_X509_SIGN_ATTACHMENT, new JsonArray ().addAll (aValue.x509SignAttachments ()));
    if (aValue.hasX509SignatureCertificate ())
      ret.add (ELEMENT_X509_SIGNATURE_CERTIFICATE, aValue.getX509SignatureCertificate ());
    if (aValue.hasX509SignatureHashFunction ())
      ret.add (ATTR_X509_SIGNATURE_HASH_FUNCTION, aValue.getX509SignatureHashFunctionID ());
    if (aValue.hasX509SignatureAlgorithm ())
      ret.add (ATTR_X509_SIGNATURE_ALGORITHM, aValue.getX509SignatureAlgorithmID ());

    if (aValue.x509EncryptionEncryptElements ().isNotEmpty ())
      ret.addJson (ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT,
                   new JsonArray ().addAll (aValue.x509EncryptionEncryptElements ()));
    if (aValue.x509EncryptionEncryptAttachments ().isNotEmpty ())
      ret.addJson (ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT,
                   new JsonArray ().addAll (aValue.x509EncryptionEncryptAttachments ()));
    if (aValue.hasX509EncryptionCertificate ())
      ret.add (ELEMENT_X509_ENCRYPTION_CERTIFICATE, aValue.getX509EncryptionCertificate ());
    ret.add (ATTR_X509_ENCRYPTION_ALGORITHM, aValue.getX509EncryptionAlgorithmID ());
    if (aValue.hasX509EncryptionMinimumStrength ())
      ret.add (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH, aValue.getX509EncryptionMinimumStrength ().intValue ());

    if (aValue.hasUsernameTokenUsername ())
      ret.add (ATTR_USERNAME_TOKEN_USERNAME, aValue.getUsernameTokenUsername ());
    if (aValue.hasUsernameTokenPassword ())
      ret.add (ATTR_USERNAME_TOKEN_PASSWORD, aValue.getUsernameTokenPassword ());
    if (aValue.isUsernameTokenDigestDefined ())
      ret.add (ATTR_USERNAME_TOKEN_DIGEST, aValue.isUsernameTokenDigest ());
    if (aValue.isUsernameTokenNonceDefined ())
      ret.add (ATTR_USERNAME_TOKEN_NONCE, aValue.isUsernameTokenNonce ());
    if (aValue.isUsernameTokenCreatedDefined ())
      ret.add (ATTR_USERNAME_TOKEN_CREATED, aValue.isUsernameTokenCreated ());

    if (aValue.isPModeAuthorizeDefined ())
      ret.add (ATTR_PMODE_AUTHORIZE, aValue.isPModeAuthorize ());
    if (aValue.isSendReceiptDefined ())
      ret.add (ATTR_SEND_RECEIPT, aValue.isSendReceipt ());
    if (aValue.hasSendReceiptReplyPattern ())
      ret.add (ATTR_SEND_RECEIPT_REPLY_PATTERN, aValue.getSendReceiptReplyPatternID ());
    if (aValue.isSendReceiptNonRepudiationDefined ())
      ret.add (ATTR_SEND_RECEIPT_NON_REPUDIATION, aValue.isSendReceiptNonRepudiation ());
    return ret;
  }

  /**
   * Convert the provided JSON to a {@link PModeLegSecurity} object.
   *
   * @param aElement
   *        The JSON object to be converted. May not be <code>null</code>.
   * @return A non-<code>null</code> {@link PModeLegSecurity}
   * @throws IllegalStateException
   *         In case of an unsupported value
   */
  @Nonnull
  public static PModeLegSecurity convertToNative (@Nonnull final IJsonObject aElement)
  {
    final String sWSSVersion = aElement.getAsString (ATTR_WSS_VERSION);
    final EWSSVersion eWSSVersion = EWSSVersion.getFromVersionOrNull (sWSSVersion);
    if (eWSSVersion == null && sWSSVersion != null)
      throw new IllegalStateException ("Invalid WSS version '" + sWSSVersion + "'");

    final ICommonsList <String> aX509SignElements = new CommonsArrayList <> ();
    final IJsonArray aSignElement = aElement.getAsArray (ELEMENT_X509_SIGN_ELEMENT);
    if (aSignElement != null)
      for (final IJsonValue aItem : aSignElement.iteratorValues ())
        aX509SignElements.add (aItem.getAsString ());

    final ICommonsList <String> aX509SignAttachments = new CommonsArrayList <> ();
    final IJsonArray aSignAttachment = aElement.getAsArray (ELEMENT_X509_SIGN_ATTACHMENT);
    if (aSignAttachment != null)
      for (final IJsonValue aItem : aSignAttachment.iteratorValues ())
        aX509SignAttachments.add (aItem.getAsString ());

    final String sX509SignatureCertificate = aElement.getAsString (ELEMENT_X509_SIGNATURE_CERTIFICATE);
    final String sX509SignatureHashFunction = aElement.getAsString (ATTR_X509_SIGNATURE_HASH_FUNCTION);
    final ECryptoAlgorithmSignDigest eX509SignatureHashFunction = ECryptoAlgorithmSignDigest.getFromIDOrNull (sX509SignatureHashFunction);
    if (eX509SignatureHashFunction == null && sX509SignatureHashFunction != null)
      throw new IllegalStateException ("Invalid signature hash function '" + sX509SignatureHashFunction + "'");

    final String sX509SignatureAlgorithm = aElement.getAsString (ATTR_X509_SIGNATURE_ALGORITHM);
    final ECryptoAlgorithmSign eX509SignatureAlgorithm = ECryptoAlgorithmSign.getFromIDOrNull (sX509SignatureAlgorithm);
    if (eX509SignatureAlgorithm == null && sX509SignatureAlgorithm != null)
      throw new IllegalStateException ("Invalid signature algorithm '" + sX509SignatureAlgorithm + "'");

    final ICommonsList <String> aX509EncryptionElements = new CommonsArrayList <> ();
    final IJsonArray aEncryptElement = aElement.getAsArray (ELEMENT_X509_ENCRYPTION_ENCRYPT_ELEMENT);
    if (aEncryptElement != null)
      for (final IJsonValue aItem : aEncryptElement.iteratorValues ())
        aX509EncryptionElements.add (aItem.getAsString ());

    final ICommonsList <String> aX509EncryptionAttachments = new CommonsArrayList <> ();
    final IJsonArray aEncryptAttachment = aElement.getAsArray (ELEMENT_X509_ENCRYPTION_ENCRYPT_ATTACHMENT);
    if (aEncryptAttachment != null)
      for (final IJsonValue aItem : aEncryptAttachment.iteratorValues ())
        aX509EncryptionAttachments.add (aItem.getAsString ());

    final String sX509EncryptionCertificate = aElement.getAsString (ELEMENT_X509_ENCRYPTION_CERTIFICATE);
    final String sX509EncryptionAlgorithm = aElement.getAsString (ATTR_X509_ENCRYPTION_ALGORITHM);
    final ECryptoAlgorithmCrypt eX509EncryptionAlgorithm = ECryptoAlgorithmCrypt.getFromIDOrNull (sX509EncryptionAlgorithm);
    if (eX509EncryptionAlgorithm == null && sX509EncryptionAlgorithm != null)
      throw new IllegalStateException ("Invalid encrypt algorithm '" + sX509EncryptionAlgorithm + "'");

    final Integer aX509EncryptionMinimumStrength = aElement.getAsIntObj (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH);
    final String sUsernameTokenUsername = aElement.getAsString (ATTR_USERNAME_TOKEN_USERNAME);
    final String sUsernameTokenPassword = aElement.getAsString (ATTR_USERNAME_TOKEN_PASSWORD);
    final ETriState eUsernameTokenDigest = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_USERNAME_TOKEN_DIGEST),
                                                                                        PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    final ETriState eUsernameTokenNonce = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_USERNAME_TOKEN_NONCE),
                                                                                       PModeLegSecurity.DEFAULT_USERNAME_TOKEN_NONCE);
    final ETriState eUsernameTokenCreated = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_USERNAME_TOKEN_CREATED),
                                                                                         PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState ePModeAuthorize = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_PMODE_AUTHORIZE),
                                                                                   PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);
    final ETriState eSendReceipt = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_SEND_RECEIPT),
                                                                                PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    final String sSendReceiptReplyPattern = aElement.getAsString (ATTR_SEND_RECEIPT_REPLY_PATTERN);
    final EPModeSendReceiptReplyPattern eSendReceiptReplyPattern = EPModeSendReceiptReplyPattern.getFromIDOrNull (sSendReceiptReplyPattern);
    if (eSendReceiptReplyPattern == null && sSendReceiptReplyPattern != null)
      throw new IllegalStateException ("Invalid SendReceipt ReplyPattern version '" + sSendReceiptReplyPattern + "'");

    final ETriState eSendReceiptNonRepudiation = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (ATTR_SEND_RECEIPT_NON_REPUDIATION),
                                                                                              PModeLegSecurity.DEFAULT_SEND_RECEIPT_NON_REPUDIATION);

    return new PModeLegSecurity (eWSSVersion,
                                 aX509SignElements,
                                 aX509SignAttachments,
                                 sX509SignatureCertificate,
                                 eX509SignatureHashFunction,
                                 eX509SignatureAlgorithm,
                                 aX509EncryptionElements,
                                 aX509EncryptionAttachments,
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
