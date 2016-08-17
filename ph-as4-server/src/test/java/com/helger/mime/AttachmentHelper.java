package com.helger.mime;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4server.client.AttachmentCallbackHandler;
import com.helger.as4server.client.SOAPClientSAAJ;

public class AttachmentHelper
{
  private static Crypto aCrypto = null;

  public static Document getMessageWithAttachmentsAsString (final Document aPreSigningMessage) throws Exception
  {
    WSSConfig.init ();
    aCrypto = CryptoFactory.getInstance ();

    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (SOAPClientSAAJ.CF.getAsString ("key.alias"), SOAPClientSAAJ.CF.getAsString ("key.password"));

    aBuilder.getParts ().add (new WSEncryptionPart ("Body", "http://schemas.xmlsoap.org/soap/envelope/", "Content"));
    aBuilder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

    final String sAttachmentId = UUID.randomUUID ().toString ();
    final Attachment aAttachment = new Attachment ();
    aAttachment.setMimeType ("application/gzip");
    aAttachment.addHeaders (_getHeaders (sAttachmentId));
    aAttachment.setId (sAttachmentId);
    aAttachment.setSourceStream (new FileInputStream ("data/test.xml.gz"));

    final AttachmentCallbackHandler aAttachmentCallbackHandler = new AttachmentCallbackHandler (Collections.singletonList (aAttachment));
    aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);

    final Document aDoc = aPreSigningMessage;

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (
                                                                                     "http://schemas.xmlsoap.org/soap/envelope/",
                                                                                     "mustUnderstand");
    // TODO Needs to be set to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    // For SOAP 1.2 ist must be "true" or "false"!
    aMustUnderstand.setValue ("0");

    final Document aSignedDoc = aBuilder.build (aDoc, aCrypto, aSecHeader);

    return aSignedDoc;
  }

  private static Map <String, String> _getHeaders (final String aAttachmentId)
  {
    final Map <String, String> aHeaderList = new HashMap<> ();
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + aAttachmentId + ">");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    aHeaderList.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, "application/gzip");
    aHeaderList.put ("TestHeader", "testHeaderValue");
    return aHeaderList;
  }
}
