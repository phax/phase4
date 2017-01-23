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
package com.helger.as4.messaging.encrypt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;

public class EncryptionCreator
{
  private final Crypto m_aCrypto;
  private final AS4CryptoFactory m_aAS4CryptoFactory;
  private final CryptoProperties cryptoProperties;

  public EncryptionCreator ()
  {
    this (null);
  }

  public EncryptionCreator (@Nullable final String sCryptoProperties)
  {
    m_aAS4CryptoFactory = new AS4CryptoFactory (sCryptoProperties);
    m_aCrypto = m_aAS4CryptoFactory.getCrypto ();
    cryptoProperties = m_aAS4CryptoFactory.getCryptoProperties ();
  }

  @Nonnull
  public Document encryptSoapBodyPayload (@Nonnull final ESOAPVersion eSOAPVersion,
                                          @Nonnull final Document aDoc,
                                          final boolean bMustUnderstand) throws Exception
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");

    final WSSecEncrypt aBuilder = new WSSecEncrypt ();
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSymmetricEncAlgorithm (WSS4JConstants.AES_128_GCM);
    aBuilder.setUserInfo (cryptoProperties.getKeyAlias (), cryptoProperties.getKeyPassword ());

    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));
    return aBuilder.build (aDoc, m_aCrypto, aSecHeader);
  }

  @Nonnull
  public MimeMessage encryptMimeMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                         @Nonnull final Document aDoc,
                                         final boolean bMustUnderstand,
                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                         @Nonnull final AS4ResourceManager aResMgr) throws Exception
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");

    final WSSecEncrypt aBuilder = new WSSecEncrypt ();
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (WSS4JConstants.AES_128_GCM);
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (cryptoProperties.getKeyAlias (), cryptoProperties.getKeyPassword ());

    aBuilder.getParts ().add (new WSEncryptionPart (CreateUserMessage.PREFIX_CID + "Attachments", "Content"));

    WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = null;
    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments, aResMgr);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));

    // Main sign and/or encrypt
    final Document aEncryptedDoc = aBuilder.build (aDoc, m_aCrypto, aSecHeader);

    // The attachment callback handler contains the encrypted attachments
    // Important: read the attachment stream only once!
    ICommonsList <WSS4JAttachment> aEncryptedAttachments = null;
    if (aAttachmentCallbackHandler != null)
    {
      aEncryptedAttachments = aAttachmentCallbackHandler.getAllResponseAttachments ();
      // MIME Type and CTE must be set for encrypted attachments!
      aEncryptedAttachments.forEach (x -> {
        x.overwriteMimeType (CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
        x.setContentTransferEncoding (EContentTransferEncoding.BINARY);
      });
    }

    // Use the encrypted attachments!
    return new MimeMessageCreator (eSOAPVersion).generateMimeMessage (aEncryptedDoc, aEncryptedAttachments);
  }
}
