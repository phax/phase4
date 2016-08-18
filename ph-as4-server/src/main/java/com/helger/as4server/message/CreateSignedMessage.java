package com.helger.as4server.message;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4server.client.AttachmentCallbackHandler;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

public class CreateSignedMessage
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("crypto.properties").build ();
  private Crypto aCrypto;

  /**
   * This method must be used if the message does not contain attachments, that
   * should be in a additional mime message part.
   *
   * @param aDocument
   * @param eSOAPVersion
   * @return
   * @throws WSSecurityException
   */
  public Document createSignedMessage (@Nonnull final Document aDocument,
                                       @Nonnull final ESOAPVersion eSOAPVersion) throws WSSecurityException
  {
    WSSConfig.init ();
    // Uses crypto.properties => needs exact name crypto.properties
    aCrypto = CryptoFactory.getInstance ();
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.alias"),
                          CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.password"));
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo ("http://www.w3.org/2001/04/xmlenc#sha256");
    final Document aDoc = aDocument;
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    // TODO if you set attribute with NS it adds the same namespace again since
    // it does not take the one from envelope => Change NS between S11 and S12
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    // TODO Needs to be set to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    // For SOAP 1.2 ist must be "true" or "false"!
    aMustUnderstand.setValue ("0");

    final Document aSignedDoc = aBuilder.build (aDoc, aCrypto, aSecHeader);
    return aSignedDoc;
  }

  public Document getMessageWithAttachmentsAsString (final Document aPreSigningMessage,
                                                     @Nonnull final ESOAPVersion eSOAPVersion,
                                                     @Nonnull final ICommonsList <String> aAttachmentList) throws Exception
  {
    WSSConfig.init ();
    aCrypto = CryptoFactory.getInstance ();

    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (CreateSignedMessage.CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.alias"),
                          CreateSignedMessage.CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.password"));
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo ("http://www.w3.org/2001/04/xmlenc#sha256");

    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
    aBuilder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

    final ICommonsList <Attachment> aAttachments = new CommonsArrayList<> ();

    for (final String sAttachment : aAttachmentList)
    {
      final String sAttachmentId = sAttachment;
      final Attachment aAttachment = new Attachment ();
      aAttachment.setMimeType ("application/gzip");
      aAttachment.addHeaders (_getHeaders (sAttachmentId));
      aAttachment.setId (sAttachmentId);
      aAttachment.setSourceStream (new FileInputStream ("data/test.xml.gz"));

      aAttachments.add (aAttachment);
    }

    final AttachmentCallbackHandler aAttachmentCallbackHandler = new AttachmentCallbackHandler (aAttachments);
    aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);

    final Document aDoc = aPreSigningMessage;
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    // TODO Needs to be set to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    // For SOAP 1.2 it must be "true" or "false"!
    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
      aMustUnderstand.setValue ("0");
    else
      aMustUnderstand.setValue ("false");

    final Document aSignedDoc = aBuilder.build (aDoc, aCrypto, aSecHeader);

    return aSignedDoc;
  }

  /**
   * Sets the MIME - headers for each Attachment
   *
   * @param aAttachmentId
   * @return
   */
  private Map <String, String> _getHeaders (final String aAttachmentId)
  {
    final Map <String, String> aHeaderList = new HashMap<> ();
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + aAttachmentId + ">");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, "application/gzip");
    return aHeaderList;
  }

}
