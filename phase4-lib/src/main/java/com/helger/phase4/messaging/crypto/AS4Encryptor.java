/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.MimeMessageCreator;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.wss.WSSConfigManager;
import com.helger.phase4.wss.WSSSynchronizer;

import jakarta.mail.MessagingException;

/**
 * Encryption helper
 *
 * @author Philip Helger
 */
@Immutable
public final class AS4Encryptor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4Encryptor.class);

  private AS4Encryptor ()
  {}

  @Nonnull
  private static WSSecEncrypt _createEncrypt (@Nonnull final WSSecHeader aSecHeader,
                                              @Nonnull final AS4CryptParams aCryptParams)
  {
    final WSSecEncrypt aBuilder = new WSSecEncrypt (aSecHeader);
    // As the receiver MAY not have pre-configured the signing leaf certificate,
    // a BinarySecurityToken token reference MUST be used to reference the
    // signing certificate.
    aBuilder.setKeyIdentifierType (aCryptParams.getKeyIdentifierType ().getTypeID ());
    aBuilder.setSymmetricEncAlgorithm (aCryptParams.getAlgorithmCrypt ().getAlgorithmURI ());
    aBuilder.setKeyEncAlgo (aCryptParams.getKeyEncAlgorithm ());
    aBuilder.setMGFAlgorithm (aCryptParams.getMGFAlgorithm ());
    aBuilder.setDigestAlgorithm (aCryptParams.getDigestAlgorithm ());
    // Encrypted key must be contained
    aBuilder.setEncryptSymmKey (true);
    if (aCryptParams.hasCertificate ())
    {
      // Certificate was provided externally
      aBuilder.setUseThisCert (aCryptParams.getCertificate ());
    }
    else
      if (aCryptParams.hasAlias ())
      {
        // No PW needed here, because we encrypt with the public key
        aBuilder.setUserInfo (aCryptParams.getAlias ());
      }
    return aBuilder;
  }

  @Nonnull
  private static Document _encryptSoapBodyPayload (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                   @Nonnull final ESoapVersion eSoapVersion,
                                                   @Nonnull final Document aDoc,
                                                   final boolean bMustUnderstand,
                                                   @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException
  {
    LOGGER.info ("Now encrypting AS4 SOAP message. KeyIdentifierType=" +
                 aCryptParams.getKeyIdentifierType ().name () +
                 "; EncAlgo=" +
                 aCryptParams.getAlgorithmCrypt ().getAlgorithmURI () +
                 "; KeyEncAlgo=" +
                 aCryptParams.getKeyEncAlgorithm () +
                 "; MgfAlgo=" +
                 aCryptParams.getMGFAlgorithm () +
                 "; DigestAlgo=" +
                 aCryptParams.getDigestAlgorithm () +
                 (aCryptParams.hasAlias () ? "; KeyAlias=" + aCryptParams.getAlias () : "") +
                 (aCryptParams.hasCertificate () ? "; CertificateSubjectCN=" +
                                                   aCryptParams.getCertificate ().getSubjectDN ().getName ()
                                                 : ""));

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = _createEncrypt (aSecHeader, aCryptParams);
    aBuilder.getParts ()
            .add (new WSEncryptionPart ("Body", eSoapVersion.getNamespaceURI (), AS4Signer.ENCRYPTION_MODE_CONTENT));

    // Ensure mustUnderstand value
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSoapVersion.getMustUnderstandValue (bMustUnderstand));

    // Generate a session key
    final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_128);
    final SecretKey aSymmetricKey = aKeyGen.generateKey ();

    return aBuilder.build (aCryptoFactory.getCrypto (), aSymmetricKey);
  }

  /**
   * Encrypt the SOAP "Body" content.
   *
   * @param aCryptoFactory
   *        Crypto factory to use. May not be <code>null</code>.
   * @param eSoapVersion
   *        The SOAP version to use. May not be <code>null</code>.
   * @param aDoc
   *        The SOAP XML document to be encrypted. May not be <code>null</code>.
   * @param bMustUnderstand
   *        must understand indicator.
   * @param aCryptParams
   *        Encryption parameter settings. May not be <code>null</code>.
   * @return The XML document with the encrypted SOAP "Body".
   * @throws WSSecurityException
   *         in case of error
   */
  @Nonnull
  public static Document encryptSoapBodyPayload (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                 @Nonnull final ESoapVersion eSoapVersion,
                                                 @Nonnull final Document aDoc,
                                                 final boolean bMustUnderstand,
                                                 @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notNull (aCryptParams, "CryptParams");

    if (AS4Configuration.isWSS4JSynchronizedSecurity ())
    {
      // Synchronize
      return WSSSynchronizer.call ( () -> _encryptSoapBodyPayload (aCryptoFactory,
                                                                   eSoapVersion,
                                                                   aDoc,
                                                                   bMustUnderstand,
                                                                   aCryptParams));
    }

    // Ensure WSSConfig is initialized
    WSSConfigManager.getInstance ();

    return _encryptSoapBodyPayload (aCryptoFactory, eSoapVersion, aDoc, bMustUnderstand, aCryptParams);
  }

  @Nonnull
  private static AS4MimeMessage _encryptMimeMessage (@Nonnull final ESoapVersion eSoapVersion,
                                                     @Nonnull final Document aDoc,
                                                     @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                     @Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                     final boolean bMustUnderstand,
                                                     @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                     @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException
  {
    LOGGER.info ("Now encrypting AS4 MIME message. KeyIdentifierType=" +
                 aCryptParams.getKeyIdentifierType ().name () +
                 "; EncAlgo=" +
                 aCryptParams.getAlgorithmCrypt ().getAlgorithmURI () +
                 "; KeyEncAlgo=" +
                 aCryptParams.getKeyEncAlgorithm () +
                 "; MgfAlgo=" +
                 aCryptParams.getMGFAlgorithm () +
                 "; DigestAlgo=" +
                 aCryptParams.getDigestAlgorithm () +
                 (aCryptParams.hasAlias () ? "; KeyAlias=" + aCryptParams.getAlias () : "") +
                 (aCryptParams.hasCertificate () ? "; CertificateSubjectCN=" +
                                                   aCryptParams.getCertificate ().getSubjectDN ().getName ()
                                                 : ""));

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = _createEncrypt (aSecHeader, aCryptParams);

    // "cid:Attachments" is a predefined ID
    aBuilder.getParts ()
            .add (new WSEncryptionPart (MessageHelperMethods.PREFIX_CID + "Attachments",
                                        AS4Signer.ENCRYPTION_MODE_CONTENT));

    WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = null;
    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments, aResHelper);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    // Ensure mustUnderstand value
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSoapVersion.getMustUnderstandValue (bMustUnderstand));

    // Generate a session key
    final KeyGenerator aKeyGen = KeyUtils.getKeyGenerator (WSS4JConstants.AES_128);
    final SecretKey aSymmetricKey = aKeyGen.generateKey ();

    // Main sign and/or encrypt
    final Document aEncryptedDoc = aBuilder.build (aCryptoFactory.getCrypto (), aSymmetricKey);

    // The attachment callback handler contains the encrypted attachments
    // Important: read the attachment stream only once!
    ICommonsList <WSS4JAttachment> aEncryptedAttachments = null;
    if (aAttachmentCallbackHandler != null)
    {
      aEncryptedAttachments = aAttachmentCallbackHandler.getAllResponseAttachments ();
      // MIME Type and CTE must be set for encrypted attachments!
      for (final WSS4JAttachment aAttachment : aEncryptedAttachments)
      {
        aAttachment.overwriteMimeType (CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
        aAttachment.setContentTransferEncoding (EContentTransferEncoding.BINARY);
      }
    }

    // Use the encrypted attachments!
    try
    {
      return MimeMessageCreator.generateMimeMessage (eSoapVersion, aEncryptedDoc, aEncryptedAttachments);
    }
    catch (final MessagingException ex)
    {
      throw new WSSecurityException (ErrorCode.FAILURE, ex, "Failed to generate MIME message");
    }
  }

  @Nonnull
  public static AS4MimeMessage encryptMimeMessage (@Nonnull final ESoapVersion eSoapVersion,
                                                   @Nonnull final Document aDoc,
                                                   @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                   @Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                   final boolean bMustUnderstand,
                                                   @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                   @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aCryptParams, "CryptParams");

    if (AS4Configuration.isWSS4JSynchronizedSecurity ())
    {
      // Synchronize
      return WSSSynchronizer.call ( () -> _encryptMimeMessage (eSoapVersion,
                                                               aDoc,
                                                               aAttachments,
                                                               aCryptoFactory,
                                                               bMustUnderstand,
                                                               aResHelper,
                                                               aCryptParams));
    }

    // Ensure WSSConfig is initialized
    WSSConfigManager.getInstance ();

    return _encryptMimeMessage (eSoapVersion,
                                aDoc,
                                aAttachments,
                                aCryptoFactory,
                                bMustUnderstand,
                                aResHelper,
                                aCryptParams);
  }
}
