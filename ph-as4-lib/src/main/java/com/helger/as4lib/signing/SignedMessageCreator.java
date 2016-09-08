package com.helger.as4lib.signing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.AttachmentCallbackHandler;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
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
  private WSSecSignature _getBasicBuilder ()
  {
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (AS4CryptoFactory.getKeyAlias (), AS4CryptoFactory.getKeyPassword ());
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm (CAS4.SIGNATURE_ALGORITHM_RSA_SHA256);
    // PMode indicates the DigestAlgorithm as Hash Function
    aBuilder.setDigestAlgo (WSS4JConstants.SHA256);
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
                                       @Nullable final Iterable <? extends IAS4Attachment> aAttachments,
                                       final boolean bMustUnderstand) throws WSSecurityException
  {
    final WSSecSignature aBuilder = _getBasicBuilder ();

    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      // Modify builder for attachments
      aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
      // XXX where is this ID used????
      aBuilder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

      // Convert to WSS4J attachments
      final ICommonsList <Attachment> aWSS4JAttachments = new CommonsArrayList<> (aAttachments,
                                                                                  IAS4Attachment::getAsWSS4JAttachment);

      final AttachmentCallbackHandler aAttachmentCallbackHandler = new AttachmentCallbackHandler (aWSS4JAttachments);
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
