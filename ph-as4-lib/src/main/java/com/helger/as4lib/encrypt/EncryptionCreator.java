package com.helger.as4lib.encrypt;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.AttachmentCallbackHandler;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.crypto.AS4CryptoFactory;
import com.helger.as4lib.mime.MimeMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

public class EncryptionCreator
{
  private final Crypto m_aCrypto;

  public EncryptionCreator ()
  {
    this (AS4CryptoFactory.createCrypto ());
  }

  public EncryptionCreator (@Nonnull final Crypto aCrypto)
  {
    m_aCrypto = ValueEnforcer.notNull (aCrypto, "Crypto");
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
    aBuilder.setUserInfo (AS4CryptoFactory.getKeyAlias (), AS4CryptoFactory.getKeyPassword ());

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
                                         @Nonnull @Nonempty final Iterable <? extends IAS4Attachment> aAttachments) throws Exception
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notEmpty (aAttachments, "Attachments");

    final WSSecEncrypt aBuilder = new WSSecEncrypt ();
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (WSS4JConstants.AES_128_GCM);
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (AS4CryptoFactory.getKeyAlias (), AS4CryptoFactory.getKeyPassword ());

    aBuilder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

    // Convert to WSS4J attachments
    final ICommonsList <Attachment> aWSS4JAttachments = new CommonsArrayList<> (aAttachments,
                                                                                IAS4Attachment::getAsWSS4JAttachment);

    final AttachmentCallbackHandler aAttachmentCallbackHandler = new AttachmentCallbackHandler (aWSS4JAttachments);
    aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));

    // Main sign and/or encrypt
    final Document aDocTest = aBuilder.build (aDoc, m_aCrypto, aSecHeader);

    // The attachment callback handler contains the encrypted attachments
    // Important: read the attachment stream only once!
    final ICommonsList <Attachment> aEncryptedAttachments = aAttachmentCallbackHandler.getResponseAttachments ();

    // Use the encrypted attachments!
    return new MimeMessageCreator (eSOAPVersion).generateMimeMessage (aDocTest, null, aEncryptedAttachments);
  }
}
