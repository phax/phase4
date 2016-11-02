/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as4lib.partner;

import javax.annotation.concurrent.Immutable;

import com.helger.as4lib.attachment.EAS4CompressionMode;

/**
 * Partnership IDs and attribute names. The IDs (starting with PID_) are using
 * for setSenderID/setReceiverID where as the attributes (starting with PA_) are
 * used with setAttribute.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPartnershipIDs
{
  /** AS2 ID (sender or receiver) */
  public static final String PID_AS2 = "as2_id";
  /** Alias to an X509 Certificate (sender or receiver) */
  public static final String PID_X509_ALIAS = "x509_alias";
  /** Email address (sender or receiver) */
  public static final String PID_EMAIL = "email";

  /** URL destination for AS2 transactions */
  public static final String PA_AS2_URL = "as2_url";
  /** Fill in to request an MDN for a transaction */
  public static final String PA_AS2_MDN_TO = "as2_mdn_to";
  /** Requested options for returned MDN */
  public static final String PA_AS2_MDN_OPTIONS = "as2_mdn_options";
  /** URL destination for an async MDN */
  public static final String PA_AS2_RECEIPT_OPTION = "as2_receipt_option";
  /** format to use for message-id if not default */
  public static final String PA_MESSAGEID_FORMAT = "messageid";
  /** Subject sent in MDN messages */
  public static final String PA_MDN_SUBJECT = "mdnsubject";
  /**
   * If set and an error occurs while processing a document, an error MDN will
   * not be sent. This flag was made because some AS2 products don't provide
   * email or some other external notification when an error MDN is received.
   */
  public static final String PA_BLOCK_ERROR_MDN = "blockerrormdn";

  /** set this to override the date format used when generating message IDs */
  public static final String PA_DATE_FORMAT = "mid_date_format";

  /**
   * Set this to the algorithm to use for encryption, check
   * {@link com.helger.as4lib.crypto.ECryptoAlgorithmCrypt} constants for values
   * (using the value of the <code>getID()</code> method)
   */
  public static final String PA_ENCRYPT = "encrypt";
  /**
   * Set this to the signature digest algorithm to sign sent messages, check
   * {@link com.helger.as4lib.crypto.ECryptoAlgorithmSign} constants for values
   * (using the value of the <code>getID()</code> method)
   */
  public static final String PA_SIGN = "sign";
  /** AS1 or AS2 */
  public static final String PA_PROTOCOL = "protocol";
  /** Subject sent in messages */
  public static final String PA_SUBJECT = "subject";
  /** optional content transfer encoding value for outgoing messages */
  public static final String PA_CONTENT_TRANSFER_ENCODING = "content_transfer_encoding";
  /**
   * optional content transfer encoding value for incoming messages if not
   * specified
   */
  public static final String PA_CONTENT_TRANSFER_ENCODING_RECEIVE = "content_transfer_encoding_receive";
  /**
   * Optional compression type. Check {@link EAS4CompressionMode} constants for
   * values (using the value of the <code>getID()</code> method)
   */
  public static final String PA_COMPRESSION_TYPE = "compression_type";
  /**
   * Optional compression mode
   */
  public static final String PA_COMPRESSION_MODE = "compression_mode";
  /**
   * Value for {@link #PA_COMPRESSION_MODE}: compress before sign
   */
  public static final String COMPRESS_BEFORE_SIGNING = "compress-before-signing";
  /**
   * Value for {@link #PA_COMPRESSION_MODE}: compress after sign
   */
  public static final String COMPRESS_AFTER_SIGNING = "compress-after-signing";
  /**
   * Special attribute to force decryption of a received message, even if the
   * Content-Type header claims the messages is not encrypted. This is a
   * work-around for non spec-compliant senders. Must be set to
   * <code>true</code> to take effect.
   */
  public static final String PA_FORCE_DECRYPT = "force_decrypt";
  /**
   * Special attribute to disable decryption of a received message, even if the
   * Content-Type header claims the messages is encrypted. This is a work-around
   * for non spec-compliant senders. Must be set to <code>true</code> to take
   * effect.
   */
  public static final String PA_DISABLE_DECRYPT = "disable_decrypt";
  /**
   * Special attribute to force signature verification of a received message,
   * even if the Content-Type header claims the messages is not signed. This is
   * a work-around for non spec-compliant senders. Must be set to
   * <code>true</code> to take effect.
   */
  public static final String PA_FORCE_VERIFY = "force_verify";
  /**
   * Special attribute to disable signature verification of a received message,
   * even if the Content-Type header claims the messages is signed. This is a
   * work-around for non spec-compliant senders. Must be set to
   * <code>true</code> to take effect.
   */
  public static final String PA_DISABLE_VERIFY = "disable_verify";
  /**
   * Indicates whether the certificate used for signing should be part of the
   * signed content (when <code>true</code>) or not (when <code>false</code>).
   * If not set the value of the AS2 session is used. Must be set to
   * <code>true</code> or <code>false</code> to take effect.
   */
  public static final String PA_SIGN_INCLUDE_CERT_IN_BODY_PART = "sign_include_cert_in_body_part";
  /**
   * Define whether a certificate passed in the signed MIME body part shall be
   * used to verify the signature (when <code>true</code>) or whether to always
   * use the certificate provided in the partnership (when <code>false</code>).
   * If not set the value of the AS2 session is used. Must be set to
   * <code>true</code> or <code>false</code> to take effect.
   */
  public static final String PA_VERIFY_USE_CERT_IN_BODY_PART = "verify_use_cert_in_body_part";
  /**
   * Special attribute to disable decompression of a received message, even if
   * the Content-Type header claims the messages is compressed. This is a
   * work-around for non spec-compliant senders. Must be set to
   * <code>true</code> to take effect.
   */
  public static final String PA_DISABLE_DECOMPRESS = "disable_decompress";

  private CPartnershipIDs ()
  {}
}
