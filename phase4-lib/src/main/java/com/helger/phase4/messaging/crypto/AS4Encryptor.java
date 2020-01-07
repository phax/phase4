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
package com.helger.phase4.messaging.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.mail.MessagingException;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.crypto.AS4CryptParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.messaging.mime.MimeMessageCreator;
import com.helger.phase4.soap.ESOAPVersion;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Encryption helper
 *
 * @author Philip Helger
 */
public final class AS4Encryptor
{
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
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
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
  public static Document encryptSoapBodyPayload (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                 @Nonnull final ESOAPVersion eSOAPVersion,
                                                 @Nonnull final Document aDoc,
                                                 final boolean bMustUnderstand,
                                                 @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notNull (aCryptParams, "CryptParams");

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = _createEncrypt (aSecHeader, aCryptParams);

    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));
    return aBuilder.build (aCryptoFactory.getCrypto ());
  }

  @Nonnull
  public static AS4MimeMessage encryptMimeMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                                   @Nonnull final Document aDoc,
                                                   @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                                   @Nonnull final IAS4CryptoFactory aCryptoFactory,
                                                   final boolean bMustUnderstand,
                                                   @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                                   @Nonnull final AS4CryptParams aCryptParams) throws WSSecurityException,
                                                                                               MessagingException
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aCryptParams, "CryptParams");

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = _createEncrypt (aSecHeader, aCryptParams);

    // "cid:Attachments" is a predefined constant
    aBuilder.getParts ().add (new WSEncryptionPart (MessageHelperMethods.PREFIX_CID + "Attachments", "Content"));

    WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = null;
    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments, aResHelper);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));

    // Main sign and/or encrypt
    final Document aEncryptedDoc = aBuilder.build (aCryptoFactory.getCrypto ());

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
    return MimeMessageCreator.generateMimeMessage (eSOAPVersion, aEncryptedDoc, aEncryptedAttachments);
  }
}
