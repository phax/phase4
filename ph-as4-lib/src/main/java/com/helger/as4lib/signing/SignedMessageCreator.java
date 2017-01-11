/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.signing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.WSS4JAttachment;
import com.helger.as4lib.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.ICommonsList;

public class SignedMessageCreator
{
  private final Crypto m_aCrypto;

  public SignedMessageCreator ()
  {
    this (AS4CryptoFactory.createCrypto ());
  }

  public SignedMessageCreator (@Nonnull final Crypto aCrypto)
  {
    m_aCrypto = ValueEnforcer.notNull (aCrypto, "Crypto");
  }

  @Nonnull
  private WSSecSignature _getBasicBuilder (@Nonnull final ECryptoAlgorithmSign eECryptoAlgorithmSign,
                                           @Nonnull final ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest)
  {
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (AS4CryptoFactory.getKeyAlias (), AS4CryptoFactory.getKeyPassword ());
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm (eECryptoAlgorithmSign.getAlgorithmURI ());
    // PMode indicates the DigestAlgorithm as Hash Function
    aBuilder.setDigestAlgo (eECryptoAlgorithmSignDigest.getAlgorithmURI ());
    return aBuilder;
  }

  /**
   * This method must be used if the message does not contain attachments, that
   * should be in a additional mime message part.
   *
   * @param aPreSigningMessage
   *        SOAP Document before signing
   * @param eSOAPVersion
   *        SOAP version to use
   * @param aAttachments
   *        Optional list of attachments
   * @param bMustUnderstand
   *        Must understand
   * @return The created signed SOAP document
   * @throws WSSecurityException
   *         If an error occurs during signing
   */
  @Nonnull
  public Document createSignedMessage (@Nonnull final Document aPreSigningMessage,
                                       @Nonnull final ESOAPVersion eSOAPVersion,
                                       @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                       @Nonnull final AS4ResourceManager aResMgr,
                                       final boolean bMustUnderstand,
                                       @Nonnull final ECryptoAlgorithmSign eECryptoAlgorithmSign,
                                       @Nonnull final ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest) throws WSSecurityException
  {
    final WSSecSignature aBuilder = _getBasicBuilder (eECryptoAlgorithmSign, eECryptoAlgorithmSignDigest);

    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      // Modify builder for attachments
      aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
      // XXX where is this ID used????
      aBuilder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

      final WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments,
                                                                                                            aResMgr);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    // Start signing the document
    final WSSecHeader aSecHeader = new WSSecHeader (aPreSigningMessage);
    aSecHeader.insertSecurityHeader ();
    // Set the mustUnderstand header of the wsse:Security element as well
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));

    return aBuilder.build (aPreSigningMessage, m_aCrypto, aSecHeader);
  }
}
