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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.dom.WSConstants;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Enumeration with all crypto key identification types (how the key
 * identification is transmitted). Deprecated constants have been taken over
 * deprecated.
 *
 * @author Philip Helger
 * @since 0.11.0
 */
public enum ECryptoKeyIdentifierType implements IHasID <String>
{
  /**
   * Send the signing certificate as a <code>BinarySecurityToken</code>. <br>
   * The signing method takes the signing certificate, converts it to a
   * <code>BinarySecurityToken</code>, puts it in the security header, and
   * inserts a <code>Reference</code> to the binary security token into the
   * <code>wsse:SecurityReferenceToken</code>. Thus the whole signing
   * certificate is transfered to the receiver. The X509 profile recommends to
   * use {@link #ISSUER_SERIAL} instead of sending the whole certificate. <br>
   * Please refer to WS Security specification X509 1.1 profile, chapter 3.3.2
   * and to WS Security SOAP Message security 1.1 specification, chapter 7.2
   * <br>
   * Note: only local references to BinarySecurityToken are supported
   */
  BST_DIRECT_REFERENCE ("bst", WSConstants.BST_DIRECT_REFERENCE),
  /**
   * Send the issuer name and the serial number of a certificate to the
   * receiver. <br>
   * In contrast to {@link #BST_DIRECT_REFERENCE} only the issuer name and the
   * serial number of the signing certificate are sent to the receiver. This
   * reduces the amount of data being sent. The encryption method uses the
   * public key associated with this certificate to encrypt the symmetric key
   * used to encrypt data. <br>
   * Please refer to WS Security specification X509 1.1 profile, chapter 3.3.3
   */
  ISSUER_SERIAL ("issuer-serial", WSConstants.ISSUER_SERIAL),
  /**
   * Send the certificate used to encrypt the symmetric key. <br>
   * The encryption method uses the public key associated with this certificate
   * to encrypt the symmetric key used to encrypt data. The certificate is
   * converted into a <code>KeyIdentifier</code> token and sent to the receiver.
   * Thus the complete certificate data is transfered to receiver. The X509
   * profile recommends to use {@link #ISSUER_SERIAL} instead of sending the
   * whole certificate. <br>
   * Please refer to WS Security SOAP Message security 1.1 specification,
   * chapter 7.3. Note that this is a NON-STANDARD method. The standard way to
   * refer to an X.509 Certificate via a KeyIdentifier is to use
   * {@link #SKI_KEY_IDENTIFIER}
   */
  X509_KEY_IDENTIFIER ("x509-key-id", WSConstants.X509_KEY_IDENTIFIER),
  /**
   * Send a <code>SubjectKeyIdentifier</code> to identify the signing
   * certificate. <br>
   * Refer to WS Security specification X509 1.1 profile, chapter 3.3.1
   */
  SKI_KEY_IDENTIFIER ("ski-key-id", WSConstants.SKI_KEY_IDENTIFIER),
  /**
   * <code>UT_SIGNING</code> is used internally only to set a specific Signature
   * behavior. The signing token is constructed from values in the UsernameToken
   * according to WS-Trust specification.
   */
  UT_SIGNING ("ui-signing", WSConstants.UT_SIGNING),
  /**
   * <code>THUMPRINT_IDENTIFIER</code> is used to set the specific key
   * identifier ThumbprintSHA1. This identifier uses the SHA-1 digest of a
   * security token to identify the security token. Please refer to chapter 7.2
   * of the OASIS WSS 1.1 specification.
   */
  THUMBPRINT_IDENTIFIER ("thumbprint-id", WSConstants.THUMBPRINT_IDENTIFIER),
  /**
   * <code>CUSTOM_SYMM_SIGNING</code> is used internally only to set a specific
   * Signature behavior. The signing key, reference id and value type are set
   * externally.
   */
  CUSTOM_SYMM_SIGNING ("custom-symm-signing", WSConstants.CUSTOM_SYMM_SIGNING),
  /**
   * <code>ENCRYPTED_KEY_SHA1_IDENTIFIER</code> is used to set the specific key
   * identifier EncryptedKeySHA1. This identifier uses the SHA-1 digest of a
   * security token to identify the security token. Please refer to chapter 7.3
   * of the OASIS WSS 1.1 specification.
   */
  ENCRYPTED_KEY_SHA1_IDENTIFIER ("encry-key-sha1-id", WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER),
  /**
   * <code>CUSTOM_SYMM_SIGNING_DIRECT</code> is used internally only to set a
   * specific Signature behavior. The signing key, reference id and value type
   * are set externally.
   */
  CUSTOM_SYMM_SIGNING_DIRECT ("custom-symm-signing-default", WSConstants.CUSTOM_SYMM_SIGNING_DIRECT),
  /**
   * <code>CUSTOM_KEY_IDENTIFIER</code> is used to set a KeyIdentifier to a
   * particular ID The reference id and value type are set externally.
   */
  CUSTOM_KEY_IDENTIFIER ("custom-key-id", WSConstants.CUSTOM_KEY_IDENTIFIER),
  /**
   * <code>KEY_VALUE</code> is used to set a ds:KeyInfo/ds:KeyValue element to
   * refer to either an RSA or DSA public key.
   */
  KEY_VALUE ("key-value", WSConstants.KEY_VALUE),
  /**
   * <code>ENDPOINT_KEY_IDENTIFIER</code> is used to specify service endpoint as
   * public key identifier. Constant is useful in case of symmetric holder of
   * key, where token service can determine target service public key to encrypt
   * shared secret.
   */
  ENDPOINT_KEY_IDENTIFIER ("endpoint-key-id", WSConstants.ENDPOINT_KEY_IDENTIFIER),
  /**
   * Sets the
   * <code>org.apache.wss4j.dom.message.WSSecSignature.build(Crypto)</code> or
   * the
   * <code>org.apache.wss4j.dom.message.WSSecEncrypt.build(Crypto, SecretKey)</code>
   * method to send the issuer name and the serial number of a certificate to
   * the receiver.<br>
   * In contrast to {@link #BST_DIRECT_REFERENCE} only the issuer name and the
   * serial number of the signing certificate are sent to the receiver. This
   * reduces the amount of data being sent. The encryption method uses the
   * public key associated with this certificate to encrypt the symmetric key
   * used to encrypt data. The issuer name format will use a quote delimited Rfc
   * 2253 format if necessary which is recognized by the Microsoft's WCF stack.
   * It also places a space before each subsequent RDN also required for WCF
   * interoperability. In addition, this format is know to be correctly
   * interpreted by Java.<br>
   * Please refer to WS Security specification X509 1.1 profile, chapter 3.3.3
   */
  ISSUER_SERIAL_QUOTE_FORMAT ("issuer-serial-quote-format", WSConstants.ISSUER_SERIAL_QUOTE_FORMAT);

  private final String m_sID;
  private final int m_nTypeID;

  ECryptoKeyIdentifierType (@Nonnull @Nonempty final String sID, final int nTypeID)
  {
    m_sID = sID;
    m_nTypeID = nTypeID;
  }

  /**
   * The String ID for usage in phase4.
   */
  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The WSS4J internal type ID.
   */
  public int getTypeID ()
  {
    return m_nTypeID;
  }

  @Nullable
  public static ECryptoKeyIdentifierType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoKeyIdentifierType.class, sID);
  }

  @Nonnull
  public static ECryptoKeyIdentifierType getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (ECryptoKeyIdentifierType.class, sID);
  }

  @Nullable
  public static ECryptoKeyIdentifierType getFromIDOrDefault (@Nullable final String sID,
                                                             @Nullable final ECryptoKeyIdentifierType eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoKeyIdentifierType.class, sID, eDefault);
  }

  @Nullable
  public static ECryptoKeyIdentifierType getFromTypeIDOrNull (final int nTypeID)
  {
    return EnumHelper.findFirst (ECryptoKeyIdentifierType.class, x -> x.getTypeID () == nTypeID);
  }
}
