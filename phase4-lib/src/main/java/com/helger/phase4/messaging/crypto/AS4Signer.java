/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Message singing helper.
 *
 * @author Philip Helger
 */
public final class AS4Signer
{
  private AS4Signer ()
  {}

  /**
   * This method must be used if the message does not contain attachments, that
   * should be in a additional mime message part.
   *
   * @param aCryptoFactory
   *        CryptoFactory to use. May not be <code>null</code>.
   * @param aPreSigningMessage
   *        SOAP Document before signing
   * @param eSoapVersion
   *        SOAP version to use
   * @param sMessagingID
   *        The ID of the "Messaging" element to sign.
   * @param aAttachments
   *        Optional list of attachments
   * @param aResHelper
   *        Resource helper to be used.
   * @param bMustUnderstand
   *        Must understand?
   * @param aSigningParams
   *        Signing parameters. May not be <code>null</code>.
   * @return The created signed SOAP document
   * @throws WSSecurityException
   *         If an error occurs during signing
   */
  @Nonnull
  public static Document createSignedMessage (@Nonnull final IAS4CryptoFactory aCryptoFactory,
                                              @Nonnull final Document aPreSigningMessage,
                                              @Nonnull final ESoapVersion eSoapVersion,
                                              @Nonnull @Nonempty final String sMessagingID,
                                              @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                              @Nonnull @WillNotClose final AS4ResourceHelper aResHelper,
                                              final boolean bMustUnderstand,
                                              @Nonnull final AS4SigningParams aSigningParams) throws WSSecurityException
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aPreSigningMessage, "PreSigningMessage");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notEmpty (sMessagingID, "MessagingID");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aSigningParams, "SigningParams");

    // Start signing the document
    final WSSecHeader aSecHeader = new WSSecHeader (aPreSigningMessage);
    aSecHeader.insertSecurityHeader ();

    final WSSecSignature aBuilder = new WSSecSignature (aSecHeader);
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    // Set keystore alias and key password
    aBuilder.setUserInfo (aCryptoFactory.getKeyAlias (), aCryptoFactory.getKeyPassword ());
    aBuilder.setSignatureAlgorithm (aSigningParams.getAlgorithmSign ().getAlgorithmURI ());
    // PMode indicates the DigestAlgorithm as Hash Function
    aBuilder.setDigestAlgo (aSigningParams.getAlgorithmSignDigest ().getAlgorithmURI ());

    // Sign the messaging element itself
    aBuilder.getParts ().add (new WSEncryptionPart (sMessagingID, "Content"));

    // Sign the SOAP body
    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSoapVersion.getNamespaceURI (), "Content"));

    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      // Modify builder for attachments

      // "cid:Attachments" is a predefined ID used inside WSSecSignatureBase
      aBuilder.getParts ().add (new WSEncryptionPart (MessageHelperMethods.PREFIX_CID + "Attachments", "Content"));

      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                            aResHelper);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    // Set the mustUnderstand header of the wsse:Security element as well
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSoapVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSoapVersion.getMustUnderstandValue (bMustUnderstand));

    return aBuilder.build (aCryptoFactory.getCrypto ());
  }
}
