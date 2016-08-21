package com.helger.as4server.message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.attachment.AttachmentCallbackHandler;
import com.helger.as4lib.attachment.IAS4Attachment;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

public class CreateSignedMessage
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("crypto.properties").build ();

  static
  {
    WSSConfig.init ();
  }

  private final Crypto m_aCrypto;

  public CreateSignedMessage () throws WSSecurityException
  {
    // Uses crypto.properties => needs exact name crypto.properties
    m_aCrypto = CryptoFactory.getInstance ();
  }

  @Nonnull
  private WSSecSignature _getBasicBuilder ()
  {
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.alias"),
                          CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.password"));
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm (MessageHelperMethods.SIGNATURE_ALGORITHM_RSA_SHA256);
    // TODO DONT FORGET: PMode indicates the DigestAlgorithm as Hash Function
    aBuilder.setDigestAlgo (MessageHelperMethods.DIGEST_ALGORITHM_SHA256);
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
